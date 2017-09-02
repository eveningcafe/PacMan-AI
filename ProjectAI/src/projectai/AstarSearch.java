package projectai;

import java.util.*;

/**
 *
 * @author Ngô Quang Hòa
 */
public class AstarSearch {

    static Stack<int[]> path = new Stack(); // path mà khi gọi AstarFind: đường đi được lưu ở đây ,AstarFind trả về chi phí cần thiết

    public static class Node {

        Position pos;
        Node came_from;
        int[] Directions_from;
        public float g;
        public float f;
        public float h;

        Node(Position pos, int[] action) {
            g = 0;
            h = 0;
            f = 0;
            this.pos = pos;
            this.came_from = null;
            Directions_from = action;
        }
    }
    static Node nodeStart;
    static Node current;

    static float AstarFind(Position start, Position end, GameState state) {
        path.clear();
        if (end == null) {
            return Float.POSITIVE_INFINITY;
        }
        ArrayList<Node> Open = new ArrayList();
        ArrayList<Node> Close = new ArrayList();
        Open.clear();
        Close.clear();
        nodeStart = new Node(start, Directions.STOP);
        nodeStart.h = Util.manhattanDistance(start, end);
        nodeStart.f = nodeStart.h + nodeStart.g;
        Open.add(nodeStart);
        while (!Open.isEmpty()) {
            //tim node co f nho nhat
            current = Open.get(0);
            for (int i = 0; i < Open.size(); i++) {
                if (Open.get(i).f < current.f) {
                    current = Open.get(i);
                }
            }
            Open.remove(current);
            Close.add(current);
            if (current.pos.equals(end)) {// nếu current là trạng thái cần tìm:
                Open.clear();
                Close.clear();

                path.clear();
                Node temp;
                temp = current;
                while (!(temp.Directions_from[0] == 0 && temp.Directions_from[1] == 0)) {
                    path.push(temp.Directions_from);
                    temp = temp.came_from;
                }
                return current.g;
            } else {
                for (Node next : makeListNodeNext(current, state)) {
                    next.came_from = current;
                    boolean have = false;
                    for (Node _node : Close) {
                        if (_node.pos.equals(next.pos)) {
                            have = true;
                            break;
                        }
                    }
                    if (have == true) {
                        continue;
                    }
                    // nếu new đã có trong Close, bỏ qua
                    next.g = current.g + 1;
                    next.h = Util.manhattanDistance(next.pos, end);
                    next.f = next.g + next.h;
                    //System.out.println("next.f  "+next.f );
                    // neu next chua co, hoặc đã có nhưng cải thiện đc. thêm vào open theo ưu tiên f
                    if (Open.isEmpty()) {
                        Open.add(next);
                        continue;
                    }
                    for (int i = 0; i < Open.size(); i++) {
                        if (Open.get(i).pos.equals(next.pos)) {
                            have = true;
                            if (Open.get(i).f > next.f) {
                                Open.add(i, next);
                                Open.remove(i + 1);
                            }
                            break;
                        }
                    }
                    if (have == false) {
                        int i;
                        for (i = 0; i < Open.size(); i++) {
                            if (Open.get(i).f >= next.f) {
                                break;
                            }
                        }
                        Open.add(i, next);
                    }
                }

            }

        }
        return Float.POSITIVE_INFINITY; // nếu không tìm đc đường  trả về INFINITY
    }
    static ArrayList< Node> goNode;

    static ArrayList< Node> makeListNodeNext(Node node, GameState state) {
        goNode = new ArrayList();
        int x_int = (int) (node.pos.x);
        int y_int = (int) (node.pos.y);
        if ((node.pos.x - x_int) + (node.pos.y - y_int) > 0) {//nếu node là lẻ 0.5:
            Position a = new Position(node.pos.x, node.pos.y + 0.5f);
            if (!state.gameStateData.layout.isWall(a)) {
                goNode.add(new Node(a, Directions.NORTH));
            }
            //System.out.println("size goNode= "+goNode.size());
            Position b = new Position(node.pos.x + 0.5f, node.pos.y + 0);
            if (!state.gameStateData.layout.isWall(b)) {
                goNode.add(new Node(b, Directions.EAST));
            }
            //System.out.println("size goNode2= "+goNode.size());
            Position c = new Position(node.pos.x + 0, node.pos.y + -0.5f);
            if (!state.gameStateData.layout.isWall(c)) {
                goNode.add(new Node(c, Directions.SOUTH));
            }
            //System.out.println("size goNode3= "+goNode.size());
            Position d = new Position(node.pos.x + -0.5f, node.pos.y + 0);
            if (!state.gameStateData.layout.isWall(d)) {
                goNode.add(new Node(d, Directions.WEST));
            }
        } else {
            Position a = new Position(node.pos.x + 0, node.pos.y + 1);
            if (!state.gameStateData.layout.isWall(a)) {
                goNode.add(new Node(a, Directions.NORTH));
            }
            //System.out.println("size goNode= "+goNode.size());
            Position b = new Position(node.pos.x + 1, node.pos.y + 0);
            if (!state.gameStateData.layout.isWall(b)) {
                goNode.add(new Node(b, Directions.EAST));
            }
            //System.out.println("size goNode2= "+goNode.size());
            Position c = new Position(node.pos.x + 0, node.pos.y + -1);
            if (!state.gameStateData.layout.isWall(c)) {
                goNode.add(new Node(c, Directions.SOUTH));
            }
            //System.out.println("size goNode3= "+goNode.size());
            Position d = new Position(node.pos.x + -1, node.pos.y + 0);
            if (!state.gameStateData.layout.isWall(d)) {
                goNode.add(new Node(d, Directions.WEST));
            }
            //System.out.println("size goNode4= "+goNode.size());
        }
        return goNode ;
    }
}
