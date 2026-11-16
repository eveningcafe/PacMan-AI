"""Shared pieces for the DQN Pacman: observation parsing, Java env, network."""
import os
import subprocess

import numpy as np
import torch
import torch.nn as nn

W, H = 17, 11
N_ACTIONS = 4            # 0 up, 1 down, 2 left, 3 right (screen directions)
N_GHOSTS = 4
SCARED_MAX = 40          # Rules.SCARED_TIME
# Channels: 0 wall, 1 dot, 2 capsule, then two frames (now, previous) of
# [pacman, ghost1..4 (dangerous), scared ghosts], then 1 scatter-mode plane.
AGENT_CH = 1 + N_GHOSTS + 1
N_CH = 3 + 2 * AGENT_CH + 1

PROJECT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
JAR = os.path.join(PROJECT, "out", "PacmanAI.jar")


def _find_java():
    """$PACMAN_JAVA, else $JAVA_HOME/bin/java, else ~/.jdks/openjdk-20, else java on PATH."""
    if os.environ.get("PACMAN_JAVA"):
        return os.environ["PACMAN_JAVA"]
    cands = [os.path.expanduser("~/.jdks/openjdk-20/bin/java")]
    if os.environ.get("JAVA_HOME"):
        cands.insert(0, os.path.join(os.environ["JAVA_HOME"], "bin", "java"))
    for c in cands:
        if os.path.isfile(c):
            return c
    return "java"


JAVA = _find_java()

_GRID_LUT = np.zeros((256, 3), dtype=np.uint8)
_GRID_LUT[ord("#"), 0] = 1
_GRID_LUT[ord("."), 1] = 1
_GRID_LUT[ord("o"), 2] = 1


def parse(line):
    """Parse one '@ ...' observation line from RemotePacmanAgent."""
    t = line.split()
    reward, done, win = int(t[1]), t[2] == "1", t[3] == "1"
    mask = np.array([c == "1" for c in t[4]], dtype=bool)
    scatter = t[5] == "1"
    px, py, ng = int(t[6]), int(t[7]), int(t[8])
    ghosts = [(int(t[9 + 3 * i]), int(t[10 + 3 * i]), int(t[11 + 3 * i])) for i in range(ng)]
    grid = t[9 + 3 * ng]
    static = _GRID_LUT[np.frombuffer(grid.encode(), dtype=np.uint8)].reshape(H, W, 3)
    agents = np.zeros((AGENT_CH, H, W), dtype=np.uint8)
    if 0 <= px < W and 0 <= py < H:
        agents[0, py, px] = 1
    for i, (gx, gy, scared) in enumerate(ghosts[:N_GHOSTS]):
        if not (0 <= gx < W and 0 <= gy < H):
            continue
        if scared > 0:
            agents[AGENT_CH - 1, gy, gx] = max(agents[AGENT_CH - 1, gy, gx], scared)
        else:
            agents[1 + i, gy, gx] = 1
    return dict(reward=reward, done=done, win=win, mask=mask,
                static=static.transpose(2, 0, 1), agents=agents, scatter=scatter)


class Stacker:
    """Builds the uint8 state tensor; remembers the previous frame's agents."""

    def __init__(self):
        self.prev = None

    def reset(self):
        self.prev = None

    def __call__(self, ob):
        prev = ob["agents"] if self.prev is None else self.prev
        self.prev = ob["agents"]
        plane = np.full((1, H, W), 1 if ob["scatter"] else 0, dtype=np.uint8)
        return np.concatenate([ob["static"], ob["agents"], prev, plane], axis=0)


def to_float(states):
    """uint8 states (B,C,H,W) -> float tensor; scared timers scaled to [0,1]."""
    x = states.float()
    for base in (3, 3 + AGENT_CH):
        x[:, base + AGENT_CH - 1] /= SCARED_MAX
    return x


class QNet(nn.Module):
    """Small dueling conv net over the 17x11 maze."""

    def __init__(self):
        super().__init__()
        # Sized for CPU training (~20 ms per batch-128 update).
        self.body = nn.Sequential(
            nn.Conv2d(N_CH, 32, 3, padding=1), nn.ReLU(),
            nn.Conv2d(32, 32, 3, padding=1), nn.ReLU(),
            nn.Conv2d(32, 32, 3, padding=1), nn.ReLU(),
            nn.Flatten(),
            nn.Linear(32 * H * W, 256), nn.ReLU())
        self.value = nn.Linear(256, 1)
        self.adv = nn.Linear(256, N_ACTIONS)

    def forward(self, x):
        h = self.body(x)
        a = self.adv(h)
        return self.value(h) + a - a.mean(dim=1, keepdim=True)


def masked_argmax(q, mask):
    q = q.masked_fill(~mask, float("-inf"))
    return q.argmax(dim=1)


class JavaEnv:
    """One headless Java game process ('-serve'); auto-starts the next game."""

    def __init__(self, ghost="classic"):
        self.p = subprocess.Popen(
            [JAVA, "-jar", JAR, "-serve", "-ghost", ghost],
            cwd=PROJECT, stdin=subprocess.PIPE, stdout=subprocess.PIPE,
            stderr=subprocess.DEVNULL, text=True, bufsize=1)

    def read(self):
        while True:
            line = self.p.stdout.readline()
            if not line:
                raise RuntimeError("Java env exited")
            if line.startswith("@"):
                return parse(line)

    def send(self, action):
        self.p.stdin.write(f"{int(action)}\n")
        self.p.stdin.flush()

    def close(self):
        try:
            self.p.stdin.close()
            self.p.wait(timeout=5)
        except Exception:
            self.p.kill()


class VecEnv:
    """N Java processes stepped together. step() auto-resets finished games."""

    def __init__(self, n, ghost="classic"):
        self.envs = [JavaEnv(ghost) for _ in range(n)]
        self.stackers = [Stacker() for _ in range(n)]
        self.score = np.zeros(n)

    def reset(self):
        obs = [e.read() for e in self.envs]
        states = np.stack([s(o) for s, o in zip(self.stackers, obs)])
        masks = np.stack([o["mask"] for o in obs])
        return states, masks

    def step(self, actions):
        for e, a in zip(self.envs, actions):
            e.send(a)
        n = len(self.envs)
        rewards, dones = np.zeros(n), np.zeros(n, dtype=bool)
        self.last_wins = np.zeros(n, dtype=bool)
        states, masks, finished = [], [], []
        for i, e in enumerate(self.envs):
            ob = e.read()
            rewards[i] = ob["reward"]
            self.score[i] += ob["reward"]
            if ob["done"]:
                dones[i] = True
                self.last_wins[i] = ob["win"]
                finished.append((self.score[i], ob["win"]))
                self.score[i] = 0
                self.stackers[i].reset()
                ob = e.read()  # first observation of the next game
            states.append(self.stackers[i](ob))
            masks.append(ob["mask"])
        return np.stack(states), np.stack(masks), rewards, dones, finished

    def close(self):
        for e in self.envs:
            e.close()
