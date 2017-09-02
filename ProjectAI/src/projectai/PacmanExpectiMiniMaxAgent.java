/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package projectai;

import java.util.ArrayList;

/**
 *
 * @author Phan Vũ Hồng Hải
 */
public class PacmanExpectiMiniMaxAgent extends Agent{
    
    /*
    * Pacman đóng vai trò là nút MAX. Ghost có index lớn nhất là nút CHANCE.
    * Còn lại đóng vai trò là nút MIN.
    */
    
    private final int depth;
    int score;
    long maxTime;
    long avgTime;
    
    public PacmanExpectiMiniMaxAgent(int index, int depth) {
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
        ActVal av = this.expectiAV(state, nodeDepthCount);
        return av.action;
    }
    
    /*
    * Lựa chọn đặt tác tử vào vai trò MAX, MIN, hay CHANCE.
    * Trả lại hàm đánh giá nếu trạng thái là kết thúc game hoặc
    * độ sâu cây tìm kiếm vượt quá depth.
    */
    private ActVal expectiAV(GameState state, int nodeDepthCount ) {
        int numAgent = state.gameStateData.agentStates.size();
        int agentIndex = nodeDepthCount % numAgent;
        if (state.isLose() || state.isWin() || this.cutoffTest(nodeDepthCount,
                numAgent)) {
            return (new ActVal(null, this.evaluationFunction(state)));
        } else if (agentIndex == this.index) {
            return this.maxValue(state, nodeDepthCount);
        } else if (agentIndex != (numAgent - 1)) {
            return this.minValue(state, agentIndex, nodeDepthCount);
        } else 
            return this.chanceValue(state, numAgent-1, nodeDepthCount);
    }
    
    /*
    * Trả lại hành động và giá trị ứng với nút MAX.
    */
    private ActVal maxValue(GameState state, int nodeDepthCount) {

        ActVal av = new ActVal(null, Double.NEGATIVE_INFINITY);
        ArrayList<int[]> actions = state.getLegalAction(index);
        actions.forEach((action) -> {
            GameState newState = state.generateSuccessor(0, action);
            ActVal temp = this.expectiAV(newState, nodeDepthCount + 1);
            if (av.value < temp.value) {
                av.action = action;
                av.value = temp.value;
            }
        });
        return av;
    }

    /*
    * Trả lại hành động và giá trị ứng với nút MIN.
    */
    private ActVal minValue(GameState state, int agentIndex, int nodeDepthCount) {
        ActVal av = new ActVal(null, Double.POSITIVE_INFINITY);
        ArrayList<int[]> actions = state.getLegalAction(agentIndex);
        actions.forEach((action) -> {
            GameState newState = state.generateSuccessor(agentIndex, action);
            ActVal temp = this.expectiAV(newState, nodeDepthCount + 1);
            if (av.value > temp.value) {
                av.action = action;
                av.value = temp.value;
            }
        });
        return av;
    }
    
    /*
    * Trả lại giá trị với nút ngẫu nhiên.
    */
    private ActVal chanceValue(GameState state,int agentIndex, int nodeDepthCount) {
        double score = 0;
        ArrayList<int[]> actions = state.getLegalAction(agentIndex);
        score = actions.stream().map((action) -> state.generateSuccessor
        (agentIndex, action)).map((newState) -> this.expectiAV(newState, 
                nodeDepthCount + 1)).map((av) -> av.value).reduce(score, 
                        (accumulator, _item) -> accumulator + _item);
        ActVal av = new ActVal(null, score / actions.size());
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
