/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package projectai;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Phan Vũ Hồng Hải
 */
public class Game {
    
    /*
    * Chứa hàm run() để thực hiện game.
    */
    GameState state;
    ArrayList<Agent> agents;
    ClassicGameRules rule;
    GraphicDisplay display;
    boolean gameOver;
    long timeout;
    
    long mTime;
    long run;
    long maxTime;

    Game(ArrayList<Agent> agents, GraphicDisplay display,
            ClassicGameRules rule, long timeout) {
        this.agents = agents;
        this.rule = rule;
        this.gameOver = false;
        this.display = display;
        this.timeout = timeout;
        this.mTime = 0;
        this.maxTime  = 0;
        this.run = 0;
    }

    public void run() {
        /*
        * Thực hiện lấy hành động và áp dụng hành động lần lượt với các tác tử.
        * Trạng thái của Game sẽ thay đổi thành trạng thái tiếp theo sau khi áp
        * dụng hành động của tác tử.
        * Game sẽ chạy lại sau thời gian timeout đối với pacman. Đối với
        * ma game chạy liên tục.
        */
        int agentIndex = 0;
        int numAgents = agents.size();
        int moveTime;
        long startTime;
        int[] action;
        while (gameOver == false) {
            Agent agent = agents.get(agentIndex);
            moveTime = 0;
            startTime = System.currentTimeMillis();
            action = agent.getAction(state);
            moveTime += System.currentTimeMillis() - startTime;
            if (moveTime < timeout && agentIndex == 0) {
                try {
                    Thread.sleep(timeout - moveTime);
                } catch (InterruptedException ex) {
                    Logger.getLogger(Game.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
            if (agentIndex == 0) { 
                this.mTime += moveTime;
                this.run ++;
                if (this.maxTime < moveTime) {
                    this.maxTime = moveTime;
                }
            }
            
            this.state = this.state.generateSuccessor(agentIndex, action);
            this.display.update(this.state);
            this.rule.process(this.state, this);
            agentIndex = (agentIndex + 1) % numAgents;
        }
    }
}

class Agent {

    /* 
     * Lop truu tuong de trien khai cac tac tu thong minh.
     * Cac lop ke thua can trien khai getAction de tra lai hanh dong can thiet
     */
    int index;

    Agent(int index) {
        this.index = index;
    }

    int[] getAction(GameState state) {
        return Directions.STOP;
    }
}

class Directions {

    /*
     * Chua cac bien static mo ta huong di hoac hanh dong cua tac tu
     */
    final static int[] NORTH = {0, 1};
    final static int[] SOUTH = {0, -1};
    final static int[] EAST = {1, 0};
    final static int[] WEST = {-1, 0};
    final static int[] STOP = {0, 0};

}

class GameStateData {

    /*
     * Chứa dữ liệu về trạng thái của game.
     * Khởi tạo trạng thái từ trạng thái trước.
     */
    ArrayList<Position> food;
    ArrayList<Position> capsules;
    ArrayList<Position> powerPellet;
    int score;
    int scoreChange;
    int numGhosts;
    boolean _win;
    boolean _lose;
    Layout layout;
    ArrayList<AgentState> agentStates;

    public GameStateData(GameStateData prevStateData) {
        if (prevStateData != null) {
            /*
            * Nhân bản trạng thái trước đó nếu nó khác rỗng.
            */
            this.layout = (Layout) prevStateData.layout.deepClone();
            this.food = this.layout.food;
            this.capsules = this.layout.capsules;
            this.agentStates = this.copyAgentStates(prevStateData);
            this.score = prevStateData.score;
            this.scoreChange = 0;
            this.numGhosts = prevStateData.numGhosts;
            this._lose = false;
            this._win = false;
        } else {
            /*
            * Tạo trạng thái mới nếu trước đó chưa có trạng thái nào.
            */
            Layout _layout = new Layout();
            _layout.initLayout();
            this.food = _layout.food;
            this.capsules = _layout.capsules;
            this.layout = _layout;
            this.numGhosts = _layout.ghostCount;
            this.score = 0;
            this.scoreChange = 0;
            this._lose = false;
            this._win = false;
            agentStates = new ArrayList();
            layout.agentPositions.stream().map((_agentPosition) -> {
                int isPacman = 0;
                if (_agentPosition.agentIndex == 0) {
                    isPacman = 1;
                }
                Configuration _config = new Configuration(_agentPosition.position, Directions.STOP);
                AgentState _agentState = new AgentState(_config, isPacman);
                return _agentState;
            }).forEachOrdered((_agentState) -> {
                this.agentStates.add(_agentState);
            });

        }
    }

    /*
    * Nhân bản trạng thái của các tác tử.
    */
    public final ArrayList<AgentState> copyAgentStates(GameStateData prevData) {
        ArrayList<AgentState> newAgentStates = new ArrayList();
        prevData.agentStates.forEach((agentState) -> {
            newAgentStates.add(agentState.deepClone());
        });
        return newAgentStates;
    }

}

class Configuration {

    /*
     * Lưu giữ tọa độ và hướng của các tác tử.
     */
    Position position;
    int[] direction;

    public Position getPosition() {
        return position;
    }

    public int[] getDirection() {
        return direction;
    }
    
    public Configuration(Position position, int[] direction) {
        this.position = position;
        this.direction = direction;
    }

    /*
    * Nhân bản cấu hình của 1 tác tử. Không viết clone() do lười ghi đè.
    */
    public Configuration deepClone() {
        Position newPos = new Position(this.position.x, this.position.y);
        Configuration newConfig = new Configuration(newPos, this.direction);
        return newConfig;
    }
    /*
    * Tạo trạng thái mới với 1 vector truyền vào.
    * Vector này tương ứng với tích của tốc độ và vector hướng đi.
    */
    public Configuration generateSuccessor(float[] vector) {
        /* 
         * Tao mot cau hinh moi sinh ra boi viec thuc hien theo vector. 
         */
        float x = position.x;
        float y = position.y;
        int[] newDirection = Actions.vectorToDirection(vector);
        if (newDirection[0] == 0 && newDirection[1] == 0) {
            newDirection = this.direction;
        }
        Position newPosition = new Position(x + vector[0], y + vector[1]);
        Configuration newConfiguration = new Configuration(newPosition,
                newDirection);
        return newConfiguration;
    }
}

class AgentState {

    /*
     * Lưu giữ trạng thái của tác tử.
     */
    int isPacman;
    int scaredTimer;
    Configuration configuration;
    Configuration startConfig;

    public AgentState(Configuration startConfig, int isPacman) {
        this.isPacman = isPacman;
        this.configuration = startConfig;
        this.startConfig = startConfig;
        this.scaredTimer = 0;
    }

    public Position getPosition() {
        return configuration.getPosition();
    }

    public int[] getDirection() {
        return configuration.getDirection();
    }

    public AgentState shallowClone() {
        AgentState agentState = new AgentState(this.startConfig, isPacman);
        agentState.configuration = this.configuration;
        agentState.scaredTimer = this.scaredTimer;
        return agentState;
    }

    public AgentState deepClone() {
        Configuration newConfig = this.configuration.deepClone();
        AgentState newState = new AgentState(newConfig, isPacman);
        newState.scaredTimer = this.scaredTimer;
        newState.startConfig = this.startConfig;
        return newState;
    }
}

class Actions {
    
    /*
    * Cac phuong thuc static de thuc thi hanh dong.
    */
    public static int[] vectorToDirection(float[] vector) {
        if (vector[0] < 0) {
            return Directions.WEST;
        }
        if (vector[0] > 0) {
            return Directions.EAST;
        }
        if (vector[1] > 0) {
            return Directions.NORTH;
        }
        if (vector[1] < 0) {
            return Directions.SOUTH;
        }
        return Directions.STOP;
    }

    public static float[] directionToVector(int[] direction, float speed) {
        float[] vector = {direction[0] * speed, direction[1] * speed};
        return vector;
    }

    public static ArrayList getPossibleActions(Configuration config, ArrayList<
            Position> walls) {
        ArrayList<int[]> posAct = new ArrayList();
        float x = config.position.x;
        float y = config.position.y;
        int x_int = (int) (x + 0.5f);
        int y_int = (int) (y + 0.5f);

        // Neu tac tu dang o giua ranh gioi cac o thi tiep tuc di thang
        if (Math.abs(x - x_int) + Math.abs(y - y_int) > 0) {
            posAct.add(config.getDirection());
            return posAct;
        }

        int next_x, next_y;
        int[][] listDirection = {Directions.NORTH, Directions.SOUTH,
            Directions.EAST, Directions.WEST};
        for (int[] direction : listDirection) {
            next_x = x_int + direction[0];
            next_y = y_int + direction[1];
            if (walls.contains(new Position(next_x, next_y)) == false) {
                posAct.add(direction);
            }
        }
        return posAct;
    }
}

class GameState {

    /*
     * Luu giu trang thai cua game va cac phuong thuc de khai thac thong tin tu
     * trang thai.
     */
    GameStateData gameStateData;

    public GameState(GameState prevState) {
        if (prevState != null) {
            this.gameStateData = new GameStateData(prevState.gameStateData);
        } else {
            this.gameStateData = new GameStateData(null);
        }
    }

    public boolean isWin() {
        return (gameStateData.food.isEmpty());
    }

    public boolean isLose() {
        return (gameStateData._lose);
    }

    public int getNumCapsules() {
        return gameStateData.layout.capsules.size();
    }

    public int getNumFood() {
        return gameStateData.layout.food.size();
    }

    public int getScore() {
        return gameStateData.score;
    }

    public ArrayList getGhostPositions() {
        ArrayList _ghostPos = new ArrayList();
        gameStateData.layout.agentPositions.stream().filter((i) -> (i.agentIndex != 0)).forEachOrdered((i) -> {
            _ghostPos.add(i.position);
        });
        return _ghostPos;
    }

    public Position getGhostPosition(int agentIndex) {
        /* Need Exception */
        return this.gameStateData.agentStates.get(agentIndex).getPosition();
    }

    public ArrayList getLegalAction(int agentIndex) {
        /*
         * Tra lai cac hanh dong hop le cho tac tu tu trang thai hien tai
         */

        if (this.isWin() || this.isLose()) {
            return null;
        }
        if (agentIndex == 0) {
            return PacmanRules.getLegalActions(this);
        } else {
            return GhostRules.getLegalActions(this, agentIndex);
        }
    }

    public GameState generateSuccessor(int agentIndex, int[] action) {
        /*
         * Tra ve trang thai con cua trang thai hien tai sinh ra khi thuc hien hanh dong.
         */

        if (this.isWin() || this.isLose()) {
            return null;
        }
        GameState newGameState = new GameState(this);
        if (agentIndex == 0) {
            PacmanRules.applyAction(newGameState, action);
        } else {
            GhostRules.applyAction(newGameState, action, agentIndex);
        }
        
        if (agentIndex == 0) {
            // Trừ điểm của Pacman khi thực hiện hành động.
            newGameState.gameStateData.scoreChange -= 1;
        } else {
            GhostRules.decrementTimer(newGameState, agentIndex);
        }
        GhostRules.checkDeath(newGameState, agentIndex);
        newGameState.gameStateData.score += newGameState.gameStateData.scoreChange;
        return newGameState;
    }

    public ArrayList<AgentState> getGhostStates() {
        ArrayList<AgentState> ghostStates = new ArrayList();
        for (int i = 1; i < gameStateData.agentStates.size(); i++) {
            ghostStates.add(gameStateData.agentStates.get(i));
        }
        return ghostStates;
    }

    public AgentState getGhostState(int agentIndex) {
        return this.gameStateData.agentStates.get(agentIndex);
    }

    public AgentState getPacmanState() {
        AgentState pacmanState = this.gameStateData.agentStates.get(0)
                .shallowClone();
        return pacmanState;
    }

    public Position getPacmanPosition() {
        return this.gameStateData.agentStates.get(0).getPosition();
    }
}
