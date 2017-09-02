package projectai;

import java.util.*;

/**
 *
 * @author Phan Vũ Hồng Hải
 */
public class Layout {
    
    /*
     * Chua thong tin co ban cua game: tuong, food, capsules, powerPellet, vi 
     * tri cua cac tac tu.
     * food la cac cham diem, neu Pacman an het thi gianh chien thang.
     * Pacman chuyen ma ve trang thai scare sau khi an capsule.
     * powerPellet giup Pacman dich chuyen sang map khac.
     *
     * Quy uoc agentIndex cua Pacman la 0, cua ma bat dau tu 1.
     */
    static final int WIDTH = 17;
    static final int HEIGHT = 11;
    int ghostCount;
    ArrayList<Position> walls;
    ArrayList<Position> food;
    ArrayList<Position> capsules;
    ArrayList<AgentPosition> agentPositions;
    
    public void initLayout() {
        
        /*
         * Khởi tạo thông tin cho toàn bộ game.
         */
        this.food = new ArrayList();
        this.capsules = new ArrayList();
        this.walls = new ArrayList();
        this.agentPositions = new ArrayList();
        this.ghostCount = 0;
        // Bản đồ tiện cho việc sửa đổi.
        char layoutText[][]
                = {
                    {'w', 'w', 'w', 'w', 'w', 'w', 'w', 'w', 'w', 'w', 'w', 'w', 'w', 'w', 'w', 'w', 'w'},
                    {'w', 'C', '.', '.', '.', 'w', '.', '.', '.', '.', '.', 'w', '.', '.', '.', 'C', 'w'},
                    {'w', '.', 'w', 'w', '.', 'w', '.', 'w', 'w', 'w', '.', 'w', '.', 'w', 'w', '.', 'w'},
                    {'w', '.', 'w', '.', '.', '.', '.', '.', '.', '.', '.', '.', '.', '.', 'w', '.', 'w'},
                    {'w', '.', 'w', '.', '.', '.', 'w', '%', '%', '%', 'w', '.', '.', '.', 'w', '.', 'w'},
                    {'w', '.', '.', '.', '.', '.', 'w', 'G', 'G', 'G', 'w', '.', '.', '.', '.', '.', 'w'},
                    {'w', '.', 'w', '.', '.', '.', 'w', 'w', 'w', 'w', 'w', '.', '.', '.', 'w', '.', 'w'},
                    {'w', '.', 'w', '.', '.', '.', '.', '.', '.', '.', '.', '.', '.', '.', 'w', '.', 'w'},
                    {'w', '.', 'w', 'w', '.', 'w', '.', 'w', 'w', 'w', '.', 'w', '.', 'w', 'w', '.', 'w'},
                    {'w', 'C', '.', '.', '.', 'w', '.', '.', 'P', '.', '.', 'w', '.', '.', '.', 'C', 'w'},
                    {'w', 'w', 'w', 'w', 'w', 'w', 'w', 'w', 'w', 'w', 'w', 'w', 'w', 'w', 'w', 'w', 'w'}
                };
        for (int i = 0; i < HEIGHT; i++) {
            for (int j = 0; j < WIDTH; j++) {
                if (layoutText[i][j] == 'w') {
                    this.walls.add(new Position(j, i));
                }
                if (layoutText[i][j] == '.') {
                    this.food.add(new Position(j, i));
                }
                if (layoutText[i][j] == 'C') {
                    this.capsules.add(new Position(j, i));
                }
                if (layoutText[i][j] == 'G') {
                    this.agentPositions.add(new AgentPosition(++ghostCount, j, i));
                }
                if (layoutText[i][j] == 'P') {
                    this.agentPositions.add(new AgentPosition(0, j, i));
                }
            }
        }
        // Sắp xếp vị trí của tác tử theo index tiện cho việc sử dụng.
        Collections.sort(agentPositions, new AgentPositionComparator());
    }
    
    /*
    * Kiểm tra tường
    */
    boolean isWall(Position pos) {
        return walls.contains(pos);
    }
    
    /*
    * Lấy một vị trí hợp lệ ngẫu nhiên trên bản đồ.
    */
    Position getRandomLegalPosition() {
        int x = (int) (Math.random() * WIDTH * 2 - WIDTH);
        int y = (int) (Math.random() * HEIGHT * 2 - HEIGHT);
        Position pos = new Position(x, y);
        while (isWall(pos)) {
            x = (int) (Math.random() * WIDTH * 2 - WIDTH);
            y = (int) (Math.random() * HEIGHT * 2 - HEIGHT);
            pos = new Position(x, y);
        }
        return pos;
    }
    
    /* 
     * Nhân bản layout cho trạng thái kế tiếp.
     */
    public Layout deepClone() {
        Layout newLayout = new Layout();
        newLayout.capsules = (ArrayList) this.capsules.clone();
        newLayout.food = (ArrayList) this.food.clone();
        newLayout.walls = (ArrayList) this.walls.clone();
        newLayout.agentPositions = new ArrayList();
        this.agentPositions.forEach((i) -> {
            AgentPosition newAP = new AgentPosition(i.agentIndex, i.position.x, i.position.y);
            newLayout.agentPositions.add(i);
        });
        newLayout.ghostCount = this.ghostCount;
        return newLayout;
    }

}
