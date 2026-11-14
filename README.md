# PacMan-AI

This is my first AI project i worked on , build from scratch. The pacman run by alpha-beta pruning algorithm.

Actually "Phan Vu Hong Hai" is the guy do all stub. I just bothering him :) 

That why now he on Canada, work with google on fighting the robot invasion , me is doing linear regression for predict disk full. 

## Extension (2026)

Classic arcade-style ghosts, plus three new Pacman AIs. Results against the 4 classic ghosts (1 life):

| Pacman | Win rate | ms / move |
|---|---|---|
| Alpha-beta, depth 3 (original) | 18% | 2.5 |
| Approximate Q-learning | 43% | < 0.01 |
| DQN (PyTorch, CPU) | ~0% (learned to farm ghosts: reward hacking) | ~3 |
| Model-based search + Q-learning value, depth 10 | 81% | 65 |

Reports: `ProjectAI_MoRong.pptx` (classic ghosts, Q-learning), `ProjectAI_MoRong_2.pptx` (DQN, model-based search).

### Run (from `ProjectAI/`, needs a JDK with GUI support)

```bash
./build.sh run                        # play with the arrow keys
./build.sh run -albe 3                # alpha-beta Pacman
./build.sh run -qlearn                # Q-learning Pacman (weights: qweights.txt)
./build.sh run -qlearn -train 20000   # retrain Q-learning headless
./build.sh run -search 8              # model-based search Pacman
./build.sh run -dqn dqn/model.pt      # DQN Pacman (python3 + torch)
./build.sh run -search 10 -test 100   # any AI: headless win rate over N games
./build.sh run -ghost ab              # original alpha-beta ghosts
python3 dqn/train.py --steps 1000000  # train DQN (models are git-ignored)
```
