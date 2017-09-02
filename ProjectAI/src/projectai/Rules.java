package projectai;
import java.util.*;

/**
 *
 * @author Phan Vũ Hồng Hải
 */

/*
* Quy dinh cac luat cho cac tac tu.
*/

public class Rules {

    final static int SCARED_TIME = 40;
    final static float DEAD_DISTANCE = 0.7f;
    final static int PACMAN_LIFE = 3;
    final static int TIME_OUT = 30;
}

class ClassicGameRules extends Rules {

    /*
    * Luật cho toàn game.
    */
    int timeout;
    boolean quiet;
    GameState initialState;

    public ClassicGameRules(int timeout, boolean quiet) {
        this.quiet = quiet;
        initialState = null;
        this.timeout = timeout;
    }
    
    /*
    * Khởi tạo một game mới.
    */
    public Game newGame(GameState initState, GraphicDisplay display,
            ArrayList<Agent> ghostAgents, Agent pacmanAgent, Boolean quiet) {
        ArrayList<Agent> agents = new ArrayList();
        agents.add(pacmanAgent);
        ghostAgents.forEach((i) -> {
            agents.add(i);
        });
        this.initialState = initState;
        Game game = new Game(agents, display, this, timeout);
        game.state = initState;
        return game;
    }
   
    /*
    * Kết thúc game khi thắng, in ra nếu quiet = false.
    */
    public void win(GameState state, Game game) {
        if (this.quiet == false) {
            System.out.println("Pacman thang! Diem: " + state.gameStateData.score);
        }
        game.gameOver = true;
    }
    
    /*
    * Kết thúc game khi thua, in ra nếu quiet = true.
    */
    public void lose(GameState state, Game game) {
        if (this.quiet == false) {
            System.out.println("Pacman thua! Diem: " + state.gameStateData.score);
        }
        game.gameOver = true;
    }
    
    
    /*
    * Kiểm tra trạng thái và thực hiện 2 method trên.
    */
    public void process(GameState state, Game game) {
        if (state.isWin()) {
            this.win(state, game);
        }
        if (state.isLose()) {
            this.lose(state, game);
        }
    }
}

class PacmanRules extends Rules {

    /*
     * Luật cho pacman tự động.
     */
    final static int PACMAN_SPEED = 1;

    /*
    * Lấy các hành động hợp lệ cho pacman.
    */
    public static ArrayList getLegalActions(GameState state) {
        return Actions.getPossibleActions(state.getPacmanState().configuration,
                state.gameStateData.layout.walls);
    }

    /*
    * Phương thức ăn food và capsule.
    */
    public static void consume(Position position, GameState state) {

        // Eat foood
        if (state.gameStateData.food.contains(position)) {
            state.gameStateData.scoreChange += 10;
            state.gameStateData.food.remove(position);
            int numFood = state.gameStateData.food.size();
            if (numFood == 0 && state.gameStateData._lose == false) {
                state.gameStateData.scoreChange += 500;
                state.gameStateData._win = true;
            }
        }

        // Eat capsule
        if (state.gameStateData.capsules.contains(position)) {
            state.gameStateData.capsules.remove(position);
            for (int i = 1; i <= state.gameStateData.numGhosts; ++i) {
                state.gameStateData.agentStates.get(i).scaredTimer = Rules.SCARED_TIME;
            }
        }

    }

    /*
    * Tạo trạng mới thái từ hành động của pacman.
    */
    public static void applyAction(GameState state, int[] action) {
        
        AgentState pacmanState = state.gameStateData.agentStates.get(0);

        // Update configuration
        float[] vector = Actions.directionToVector(action, PACMAN_SPEED);
        pacmanState.configuration = pacmanState.configuration.generateSuccessor(vector);

        // Eat
        Position nextPos = pacmanState.configuration.getPosition();
        Position nearestPos = new Position((int) (nextPos.x + 0.5), (int) (nextPos.y + 0.5));
        if (Util.manhattanDistance(nearestPos, nextPos) <= 0.5) {
            PacmanRules.consume(nearestPos, state);
        }
    }
}

class GhostRules extends Rules {

    /*
    * Luật cho ma.
     */
    final static float GHOST_SPEED = 1.0f;
    final static float SCARED_SPEED = 0.5f;
    static int killCount;

    /*
    * Lấy hành động hợp lệ của ma có index là ghostIndex.
    * Ma không được dừng. Ma chỉ có thể quay đầu khi gặp ngõ cụt.
    * 
    */
    public static ArrayList getLegalActions(GameState state, int ghostIndex) {
        Configuration conf = state.getGhostState(ghostIndex).configuration;
        ArrayList<int[]> possibleActions = Actions.getPossibleActions(conf,
                state.gameStateData.layout.walls);
        
        ArrayList<int[]> removeActions = new ArrayList();
        possibleActions.stream().map((action) -> {
            // Ma không được dừng.
            if (action[0] == 0 && action[1] == 0) {
                removeActions.add(action);
            }
            // Ma chỉ có thể quay đầu khi gặp ngõ cụt.
            return action;
        }).filter((action) -> (action[0] == -conf.direction[0] && action[1]
                == -conf.direction[1] && possibleActions.size()
                > 1)).forEachOrdered((action) -> {
            removeActions.add(action);
        });
        
        possibleActions.removeAll(removeActions);
        return possibleActions;
    }

    /*
    * Tạo trạng thái mới từ hành động của ma.
    */
    public static void applyAction(GameState state, int[] action, int ghostIndex) {
        AgentState ghostState = state.getGhostState(ghostIndex);
        float speed = GhostRules.GHOST_SPEED;
        if (ghostState.scaredTimer > 0) {
            speed = GhostRules.SCARED_SPEED;
        }
        float[] vector = Actions.directionToVector(action, speed);
        ghostState.configuration = ghostState.configuration.generateSuccessor(vector);
    }

    /*
    * Giảm thời gian sợ đi 1 đơn vị với mỗi nước đi.
    * Chuyển ma về trong một ô nếu thời gian sợ còn lại là 1.
    */
    public static void decrementTimer(GameState state, int agentIndex) {
        AgentState ghostState = state.gameStateData.agentStates.get(agentIndex);
        int timer = ghostState.scaredTimer;
        if (timer == 1) {
            Position pos = ghostState.configuration.position;
            pos.x = (int) (pos.x + 0.5);
            pos.y = (int) (pos.y + 0.5);
        }
        ghostState.scaredTimer = Math.max(0, timer - 1);
    }

    /*
    * Kiểm soát va chạm giữa ma với pacman.
    * Nếu pacman bị ăn thì trừ 500 điểm và thua.
    * Nếu pacman ăn ma thì được 200 điểm.
    */
    public static void collide(GameState state, AgentState ghostState,
            int ghostIndex) {
        if (ghostState.scaredTimer > 0) {
            state.gameStateData.scoreChange += 200;
            GhostRules.reSpawn(state, ghostState);
            ghostState.scaredTimer = 0;
        } else {
            if (state.gameStateData._win == false) {
                state.gameStateData.scoreChange -= 500;
                state.gameStateData._lose = true;

            }
        }

    }

    /*
    * Kiểm tra xem ma hay pacman có thể ăn được nhau hay không.
    */
    public static boolean canKill(Position pacmanPosition, Position ghostPosition) {
        return Util.manhattanDistance(pacmanPosition, ghostPosition)
                <= Rules.DEAD_DISTANCE;
    }

    /*
    * Hồi sinh ma sau khi chết vào vị trí ban đầu và đặt thời gian sợ là 0.
    */
    public static void reSpawn(GameState state, AgentState ghostState) {
        ghostState.configuration = ghostState.startConfig;
    }
    
    /*
    * Kiểm tra trạng thái sống chết của tác tử.
    * Nếu va chạm thì thực hiện phương thức collide.
    */
    public static void checkDeath(GameState state, int agentIndex) {
        Position pacmanPos = state.getPacmanPosition();
        if (agentIndex == 0) {
            // Double check //
            state.getGhostStates().forEach((i) -> {
                Position ghostPosition = i.getPosition();
                int ghostIndex = state.gameStateData.agentStates.indexOf(i);
                if (GhostRules.canKill(pacmanPos, ghostPosition)) {
                    GhostRules.collide(state, i, ghostIndex);
                }
            });
        } else {
            AgentState ghostState = state.gameStateData.agentStates.get(agentIndex);
            Position ghostPosition = ghostState.getPosition();
            if (GhostRules.canKill(pacmanPos, ghostPosition)) {
                GhostRules.collide(state, ghostState, agentIndex);
            }
        }
    }
}
