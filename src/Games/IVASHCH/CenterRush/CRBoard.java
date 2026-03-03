package Games.IVASHCH.CenterRush;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import Games.Board;

public class CRBoard implements Board {
    public static final int PLAYER_1 = 1;
    public static final int PLAYER_2 = -1;
    public static final int WIN = 1000000;
    public static final int DRAW = 0;
    public static final int SIZE = 5;
    public static final int EMPTY = 0;
    public static final int CENTER_ROW = 3;
    public static final int CENTER_COL = 3;
    
    
    // Piece types
    public static final int QUEEN_1 = 1;    // X
    public static final int DRONE_1 = 2;    // /
    public static final int QUEEN_2 = -1;   // @
    public static final int DRONE_2 = -2;   // o

    // Make board 6x6 to accommodate 1-based indexing
    private int[][] board = new int[SIZE + 1][SIZE + 1];
    private int currentPlayer = PLAYER_1;
    private int moveCount = 0;
    private List<CenterRushMove> validMoves = new ArrayList<>();
    private List<CenterRushMove> movesHistory = new ArrayList<>();

    // Value matrix for board positions (1-based)
    private static final int[][] VALUE_MATRIX = {
        {0, 0, 0, 0, 0, 0},
        {0, 1, 0, 1, 0, 1},
        {0, 0, 2, 2, 2, 0},
        {0, 1, 2, 0, 2, 1},
        {0, 0, 2, 2, 2, 0},
        {0, 1, 0, 1, 0, 1}
    };

    public CRBoard() {
        // Initialize the board with starting positions
        // Player 2 (top)
        board[1][1] = DRONE_2;
        board[1][2] = DRONE_2;
        board[1][3] = QUEEN_2;
        board[1][4] = DRONE_2;
        board[1][5] = DRONE_2;
        
        // Player 1 (bottom)
        board[5][1] = DRONE_1;
        board[5][2] = DRONE_1;
        board[5][3] = QUEEN_1;
        board[5][4] = DRONE_1;
        board[5][5] = DRONE_1;
    }

    @Override
    public Board.Move createMove() {
        return new CenterRushMove();
    }

    @Override
    public void applyMove(Board.Move m) throws Board.InvalidMoveException {
        if (isGameOver()) {
            throw new Board.InvalidMoveException("Game is over");
        }

        CenterRushMove move = (CenterRushMove) m;

        // Check bounds
        if (move.fromRow < 1 || move.fromRow > SIZE || move.fromCol < 1 
            || move.fromCol > SIZE ||
            move.toRow < 1 || move.toRow > SIZE || move.toCol < 1 
            || move.toCol > SIZE) {
            throw new Board.InvalidMoveException("Move out of bounds");
        }

        // Check if source has a piece
        int piece = board[move.fromRow][move.fromCol];
        if (piece == EMPTY) {
            throw new Board.InvalidMoveException(" ");
        }

        // Check if piece belongs to the current player
        if ((currentPlayer == PLAYER_1 && piece <= 0) || 
            (currentPlayer == PLAYER_2 && piece >= 0)) {
            throw new Board.InvalidMoveException("Not your piece");
        }

        // Check if move is in a straight line (horizontal, vertical, or diagonal)
        int rowDiff = move.toRow - move.fromRow;
        int colDiff = move.toCol - move.fromCol;
        
        // For diagonal moves, the absolute differences must be equal
        // For horizontal moves, rowDiff must be 0
        // For vertical moves, colDiff must be 0
        if (!((rowDiff == 0) || // horizontal
              (colDiff == 0) || // vertical
              (Math.abs(rowDiff) == Math.abs(colDiff)))) { // diagonal
            throw new Board.InvalidMoveException(
                "Move must be in a straight line");
        }

        // Check if path is clear
        int rowStep = rowDiff == 0 ? 0 : rowDiff / Math.abs(rowDiff);
        int colStep = colDiff == 0 ? 0 : colDiff / Math.abs(colDiff);
        int currentRow = move.fromRow + rowStep;
        int currentCol = move.fromCol + colStep;
        
        while (currentRow != move.toRow || currentCol != move.toCol) {
            if (board[currentRow][currentCol] != EMPTY) {
                throw new Board.InvalidMoveException(
                    "Path is not clear");
            }
            currentRow += rowStep;
            currentCol += colStep;
        }

        // Check if destination is empty
        if (board[move.toRow][move.toCol] != EMPTY) {
            throw new Board.InvalidMoveException(
                "Destination is not empty");
        }

        // Check if we can stop at the destination (next to wall or another piece)
        boolean canStop = false;
        if (move.toRow == 1 || move.toRow == SIZE || move.toCol == 1 
            || move.toCol == SIZE) {
            canStop = true; // Next to a wall
        } else {
            for (int checkRow = move.toRow - 1; checkRow <= move.toRow + 1; 
                            checkRow++) {
                for (int checkCol = move.toCol - 1; checkCol <= move.toCol + 1;
                             checkCol++) {
                    if (checkRow >= 1 && checkRow <= SIZE && 
                        checkCol >= 1 && checkCol <= SIZE) {
                        if (board[checkRow][checkCol] != EMPTY && 
                            (checkRow != move.fromRow 
                            || checkCol != move.fromCol)) {
                            canStop = true;
                            break;
                        }
                    }
                }
                if (canStop) break;
            }
        }

        if (!canStop) {
            throw new Board.InvalidMoveException
            ("Piece must slide until hitting a wall or another piece");
        }

        // Check center square rules
       /*  if (move.toRow == CENTER_ROW && move.toCol == CENTER_COL) {
            // Only queens (1 or -1) can stop in center if it has a piece infront of it
            
            if (Math.abs(piece) != 1 && board[move.toRow][move.toCol] == EMPTY) { // Only queens (1 or -1) can stop in center
                canStop = false;
                throw new Board.InvalidMoveException("Only queens can stop in center"); 
            }
        } */
        if (move.toRow == CENTER_ROW && move.toCol == CENTER_COL) {
            if (Math.abs(piece) != 1) { // If it's NOT a queen
                throw new Board.InvalidMoveException(" ");
            }
        
            // Check the space beyond the center in the movement direction
            int beyondRow = move.toRow + rowStep;
            int beyondCol = move.toCol + colStep;
        
            if (beyondRow >= 1 && beyondRow <= SIZE && beyondCol >= 1 
                && beyondCol <= SIZE) {
                if (board[beyondRow][beyondCol] == EMPTY) {
                    throw new Board.InvalidMoveException(" ");
                }
            }
        }

        // Apply the move
        board[move.fromRow][move.fromCol] = EMPTY;
        board[move.toRow][move.toCol] = piece;

        // Update move history
        CenterRushMove moveCopy = new CenterRushMove
        (move.fromRow, move.fromCol, move.toRow, move.toCol);

        movesHistory.add(moveCopy);
       // movesHistory.add(move);
        validMoves.remove(move);
        currentPlayer = -currentPlayer;
        moveCount++;
       // move.capturedPiece = capturedPiece;
    }

    @Override
    public List<CenterRushMove> getValidMoves() {
        validMoves.clear();

        // Find all pieces for current player
        for (int fromRow = 1; fromRow <= SIZE; fromRow++) {
            for (int fromCol = 1; fromCol <= SIZE; fromCol++) {
                int piece = board[fromRow][fromCol];
                if ((currentPlayer == PLAYER_1 && piece > 0) || 
                    (currentPlayer == PLAYER_2 && piece < 0)) {

                    // Try all possible directions (horizontal, vertical, and diagonal)
                    int[] directions = {-1, 0, 1};
                    for (int rowDir : directions) {
                        for (int colDir : directions) {
                            if (rowDir == 0 && colDir == 0) continue; // Skip no movement
                            
                            // Skip moves that aren't in a straight line
                            if (!((rowDir == 0) || // horizontal
                                  (colDir == 0) || // vertical
                                  (Math.abs(rowDir) == Math.abs(colDir)))) { // diagonal
                                continue;
                            }

                            int toRow = fromRow;
                            int toCol = fromCol;
                            boolean canMove = true;
                            
                            while (canMove) {
                                toRow += rowDir;
                                toCol += colDir;

                                // Check bounds
                                if (toRow < 1 || toRow > SIZE || toCol < 1 
                                || toCol > SIZE) {

                                    canMove = false;
                                    // If we hit a wall, check if we can stop at the last valid position
                                    toRow -= rowDir;
                                    toCol -= colDir;
                                    break;
                                }

                                // Check if we hit another piece
                                if (board[toRow][toCol] != EMPTY) {
                                    canMove = false;
                                    // Move back one step to the last valid position
                                    toRow -= rowDir;
                                    toCol -= colDir;
                                    break;
                                }
                            }

                            // If we found a valid stopping point, check if we can stop there
                            if (toRow != fromRow || toCol != fromCol) {
                                boolean canStop = false;
                                
                                // Check if next to wall
                                if (toRow == 1 || toRow == SIZE || toCol == 1 
                                || toCol == SIZE) {

                                    canStop = true;
                                }
                                
                                // Check if next to another piece
                                if (!canStop) {
                                    for (int checkRow = toRow - 1; 
                                    checkRow <= toRow + 1; checkRow++) {

                                        for (int checkCol = toCol - 1; 
                                        checkCol <= toCol + 1; checkCol++) {

                                            if (checkRow >= 1 
                                                && checkRow <= SIZE && 
                                                checkCol >= 1 
                                                && checkCol <= SIZE) {
                                                if (board[checkRow][checkCol] 
                                                != EMPTY && 
                                                    (checkRow != fromRow 
                                                    || checkCol != fromCol)) {

                                                    canStop = true;
                                                    break;
                                                }
                                            }
                                        }
                                        if (canStop) break;
                                    }
                                }

                                // Queen can stop in center, but drones cannot
                                if (toRow == CENTER_ROW && toCol == CENTER_COL){
                                    // Math.abs(piece) != 1 means && canMove)
                                    if (Math.abs(piece) != 1 
                                    && board[toRow][toCol] == EMPTY) { // Only queens (1 or -1) can stop in center
                                       
                                        canStop = false;
                                    }
                                }

                                if (canStop) {
                                    validMoves.add(new CenterRushMove
                                    (fromRow, fromCol, toRow, toCol));
                                }
                            }
                        }
                    }
                }
            }
        }

        validMoves.sort(null);
        return validMoves;
    }
    
    

    @Override
    public int getCurrentPlayer() {
        return currentPlayer;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("  1 2 3 4 5 \n");
        
        for (int i = 1; i <= SIZE; i++) {
            sb.append(i).append(" ");
            for (int j = 1; j <= SIZE; j++) {
                switch (board[i][j]) {
                    case QUEEN_1: sb.append("X"); break;
                    case DRONE_1: sb.append("/"); break;
                    case QUEEN_2: sb.append("@"); break;
                    case DRONE_2: sb.append("o"); break;
                    case EMPTY: 
                        if (i == CENTER_ROW && j == CENTER_COL) {
                            sb.append("+");
                        } else {
                            sb.append(".");
                        }
                        break;
                }
                if (j < SIZE) sb.append(" ");
            }
            sb.append("\n");
        }
        return sb.toString();
    }

    @Override
    public List<CenterRushMove> getMoveHistory() {
        return new ArrayList<>(movesHistory);  
        //return movesHistory;
    }

    @Override
    public void undoMove() {
       
       if (!movesHistory.isEmpty()) {
            // Retrieve the last move
            CenterRushMove lastMove = movesHistory.remove(
                movesHistory.size() - 1);
            
          
            // Restore the piece to its original position (fromRow, fromCol)
            board[lastMove.fromRow][lastMove.fromCol] = 
                board[lastMove.toRow][lastMove.toCol];
            //System.out.println("Attempting reverse move: " + lastMove.capturedPiece);

    
            // Clear the target position (toRow, toCol)
            board[lastMove.toRow][lastMove.toCol] = 0; 
    
            // Switch the player back to the previous one
            currentPlayer = -currentPlayer;
            moveCount--;
            validMoves.add(lastMove);
            validMoves.sort(null);
    
        
         }
    }

    public static class CenterRushMove implements Board.Move {
        int fromRow, fromCol, toRow, toCol;

        public int capturedPiece; // Field to store the captured piece, if any

        public CenterRushMove() {}

        public CenterRushMove(int fromRow, int fromCol, int toRow, int toCol){
            this.fromRow = fromRow;
            this.fromCol = fromCol;
            this.toRow = toRow;
            this.toCol = toCol;
        }

        @Override
        public void fromString(String s) throws java.io.IOException {
            String[] parts = s.split("-");
            if (parts.length != 2) throw new java.io.IOException(
                "Invalid move format");
            
            try {
                fromRow = Integer.parseInt(parts[0].substring
                (0, 1));
                fromCol = Integer.parseInt(parts[0].substring
                (1));
                toRow = Integer.parseInt(parts[1].substring
                (0, 1));
                toCol = Integer.parseInt(parts[1].substring
                (1));
            } catch (NumberFormatException e) {
                throw new java.io.IOException(
                    "Invalid number format in move");
            }
        }

        @Override
        public String toString() {
            return String.format("%d%d-%d%d", 
                fromRow, fromCol, 
                toRow, toCol);
        }

        @Override
        public int compareTo(Board.Move o) {
            CenterRushMove other = (CenterRushMove) o;
            if (this.fromRow != other.fromRow)
                return Integer.compare(this.fromRow, other.fromRow);
            if (this.fromCol != other.fromCol) 
                return Integer.compare(this.fromCol, other.fromCol);
            if (this.toRow != other.toRow) 
                return Integer.compare(this.toRow, other.toRow);
            return Integer.compare(this.toCol, other.toCol);
        }
    }

    public boolean isGameOver() {
        // Check if either queen is in the center
        if (board[CENTER_ROW][CENTER_COL] == QUEEN_1 || 
            board[CENTER_ROW][CENTER_COL] == QUEEN_2) {
            return true;
        }
        
        // Check if there are any valid moves left
        return getValidMoves().isEmpty();
    }

    @Override
    public int getValue() {
        // Check for win condition (queen in center)
        if (board[CENTER_ROW][CENTER_COL] == QUEEN_1) return WIN;
        if (board[CENTER_ROW][CENTER_COL] == QUEEN_2) return -WIN;

        // Calculate value based on queen positions
        int value = 0;
        for (int i = 1; i <= SIZE; i++) {
            for (int j = 1; j <= SIZE; j++) {
                if (board[i][j] == QUEEN_1) {
                    value += VALUE_MATRIX[i][j];
                } else if (board[i][j] == QUEEN_2) {
                    value -= VALUE_MATRIX[i][j];
                }
            }
        }
        return value;
    }

    public void saveBoard(String fileName) {
        try (BufferedWriter writer = new BufferedWriter
        (new FileWriter(fileName))) {
            writer.write(String.valueOf(movesHistory.size()));
            writer.newLine();
            
            for (CenterRushMove move : movesHistory) {
                writer.write(move.toString());
                writer.newLine();
            }
            //System.out.println("Board saved successfully to " + fileName);
        } catch (IOException e) {
           // System.out.println("Error saving the board: " + e.getMessage());
        }
    }

    public void loadBoard(String fileName) {
        try (BufferedReader reader = new BufferedReader
        (new FileReader(fileName))) {
            // Reset the board to initial state
            for (int i = 1; i <= SIZE; i++) {
                for (int j = 1; j <= SIZE; j++) {
                    board[i][j] = EMPTY;
                }
            }
            
            // Set up initial positions
            // Player 2 (top)
            board[1][1] = DRONE_2;
            board[1][2] = DRONE_2;
            board[1][3] = QUEEN_2;
            board[1][4] = DRONE_2;
            board[1][5] = DRONE_2;
            
            // Player 1 (bottom)
            board[5][1] = DRONE_1;
            board[5][2] = DRONE_1;
            board[5][3] = QUEEN_1;
            board[5][4] = DRONE_1;
            board[5][5] = DRONE_1;
            
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
                CenterRushMove move = new CenterRushMove();
                move.fromString(line);
                try {
                    applyMove(move);
                } catch (InvalidMoveException e) {
                    System.out.println(
                        "Error loading move " + line + ": " + e.getMessage());
                    // Reset the board to initial state
                    loadBoard(fileName);
                    return;
                }
            }
            //System.out.println("Board loaded successfully from " + fileName);
        } catch (IOException | NumberFormatException e) {
            System.out.println("Error loading the board: " + e.getMessage());
        }
    }
} 