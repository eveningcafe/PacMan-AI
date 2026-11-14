package projectai;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

/**
 * Approximate Q-learning Pacman.
 *
 *   Q(s, a) = sum_i w_i * f_i(s, a)
 *
 * After every move: diff = [r + gamma * max_a' Q(s', a')] - Q(s, a)
 *                   w_i += alpha * diff * f_i(s, a)
 *
 * The reward r is the change in game score between two Pacman decisions
 * (-1 per move, +10 dot, +200 ghost eaten, -500 death, +500 clear board).
 * Exploration is epsilon-greedy. Distances are real maze distances (BFS),
 * precomputed once because the maze never changes.
 */
public class QLearningPacmanAgent extends Agent {

    static final String[] FEATURES = {
        "bias",
        "ghost-1-step",          // dangerous ghosts that can reach the next tile now
        "ghost-2-steps",         // dangerous ghosts two steps from the next tile
        "eats-food",             // next tile has a dot and is safe
        "closest-food",          // maze distance to nearest dot (normalized)
        "eats-capsule",          // next tile has a capsule and is safe
        "capsule-when-chased",   // closeness to a capsule while a ghost is near
        "eats-scared-ghost",     // a frightened ghost is on/next to the next tile
        "closest-scared-ghost",  // closeness to the nearest catchable scared ghost
    };
    static final int N = FEATURES.length;

    double[] weights = new double[N];
    double alpha = 0.02;    // learning rate
    double gamma = 0.9;     // discount
    double epsilon = 0.0;   // exploration rate
    boolean learning = false;

    private final Random rng = new Random();
    private GameState lastState;
    private double[] lastFeatures;

    // Maze distance tables (built lazily from the first state seen).
    private int W, H;
    private int[][] dist;          // dist[cellA][cellB], -1 = unreachable
    private double maxDist = 1;

    public QLearningPacmanAgent(int index) {
        super(index);
    }

    // ------------------------------------------------------------ acting

    @Override
    int[] getAction(GameState state) {
        ensureDistances(state);
        ArrayList<int[]> actions = state.getLegalAction(index);
        if (actions == null || actions.isEmpty()) {
            return Directions.STOP;
        }

        if (learning && lastState != null) {
            double reward = state.getScore() - lastState.getScore();
            update(reward, maxQ(state, actions));
        }

        int[] chosen;
        if (learning && rng.nextDouble() < epsilon) {
            chosen = actions.get(rng.nextInt(actions.size()));
        } else {
            chosen = bestAction(state, actions);
        }
        lastState = state;
        lastFeatures = features(state, chosen);
        return chosen;
    }

    @Override
    void gameOver(GameState finalState) {
        if (learning && lastState != null) {
            double reward = finalState.getScore() - lastState.getScore();
            update(reward, 0); // terminal: no future value
        }
        lastState = null;
        lastFeatures = null;
    }

    private void update(double reward, double nextValue) {
        double diff = reward + gamma * nextValue - dot(weights, lastFeatures);
        for (int i = 0; i < N; i++) {
            weights[i] += alpha * diff * lastFeatures[i];
        }
    }

    private int[] bestAction(GameState state, List<int[]> actions) {
        double best = Double.NEGATIVE_INFINITY;
        ArrayList<int[]> ties = new ArrayList<>();
        for (int[] a : actions) {
            double q = dot(weights, features(state, a));
            if (q > best + 1e-9) {
                best = q;
                ties.clear();
                ties.add(a);
            } else if (Math.abs(q - best) <= 1e-9) {
                ties.add(a);
            }
        }
        return ties.get(rng.nextInt(ties.size()));
    }

    private double maxQ(GameState state, List<int[]> actions) {
        double best = Double.NEGATIVE_INFINITY;
        for (int[] a : actions) {
            best = Math.max(best, dot(weights, features(state, a)));
        }
        return best;
    }

    private static double dot(double[] w, double[] f) {
        double s = 0;
        for (int i = 0; i < w.length; i++) {
            s += w[i] * f[i];
        }
        return s;
    }

    // ------------------------------------------------------------ features

    double[] features(GameState state, int[] action) {
        double[] f = new double[N];
        GameStateData data = state.gameStateData;
        Position pac = state.getPacmanPosition();
        int nx = Math.round(pac.x) + action[0];
        int ny = Math.round(pac.y) + action[1];
        int next = cell(nx, ny);
        int here = cell(Math.round(pac.x), Math.round(pac.y));

        f[0] = 1.0;

        // Ghosts: split into dangerous and catchable (frightened).
        int danger1 = 0, danger2 = 0;
        boolean dangerNearPacman = false;
        double nearestScared = Double.MAX_VALUE;
        boolean eatsScared = false;
        for (AgentState g : state.getGhostStates()) {
            int d = ghostDistance(next, g.getPosition());
            int dHere = ghostDistance(here, g.getPosition());
            // A ghost whose fright ends before Pacman can reach it is dangerous.
            boolean scared = g.scaredTimer > Math.max(2, d);
            if (!scared) {
                if (d <= 1) {
                    danger1++;
                } else if (d == 2) {
                    danger2++;
                }
                if (dHere >= 0 && dHere <= 4) {
                    dangerNearPacman = true;
                }
            } else if (d < 999) {
                if (d <= 1) {
                    eatsScared = true;
                }
                nearestScared = Math.min(nearestScared, d);
            }
        }
        f[1] = danger1;
        f[2] = danger2;

        Position nextPos = new Position(nx, ny);
        if (danger1 == 0 && data.food.contains(nextPos)) {
            f[3] = 1.0;
        }
        int foodD = nearest(next, data.food);
        if (foodD >= 0) {
            f[4] = foodD / maxDist;
        }
        if (danger1 == 0 && data.capsules.contains(nextPos)) {
            f[5] = 1.0;
        }
        if (dangerNearPacman) {
            int capD = nearest(next, data.capsules);
            if (capD >= 0) {
                f[6] = 1.0 - capD / maxDist;
            }
        }
        if (eatsScared && danger1 == 0) {
            f[7] = 1.0;
        }
        if (nearestScared != Double.MAX_VALUE) {
            f[8] = 1.0 - nearestScared / maxDist;
        }
        return f;
    }

    /** Distance from a cell to a ghost that may sit between two tiles. */
    private int ghostDistance(int from, Position g) {
        int best = Integer.MAX_VALUE;
        int[] xs = {(int) Math.floor(g.x), (int) Math.ceil(g.x)};
        int[] ys = {(int) Math.floor(g.y), (int) Math.ceil(g.y)};
        for (int x : xs) {
            for (int y : ys) {
                int c = cell(x, y);
                if (c >= 0 && dist[from][c] >= 0) {
                    best = Math.min(best, dist[from][c]);
                }
            }
        }
        return best == Integer.MAX_VALUE ? 999 : best;
    }

    private int nearest(int from, List<Position> targets) {
        int best = -1;
        for (Position p : targets) {
            int c = cell(Math.round(p.x), Math.round(p.y));
            if (c >= 0 && dist[from][c] >= 0 && (best < 0 || dist[from][c] < best)) {
                best = dist[from][c];
            }
        }
        return best;
    }

    private int cell(int x, int y) {
        if (x < 0 || y < 0 || x >= W || y >= H) {
            return -1;
        }
        return y * W + x;
    }

    private void ensureDistances(GameState state) {
        if (dist != null) {
            return;
        }
        W = Layout.WIDTH;
        H = Layout.HEIGHT;
        boolean[] wall = new boolean[W * H];
        for (Position p : state.gameStateData.layout.walls) {
            int c = cell(Math.round(p.x), Math.round(p.y));
            if (c >= 0) {
                wall[c] = true;
            }
        }
        dist = new int[W * H][W * H];
        int[][] dirs = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
        for (int s = 0; s < W * H; s++) {
            Arrays.fill(dist[s], -1);
            if (wall[s]) {
                continue;
            }
            ArrayDeque<Integer> q = new ArrayDeque<>();
            dist[s][s] = 0;
            q.add(s);
            while (!q.isEmpty()) {
                int c = q.poll();
                int cx = c % W, cy = c / W;
                for (int[] d : dirs) {
                    int n = cell(cx + d[0], cy + d[1]);
                    if (n >= 0 && !wall[n] && dist[s][n] < 0) {
                        dist[s][n] = dist[s][c] + 1;
                        maxDist = Math.max(maxDist, dist[s][n]);
                        q.add(n);
                    }
                }
            }
        }
    }

    // ------------------------------------------------------------ weights I/O

    void save(String path) throws IOException {
        StringBuilder sb = new StringBuilder("# Approximate Q-learning weights\n");
        for (int i = 0; i < N; i++) {
            sb.append(String.format(Locale.ROOT, "%s %.6f%n", FEATURES[i], weights[i]));
        }
        Files.write(Paths.get(path), sb.toString().getBytes(StandardCharsets.UTF_8));
    }

    void load(String path) throws IOException {
        Map<String, Integer> idx = new HashMap<>();
        for (int i = 0; i < N; i++) {
            idx.put(FEATURES[i], i);
        }
        for (String line : Files.readAllLines(Paths.get(path), StandardCharsets.UTF_8)) {
            line = line.trim();
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }
            String[] parts = line.split("\\s+");
            Integer i = idx.get(parts[0]);
            if (i != null && parts.length > 1) {
                weights[i] = Double.parseDouble(parts[1]);
            }
        }
    }

    String weightsToString() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < N; i++) {
            sb.append(String.format(Locale.ROOT, "  %-22s %10.2f%n", FEATURES[i], weights[i]));
        }
        return sb.toString();
    }
}
