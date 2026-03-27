package Games.IVASHCH.TicTacToe;
import Games.Board;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.io.BufferedWriter;
import java.io.BufferedReader;
import java.io.FileWriter;
import java.io.FileReader;
import java.io.IOException;


public class TTTBoard implements Board {
    public static final int PLAYER_1 = 1;
    public static final int PLAYER_2 = -1;
    public static final int WIN = 1000000;
    public static final int DRAW = 0;
    public static final int SIZE = 3;
    public static final int EMPTY = 0;
   
    // Make board 4x4 to accommodate 1-based indexing
    private int[][] board = new int[SIZE + 1][SIZE + 1];
    private int currentPlayer = PLAYER_1;
    private int value = 0;
    private int moveCount = 0;
    private int lastMoveRow = -1;
    private int lastMoveCol = -1;
    private List<TTTMove> validMoves = new ArrayList<>();
    private List<TTTMove> movesHistory = new ArrayList<>();

    // The TTTMove class implements the Board.Move interface
    public static class TTTMove implements Board.Move {
        int row, col;

        public TTTMove() {}

        public TTTMove(int row, int col) {
            this.row = row;
            this.col = col;
        }

        @Override
        public void fromString(String s) throws java.io.IOException {
            String[] parts = s.split("[ ,]+");
            if (parts.length != 2) throw new java.io.IOException("Invalid move format");
            try {
                row = Integer.parseInt(parts[0].trim());
                col = Integer.parseInt(parts[1].trim());
            } catch (NumberFormatException e) {
                throw new java.io.IOException("Invalid number format in move");
            }
        }

        @Override
        public String toString() {
            return row + "," + col;
        }

        @Override
        public int compareTo(Board.Move o) {
            TTTMove other = (TTTMove) o;
            if (this.row != other.row) return Integer.compare(this.row, other.row);
            return Integer.compare(this.col, other.col);
        }
    }

    @Override
    public int getCurrentPlayer() {
        return currentPlayer;
    }

    public TTTBoard() {
        // Initialize the board with empty values (0)
        for (int i = 1; i <= SIZE; i++) {
            for (int j = 1; j <= SIZE; j++) {
                board[i][j] = EMPTY;
            }
        }
    }
    
    @Override
    public Board.Move createMove() {
        return new TTTMove();  // Create a new TTTMove instance
    }

    @Override
    public void applyMove(Board.Move m) throws Board.InvalidMoveException {
        if (isGameOver()) {
            throw new Board.InvalidMoveException("Game is over");
        }
        TTTMove move = (TTTMove) m;
        if (move.row < 1 || move.row > SIZE || move.col < 1 || move.col > SIZE) {
            System.out.println("Not a permitted move.");
            return;
        }
        if (board[move.row][move.col] != EMPTY) {
            throw new Board.InvalidMoveException("Cell is already occupied");
        }
        board[move.row][move.col] = currentPlayer;
        movesHistory.add(new TTTMove(move.row, move.col));
        validMoves.remove(move);
        currentPlayer = -currentPlayer;  // Switch to the other player
        moveCount++;
    }

    @Override
    public List<TTTMove> getValidMoves() {
        validMoves.clear();
        for (int i = 1; i <= SIZE; i++) {
            for (int j = 1; j <= SIZE; j++) {
                if (board[i][j] == EMPTY) {
                    validMoves.add(new TTTMove(i, j));
                }
            }
        } 
        return validMoves;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (int i = 1; i <= SIZE; i++) {
            for (int j = 1; j <= SIZE; j++) {
                sb.append(board[i][j] == EMPTY ? " " : (board[i][j] == PLAYER_1 ? "X" : "O"));
                if (j < SIZE) sb.append("|");
            }
            if (i < SIZE) sb.append("\n-----\n");
        }
        return sb.toString();
    }

    @Override
    public List<TTTMove> getMoveHistory() {
        return movesHistory;
    }

    @Override
    public void undoMove() {
        if (!movesHistory.isEmpty()) {
            TTTMove lastMove = movesHistory.remove(movesHistory.size() - 1);
            board[lastMove.row][lastMove.col] = EMPTY;  // Undo the move
            currentPlayer = -currentPlayer;  // Switch back to the previous player
            moveCount--;
            validMoves.add(lastMove);  // Add the move back to the list of valid moves
            validMoves.sort(null);  // Sort the list of valid moves
        }
    }

    public boolean isGameOver() {
        // First check for a win
        // Check rows
        for (int i = 1; i <= SIZE; i++) {
            if (board[i][1] != EMPTY && board[i][1] == board[i][2] && board[i][2] == board[i][3]) {
                return true;
            }
        }
        
        // Check columns
        for (int i = 1; i <= SIZE; i++) {
            if (board[1][i] != EMPTY && board[1][i] == board[2][i] && board[2][i] == board[3][i]) {
                return true;
            }
        }
        
        // Check diagonals
        if (board[1][1] != EMPTY && board[1][1] == board[2][2] && board[2][2] == board[3][3]) {
            return true;
        }
        if (board[1][3] != EMPTY && board[1][3] == board[2][2] && board[2][2] == board[3][1]) {
            return true;
        }
        
        // Then check for a draw (board full)
        boolean isFull = true;
        for (int i = 1; i <= SIZE; i++) {
            for (int j = 1; j <= SIZE; j++) {
                if (board[i][j] == EMPTY) {
                    isFull = false;
                    break;
                }
            }
            if (!isFull) break;
        }
        
        return isFull;
    }

    @Override
    public int getValue() {
        // Check rows
        for (int i = 1; i <= SIZE; i++) {
            if (board[i][1] != EMPTY && board[i][1] == board[i][2] && board[i][2] == board[i][3]) {
                return board[i][1] == PLAYER_1 ? WIN : -WIN;
            }
        }
        
        // Check columns
        for (int i = 1; i <= SIZE; i++) {
            if (board[1][i] != EMPTY && board[1][i] == board[2][i] && board[2][i] == board[3][i]) {
                return board[1][i] == PLAYER_1 ? WIN : -WIN;
            }
        }
        
        // Check diagonals
        if (board[1][1] != EMPTY && board[1][1] == board[2][2] && board[2][2] == board[3][3]) {
            return board[1][1] == PLAYER_1 ? WIN : -WIN;
        }
        if (board[1][3] != EMPTY && board[1][3] == board[2][2] && board[2][2] == board[3][1]) {
            return board[1][3] == PLAYER_1 ? WIN : -WIN;
        }
        
        // If no win, check for draw
        if (isGameOver()) {
            return DRAW;
        }
        
        // If game is still ongoing, calculate heuristic value
        int value = 0;
        
        // Check for two-in-a-row situations
        // Rows
        for (int i = 1; i <= SIZE; i++) {
            if (board[i][1] != EMPTY && board[i][1] == board[i][2] && board[i][3] == EMPTY) value += (board[i][1] == PLAYER_1 ? 1 : -1);
            if (board[i][2] != EMPTY && board[i][2] == board[i][3] && board[i][1] == EMPTY) value += (board[i][2] == PLAYER_1 ? 1 : -1);
            if (board[i][1] != EMPTY && board[i][1] == board[i][3] && board[i][2] == EMPTY) value += (board[i][1] == PLAYER_1 ? 1 : -1);
        }
        
        // Columns
        for (int i = 1; i <= SIZE; i++) {
            if (board[1][i] != EMPTY && board[1][i] == board[2][i] && board[3][i] == EMPTY) value += (board[1][i] == PLAYER_1 ? 1 : -1);
            if (board[2][i] != EMPTY && board[2][i] == board[3][i] && board[1][i] == EMPTY) value += (board[2][i] == PLAYER_1 ? 1 : -1);
            if (board[1][i] != EMPTY && board[1][i] == board[3][i] && board[2][i] == EMPTY) value += (board[1][i] == PLAYER_1 ? 1 : -1);
        }
        
        // Diagonals
        if (board[1][1] != EMPTY && board[1][1] == board[2][2] && board[3][3] == EMPTY) value += (board[1][1] == PLAYER_1 ? 1 : -1);
        if (board[2][2] != EMPTY && board[2][2] == board[3][3] && board[1][1] == EMPTY) value += (board[2][2] == PLAYER_1 ? 1 : -1);
        if (board[1][1] != EMPTY && board[1][1] == board[3][3] && board[2][2] == EMPTY) value += (board[1][1] == PLAYER_1 ? 1 : -1);
        
        if (board[1][3] != EMPTY && board[1][3] == board[2][2] && board[3][1] == EMPTY) value += (board[1][3] == PLAYER_1 ? 1 : -1);
        if (board[2][2] != EMPTY && board[2][2] == board[3][1] && board[1][3] == EMPTY) value += (board[2][2] == PLAYER_1 ? 1 : -1);
        if (board[1][3] != EMPTY && board[1][3] == board[3][1] && board[2][2] == EMPTY) value += (board[1][3] == PLAYER_1 ? 1 : -1);
        
        return value;
    }
    
    public boolean checkWin() {
        // Check rows
        for (int i = 0; i < SIZE; i++) {
            if (board[i][0] != EMPTY && board[i][0] == board[i][1] && board[i][1] == board[i][2]) {
                return true; // Player wins by row
            }
        }
    
        // Check columns
        for (int i = 0; i < SIZE; i++) {
            if (board[0][i] != EMPTY && board[0][i] == board[1][i] && board[1][i] == board[2][i]) {
                return true; // Player wins by column
            }
        }
    
        // Check diagonals
        if (board[0][0] != EMPTY && board[0][0] == board[1][1] && board[1][1] == board[2][2]) {
            return true; // Player wins by diagonal
        }
        if (board[0][2] != EMPTY && board[0][2] == board[1][1] && board[1][1] == board[2][0]) {
            return true; // Player wins by diagonal
        }
    
        return false; // No winner yet
    }
    public boolean checkGameOver() {
        // Check for win conditions
        if (checkWin()) {
            System.out.println("Player " + (currentPlayer == PLAYER_1 ? "1" : "2") + " wins!");
            return true;
        }
    
        // Check for draw condition (no moves left)
        if (getValidMoves().isEmpty()) {
            System.out.println("It's a draw!");
            return true;
        }
    
        return false; // Game is still ongoing
    }
    public void saveBoard(String fileName) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(fileName))) {
            writer.write(String.valueOf(movesHistory.size()));
            writer.newLine();
            
            for (TTTMove move : movesHistory) {
                writer.write(move.row + "," + move.col);
                writer.newLine();
            }
            System.out.println("Board saved successfully to " + fileName);
        } catch (IOException e) {
            System.out.println("Error saving the board: " + e.getMessage());
        }
    }

    public void loadBoard(String fileName) {
        try (BufferedReader reader = new BufferedReader(new FileReader(fileName))) {
            // Reset the board to initial state
            for (int i = 1; i <= SIZE; i++) {
                for (int j = 1; j <= SIZE; j++) {
                    board[i][j] = EMPTY;
                }
            }
            movesHistory.clear();
            validMoves.clear();
            currentPlayer = PLAYER_1;
            moveCount = 0;

            // Read number of moves
            String line = reader.readLine();
            int numMoves = Integer.parseInt(line);

            // Read and apply each move
            for (int i = 0; i < numMoves; i++) {
                line = reader.readLine();
                String[] parts = line.split(",");
                int row = Integer.parseInt(parts[0]);
                int col = Integer.parseInt(parts[1]);
                
                TTTMove move = new TTTMove(row, col);
                try {
                    applyMove(move);
                } catch (InvalidMoveException e) {
                    System.out.println("Error loading move (" + row + "," + col + "): " + e.getMessage());
                    // Reset the board since we encountered an invalid state
                    for (int r = 1; r <= SIZE; r++) {
                        for (int c = 1; c <= SIZE; c++) {
                            board[r][c] = EMPTY;
                        }
                    }
                    movesHistory.clear();
                    validMoves.clear();
                    currentPlayer = PLAYER_1;
                    moveCount = 0;
                    throw new IOException("Invalid move in save file");
                }
            }
            System.out.println("Board loaded successfully from " + fileName);
        } catch (IOException | NumberFormatException e) {
            System.out.println("Error loading the board: " + e.getMessage());
        }
    }
}