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

    public BoardDriver(Board board) {
        this.board = board;
        this.currentMove = board.createMove();
    }

    public void testPlay(int seed, int moveCount) {
        Random rnd = new Random(seed);
        for (int i = 0; i < moveCount; i++) {
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
        int stepsMade = 0;
        while (stepsMade < stepCount) {
            List<? extends Board.Move> validMoves = board.getValidMoves();
            List<? extends Board.Move> moveHist = board.getMoveHistory();
            if (validMoves.isEmpty() || board.getValue() == Board.WIN || board.getValue() == -Board.WIN || board.isGameOver()) {
                if (!moveHist.isEmpty()) {
                    int revertMovesCount = rnd.nextInt(moveHist.size()) + 1;
                    for (int i = 0; i < revertMovesCount && !board.getMoveHistory().isEmpty(); i++) {
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
            if (comparison < 0) {
                System.out.println("Current move is less");
            } else if (comparison > 0) {
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
            System.out.println("No valid moves available.");
            return;
        }

        int maxMoveLength = moves.stream().mapToInt(m -> m.toString().length()).max().orElse(0);
        int columns = MAX_LINE_LENGTH / (maxMoveLength + 1);

        for (int i = 0; i < moves.size(); i++) {
            System.out.printf("%-" + (maxMoveLength + 1) + "s", moves.get(i).toString());
            if ((i + 1) % columns == 0 || i == moves.size() - 1) {
                System.out.println();
            }
        }
    }

    public void enterMove(String moveString) {
        try {
            currentMove.fromString(moveString);
        } catch (IOException e) {
            System.out.println("Invalid move format: " + e.getMessage());
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
        enterMove(moveString);
        applyMove();
    }

    public void undoMoves(int count) {
        int toUndo = Math.min(count, board.getMoveHistory().size());
        for (int i = 0; i < toUndo; i++) {
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
        final int COL_WIDTH = 40;
        final int COLS = 2;
        for (int i = 0; i < history.size(); i++) {
            System.out.printf("%-" + COL_WIDTH + "s", history.get(i).toString());
            if ((i + 1) % COLS == 0 || i == history.size() - 1) {
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
            this.currentMove = newBoard.createMove();
        } catch (IOException e) {
            System.out.println("Error loading board: " + e.getMessage());
        } catch (ReflectiveOperationException e) {
            System.out.println("Error loading board: " + e.getMessage());
        }
    }

    /** Returns false to quit, true to continue. */
    public boolean handleCommand(String command) {
        String[] commandParts = command.split(" ", 2);
        String cmd = commandParts[0].toLowerCase();

        switch (cmd) {
            case "showboard":
                showBoard();
                break;
            case "showmoves":
                showMoves();
                break;
            case "entermove":
                if (commandParts.length > 1) {
                    enterMove(commandParts[1].trim());
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
                if (commandParts.length > 1) {
                    doMove(commandParts[1].trim());
                } else {
                    System.out.println("Invalid move format.");
                }
                break;
            case "undomoves":
                if (commandParts.length > 1) {
                    try {
                        int count = Integer.parseInt(commandParts[1]);
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
                if (commandParts.length > 1) {
                    saveBoard(commandParts[1].trim());
                } else {
                    System.out.println("Please specify a filename.");
                }
                break;
            case "loadboard":
                if (commandParts.length > 1) {
                    loadBoard(commandParts[1].trim());
                } else {
                    System.out.println("Please specify a filename.");
                }
                break;
            case "quit":
                return false;
            case "testplay":
                if (commandParts.length > 1) {
                    try {
                        String[] params = commandParts[1].split(" ");
                        int seed = Integer.parseInt(params[0].trim());
                        int moveCount = Integer.parseInt(params[1].trim());
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
                if (commandParts.length > 1) {
                    try {
                        String[] params = commandParts[1].split(" ");
                        int seed = Integer.parseInt(params[0].trim());
                        int stepCount = Integer.parseInt(params[1].trim());
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
                if (commandParts.length > 1) {
                    compareMove(commandParts[1].trim());
                } else {
                    System.out.println("Please specify a move to compare.");
                }
                break;
            case "showplayer":
                showPlayer();
                break;
            default:
                System.out.println("Unknown command: " + command);
                break;
        }
        return true;
    }

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        
        if (args.length != 1) {
            System.out.println("Usage: java -cp bin Games.BoardDriver <BoardClassName>");
            System.out.println("  TicTacToe:  Games.IVASHCH.TicTacToe.TTTBoard");
            System.out.println("  CenterRush: Games.IVASHCH.CenterRush.CRBoard");
            System.exit(1);
        }

        Board board = null;
        try {
            Class<?> boardClass = Class.forName(args[0]);
            board = (Board) boardClass.getDeclaredConstructor().newInstance();
            //System.out.println("Starting game with " + boardClass.getSimpleName());
        } catch (Exception e) {
            System.out.println("Error creating game board: " + e.getMessage());
            System.exit(1);
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
