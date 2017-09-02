package projectai;

import java.util.*;

/**
 *
 * @author Phan Vũ Hồng Hải
 */
public class PacmanAlphaBetaPrunningAgent extends Agent {

    /*
    * Pacman đóng vai trò là MAX. Ma đóng vai trò là MIN.
    */
    private final int depth;
    int score;
    long maxTime;
    long avgTime;

    public PacmanAlphaBetaPrunningAgent(int index, int depth) {
        super(index);
        this.depth = depth;
        this.score = 0;
        this.maxTime = 0;
        this.avgTime = 0;
    }

    /*
    * Cắt cây tìm kiếm khi đạt độ sâu lớn hơn depth.
    */
    public boolean cutoffTest(int nodeDepthCount, int numAgent) {
        return (this.depth * numAgent == nodeDepthCount);
    }

    /*
    * Trả lại hành động mang lại giá trị lớn nhất cho Pacman.
    */
    @Override
    int[] getAction(GameState state) {
        int nodeDepthCount = 0;
        double alpha = Double.NEGATIVE_INFINITY;
        double beta = Double.POSITIVE_INFINITY;
        ActVal av = this.alphaBetaAV(state, nodeDepthCount, alpha, beta);
        return av.action;
    }
    
    /*
    * Lựa chọn đặt tác tử vào vai trò MAX hay MIN.
    * Trả lại hàm đánh giá nếu trạng thái là kết thúc game hoặc
    * độ sâu cây tìm kiếm vượt quá depth.
    */
    private ActVal alphaBetaAV(GameState state, int nodeDepthCount, double alpha,
            double beta) {
        int numAgent = state.gameStateData.agentStates.size();
        int agentIndex = nodeDepthCount % numAgent;
        if (state.isLose() || state.isWin() || this.cutoffTest(nodeDepthCount,
                numAgent)) {
            return (new ActVal(null, this.evaluationFunction(state)));
        } else if (agentIndex == this.index) {
            return this.maxValue(state, nodeDepthCount, alpha, beta);
        } else {
            return this.minValue(state, agentIndex, nodeDepthCount, alpha, beta);
        }
    }
    
    /*
    * Trả lại hành động và giá trị ứng với nút MAX.
    */
    private ActVal maxValue(GameState state, int nodeDepthCount, double alpha,
            double beta) {

        ActVal av = new ActVal(null, Double.NEGATIVE_INFINITY);
        ArrayList<int[]> actions = state.getLegalAction(index);
        for (int[] action : actions) {
            GameState newState = state.generateSuccessor(0, action);
            //System.out.println(newState.gameStateData.layout.agentPositions.get(0));
            ActVal temp = this.alphaBetaAV(newState, nodeDepthCount + 1,
                    alpha, beta);
            if (av.value < temp.value) {
                av.action = action;
                av.value = temp.value;
            }
            if (av.value >= beta) {
                return av;
            }
            alpha = Math.max(av.value, alpha);
        }
        return av;
    }

    /*
    * Trả lại hành động và  giá trị ứng với nút MIN.
    */
    private ActVal minValue(GameState state, int agentIndex, int nodeDepthCount, double alpha,
            double beta) {
        ActVal av = new ActVal(null, Double.POSITIVE_INFINITY);
        ArrayList<int[]> actions = state.getLegalAction(agentIndex);
        for (int[] action : actions) {
            GameState newState = state.generateSuccessor(agentIndex, action);
            ActVal temp = this.alphaBetaAV(newState, nodeDepthCount + 1,
                    alpha, beta);
            
            if (av.value > temp.value) {
                av.action = action;
                av.value = temp.value;
            }
            if (av.value <= alpha) {
                return av;
            }
            beta = Math.min(av.value, beta);
        }
        return av;
    }
    
    /*
    * Hàm đánh giá trạng thái cho pacman.
    */
    private double evaluationFunction(GameState state) {
        double evalScore = 0;
        AgentState pacmanState = state.getPacmanState();
        evalScore += state.getGhostStates().stream().map((ghostState)
                -> Util.nearGhostEvaluate(pacmanState, ghostState))
                .reduce(evalScore, (accumulator, _item) -> accumulator + _item);
        evalScore += Util.foodRemainEvaluate(state);
        evalScore += Util.capsuleRemainPunish(state);
        evalScore += Util.nearestCapuleEvaluate(state);
        evalScore += Util.nearestFoodEvaluate(state);
        evalScore += 2 * state.gameStateData.score;
        return evalScore;
    }
}
