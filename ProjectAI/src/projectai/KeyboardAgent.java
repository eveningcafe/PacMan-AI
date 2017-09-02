package projectai;

import java.awt.event.*;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
/**
 *
 * @author Phan Vũ Hồng Hải, Đỗ Mạnh Khoa
 */
public class KeyboardAgent extends Agent implements KeyListener{

    int[] direction;
    int[] prevDirection;
    
    public KeyboardAgent(int index) { 
        super(index);
        this.direction = Directions.STOP;
        this.prevDirection = Directions.STOP;
    }
    
    @Override
    int[] getAction(GameState state) {
        try {
            Thread.sleep(50);
        } catch (InterruptedException ex) {
            Logger.getLogger(KeyboardAgent.class.getName()).log(Level.SEVERE, null, ex);
        }
        ArrayList<int[]> directions = state.getLegalAction(index);
        int[] tempDirect = direction;
        for (int[] direct : directions) {
            if (tempDirect[0] == direct[0] && tempDirect[1] == direct[1]) {
                prevDirection = tempDirect;
                return tempDirect;
            }
        }
        for (int[] direct : directions) {
            if (prevDirection[0] == direct[0] && prevDirection[1] == direct[1]) {
                return prevDirection;
            }
        }
        return Directions.STOP;

    }
    
    
    @Override
    public void keyReleased(KeyEvent e) {
        int key = e.getKeyCode();
        if (key == KeyEvent.VK_LEFT) {
            this.direction = Directions.WEST;
        }
        if (key == KeyEvent.VK_RIGHT) { 
            this.direction = Directions.EAST;
        }
        if (key == KeyEvent.VK_UP) {
            this.direction = Directions.SOUTH;
        }
        if (key == KeyEvent.VK_DOWN) {
            this.direction = Directions.NORTH;
        }
    }

    @Override
    public void keyPressed(KeyEvent e) {
        int key = e.getKeyCode();
        if (key == KeyEvent.VK_LEFT) {
            this.direction = Directions.WEST;
        }
        if (key == KeyEvent.VK_RIGHT) { 
            this.direction = Directions.EAST;
        }
        if (key == KeyEvent.VK_UP) {
            this.direction = Directions.SOUTH;
        }
        if (key == KeyEvent.VK_DOWN) {
            this.direction = Directions.NORTH;
        }
    }

    @Override
    public void keyTyped(KeyEvent e) {
    }
    
}
