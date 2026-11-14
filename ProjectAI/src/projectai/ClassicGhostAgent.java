package projectai;

import java.util.*;

/**
 * Arcade-style ghost AI: no game-tree search. Each ghost has a TARGET TILE and,
 * at every tile, takes the legal exit (no reversing) whose next tile is closest
 * in straight-line distance to that target. Ties break in order up, left,
 * down, right.
 *
 * The behavior comes from how the target is chosen:
 *   - a global SCATTER / CHASE schedule (ghosts reverse on every switch),
 *   - a per-ghost personality that picks the CHASE target,
 *   - FRIGHTENED mode (after a capsule): reverse once, then turn randomly.
 *
 * References: Jamey Pittman, "The Pac-Man Dossier"; Chad Birch,
 * "Understanding Pac-Man Ghost Behavior" (GameInternals).
 *
 * Coordinates: x grows right, y grows DOWN the screen (row index).
 * (Directions.NORTH = {0,1} therefore moves down on screen.)
 */
public class ClassicGhostAgent extends Agent {

    enum Personality {
        CHASER,   // targets Pacman's tile
        AMBUSHER, // targets AMBUSH_AHEAD tiles in front of Pacman
        FLANKER,  // mirrors the CHASER around a point 2 tiles ahead of Pacman
        SHY       // chases when far away, retreats to its corner when close
    }

    enum Mode { SCATTER, CHASE, FRIGHTENED }

    // Scatter/chase schedule in Pacman moves (~5.5 moves/s at 180 ms/move):
    // scatter 7s, chase 20s, scatter 7s, chase 20s, scatter 5s, chase 20s,
    // scatter 5s, then chase forever. Values are cumulative end times.
    static final int[] SCHEDULE = {39, 150, 189, 300, 328, 439, 467};

    static final int AMBUSH_AHEAD = 4;
    static final int FLANK_AHEAD = 2;
    static final double SHY_RADIUS = 8;

    // Tie-break preference: screen up, left, screen down, right.
    private static final int[][] PREFERENCE = {
        Directions.SOUTH, Directions.WEST, Directions.NORTH, Directions.EAST};

    final Personality personality;
    private final Position scatterTarget;
    private final int releaseDots;   // leave the house after this many dots eaten...
    private final int releaseTime;   // ...or after this many moves, whichever is first
    private final Random rng = new Random();

    // Per-game memory (reset when a new game starts).
    private Mode lastMode;
    private boolean released;
    private int lastClock = -1;

    public ClassicGhostAgent(int index, Personality personality) {
        super(index);
        this.personality = personality;
        int w = Layout.WIDTH, h = Layout.HEIGHT;
        switch (personality) {
            case CHASER:
                scatterTarget = new Position(w - 2, -3);
                releaseDots = 0; releaseTime = 0;
                break;
            case AMBUSHER:
                scatterTarget = new Position(1, -3);
                releaseDots = 0; releaseTime = 0;
                break;
            case FLANKER:
                scatterTarget = new Position(w - 1, h + 1);
                releaseDots = 10; releaseTime = 25;
                break;
            default: // SHY
                scatterTarget = new Position(0, h + 1);
                releaseDots = 25; releaseTime = 50;
                break;
        }
    }

    @Override
    Agent simCopy() {
        ClassicGhostAgent c = new ClassicGhostAgent(index, personality);
        c.lastMode = lastMode;
        c.released = released;
        c.lastClock = lastClock;
        return c;
    }

    @Override
    void gameOver(GameState finalState) {
        lastMode = null;
        released = false;
        lastClock = -1;
    }

    /** Personality for ghost number i (1-based), in arcade release order. */
    static Personality personalityFor(int ghostIndex) {
        Personality[] order = Personality.values();
        return order[(ghostIndex - 1) % order.length];
    }

    static Mode scheduledMode(int clock) {
        for (int i = 0; i < SCHEDULE.length; i++) {
            if (clock < SCHEDULE[i]) {
                return (i % 2 == 0) ? Mode.SCATTER : Mode.CHASE;
            }
        }
        return Mode.CHASE;
    }

    @Override
    int[] getAction(GameState state) {
        GameStateData data = state.gameStateData;
        AgentState me = state.getGhostState(index);
        int[] dir = me.getDirection();

        // New game started (clock went backwards): forget per-game memory.
        if (data.modeClock < lastClock) {
            lastMode = null;
            released = false;
        }
        lastClock = data.modeClock;

        // 1. Stay in the house until released.
        if (!released) {
            int dotsEaten = data.initialFood - data.food.size();
            released = dotsEaten >= releaseDots || data.modeClock >= releaseTime;
            if (!released) {
                return Directions.STOP;
            }
        }

        Position tile = tileOf(me.getPosition());
        boolean inHouse = data.layout.isGhostHouse(tile);

        // 2. Work out the mode; forced reversal on scatter<->chase and on
        //    entering frightened (not when frightened ends).
        Mode mode = me.scaredTimer > 0 ? Mode.FRIGHTENED : scheduledMode(data.modeClock);
        boolean reverse = lastMode != null && mode != lastMode && lastMode != Mode.FRIGHTENED
                && !inHouse && !isStop(dir);
        lastMode = mode;
        if (reverse) {
            return new int[]{-dir[0], -dir[1]};
        }

        // 3. Candidate moves: legal, no reversing, and never back into the house.
        ArrayList<int[]> legal = state.getLegalAction(index);
        if (legal == null || legal.isEmpty()) {
            return Directions.STOP;
        }
        if (legal.size() == 1) {
            return legal.get(0); // corridor or between tiles: nothing to decide
        }
        ArrayList<int[]> moves = new ArrayList<>();
        for (int[] a : legal) {
            Position next = new Position(tile.x + a[0], tile.y + a[1]);
            if (inHouse || !data.layout.isGhostHouse(next)) {
                moves.add(a);
            }
        }
        if (moves.isEmpty()) {
            moves = legal;
        }

        // 4. Frightened: random turn at each intersection.
        if (mode == Mode.FRIGHTENED && !inHouse) {
            return moves.get(rng.nextInt(moves.size()));
        }

        // 5. Otherwise head for the target tile.
        Position target = inHouse ? data.layout.houseExit : targetTile(state, mode);
        return closestMove(tile, moves, target);
    }

    private Position targetTile(GameState state, Mode mode) {
        if (mode == Mode.SCATTER) {
            return scatterTarget;
        }
        AgentState pac = state.gameStateData.agentStates.get(0);
        Position p = tileOf(pac.getPosition());
        int[] pd = pac.getDirection();
        switch (personality) {
            case CHASER:
                return p;
            case AMBUSHER:
                return new Position(p.x + AMBUSH_AHEAD * pd[0], p.y + AMBUSH_AHEAD * pd[1]);
            case FLANKER: {
                Position pivot = new Position(p.x + FLANK_AHEAD * pd[0], p.y + FLANK_AHEAD * pd[1]);
                Position chaser = findChaser(state);
                if (chaser == null) {
                    return pivot;
                }
                return new Position(2 * pivot.x - chaser.x, 2 * pivot.y - chaser.y);
            }
            default: { // SHY
                Position me = tileOf(state.getGhostState(index).getPosition());
                return euclid(me, p) > SHY_RADIUS ? p : scatterTarget;
            }
        }
    }

    /** Position of the CHASER ghost (ghost 1 in the default line-up). */
    private Position findChaser(GameState state) {
        ArrayList<AgentState> states = state.gameStateData.agentStates;
        for (int i = 1; i < states.size(); i++) {
            if (personalityFor(i) == Personality.CHASER && i != index) {
                return tileOf(states.get(i).getPosition());
            }
        }
        return null;
    }

    private static int[] closestMove(Position tile, List<int[]> moves, Position target) {
        int[] best = null;
        double bestDist = Double.MAX_VALUE;
        for (int[] pref : PREFERENCE) {
            for (int[] a : moves) {
                if (a[0] == pref[0] && a[1] == pref[1]) {
                    double d = euclid(new Position(tile.x + a[0], tile.y + a[1]), target);
                    if (d < bestDist) { // strict: earlier preference wins ties
                        bestDist = d;
                        best = a;
                    }
                }
            }
        }
        return best != null ? best : moves.get(0);
    }

    private static double euclid(Position a, Position b) {
        double dx = a.x - b.x, dy = a.y - b.y;
        return Math.sqrt(dx * dx + dy * dy);
    }

    private static Position tileOf(Position p) {
        return new Position(Math.round(p.x), Math.round(p.y));
    }

    private static boolean isStop(int[] d) {
        return d[0] == 0 && d[1] == 0;
    }
}
