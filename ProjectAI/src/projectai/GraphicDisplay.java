package projectai;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.util.ArrayList;
import javax.swing.ImageIcon;
import javax.swing.JPanel;
import javax.swing.Timer;

/**
 *
 * @author Dỗ Mạnh Khoa
 */
public class GraphicDisplay extends JPanel {

    private final int width;
    private final int height;
    private final int blocksize = 38;
    private final int scrsizeY;
    private final int scrsizeX;
    private int[][] gridWall;
    private GameState state;
    private final Color dotcolor = new Color(192, 192, 0);
    private final Dimension d = new Dimension(900, 900);
    private Image ghost2, ghost1;
    // One body color per ghost, in ghost-index order
    // (classic mode: Chaser, Ambusher, Flanker, Shy).
    private static final java.awt.Color[] GHOST_COLORS = {
        new java.awt.Color(255, 40, 40),   // 1 Chaser   - red
        new java.awt.Color(190, 90, 255),  // 2 Ambusher - purple
        new java.awt.Color(40, 220, 90),   // 3 Flanker  - green
        new java.awt.Color(255, 160, 40)   // 4 Shy      - orange
    };
    private Image[] ghostColored;
    private Timer timer;
    private Image pacman1, pacman2up, pacman2left, pacman2right, pacman2down;
    private Image pacman3up, pacman3down, pacman3left, pacman3right;
    private Image pacman4up, pacman4down, pacman4left, pacman4right;
    Image img;

    GraphicDisplay(GameState state) {
        this.width = Layout.WIDTH;
        this.height = Layout.HEIGHT;
        this.scrsizeX = this.width * this.blocksize;
        this.scrsizeY = this.height * this.blocksize;
        this.state = state;
        gridWall = new int[height][width];
        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                gridWall[i][j] = 0;
            }
        }
        state.gameStateData.layout.walls.forEach((pos) -> {
            gridWall[(int) pos.y][(int) pos.x] = 1;
        });
        loadImages();
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        doDrawing(g);
    }

    private final int pacanimdelay = 2;
    private int pacanimcount = pacanimdelay;
    private int pacanimdir = 1;
    private int pacmananimpos = 0;
    private final int pacmananimcount = 4;

    private void doAnim() {

        pacanimcount--;
        if (pacanimcount <= 0) {
            pacanimcount = pacanimdelay;
            pacmananimpos = pacmananimpos + pacanimdir;

            if (pacmananimpos == (pacmananimcount - 1) || pacmananimpos == 0) {
                pacanimdir = -pacanimdir;
            }
        }
    }

    private Image loadImage(String name) {
        java.net.URL url = getClass().getResource("/images/" + name);
        if (url == null) {
            System.err.println("Missing image: " + name);
            return new ImageIcon().getImage();
        }
        return new ImageIcon(url).getImage();
    }

    /*
    * Recolor the red body of the ghost sprite, keeping eyes and alpha glow.
    */
    private static Image tint(Image src, java.awt.Color c) {
        int w = src.getWidth(null), h = src.getHeight(null);
        if (w <= 0 || h <= 0) {
            return src;
        }
        java.awt.image.BufferedImage img = new java.awt.image.BufferedImage(
                w, h, java.awt.image.BufferedImage.TYPE_INT_ARGB);
        img.getGraphics().drawImage(src, 0, 0, null);
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int argb = img.getRGB(x, y);
                int a = argb >>> 24, r = (argb >> 16) & 255, g = (argb >> 8) & 255, b = argb & 255;
                if (r > g + 60 && r > b + 60) { // reddish body pixel
                    float k = r / 255f;
                    int nr = (int) (c.getRed() * k), ng = (int) (c.getGreen() * k), nb = (int) (c.getBlue() * k);
                    img.setRGB(x, y, (a << 24) | (nr << 16) | (ng << 8) | nb);
                }
            }
        }
        return img;
    }

    private void loadImages() {
        ghost1 = loadImage("GhostScared1.png");
        ghost2 = loadImage("Ghost.png");
        ghostColored = new Image[GHOST_COLORS.length];
        for (int i = 0; i < GHOST_COLORS.length; i++) {
            ghostColored[i] = tint(ghost2, GHOST_COLORS[i]);
        }
        pacman1 = loadImage("pacman.png");
        pacman2up = loadImage("up1.png");
        pacman3up = loadImage("up2.png");
        pacman4up = loadImage("up3.png");
        pacman2down = loadImage("down1.png");
        pacman3down = loadImage("down2.png");
        pacman4down = loadImage("down3.png");
        pacman2left = loadImage("left1.png");
        pacman3left = loadImage("left2.png");
        pacman4left = loadImage("left3.png");
        pacman2right = loadImage("right1.png");
        pacman3right = loadImage("right2.png");
        pacman4right = loadImage("right3.png");

    }

    private void drawGhost(Graphics2D g2d, GameState state) {
        ArrayList<AgentState> ghosts = state.getGhostStates();
        for (int i = 0; i < ghosts.size(); i++) {
            AgentState ghost = ghosts.get(i);
            Image img = ghost.scaredTimer > 0 ? ghost1
                    : ghostColored[i % ghostColored.length];
            g2d.drawImage(img, (int) (ghost.configuration.position.x * blocksize),
                    (int) (ghost.configuration.position.y * blocksize), this);
        }

    }

    private void drawPacman(Graphics2D g2d, GameState state) {
        AgentState pacman = state.getPacmanState();
        int[] direction;
        direction = pacman.configuration.direction;
        if (direction == Directions.WEST) {
            drawPacmanLeft(g2d, state);
        } else if (direction == Directions.EAST) {
            drawPacmanRight(g2d, state);
        } else if (direction == Directions.SOUTH) {
            drawPacmanUp(g2d, state);
        } else {
            drawPacmanDown(g2d, state);
        }
    }

    private void drawPacmanUp(Graphics2D g2d, GameState state) {
        AgentState pacman = state.getPacmanState();
        int pacmany = (int) pacman.configuration.position.y * blocksize;
        int pacmanx = (int) pacman.configuration.position.x * blocksize;
        switch (pacmananimpos) {
            case 1:
                g2d.drawImage(pacman2up, pacmanx + 1, pacmany + 1, this);
                break;
            case 2:
                g2d.drawImage(pacman3up, pacmanx + 1, pacmany + 1, this);
                break;
            case 3:
                g2d.drawImage(pacman4up, pacmanx + 1, pacmany + 1, this);
                break;
            default:
                g2d.drawImage(pacman1, pacmanx + 1, pacmany + 1, this);
                break;
        }
    }

    private void drawPacmanDown(Graphics2D g2d, GameState state) {
        AgentState pacman = state.getPacmanState();
        int pacmany = (int) pacman.configuration.position.y * blocksize;
        int pacmanx = (int) pacman.configuration.position.x * blocksize;
        switch (pacmananimpos) {
            case 1:
                g2d.drawImage(pacman2down, pacmanx + 1, pacmany + 1, this);
                break;
            case 2:
                g2d.drawImage(pacman3down, pacmanx + 1, pacmany + 1, this);
                break;
            case 3:
                g2d.drawImage(pacman4down, pacmanx + 1, pacmany + 1, this);
                break;
            default:
                g2d.drawImage(pacman1, pacmanx + 1, pacmany + 1, this);
                break;
        }
    }

    private void drawPacmanLeft(Graphics2D g2d, GameState state) {
        AgentState pacman = state.getPacmanState();
        int pacmany = (int) pacman.configuration.position.y * blocksize;
        int pacmanx = (int) pacman.configuration.position.x * blocksize;
        switch (pacmananimpos) {
            case 1:
                g2d.drawImage(pacman2left, pacmanx + 1, pacmany + 1, this);
                break;
            case 2:
                g2d.drawImage(pacman3left, pacmanx + 1, pacmany + 1, this);
                break;
            case 3:
                g2d.drawImage(pacman4left, pacmanx + 1, pacmany + 1, this);
                break;
            default:
                g2d.drawImage(pacman1, pacmanx + 1, pacmany + 1, this);
                break;
        }
    }

    private void drawPacmanRight(Graphics2D g2d, GameState state) {
        AgentState pacman = state.getPacmanState();
        int pacmany = (int) pacman.configuration.position.y * blocksize;
        int pacmanx = (int) pacman.configuration.position.x * blocksize;
        switch (pacmananimpos) {
            case 1:
                g2d.drawImage(pacman2right, pacmanx + 1, pacmany + 1, this);
                break;
            case 2:
                g2d.drawImage(pacman3right, pacmanx + 1, pacmany + 1, this);
                break;
            case 3:
                g2d.drawImage(pacman4right, pacmanx + 1, pacmany + 1, this);
                break;
            default:
                g2d.drawImage(pacman1, pacmanx + 1, pacmany + 1, this);
                break;
        }
    }

    private void doDrawing(Graphics g) {
        Graphics2D g2d = (Graphics2D) g;

        g2d.setColor(Color.black);
        g2d.fillRect(0, 0, d.width, d.height);
        drawMaze(g2d);
        doAnim();
        drawPacman(g2d, state);
        drawGhost(g2d, state);
        drawScore(g2d, state);
    }
    
    
    private void drawScore(Graphics2D g,GameState state) {

        int i;
        String s;

        g.setFont(new Font("Helvetica", Font.BOLD, 20));
        g.setColor(Color.RED);
        s = "Score: " + state.gameStateData.score;
        g.drawString(s, 10, scrsizeY + 20);

        
    }
    boolean ud = false;
    
    void update(GameState state) {
        this.state = state;
        repaint();
    }

    /* Draw one frame off screen (no window needed), used by -record. */
    java.awt.image.BufferedImage snapshot(GameState state) {
        this.state = state;
        int w = scrsizeX, h = scrsizeY + 30;
        java.awt.image.BufferedImage img = new java.awt.image.BufferedImage(
                w, h, java.awt.image.BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        setSize(w, h);
        paintComponent(g);
        g.dispose();
        return img;
    }
    

    private void drawMaze(Graphics2D g2d) {
        drawWall(g2d);
        drawFood(g2d);
        //truyen tham so vit tri cua ghost
    }

    private void drawWall(Graphics2D g2d) {
        int i = 0, j = 0;
        int x, y;
        for (y = 0; y < scrsizeY; y += blocksize) {
            for (x = 0; x < scrsizeX; x += blocksize) {
                g2d.setColor(new Color(5, 100, 5));
                g2d.setStroke(new BasicStroke(2));
                
                if (isWall(i,j)){
                    if (isWall(i-1,j)&&!isWall(i+1,j)&&!isWall(i,j-1)&&!isWall(i,j+1)) {
                        g2d.drawLine(x+blocksize/3, y, x+blocksize/3, y + blocksize);
                        g2d.drawLine(x+blocksize*2/3,y,x+blocksize*2/3,y+blocksize);
                        g2d.drawLine(x+blocksize/3, y+blocksize, x+blocksize*2/3, y+blocksize);
                    }
                    if (!isWall(i-1,j)&&isWall(i+1,j)&&!isWall(i,j-1)&&!isWall(i,j+1)) {
                        g2d.drawLine(x+blocksize/3, y, x+blocksize/3, y + blocksize);
                        g2d.drawLine(x+blocksize*2/3,y,x+blocksize*2/3,y+blocksize);
                        g2d.drawLine(x+blocksize/3, y, x+blocksize*2/3, y);
                    }
                    
                    if (isWall(i-1,j)&&isWall(i+1,j)&&!isWall(i,j-1)&&!isWall(i,j+1)) {
                        g2d.drawLine(x+blocksize/3, y, x+blocksize/3, y + blocksize);
                        g2d.drawLine(x+blocksize*2/3,y,x+blocksize*2/3,y+blocksize);
                    }
                    if (!isWall(i-1,j)&&!isWall(i+1,j)&&isWall(i,j-1)&&!isWall(i,j+1)) {
                        g2d.drawLine(x, y+blocksize/3, x+blocksize, y+blocksize/3);
                        g2d.drawLine(x, y+blocksize*2/3, x+blocksize, y+blocksize*2/3);
                        g2d.drawLine(x+blocksize, y+blocksize/3, x+blocksize, y+blocksize*2/3);
                    }
                    if (!isWall(i-1,j)&&!isWall(i+1,j)&&!isWall(i,j-1)&&isWall(i,j+1)) {
                        g2d.drawLine(x, y+blocksize/3, x+blocksize, y+blocksize/3);
                        g2d.drawLine(x, y+blocksize*2/3, x+blocksize, y+blocksize*2/3);
                        g2d.drawLine(x, y+blocksize/3, x, y+blocksize*2/3);
                    }
                    if (!isWall(i-1,j)&&!isWall(i+1,j)&&isWall(i,j-1)&&isWall(i,j+1)) {
                        g2d.drawLine(x, y+blocksize/3, x+blocksize, y+blocksize/3);
                        g2d.drawLine(x, y+blocksize*2/3, x+blocksize, y+blocksize*2/3);
                    }
                    if (!isWall(i-1,j)&&isWall(i+1,j)&&!isWall(i,j-1)&&isWall(i,j+1)) {
                        g2d.drawLine(x+blocksize/3, y+blocksize/3, x+blocksize, y+blocksize/3);
                        g2d.drawLine(x+blocksize/3, y+blocksize/3, x+blocksize/3, y+blocksize);
                        g2d.drawLine(x+blocksize*2/3, y+blocksize*2/3, x+blocksize, y+blocksize*2/3);
                        g2d.drawLine(x+blocksize*2/3, y+blocksize*2/3, x+blocksize*2/3, y+blocksize);
                        
                    }
                    if (!isWall(i-1,j)&&isWall(i+1,j)&&isWall(i,j-1)&&!isWall(i,j+1)) {
                        g2d.drawLine(x, y+blocksize/3, x+blocksize*2/3, y+blocksize/3);
                        g2d.drawLine(x+blocksize*2/3, y+blocksize/3, x+blocksize*2/3, y+blocksize);
                        g2d.drawLine(x, y+blocksize*2/3, x+blocksize/3, y+blocksize*2/3);
                        g2d.drawLine(x+blocksize/3, y+blocksize*2/3, x+blocksize/3, y+blocksize);
                    }
                    if (isWall(i-1,j)&&!isWall(i+1,j)&&!isWall(i,j-1)&&isWall(i,j+1)) {
                        g2d.drawLine(x+blocksize/3, y, x+blocksize/3, y+blocksize*2/3);
                        g2d.drawLine(x+blocksize/3, y+blocksize*2/3, x+blocksize, y+blocksize*2/3);
                        g2d.drawLine(x+blocksize*2/3, y, x+blocksize*2/3, y+blocksize/3);
                        g2d.drawLine(x+blocksize*2/3, y+blocksize/3, x+blocksize, y+blocksize/3);
                    }
                    if (isWall(i-1,j)&&!isWall(i+1,j)&&isWall(i,j-1)&&!isWall(i,j+1)){
                        g2d.drawLine(x+blocksize/3, y, x+blocksize/3, y+blocksize/3);
                        g2d.drawLine(x,y+blocksize/3,x+blocksize/3,y+blocksize/3);
                        g2d.drawLine(x+blocksize*2/3,y,x+blocksize*2/3,y+blocksize*2/3);
                        g2d.drawLine(x,y+blocksize*2/3,x+blocksize*2/3,y+blocksize*2/3);
                    }
                    if (isWall(i-1,j)&&isWall(i+1,j)&&!isWall(i,j-1)&&isWall(i,j+1)) {
                        g2d.drawLine(x+blocksize/3, y, x+blocksize/3, y+blocksize);
                        g2d.drawLine(x+blocksize*2/3, y, x+blocksize*2/3, y+blocksize/3);
                        g2d.drawLine(x+blocksize*2/3,y+blocksize/3,x+blocksize,y+blocksize/3);
                        g2d.drawLine(x+blocksize*2/3,y+blocksize*2/3,x+blocksize,y+blocksize*2/3);
                        g2d.drawLine(x+blocksize*2/3,y+blocksize*2/3,x+blocksize*2/3,y+blocksize);
                    }
                    if (isWall(i-1,j)&&isWall(i+1,j)&&isWall(i,j-1)&&!isWall(i,j+1)){
                        g2d.drawLine(x+blocksize*2/3, y, x+blocksize*2/3, y+blocksize);
                        g2d.drawLine(x+blocksize/3, y, x+blocksize/3, y+blocksize/3);
                        g2d.drawLine(x+blocksize/3,y+blocksize/3,x,y+blocksize/3);
                        g2d.drawLine(x,y+blocksize*2/3,x+blocksize/3,y+blocksize*2/3);
                        g2d.drawLine(x+blocksize/3,y+blocksize*2/3,x+blocksize/3,y+blocksize);
                    }
                    if (!isWall(i-1,j)&&isWall(i+1,j)&&isWall(i,j-1)&&isWall(i,j+1)){
                        g2d.drawLine(x, y+blocksize/3, x+blocksize, y+blocksize/3);
                        g2d.drawLine(x, y+blocksize*2/3, x+blocksize/3, y+blocksize*2/3);
                        g2d.drawLine(x+blocksize/3, y+blocksize*2/3, x+blocksize/3, y+blocksize);
                        g2d.drawLine(x+blocksize*2/3, y+blocksize*2/3, x+blocksize, y+blocksize*2/3);
                        g2d.drawLine(x+blocksize*2/3, y+blocksize*2/3, x+blocksize*2/3, y+blocksize);
                    }
                    if (isWall(i-1,j)&&!isWall(i+1,j)&&isWall(i,j-1)&&isWall(i,j+1)){
                        g2d.drawLine(x, y+blocksize*2/3, x+blocksize, y+blocksize*2/3);
                        g2d.drawLine(x+blocksize/3, y+blocksize/3, x+blocksize/3, y);
                        g2d.drawLine(x+blocksize/3, y+blocksize/3, x, y+blocksize/3);
                        g2d.drawLine(x+blocksize*2/3, y, x+blocksize*2/3, y+blocksize/3);
                        g2d.drawLine(x+blocksize*2/3, y+blocksize/3, x+blocksize, y+blocksize/3);
                    }
                    if (isWall(i-1,j)&&isWall(i+1,j)&&isWall(i,j-1)&&isWall(i,j+1)){
                        g2d.drawLine(x, y+blocksize/3, x+blocksize/3, y+blocksize/3);
                        g2d.drawLine(x+blocksize/3, y+blocksize/3, x, y+blocksize/3);
                        g2d.drawLine(x+blocksize*2/3, y, x+blocksize*2/3, y+blocksize/3);
                        g2d.drawLine(x+blocksize*2/3, y+blocksize/3, x+blocksize, y+blocksize/3);
                        g2d.drawLine(x, y+blocksize*2/3, x+blocksize/3, y+blocksize*2/3);
                        g2d.drawLine(x+blocksize/3, y+blocksize*2/3, x+blocksize/3, y+blocksize);
                        g2d.drawLine(x+blocksize*2/3, y+blocksize*2/3, x+blocksize, y+blocksize*2/3);
                        g2d.drawLine(x+blocksize*2/3, y+blocksize*2/3, x+blocksize*2/3, y+blocksize);
                        
                    }
                }

                j++;
            }
            i++;
            j = 0;
        }

    }

    private void drawFood(Graphics2D g2d) {
        int i = 0, j = 0, f = 0, c = 0, e = 0;
        int x, y;
        ArrayList<Position> food = state.gameStateData.food;
        ArrayList<Position> capsules = state.gameStateData.capsules;
        for (y = 0; y < scrsizeY; y += blocksize) {
            for (x = 0; x < scrsizeX; x += blocksize) {
                if (food.isEmpty() == false) {
                    if (food.get(f).x == j && food.get(f).y == i) {
                        g2d.setColor(dotcolor);
                        g2d.fillRect(x + blocksize*4 / 9, y + blocksize*4 / 9, blocksize/9, blocksize/9);
                        if (f < food.size() - 1) {
                            f++;
                        }
                    }
                }
                if (!capsules.isEmpty()) {
                    if (capsules.get(c).x == j && capsules.get(c).y == i) {
                        g2d.setColor(dotcolor);
                        g2d.fillOval(x+blocksize/3, y+blocksize/3, blocksize/3, blocksize/3);
                        if (c < capsules.size() - 1) {
                            c++;
                        }
                    }
                }
                j++;

            }
            i++;
            j = 0;
        }
    }

    private boolean isWall(int i, int j) {
        if (i<0 || i>height-1 || j<0 || j > width-1) return false;
        return gridWall[i][j]== 1;
    }
}
