package Games;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Random;
import java.util.Scanner;

public class BoardDriver {
    private Board board;
    private Board.Move currentMove;
    static final int MAX_LINE_LENGTH = 80;
    private static final int FIRST_INDEX = 0;
    private static final int NEXT_ITEM_OFFSET = 1;
    private static final int COMMAND_NAME_INDEX = 0;
    private static final int COMMAND_ARG_INDEX = 1;
    private static final int COMMAND_PART_LIMIT = 2;
    private static final int SEED_ARG_INDEX = 0;
    private static final int COUNT_ARG_INDEX = 1;
    private static final int LEVEL_ARG_INDEX = 0;
    private static final int MOVE_COUNT_ARG_INDEX = 1;
    private static final int COMPARISON_EQUAL = 0;
    private static final int HISTORY_COLUMN_WIDTH = 40;
    private static final int HISTORY_COLUMN_COUNT = 2;
    private static final int FIRST_HISTORY_COLUMN = 0;
    private static final int SECOND_HISTORY_COLUMN = 1;
    private static final int REQUIRED_MAIN_ARG_COUNT = 1;
    private static final int BOARD_CLASS_ARG_INDEX = 0;
    private static final int FAILURE_EXIT_CODE = 1;

    public BoardDriver(Board board) {
        this.board = board;
        this.currentMove = board.createMove();
    }

    public void testPlay(int seed, int moveCount) {
        Random rnd = new Random(seed);
        for (int i = FIRST_INDEX; i < moveCount; i++) {
            List<? extends Board.Move> validMoves = board.getValidMoves();
            if (validMoves.isEmpty()) {
                break;
            }
            int moveIndex = rnd.nextInt(validMoves.size());
            Board.Move move = validMoves.get(moveIndex);
            try {
                board.applyMove(move);
            } catch (Board.InvalidMoveException e) {
                break;
            }
        }
    }
    
    public void testRun(int seed, int stepCount) {
        Random rnd = new Random(seed);
        int stepsMade = FIRST_INDEX;
        while (stepsMade < stepCount) {
            List<? extends Board.Move> validMoves = board.getValidMoves();
            List<? extends Board.Move> moveHist = board.getMoveHistory();
            if (validMoves.isEmpty() || board.getValue() == Board.WIN || board.getValue() == -Board.WIN || board.isGameOver()) {
                if (!moveHist.isEmpty()) {
                    int revertMovesCount = rnd.nextInt(moveHist.size()) + NEXT_ITEM_OFFSET;
                    for (int i = FIRST_INDEX; i < revertMovesCount && !board.getMoveHistory().isEmpty(); i++) {
                        board.undoMove();
                    }
                    stepsMade++;
                } else {
                    break;
                }
            } else {
                int moveIndex = rnd.nextInt(validMoves.size());
                Board.Move move = validMoves.get(moveIndex);
                try {
                    board.applyMove(move);
                    stepsMade++;
                } catch (Board.InvalidMoveException e) {
                    break;
                }
            }
        }
    }
    public void compareMove(String moveString) {
        if (currentMove == null) {
            System.out.println("No move has been entered yet.");
            return;
        }
    
        Board.Move compareMove = board.createMove();
        try {
            compareMove.fromString(moveString);
            int comparison = currentMove.compareTo(compareMove);
            if (comparison < COMPARISON_EQUAL) {
                System.out.println("Current move is less");
            } else if (comparison > COMPARISON_EQUAL) {
                System.out.println("Current move is greater");
            } else {
                System.out.println("Current move is equal");
            }
        } catch (IOException e) {
            System.out.println("Invalid move format: " + e.getMessage());
        }
    }

    public void showPlayer() {
        int currentPlayer = board.getCurrentPlayer();
        System.out.println("" + currentPlayer);
    }

    public void showBoard() {
        System.out.println(board.toString());
    }

    public void showMoves() {
        List<? extends Board.Move> moves = board.getValidMoves();
        if (moves.isEmpty()) {
            System.out.println();
            return;
        }

        int maxMoveLength = moves.stream().mapToInt(m -> m.toString().length()).max().orElse(FIRST_INDEX);
        int columns = MAX_LINE_LENGTH / (maxMoveLength + NEXT_ITEM_OFFSET);

        for (int i = FIRST_INDEX; i < moves.size(); i++) {
            System.out.printf("%-" + (maxMoveLength + NEXT_ITEM_OFFSET) + "s", moves.get(i).toString());
            if ((i + NEXT_ITEM_OFFSET) % columns == FIRST_INDEX
                    || i == moves.size() - NEXT_ITEM_OFFSET) {
                System.out.println();
            }
        }
    }

    public boolean enterMove(String moveString) {
        try {
            currentMove.fromString(moveString);
            return true;
        } catch (IOException e) {
            System.out.println("Invalid move format: " + e.getMessage());
            return false;
        }
    }

    public void showMove() {
        System.out.println(currentMove.toString());
    }

    public void applyMove() {
        try {
            board.applyMove(currentMove);
        } catch (Board.InvalidMoveException e) {
            System.out.println("Not a permitted move");
            showMoves();
        }
    }

    public void doMove(String moveString) {
        if (enterMove(moveString)) {
            applyMove();
        }
    }

    public void undoMoves(int count) {
        int toUndo = Math.min(count, board.getMoveHistory().size());
        for (int i = FIRST_INDEX; i < toUndo; i++) {
            board.undoMove();
        }
    }

    public void showVal() {
        System.out.println("" + board.getValue());
    }

    public void showMoveHist() {
        List<? extends Board.Move> history = board.getMoveHistory();
        if (history.isEmpty()) {
            System.out.println("No moves have been made yet.");
            return;
        }
        for (int i = FIRST_INDEX; i < history.size(); i++) {
            int column = i % HISTORY_COLUMN_COUNT;
            if (column == FIRST_HISTORY_COLUMN) {
                System.out.printf("%-" + HISTORY_COLUMN_WIDTH + "s", history.get(i).toString());
            } else {
                System.out.print(history.get(i).toString());
            }
            if (column == SECOND_HISTORY_COLUMN || i == history.size() - NEXT_ITEM_OFFSET) {
                System.out.println();
            }
        }
    }

    public void saveBoard(String fileName) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(fileName))) {
            for (Board.Move move : board.getMoveHistory()) {
                writer.write(move.toString());
                writer.newLine();
            }
        } catch (IOException e) {
            System.out.println("Error saving board: " + e.getMessage());
        }
    }

    public void loadBoard(String fileName) {
        try (BufferedReader reader = new BufferedReader(new FileReader(fileName))) {
            Board newBoard = board.getClass().getDeclaredConstructor().newInstance();
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;
                Board.Move move = newBoard.createMove();
                move.fromString(line);
                try {
                    newBoard.applyMove(move);
                } catch (Board.InvalidMoveException e) {
                    System.out.println("Error loading board: " + e.getMessage());
                    return;
                }
            }
            this.board = newBoard;
            // Leave currentMove unchanged so compareMove matches expected behavior (e.g. after doMove 2 2 then loadBoard, current move stays (2,2))
        } catch (IOException e) {
            System.out.println("Error loading board: " + e.getMessage());
        } catch (ReflectiveOperationException e) {
            System.out.println("Error loading board: " + e.getMessage());
        }
    }

    /** Returns false to quit, true to continue. */
    public boolean handleCommand(String command) {
        command = command.trim();
        String[] commandParts = command.split("\\s+", COMMAND_PART_LIMIT);
        String cmd = commandParts[COMMAND_NAME_INDEX].toLowerCase();

        switch (cmd) {
            case "showboard":
                showBoard();
                break;
            case "showmoves":
                showMoves();
                break;
            case "entermove":
                if (commandParts.length > COMMAND_ARG_INDEX) {
                    enterMove(commandParts[COMMAND_ARG_INDEX].trim());
                } else {
                    System.out.println("Invalid move format.");
                }
                break;
            case "showmove":
                showMove();
                break;
            case "applymove":
                applyMove();
                break;
            case "domove":
                if (commandParts.length > COMMAND_ARG_INDEX) {
                    doMove(commandParts[COMMAND_ARG_INDEX].trim());
                } else {
                    System.out.println("Invalid move format.");
                }
                break;
            case "undomoves":
                if (commandParts.length > COMMAND_ARG_INDEX) {
                    try {
                        int count = Integer.parseInt(commandParts[COMMAND_ARG_INDEX].trim());
                        undoMoves(count);
                    } catch (NumberFormatException e) {
                        System.out.println("Invalid number of moves.");
                    }
                } else {
                    System.out.println("Please specify the number of moves to undo.");
                }
                break;
            case "showval":
                showVal();
                break;
            case "showmovehist":
                showMoveHist();
                break;
            case "saveboard":
                if (commandParts.length > COMMAND_ARG_INDEX) {
                    saveBoard(commandParts[COMMAND_ARG_INDEX].trim());
                } else {
                    System.out.println("Please specify a filename.");
                }
                break;
            case "loadboard":
                if (commandParts.length > COMMAND_ARG_INDEX) {
                    loadBoard(commandParts[COMMAND_ARG_INDEX].trim());
                } else {
                    System.out.println("Please specify a filename.");
                }
                break;
            case "quit":
                return false;
            case "testplay":
                if (commandParts.length > COMMAND_ARG_INDEX) {
                    try {
                        String[] params = commandParts[COMMAND_ARG_INDEX].trim().split("\\s+");
                        int seed = Integer.parseInt(params[SEED_ARG_INDEX].trim());
                        int moveCount = Integer.parseInt(params[COUNT_ARG_INDEX].trim());
                        testPlay(seed, moveCount);
                    } catch (NumberFormatException e) {
                        System.out.println("Invalid input. Please enter seed and move count as integers.");
                    } catch (ArrayIndexOutOfBoundsException e) {
                        System.out.println("Please provide both seed and move count (e.g., testplay 12 20).");
                    }
                } else {
                    System.out.println("Please specify the seed and move count (e.g., testplay 12 20).");
                }
                break;
            case "testrun":
                if (commandParts.length > COMMAND_ARG_INDEX) {
                    try {
                        String[] params = commandParts[COMMAND_ARG_INDEX].trim().split("\\s+");
                        int seed = Integer.parseInt(params[SEED_ARG_INDEX].trim());
                        int stepCount = Integer.parseInt(params[COUNT_ARG_INDEX].trim());
                        testRun(seed, stepCount);
                    } catch (NumberFormatException e) {
                        System.out.println("Invalid input. Please enter seed and step count as integers.");
                    } catch (ArrayIndexOutOfBoundsException e) {
                        System.out.println("Please provide both seed and step count (e.g., testrun 42 1000).");
                    }
                } else {
                    System.out.println("Please specify the seed and step count (e.g., testrun 42 1000).");
                }
                break;
            case "comparemove":
                if (commandParts.length > COMMAND_ARG_INDEX) {
                    compareMove(commandParts[COMMAND_ARG_INDEX].trim());
                } else {
                    System.out.println("Please specify a move to compare.");
                }
                break;
            case "showplayer":
                showPlayer();
                break;
            case "minimax":
                if (commandParts.length > COMMAND_ARG_INDEX) {
                    try {
                        int level = Integer.parseInt(commandParts[COMMAND_ARG_INDEX].trim());
                        Minimax.ValueMove result = new Minimax.ValueMove();
                        Minimax.minimax(board, Integer.MIN_VALUE, Integer.MAX_VALUE, level, result);
                        System.out.println("Best move: " + result.move + " with value: " + result.value);
                    } catch (NumberFormatException e) {
                        System.out.println("Invalid input. Please enter minimax level as an integer (e.g., minimax 3).");
                    } catch (Board.InvalidMoveException e) {
                        System.out.println("Error applying move: " + e.getMessage());
                    }
                } else {
                    System.out.println("Please specify the minimax level (e.g., minimax 3).");
                }
                break;
                case "autoplay":
                    if (commandParts.length > COMMAND_ARG_INDEX) {
                        try {
                            String[] params = commandParts[COMMAND_ARG_INDEX].trim().split("\\s+");
                            int level = Integer.parseInt(params[LEVEL_ARG_INDEX].trim());
                            int moveCount = Integer.parseInt(params[MOVE_COUNT_ARG_INDEX].trim());
                            Minimax.ValueMove result = new Minimax.ValueMove();
                            for (int i = FIRST_INDEX; i < moveCount; i++) {
                                if (board.isGameOver()) break;
                                Minimax.minimax(board, Integer.MIN_VALUE, Integer.MAX_VALUE, level, result);
                                System.out.println("Best move: " + result.move + " with value: " + result.value);
                                if (result.move != null) {
                                    board.applyMove(result.move);
                                }
                            }
                            System.out.println("Game over. Value: " + board.getValue());
                        } catch (NumberFormatException | ArrayIndexOutOfBoundsException e) {
                            System.out.println("Please provide level and move count (e.g., autoplay 3 9).");
                        } catch (Board.InvalidMoveException e) {
                            System.out.println("Error applying move: " + e.getMessage());
                        }
                    } else {
                        System.out.println("Please specify level and move count (e.g., autoplay 3 9).");
                    }
                    break;
            default:
                System.out.println("Unknown command: " + command);
                break;
        }
        return true;
    }

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        
        if (args.length != REQUIRED_MAIN_ARG_COUNT) {
            System.out.println("Usage: java -cp bin Games.BoardDriver <BoardClassName>");
            System.out.println("  TicTacToe:  Games.IVASHCH.TicTacToe.TTTBoard");
            System.out.println("  CenterRush: Games.IVASHCH.CenterRush.CRBoard");
            System.out.println("  Blokus:     Games.IVASHCH.Blokus.BlokusBoard");
            System.out.println("  Beehive:    Games.IVASHCH.Beehive.BeehiveBoard");
            System.exit(FAILURE_EXIT_CODE);
        }

        Board board = null;
        try {
            Class<?> boardClass = Class.forName(args[BOARD_CLASS_ARG_INDEX]);
            board = (Board) boardClass.getDeclaredConstructor().newInstance();
            //System.out.println("Starting game with " + boardClass.getSimpleName());
        } catch (Exception e) {
            System.out.println("Error creating game board: " + e.getMessage());
            System.exit(FAILURE_EXIT_CODE);
        }

        BoardDriver driver = new BoardDriver(board);

        try {
            while (true) {
                String command = scanner.nextLine().trim();
                if (!command.isEmpty() && !driver.handleCommand(command)) {
                    break;
                }
            }
        } finally {
            scanner.close();
        }
    }
}
