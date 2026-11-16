"""Train a Double + Dueling DQN Pacman against the Java game.

    python3 dqn/train.py --steps 2000000 --out dqn/model.pt

Runs N headless Java games in parallel (one process each), stores
transitions in a uint8 replay buffer, and logs win rate as it learns.
"""
import argparse
import collections
import time

import numpy as np
import torch
import torch.nn.functional as F

from common import N_ACTIONS, N_CH, H, W, QNet, VecEnv, masked_argmax, to_float

ap = argparse.ArgumentParser()
ap.add_argument("--steps", type=int, default=2_000_000, help="environment transitions")
ap.add_argument("--envs", type=int, default=8)
ap.add_argument("--update-every", type=int, default=2, help="vector steps per gradient update")
ap.add_argument("--buffer", type=int, default=200_000)
ap.add_argument("--batch", type=int, default=128)
ap.add_argument("--lr", type=float, default=1e-4)
ap.add_argument("--gamma", type=float, default=0.99)
ap.add_argument("--eps-steps", type=int, default=500_000, help="epsilon 1.0 -> 0.05 over this many steps")
ap.add_argument("--learn-start", type=int, default=20_000)
ap.add_argument("--target-every", type=int, default=2_000, help="gradient updates between target syncs")
ap.add_argument("--reward-scale", type=float, default=0.1)
ap.add_argument("--threads", type=int, default=4)
ap.add_argument("--log-every", type=int, default=25_000)
ap.add_argument("--ghost", default="classic")
ap.add_argument("--out", default="dqn/model.pt")
ap.add_argument("--init", default=None, help="start from these weights")
ap.add_argument("--eps-start", type=float, default=1.0)
# Reward shaping. The raw game score made the agent farm ghosts (+200 each)
# instead of winning (+500), so by default ghosts are worth less and a win more.
ap.add_argument("--ghost-reward", type=float, default=50, help="reward for eating a ghost (game: 200)")
ap.add_argument("--win-bonus", type=float, default=1000, help="extra reward on a win (game: +500 already)")
args = ap.parse_args()

torch.set_num_threads(args.threads)
dev = torch.device("cuda" if torch.cuda.is_available() else "cpu")
print(f"device {dev}, envs {args.envs}, steps {args.steps}", flush=True)

net, tgt = QNet().to(dev), QNet().to(dev)
if args.init:
    net.load_state_dict(torch.load(args.init, map_location=dev))
    print(f"init from {args.init}", flush=True)
tgt.load_state_dict(net.state_dict())
opt = torch.optim.Adam(net.parameters(), lr=args.lr)

# Replay buffer (uint8 states).
B = args.buffer
S = np.zeros((B, N_CH, H, W), dtype=np.uint8)
S2 = np.zeros((B, N_CH, H, W), dtype=np.uint8)
A = np.zeros(B, dtype=np.int64)
R = np.zeros(B, dtype=np.float32)
D = np.zeros(B, dtype=np.float32)
M2 = np.zeros((B, N_ACTIONS), dtype=bool)
ptr, size = 0, 0

env = VecEnv(args.envs, args.ghost)
states, masks = env.reset()
recent = collections.deque(maxlen=500)   # (score, win) of finished games
best_win, updates, steps, games = -1.0, 0, 0, 0
t0 = time.time()
next_log = args.log_every

while steps < args.steps:
    eps = max(0.05, args.eps_start - (args.eps_start - 0.05) * steps / args.eps_steps)
    with torch.no_grad():
        q = net(to_float(torch.from_numpy(states).to(dev)))
        greedy = masked_argmax(q, torch.from_numpy(masks).to(dev)).cpu().numpy()
    actions = greedy.copy()
    for i in range(len(actions)):
        if np.random.rand() < eps:
            actions[i] = np.random.choice(np.flatnonzero(masks[i]))

    nstates, nmasks, rewards, dones, finished = env.step(actions)
    shaped = rewards.copy()
    shaped[rewards >= 150] -= 200 - args.ghost_reward       # ghost eaten
    shaped[env.last_wins] += args.win_bonus
    for i in range(len(actions)):
        S[ptr], A[ptr], R[ptr] = states[i], actions[i], shaped[i] * args.reward_scale
        S2[ptr], D[ptr], M2[ptr] = nstates[i], float(dones[i]), nmasks[i]
        ptr = (ptr + 1) % B
        size = min(size + 1, B)
    for sc, win in finished:
        recent.append((sc, win))
        games += 1
    states, masks = nstates, nmasks
    steps += len(actions)

    if size >= args.learn_start and (steps // len(actions)) % args.update_every == 0:
        idx = np.random.randint(0, size, args.batch)
        s = to_float(torch.from_numpy(S[idx]).to(dev))
        s2 = to_float(torch.from_numpy(S2[idx]).to(dev))
        a = torch.from_numpy(A[idx]).to(dev)
        r = torch.from_numpy(R[idx]).to(dev)
        d = torch.from_numpy(D[idx]).to(dev)
        m2 = torch.from_numpy(M2[idx]).to(dev)
        with torch.no_grad():
            # Double DQN: online net picks a', target net evaluates it.
            a2 = masked_argmax(net(s2), m2 | (d.bool().unsqueeze(1)))
            q2 = tgt(s2).gather(1, a2.unsqueeze(1)).squeeze(1)
            target = r + args.gamma * (1 - d) * q2
        qsa = net(s).gather(1, a.unsqueeze(1)).squeeze(1)
        loss = F.smooth_l1_loss(qsa, target)
        opt.zero_grad()
        loss.backward()
        torch.nn.utils.clip_grad_norm_(net.parameters(), 10)
        opt.step()
        updates += 1
        if updates % args.target_every == 0:
            tgt.load_state_dict(net.state_dict())

    if steps >= next_log:
        next_log += args.log_every
        win = np.mean([w for _, w in recent]) if recent else 0.0
        score = np.mean([s for s, _ in recent]) if recent else 0.0
        rate = steps / (time.time() - t0)
        print(f"[dqn] steps {steps:>9,}  games {games:>6}  eps {eps:.2f}  "
              f"win(last {len(recent)}) {100 * win:5.1f}%  score {score:7.1f}  "
              f"{rate:5.0f} steps/s  {(time.time() - t0) / 60:5.1f} min", flush=True)
        torch.save(net.state_dict(), args.out.replace(".pt", "_last.pt"))
        if len(recent) >= 200 and win > best_win and eps <= 0.1:
            best_win = win
            torch.save(net.state_dict(), args.out)
            print(f"      saved best -> {args.out} (train win {100 * win:.1f}%)", flush=True)

if best_win < 0:
    torch.save(net.state_dict(), args.out)
env.close()
print("done", flush=True)
