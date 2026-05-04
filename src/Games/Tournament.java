package Games;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import Games.Board;
import java.util.Queue;
import java.util.LinkedList;
public class Tournament {
    private static final int FIRST_PLAYER_INDEX = 0;
    private static final int SECOND_PLAYER_INDEX = 1;
    private static final int PLAYER_COUNT = 2;
    private static final int MIN_SEARCH_LEVEL = 1;
    private static final int LEVEL_DECREMENT = 1;
    private static final int PLAYERS_FILE_ARG_INDEX = 0;
    private static final int TIME_PER_MOVE_ARG_INDEX = 1;
    private static final int MAX_LEVEL_ARG_INDEX = 2;
    private static final double NO_EXCESS_TIME = 0.0;
    private static final double NANOS_PER_SECOND = 1_000_000_000.0;

    public static class CmpNode {
    Board board;
    CmpNode left;
    CmpNode right;
    String name;

    public CmpNode(Board board, String name) {
        this.board = board;
        this.name = name;
    }
    }
    public static class CmpResult{
        Board winner;
        Board loser;
    }

    static CmpResult runCompetition(Board board0, Board board1, double timePerMove, int maxLevel)
            throws Board.InvalidMoveException {
        ThreadMXBean thdMx = ManagementFactory.getThreadMXBean();
        int[] levels = new int[PLAYER_COUNT];
        double[] excessTimes = new double[PLAYER_COUNT];
        Board[] boards = new Board[PLAYER_COUNT];
        levels[FIRST_PLAYER_INDEX] = maxLevel;
        levels[SECOND_PLAYER_INDEX] = maxLevel;
        excessTimes[FIRST_PLAYER_INDEX] = NO_EXCESS_TIME;
        excessTimes[SECOND_PLAYER_INDEX] = NO_EXCESS_TIME;
        boards[FIRST_PLAYER_INDEX] = board0;
        boards[SECOND_PLAYER_INDEX] = board1;
        int currentPlayer = FIRST_PLAYER_INDEX;
        CmpResult result = new CmpResult();
        while (true) {
            Minimax.ValueMove res = new Minimax.ValueMove();
            long startTime = thdMx.getCurrentThreadCpuTime();
            Minimax.minimax(boards[currentPlayer], Integer.MIN_VALUE, Integer.MAX_VALUE, levels[currentPlayer], res);
            long EndTime = thdMx.getCurrentThreadCpuTime();
            double moveTime = (EndTime - startTime) / NANOS_PER_SECOND;
            excessTimes[currentPlayer] += moveTime - timePerMove;
            if (excessTimes[currentPlayer] > NO_EXCESS_TIME) {
                levels[currentPlayer] = Math.max(MIN_SEARCH_LEVEL, levels[currentPlayer] - LEVEL_DECREMENT);
                
            }
          
            

    }
}


    public static void main(String[] args) {
        System.out.println("Welcome to the Tournament!");
        // figure out the game from a file of names
        String playersFile = args[PLAYERS_FILE_ARG_INDEX];
        double timePerMove = Double.parseDouble(args[TIME_PER_MOVE_ARG_INDEX]);
        int maxLevel = Integer.parseInt(args[MAX_LEVEL_ARG_INDEX]);
        // store them in a queue 
        Queue<Board> boards = new LinkedList<>();
    }

}
