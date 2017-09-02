package projectai;

import java.util.*;

/**
 *
 * @author Phan Vũ Hồng Hải, Ngô Quang Hòa
 */
public class GhostAlphaBetaPrunningAgent extends Agent {

    /*
    * Ghost đóng vai trò nút MAX. Pacman đóng vai trò nút MIN.
     */
    private final int depth;
    private final int typeEval;

    public GhostAlphaBetaPrunningAgent(int index, int depth, int typeEval) {
        super(index);
        this.depth = depth;
        this.typeEval = typeEval;
    }

    /*
    * Cắt cây tìm kiếm
     */
    public boolean cutoffTest(int nodeDepthCount, int numAgent) {
        return (this.depth * numAgent == nodeDepthCount);
    }

    /*
    * Trả lại hành động mang lợi ích lớn nhất cho ghost.
     */
    @Override
    int[] getAction(GameState state) {
        int nodeDepthCount = 0;
        double alpha = Double.NEGATIVE_INFINITY;
        double beta = Double.POSITIVE_INFINITY;
        ActVal av = this.alphaBetaAV(state, nodeDepthCount, alpha, beta);
        int[] action = av.action;
        return action;
    }

    /*
    * Lựa chọn đặt tác tử vào vai trò MAX, MIN, hay CHANCE.
    * Trả lại hàm đánh giá nếu trạng thái là kết thúc game hoặc
    * độ sâu cây tìm kiếm vượt quá depth.
     */
    private ActVal alphaBetaAV(GameState state, int nodeDepthCount, double alpha, double beta) {
        int numAgent = state.gameStateData.agentStates.size();
        if (state.isLose() || state.isWin() || this.cutoffTest(nodeDepthCount,
                numAgent)) {
            return (new ActVal(null, this.evaluationFunction(state, index)));
        } else if (((index + nodeDepthCount) % numAgent) != 0) {
            return this.maxValue(state, (index + nodeDepthCount) % numAgent, nodeDepthCount, alpha, beta);
        } else {
            return this.minValue(state, nodeDepthCount, alpha, beta);
        }
    }

    /*
    * Trả lại hành động và giá trị ứng với nút MAX.
     */
    private ActVal maxValue(GameState state, int ghostIndex, int nodeDepthCount,
            double alpha, double beta) {
        ActVal av = new ActVal(null, Double.NEGATIVE_INFINITY);
        ArrayList<int[]> actions = state.getLegalAction(ghostIndex);
        if (nodeDepthCount == 0) {
        }
        for (int[] action : actions) {
            GameState newState = state.generateSuccessor(ghostIndex, action);
            ActVal temp = this.alphaBetaAV(newState, nodeDepthCount + 1,
                    alpha, beta);
            if (av.value < temp.value) {
                av.action = action;
                av.value = temp.value;
            }
            if (av.value > beta) {
                return av;
            }
            alpha = Math.max(av.value, alpha);
        }
        return av;
    }

    /*
    * Trả lại hành động và giá trị ứng với nút MIN.
     */
    private ActVal minValue(GameState state, int nodeDepthCount, double alpha, double beta) {
        ActVal av = new ActVal(null, Double.POSITIVE_INFINITY);
        ArrayList<int[]> actions = state.getLegalAction(0);
        for (int[] action : actions) {
            GameState newState = state.generateSuccessor(0, action);
            ActVal temp = this.alphaBetaAV(newState, nodeDepthCount + 1,
                    alpha, beta);

            if (av.value > temp.value) {
                av.action = action;
                av.value = temp.value;
            }
            if (av.value < alpha) {
                return av;
            }
            beta = Math.min(av.value, beta);
        }
        return av;
    }

    private double evaluationFunction(GameState state, int ghostIndex) {
        double score = 0;
        AgentState ghostState = state.getGhostState(ghostIndex);
        if (ghostState.scaredTimer > 0) {
            score += Util.manhattanDistance(state.getPacmanPosition(),
                    state.getGhostPosition(ghostIndex));
        } else {
            if (this.typeEval == 0) {
                score += -Util.manhattanDistance(state.getPacmanPosition(),
                        state.getGhostPosition(ghostIndex));
            } else {
                score += -AstarSearch.AstarFind(state.getGhostPosition(ghostIndex),
                        state.getPacmanPosition(), state);

                for (int i = 0; i < state.getGhostPositions().size(); i++) {
                    if (state.getGhostState(i).scaredTimer <= 0) {
                        Position posCo = state.getGhostState(i).getPosition();
                        if ((posCo.x != state.getGhostPosition(ghostIndex).x)
                                && (posCo.y != state.getGhostPosition(ghostIndex).y)) {
                            score += 0.2 * Util.manhattanDistance(state.getGhostPosition(ghostIndex), posCo);
                        }
                    }

                }
            }
        }
        score -= 0.75 * state.gameStateData.score;
        return score;
    }
}
