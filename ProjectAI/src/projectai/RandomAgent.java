/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package projectai;

import java.util.*;

/**
 *
 * @author Phan Vũ Hồng Hải
 */
public class RandomAgent extends Agent {

    public RandomAgent(int index) {
        super(index);
    }

    @Override
    int[] getAction(GameState state) {
        ArrayList<int[]> actions = state.getLegalAction(index);
        int[] action = actions.get((int) (Math.random() * actions.size()));
        return action;
    }
}
