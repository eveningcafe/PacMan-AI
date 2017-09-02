package projectai;

import java.util.*;

/**
 *
 * @author Phan Vũ Hồng Hải
 */

/*
 * Chua cac phuong thuc static va cac kieu du lieu tien ich.
 */
public class Util {

    final static double FOOD_REMAIN_PUNISH = -150;
    final static int HUNT_GHOST = 300;
    final static int CAPSULE_REMAIN_PUNISH = -150;

    static float manhattanDistance(Position pos1, Position pos2) {
        return Math.abs(pos1.x - pos2.x) + Math.abs(pos1.y - pos2.y);
    }

    static double foodRemainEvaluate(GameState state) {
        int numFood = state.getNumFood();
        if (numFood == 0) {
            return 9999;
        }
        
        if (numFood == 1) {
            return 5000;
        }
        
        return numFood * FOOD_REMAIN_PUNISH;
    }

    static double capsuleRemainPunish(GameState state) {
        int numCapsule = state.getNumCapsules();
        return numCapsule * CAPSULE_REMAIN_PUNISH;
    }

    static double nearGhostEvaluate(AgentState pacmanState, AgentState ghostState) {
        Position ghostPosition = ghostState.getPosition();
        Position pacmanPosition = pacmanState.getPosition();
        int scaredTime = ghostState.scaredTimer;
        double distance = manhattanDistance(pacmanPosition, ghostPosition);

        if (scaredTime > 0) {
            return HUNT_GHOST / (distance + 0.1);
        } else {
            if (distance <= Rules.DEAD_DISTANCE) {
                return -9999;
            } else if (distance <= (Rules.DEAD_DISTANCE + 0.5)) {
                return -5000;
            } else if (distance <= (Rules.DEAD_DISTANCE + 1)) {
                return -200;
            } else if (distance <= (Rules.DEAD_DISTANCE + 1.5)) {
                return -40;
            } else if (distance <= (Rules.DEAD_DISTANCE + 2)) {
                return -10;
            } else {
                return 0;
            }
        }
    }

    static double nearestFoodEvaluate(GameState state) {
        double nearestFoodDistance = 9999;
        for (Position foodPos : state.gameStateData.food) {
            nearestFoodDistance = Math.min(nearestFoodDistance,
                    manhattanDistance(state.getPacmanPosition(),
                             foodPos));
        }
        if (nearestFoodDistance < 1) {
            return 50;
        } else if (nearestFoodDistance < 2) {
            return 25;
        } else if (nearestFoodDistance < 3) {
            return 12;
        } else if (nearestFoodDistance < 4) {
            return 6;
        } else {
            return 4 + 5.0 / nearestFoodDistance;
        }
    }

    static double nearestCapuleEvaluate(GameState state) {
        double nearestCapsuleDistance = 9999;
        for (Position capsulePos : state.gameStateData.capsules) {
            nearestCapsuleDistance = Math.min(nearestCapsuleDistance,
                    manhattanDistance(state.getPacmanPosition(),
                             capsulePos));
        }
        if (nearestCapsuleDistance < 1) {
            return 200;
        } else if (nearestCapsuleDistance < 2) {
            return 100;
        } else if (nearestCapsuleDistance < 3) {
            return 50;
        } else if (nearestCapsuleDistance < 4) {
            return 25;
        } else if (nearestCapsuleDistance < 5) {
            return 12;
        } else {
            return 10 + 5.0 / nearestCapsuleDistance;
        }
    }
}

class Position {

    /*
     * Luu tru vi tri cua cac doi tuong.
     */
    float x;
    float y;

    public Position(float x, float y) {
        this.x = x;
        this.y = y;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 97 * hash + Float.floatToIntBits(this.x);
        hash = 97 * hash + Float.floatToIntBits(this.y);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (this == null || this.getClass() != obj.getClass()) {
            return false;
        }
        return this.x == ((Position) obj).x && this.y == ((Position) obj).y;
    }
}

class AgentPosition {

    /*
     * Luu tru vi tri va so hieu tac tu.
     */
    public Position position;
    int agentIndex;

    public AgentPosition(int agentIndex, float x, float y) {
        this.agentIndex = agentIndex;
        this.position = new Position(x, y);
    }
}

class AgentPositionComparator implements Comparator<AgentPosition> {

    /*
     * Ghi de phuong thuc compare de so sanh cac tac tu theo vi tri.
     * Phuc vu viec sap xep danh sach tac tu.
     */
    @Override
    public int compare(AgentPosition pos1, AgentPosition pos2) {
        if (pos1 == null && pos2 == null) {
            return 0;
        }
        if (pos1 == null) {
            return -1;
        }
        if (pos2 == null) {
            return 1;
        }
        if (pos1.agentIndex < pos2.agentIndex) {
            return -1;
        } else {
            return 1;
        }
    }
}

class ActVal {

    int[] action;
    double value;

    public ActVal(int[] action, double value) {
        this.action = action;
        this.value = value;
    }
}
