package projectai;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Pacman controlled by an external program (the Python DQN) over a text pipe.
 *
 * Each Pacman turn Java writes one observation line and reads back one line
 * holding the action index. When a game ends Java writes a final line with
 * done=1 and expects no reply.
 *
 * Observation line (space separated):
 *   @ reward done win mask scatter pacX pacY nGhosts [gX gY scaredTimer]*n grid
 * reward  score change since the previous Pacman turn
 * mask    4 chars '0'/'1': legal actions in ACTIONS order
 * scatter 1 if the ghosts are in scatter mode
 * grid    Layout.WIDTH*Layout.HEIGHT chars, row by row:
 *         '#' wall, '.' dot, 'o' capsule, '_' empty
 *
 * Actions (screen directions): 0 up, 1 down, 2 left, 3 right.
 */
public class RemotePacmanAgent extends Agent {

    // Note: Directions.NORTH = {0,1} moves DOWN the screen.
    static final int[][] ACTIONS = {
        Directions.SOUTH, Directions.NORTH, Directions.WEST, Directions.EAST};

    private final BufferedReader in;
    private final PrintStream out;
    private GameState last;

    RemotePacmanAgent(int index, InputStream in, OutputStream out) {
        super(index);
        this.in = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
        this.out = new PrintStream(new BufferedOutputStream(out), false);
    }

    @Override
    int[] getAction(GameState state) {
        ArrayList<int[]> legal = state.getLegalAction(index);
        int reward = last == null ? 0 : state.getScore() - last.getScore();
        send(state, reward, false, legal);
        last = state;
        String line;
        try {
            line = in.readLine();
        } catch (IOException e) {
            line = null;
        }
        if (line == null) { // peer closed the pipe: stop
            System.exit(0);
        }
        int a = Integer.parseInt(line.trim());
        int[] dir = ACTIONS[a];
        for (int[] l : legal) {
            if (l[0] == dir[0] && l[1] == dir[1]) {
                return dir;
            }
        }
        return legal.get(0); // illegal request: fall back to any legal move
    }

    @Override
    void gameOver(GameState finalState) {
        int reward = last == null ? 0 : finalState.getScore() - last.getScore();
        send(finalState, reward, true, null);
        last = null;
    }

    private void send(GameState s, int reward, boolean done, List<int[]> legal) {
        StringBuilder sb = new StringBuilder(400);
        sb.append("@ ").append(reward).append(done ? " 1 " : " 0 ")
          .append(s.isWin() ? 1 : 0).append(' ');
        for (int[] d : ACTIONS) {
            boolean ok = false;
            if (legal != null) {
                for (int[] l : legal) {
                    ok |= l[0] == d[0] && l[1] == d[1];
                }
            }
            sb.append(ok ? '1' : '0');
        }
        boolean scatter = ClassicGhostAgent.scheduledMode(s.gameStateData.modeClock)
                == ClassicGhostAgent.Mode.SCATTER;
        sb.append(scatter ? " 1 " : " 0 ");
        Position p = s.getPacmanPosition();
        sb.append(Math.round(p.x)).append(' ').append(Math.round(p.y)).append(' ');
        ArrayList<AgentState> ghosts = s.getGhostStates();
        sb.append(ghosts.size());
        for (AgentState g : ghosts) {
            Position gp = g.getPosition();
            sb.append(' ').append(Math.round(gp.x)).append(' ').append(Math.round(gp.y))
              .append(' ').append(g.scaredTimer);
        }
        sb.append(' ');
        Layout lay = s.gameStateData.layout;
        char[] grid = new char[Layout.WIDTH * Layout.HEIGHT];
        Arrays.fill(grid, '_');
        for (Position w : lay.walls) {
            grid[Math.round(w.y) * Layout.WIDTH + Math.round(w.x)] = '#';
        }
        for (Position f : s.gameStateData.food) {
            grid[Math.round(f.y) * Layout.WIDTH + Math.round(f.x)] = '.';
        }
        for (Position c : s.gameStateData.capsules) {
            grid[Math.round(c.y) * Layout.WIDTH + Math.round(c.x)] = 'o';
        }
        sb.append(grid);
        out.println(sb);
        out.flush();
    }
}
