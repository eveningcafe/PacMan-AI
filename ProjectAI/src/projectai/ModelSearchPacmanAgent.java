package projectai;

import java.util.*;

/**
 * Model-based search Pacman: "search + learned value", like a tiny AlphaZero.
 *
 * Classic ghosts follow fixed rules, so Pacman can SIMULATE them exactly
 * (copies of the real ghost agents, including their per-game memory) instead
 * of assuming worst-case opponents the way alpha-beta does. Frightened ghosts
 * turn randomly; the simulation samples one of their choices and the plan is
 * redone every move, so a wrong guess is corrected next turn.
 *
 *   Q_d(s, a) = r(s, a) + gamma * max_a' Q_{d-1}(s', a')   (s' after all ghosts move)
 *   Q_1(s, a) = learned Q-learning value Q(s, a)
 *
 * r is the score change (dots, ghosts, death, win). Depth 1 is exactly the
 * greedy Q-learning Pacman; each extra level puts one round of exact ghost
 * simulation in front of it, so deaths inside the horizon are seen exactly.
 * (The leaf must be Q(s, a), not max_a Q(s', a): the learned weights price
 * the risk of the move itself, e.g. stepping next to a ghost.)
 */
public class ModelSearchPacmanAgent extends Agent {

    private final int depth;
    private final List<Agent> ghosts;          // the real ghost agents (model)
    private final QLearningPacmanAgent value;  // learned leaf evaluation
    private final double gamma = 0.9;
    private final Random rng = new Random();

    public ModelSearchPacmanAgent(int index, int depth, List<Agent> ghosts,
            QLearningPacmanAgent value) {
        super(index);
        this.depth = depth;
        this.ghosts = ghosts;
        this.value = value;
    }

    @Override
    int[] getAction(GameState state) {
        ArrayList<int[]> actions = state.getLegalAction(index);
        List<Agent> model = copies(ghosts);
        double best = Double.NEGATIVE_INFINITY;
        ArrayList<int[]> ties = new ArrayList<>();
        for (int[] a : actions) {
            double v = actionValue(state, a, model, depth);
            if (v > best + 1e-6) {
                best = v;
                ties.clear();
                ties.add(a);
            } else if (Math.abs(v - best) <= 1e-6) {
                ties.add(a);
            }
        }
        return ties.get(rng.nextInt(ties.size()));
    }

    /** Value of playing a now, then the best plan for the remaining depth. */
    private double actionValue(GameState s, int[] a, List<Agent> parentGhosts, int d) {
        if (d <= 1) {
            return value.qValue(s, a);
        }
        List<Agent> gs = copies(parentGhosts);
        GameState next = s.generateSuccessor(0, a);
        for (int i = 0; i < gs.size() && !next.isWin() && !next.isLose(); i++) {
            next = next.generateSuccessor(i + 1, gs.get(i).getAction(next));
        }
        double r = next.getScore() - s.getScore();
        if (next.isWin() || next.isLose()) {
            return r;
        }
        double best = Double.NEGATIVE_INFINITY;
        for (int[] a2 : (ArrayList<int[]>) next.getLegalAction(0)) {
            best = Math.max(best, actionValue(next, a2, gs, d - 1));
        }
        return r + gamma * best;
    }

    private static List<Agent> copies(List<Agent> src) {
        ArrayList<Agent> out = new ArrayList<>(src.size());
        for (Agent g : src) {
            out.add(g.simCopy());
        }
        return out;
    }
}
