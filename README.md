# PacMan-AI

This is my first AI project i worked on , build from scratch. The pacman run by alpha-beta pruning algorithm.

Actually "Phan Vu Hong Hai" is the guy do all stub. I just bothering him :) 

That why now he on Canada, work with google on fighting the robot invasion , me is doing linear regression for predict disk full. 

## Extension (2026)

Classic arcade-style ghosts, plus three new Pacman AIs.

**Ghosts** now behave like the arcade: each one picks a target tile and has its own personality and color.
- Red, Chaser: targets Pacman.
- Purple, Ambusher: targets 4 tiles ahead of Pacman.
- Green, Flanker: targets the point mirrored from the red ghost.
- Orange, Shy: chases Pacman but runs away when within 8 tiles.

They alternate between scatter and chase, leave the house one by one, and wander randomly when scared.

### Results

Each Pacman plays against the 4 classic ghosts with 1 life:

| Pacman | Win rate | ms / move |
|---|---|---|
| Alpha-beta, depth 3 (original) | 18% | 2.5 |
| Approximate Q-learning | 43% | < 0.01 |
| DQN (PyTorch, CPU) | ~0% (learned to farm ghosts: reward hacking) | ~3 |
| Model-based search + Q-learning value, depth 10 | 81% | 65 |

Reports:
- `PacmanAI_MoRong1_MaCoDien_QLearning.pptx`: classic ghosts and Q-learning.
- `PacmanAI_MoRong2_DQN_TimKiem.pptx`: DQN and model-based search.

### Requirements

- **JDK 11 or newer, with GUI support.** On Ubuntu, `openjdk-*-jdk-headless` cannot open the game window; install `openjdk-17-jdk` instead. `build.sh` (and the DQN scripts) use `$JAVA_HOME` if it is set, otherwise `java`/`javac` from `PATH`. NetBeans is not needed.
- **Python 3 with PyTorch, only for the DQN Pacman.** Install with `pip install -r ProjectAI/dqn/requirements.txt`. The CPU build of torch is enough.

### Quick start

```bash
git clone https://github.com/eveningcafe/PacMan-AI.git
cd PacMan-AI/ProjectAI
./build.sh run              # build, then play yourself with the arrow keys
./build.sh run -search 10   # watch the best AI play
```

### All options

Run these from `ProjectAI/`:

```bash
./build.sh                            # build only -> out/PacmanAI.jar
./build.sh run -albe 3                # alpha-beta Pacman, depth 3
./build.sh run -expect 2              # expectimax Pacman, depth 2
./build.sh run -qlearn                # Q-learning Pacman (weights: qweights.txt)
./build.sh run -search 8              # model-based search Pacman, depth 8
./build.sh run -dqn dqn/model.pt      # DQN Pacman (needs python3 + torch)
./build.sh run -ghost ab              # original alpha-beta ghosts instead of classic ghosts
./build.sh run -numGhost 2            # fewer ghosts
./build.sh run -search 10 -repeat 3   # play 3 games in a row

# Headless (no window): measure win rate over N games
./build.sh run -qlearn -test 1000
./build.sh run -search 10 -test 100

# Training
./build.sh run -qlearn -train 20000              # retrain Q-learning (writes qweights.txt)
python3 dqn/train.py --steps 1000000             # train DQN (writes dqn/model.pt)
python3 dqn/evaluate.py dqn/model.pt --games 500 # DQN win rate
```

If `python3` is not the interpreter that has torch, set `PACMAN_PYTHON=/path/to/python` before running `-dqn`.

### Project layout

```
ProjectAI/
  build.sh                 build + run script (javac/jar only)
  src/projectai/           game + agents (Java)
    ClassicGameRules...    game engine (from the original project)
    ClassicGhostAgent      arcade ghost AI
    QLearningPacmanAgent   approximate Q-learning (9 features)
    ModelSearchPacmanAgent search that simulates the ghosts, Q-learning value at the leaves
    RemotePacmanAgent      bridge to the Python DQN (stdin/stdout)
  qweights.txt             trained Q-learning weights
  dqn/                     DQN in PyTorch (train.py, evaluate.py, policy.py, model.pt)
```
