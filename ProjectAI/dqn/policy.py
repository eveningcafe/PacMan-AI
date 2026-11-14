"""Policy process for the Java GUI ('-dqn model.pt'): obs lines in, action lines out."""
import sys

import torch

from common import QNet, Stacker, masked_argmax, parse, to_float

torch.set_num_threads(1)
net = QNet()
net.load_state_dict(torch.load(sys.argv[1], map_location="cpu"))
net.eval()
stack = Stacker()

for line in sys.stdin:
    if not line.startswith("@"):
        continue
    ob = parse(line)
    if ob["done"]:
        stack.reset()
        continue
    s = torch.from_numpy(stack(ob)).unsqueeze(0)
    with torch.no_grad():
        a = masked_argmax(net(to_float(s)), torch.from_numpy(ob["mask"]).unsqueeze(0)).item()
    sys.stdout.write(f"{a}\n")
    sys.stdout.flush()
