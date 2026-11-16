"""Evaluate a trained DQN headless: python3 dqn/evaluate.py dqn/model.pt --games 1000"""
import argparse

import numpy as np
import torch

from common import QNet, VecEnv, masked_argmax, to_float

ap = argparse.ArgumentParser()
ap.add_argument("model")
ap.add_argument("--games", type=int, default=1000)
ap.add_argument("--envs", type=int, default=6)
ap.add_argument("--ghost", default="classic")
args = ap.parse_args()

torch.set_num_threads(4)
net = QNet()
net.load_state_dict(torch.load(args.model, map_location="cpu"))
net.eval()
env = VecEnv(args.envs, args.ghost)
states, masks = env.reset()
results = []
while len(results) < args.games:
    with torch.no_grad():
        a = masked_argmax(net(to_float(torch.from_numpy(states))), torch.from_numpy(masks)).numpy()
    states, masks, _, _, finished = env.step(a)
    results.extend(finished)
env.close()
results = results[:args.games]
wins = np.mean([w for _, w in results])
score = np.mean([s for s, _ in results])
print(f"DQN test {len(results)} games vs {args.ghost} ghosts: win rate {100 * wins:.1f}%, avg score {score:.1f}")
