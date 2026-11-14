/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package projectai;

import java.awt.Color;
import java.util.*;
import javax.swing.*;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

/**
 *
 * @author Phan Vũ Hồng Hải
 */
public class Main extends JFrame {

    public void initUI(GraphicDisplay display) {
        add(display);
        setBackground(Color.red);
        setTitle("Points");
        setSize(670, 500);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    }

    public static void main(String[] args) throws InterruptedException {
        int typePac = 0;
        int depth = 1;
        int numGhost = -1; // default: 4 for classic ghosts, 3 for alpha-beta ghosts
        String ghostType = "classic";
        int trainGames = 0;
        int testGames = 0;
        String weightsFile = "qweights.txt";
        String dqnModel = null;
        boolean serve = false;
        int evalFunc = 0;
        int iter = 0;
        int iterGiven = 0;
        CommandLine commandLine;
        Option option_a = Option.builder("albe").argName("depth").hasArg()
                .desc("Pacman cat tia alpha - beta").build();
        Option option_b = Option.builder("expect").argName("depth").hasArg()
                .desc("Pacman expectiminimax").build();
        Option option_c = Option.builder("repeat").argName("number of iteration").hasArg()
                .desc("So lan lap lai").build();
        Option option_d = Option.builder("eval").argName("evaluate function 0 or 1").hasArg()
                .desc("Ham danh gia cua Ghost").build();
        Option option_e = Option.builder("numGhost").argName("number of ghost").hasArg()
                .desc("So luong ghost").build();
        Option option_f = Option.builder("ghost").argName("classic|ab").hasArg()
                .desc("Kieu ma: classic (arcade target-tile AI, mac dinh) hoac ab (alpha-beta)").build();
        Option option_g = Option.builder("qlearn")
                .desc("Pacman approximate Q-learning (doc trong so tu -weights)").build();
        Option option_h = Option.builder("train").argName("games").hasArg()
                .desc("Huan luyen Q-learning N van (chay an), luu trong so").build();
        Option option_i = Option.builder("test").argName("games").hasArg()
                .desc("Danh gia Pacman N van (chay an), in ti le thang").build();
        Option option_j = Option.builder("weights").argName("file").hasArg()
                .desc("File trong so Q-learning (mac dinh qweights.txt)").build();
        Option option_k = Option.builder("dqn").argName("model.pt").hasArg()
                .desc("Pacman Deep Q-Network (goi dqn/policy.py bang python3)").build();
        Option option_l = Option.builder("serve")
                .desc("Chay an lien tuc, Pacman dieu khien qua stdin/stdout (huan luyen DQN)").build();
        Option option_m = Option.builder("search").argName("depth").hasArg()
                .desc("Pacman tim kiem co mo hinh: mo phong ma + gia tri Q-learning").build();
        Options options = new Options();
        options.addOption(option_m);
        options.addOption(option_k);
        options.addOption(option_l);
        options.addOption(option_g);
        options.addOption(option_h);
        options.addOption(option_i);
        options.addOption(option_j);
        CommandLineParser parser = new DefaultParser();

        options.addOption(option_a);
        options.addOption(option_b);
        options.addOption(option_c);
        options.addOption(option_d);
        options.addOption(option_e);
        options.addOption(option_f);

        String header = " Lua chon ";
        String footer = " AI project: Game Pacman";
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("Pacman", header, options, footer, true);

        String[] _args = {"-albe", "3",
            "-repeat", "10", "-eval", "0", "-numGhost", "3",};

        try {
            commandLine = parser.parse(options, args);

            if (commandLine.hasOption("albe")) {
                typePac = 1;
                if (commandLine.getOptionValue("albe") != null) {
                    depth = Math.max(1, Integer.parseInt(commandLine.getOptionValue("albe")));
                }
            }

            if (commandLine.hasOption("expect")) {
                typePac = 2;
                if (commandLine.getOptionValue("expect") != null) {
                    depth = Math.max(1, Integer.parseInt(commandLine.getOptionValue("expect")));
                }
            }

            if (commandLine.hasOption("repeat")) {
                iterGiven = 1;
                if (commandLine.getOptionValue("repeat") != null) {
                    iter = Integer.parseInt(commandLine.getOptionValue("repeat"));
                }
            }

            if (commandLine.hasOption("eval")) {
                if (commandLine.getOptionValue("eval") != null) {
                    evalFunc = Integer.parseInt(commandLine.getOptionValue("eval"));
                }
            }

            if (commandLine.hasOption("numGhost")) {
                if (commandLine.getOptionValue("numGhost") != null) {
                    numGhost = Integer.parseInt(commandLine.getOptionValue("numGhost"));
                }
            }

            if (commandLine.hasOption("qlearn")) {
                typePac = 3;
            }
            if (commandLine.hasOption("search")) {
                typePac = 5;
                depth = Math.max(1, Integer.parseInt(commandLine.getOptionValue("search")));
            }
            if (commandLine.hasOption("dqn")) {
                typePac = 4;
                dqnModel = commandLine.getOptionValue("dqn");
            }
            serve = commandLine.hasOption("serve");
            if (commandLine.hasOption("train")) {
                trainGames = Integer.parseInt(commandLine.getOptionValue("train"));
            }
            if (commandLine.hasOption("test")) {
                testGames = Integer.parseInt(commandLine.getOptionValue("test"));
            }
            if (commandLine.hasOption("weights")) {
                weightsFile = commandLine.getOptionValue("weights");
            }
            if (commandLine.hasOption("ghost") && commandLine.getOptionValue("ghost") != null) {
                ghostType = commandLine.getOptionValue("ghost").toLowerCase();
            }

        } catch (ParseException exception) {
            System.out.print("Parse error: ");
            System.out.println(exception.getMessage());
        }
        /*
        * Khoi tao cac tac tu, co the thay the cac tac tu nay bang cac tac
        * tu can kiem tra.
        * Pacman bắt buộc index = 0
         */
        ClassicGameRules newGame = new ClassicGameRules(180, false);
        ArrayList<Agent> ghostAgents = new ArrayList();
        GameState initState = new GameState(null);
        GraphicDisplay display = new GraphicDisplay(initState);
        Main m = new Main();
        

        if (evalFunc > 1 || evalFunc < 0) {
            evalFunc = 0;
        }
        if (ghostType.equals("classic")) {
            if (numGhost < 1 || numGhost > 4) {
                numGhost = 4;
            }
            for (int j = 1; j <= numGhost; j++) {
                ghostAgents.add(new ClassicGhostAgent(j, ClassicGhostAgent.personalityFor(j)));
            }
        } else if (numGhost == -1) {
            numGhost = 3; // original default for alpha-beta ghosts
        }
        if (!ghostType.equals("classic") && (numGhost < 1 || numGhost > 3)) {
            GhostAlphaBetaPrunningAgent ghost2 = new GhostAlphaBetaPrunningAgent(2, 1, evalFunc); // index 2, độ sâu tìm kiếm 1
            GhostAlphaBetaPrunningAgent ghost1 = new GhostAlphaBetaPrunningAgent(3, 2, evalFunc);
            RandomAgent ghost3 = new RandomAgent(3);
            ghostAgents.add(ghost1);
            ghostAgents.add(ghost2);
            ghostAgents.add(ghost3);
        } else if (!ghostType.equals("classic")) {
            for (int j = 1; j < numGhost; j++) {
                GhostAlphaBetaPrunningAgent ghostT = new GhostAlphaBetaPrunningAgent(j, j, evalFunc);
                ghostAgents.add(ghostT);
            }
            RandomAgent ghostR = new RandomAgent(numGhost);
            ghostAgents.add(ghostR);
        }
        // Bản đồ có 4 vị trí 'G'; chỉ giữ số ma thực sự có tác tử điều khiển.
        initState.keepGhosts(ghostAgents.size());
        System.out.println("Ghosts: " + ghostType + " x" + ghostAgents.size());
        iter = Math.max(1, iter);
        // Môi trường cho DQN: chơi ẩn mãi mãi, Pacman điều khiển qua stdin/stdout.
        if (serve) {
            RemotePacmanAgent remote = new RemotePacmanAgent(0, System.in, System.out);
            ClassicGameRules rules = new ClassicGameRules(0, true);
            while (true) {
                Game game = rules.newGame(initState, null, ghostAgents, remote, true);
                game.maxMoves = 1000;
                game.run();
            }
        }

        Agent pacman;
        switch (typePac) {
            case 5: {
                QLearningPacmanAgent q = new QLearningPacmanAgent(0);
                try {
                    q.load(weightsFile);
                } catch (java.io.IOException e) {
                    System.out.println("Khong doc duoc " + weightsFile
                            + " - hay huan luyen truoc: -qlearn -train 20000");
                    System.exit(1);
                }
                pacman = new ModelSearchPacmanAgent(0, depth, ghostAgents, q);
                break;
            }
            case 4: {
                String py = System.getenv().getOrDefault("PACMAN_PYTHON", "python3");
                ProcessBuilder pb = new ProcessBuilder(py, "dqn/policy.py", dqnModel);
                pb.redirectError(ProcessBuilder.Redirect.INHERIT);
                try {
                    Process proc = pb.start();
                    pacman = new RemotePacmanAgent(0, proc.getInputStream(), proc.getOutputStream());
                } catch (java.io.IOException e) {
                    System.out.println("Khong chay duoc " + py + " dqn/policy.py: " + e.getMessage());
                    System.exit(1);
                    return;
                }
                break;
            }
            case 1:
                pacman = new PacmanAlphaBetaPrunningAgent(0, depth);
                break;
            case 2:
                pacman = new PacmanExpectiMiniMaxAgent(0, depth);
                break;
            case 3: {
                QLearningPacmanAgent q = new QLearningPacmanAgent(0);
                if (trainGames > 0) {
                    train(q, initState, ghostAgents, trainGames, weightsFile);
                } else {
                    try {
                        q.load(weightsFile);
                        System.out.println("Loaded weights from " + weightsFile + ":");
                        System.out.print(q.weightsToString());
                    } catch (java.io.IOException e) {
                        System.out.println("Khong doc duoc " + weightsFile
                                + " - hay huan luyen truoc: -qlearn -train 2000");
                        System.exit(1);
                    }
                }
                q.learning = false;
                q.epsilon = 0;
                pacman = q;
                break;
            }
            default:
                pacman = new KeyboardAgent(0);
        }

        // Danh gia an (khong cua so): in ti le thang, diem trung binh, thoi gian.
        if (testGames > 0) {
            if (pacman instanceof KeyboardAgent) {
                System.out.println("-test can mot Pacman AI: -albe, -expect hoac -qlearn");
                System.exit(1);
            }
            Stats st = playHeadless(pacman, initState, ghostAgents, testGames);
            System.out.printf("Test %d games: win rate %.1f%%, avg score %.1f, "
                    + "avg move time %.2f ms, max move time %d ms%n",
                    testGames, 100.0 * st.wins / testGames, st.totalScore / testGames,
                    (double) st.moveTimeMs / Math.max(1, st.moves), st.maxMoveMs);
            System.exit(0);
        }
        // Training only (no -repeat given): stop after saving the weights.
        if (typePac == 3 && trainGames > 0 && iterGiven == 0) {
            System.exit(0);
        }

        if (pacman instanceof KeyboardAgent) {
            display.addKeyListener((KeyboardAgent) pacman);
            display.setFocusable(true);
        }
        m.initUI(display);
        m.setVisible(true);
        int run = 0;
        long totalScore = 0, totalMoveTime = 0, totalMoves = 0, maxTime = 0;
        while (run++ < iter) {
            Game game = newGame.newGame(initState, display, ghostAgents, pacman, true);
            game.run();
            totalScore += game.state.gameStateData.score;
            totalMoveTime += game.mTime;
            totalMoves += game.run;
            maxTime = Math.max(maxTime, game.maxTime);
        }
        if (!(pacman instanceof KeyboardAgent)) {
            System.out.println("Max Time " + maxTime);
            System.out.println("AVG Time " + totalMoveTime / Math.max(1, totalMoves));
            System.out.println("AVG score = " + (totalScore / iter));
        }
        Thread.sleep(3000);
        System.exit(0);
    }

    static class Stats {
        int wins;
        double totalScore;
        long moves, moveTimeMs, maxMoveMs;
    }

    /* Chạy ẩn n ván (không cửa sổ, không chờ 180ms), tối đa 1000 nước mỗi ván. */
    static Stats playHeadless(Agent pacman, GameState initState,
            ArrayList<Agent> ghostAgents, int n) {
        ClassicGameRules rules = new ClassicGameRules(0, true);
        Stats st = new Stats();
        for (int i = 0; i < n; i++) {
            Game game = rules.newGame(initState, null, ghostAgents, pacman, true);
            game.maxMoves = 1000;
            game.run();
            if (game.state.isWin()) {
                st.wins++;
            }
            st.totalScore += game.state.gameStateData.score;
            st.moves += game.run;
            st.moveTimeMs += game.mTime;
            st.maxMoveMs = Math.max(st.maxMoveMs, game.maxTime);
        }
        return st;
    }

    /* Huấn luyện Q-learning: epsilon giảm dần 0.3 -> 0.05, in tiến độ mỗi 10%. */
    static void train(QLearningPacmanAgent q, GameState initState,
            ArrayList<Agent> ghostAgents, int games, String weightsFile) {
        q.learning = true;
        int block = Math.max(1, games / 10);
        int wins = 0;
        double score = 0;
        long t0 = System.currentTimeMillis();
        for (int g = 1; g <= games; g++) {
            q.epsilon = 0.3 - 0.25 * (g - 1) / Math.max(1, games - 1);
            Stats st = playHeadless(q, initState, ghostAgents, 1);
            wins += st.wins;
            score += st.totalScore;
            if (g % block == 0) {
                System.out.printf("[train] games %5d-%5d  eps %.2f  win rate %5.1f%%  avg score %7.1f%n",
                        g - block + 1, g, q.epsilon, 100.0 * wins / block, score / block);
                wins = 0;
                score = 0;
            }
        }
        System.out.printf("Training done in %.1f s. Weights:%n",
                (System.currentTimeMillis() - t0) / 1000.0);
        System.out.print(q.weightsToString());
        try {
            q.save(weightsFile);
            System.out.println("Saved to " + weightsFile);
        } catch (java.io.IOException e) {
            System.out.println("Khong luu duoc trong so: " + e.getMessage());
        }
    }
}
