package Games.IVASHCH.Blokus;

import Games.Board;
import java.util.*;
import java.io.*;

public class BlokusBoard implements Board {
    public static final int SIZE = 14;
    public static final int NUM_PIECES = 21;
    public static final int EMPTY = 0;
    public static final int NUM_ORIENTATIONS = 8;
    public static final int SQUARE_VALUE = 50;
    public static final int CONSECUTIVE_PASS_LIMIT = 2;

    public static final int START_R1 = 5;
    public static final int START_C1 = 5;
    public static final int START_R2 = 10;
    public static final int START_C2 = 10;
    public static final int COL_LABEL_WRAP = 10;

    static final int ROT_270 = 3;
    static final int MIR_0 = 4;
    static final int MIR_90 = 5;
    static final int MIR_180 = 6;
    static final int MIR_270 = 7;

    static final String[] ORI_TOKENS = 
    {"^", ">", "v", "<", "^|", ">|", "v|", "<|"};

    static final String[] PIECE_NAMES = {
        "I1", "I2", "I3", "L3", "I4", "L4", "O4", "S4", "T4",
        "I5", "L5", "P5", "R5", "S5", "T5", 
        "U5", "V5", "W5", "X5", "Y5", "Z5"
    };

    static final Map<String, Integer> NAME_TO_INDEX = new HashMap<>();

    static final int[][][] PIECE_BASE = {
        {{0,0}},                                          // I1 = 0
        {{0,0},{1,0}},                                    // I2 = 1
        {{0,0},{1,0},{2,0}},                              // I3 = 2
        {{0,0},{1,0},{1,1}},                              // L3 = 3
        {{0,0},{1,0},{2,0},{3,0}},                        // I4 = 4
        {{0,0},{1,0},{2,0},{2,1}},                        // L4 = 5
        {{0,0},{0,1},{1,0},{1,1}},                        // O4 = 6
        {{0,1},{0,2},{1,0},{1,1}},                        // S4 = 7
        {{0,0},{0,1},{0,2},{1,1}},                        // T4 = 8
        {{0,0},{1,0},{2,0},{3,0},{4,0}},                  // I5 = 9
        {{0,0},{1,0},{2,0},{3,0},{3,1}},                  // L5 = 10
        {{0,0},{0,1},{1,0},{1,1},{2,0}},                  // P5 = 11
        {{0,1},{0,2},{1,0},{1,1},{2,1}},                  // R5 = 12
        {{0,2},{0,3},{1,0},{1,1},{1,2}},                  // S5 = 13
        {{0,0},{0,1},{0,2},{1,1},{2,1}},                  // T5 = 14
        {{0,0},{0,2},{1,0},{1,1},{1,2}},                  // U5 = 15
        {{0,0},{1,0},{2,0},{2,1},{2,2}},                  // V5 = 16
        {{0,0},{1,0},{1,1},{2,1},{2,2}},                  // W5 = 17
        {{0,1},{1,0},{1,1},{1,2},{2,1}},                  // X5 = 18
        {{0,1},{1,0},{1,1},{2,1},{3,1}},                  // Y5 = 19
        {{0,0},{0,1},{1,1},{2,1},{2,2}}                   // Z5 = 20
    };

    static int[] PIECE_SIZES;
    static int[][][][] CANONICAL_CELLS;
    static int[][] CANONICAL_TOKENS;
    static int[] NUM_CANONICAL;
    static int[][] TOKEN_TO_CANONICAL;

    static {
        for (int i = 0; i < PIECE_NAMES.length; i++)
            NAME_TO_INDEX.put(PIECE_NAMES[i], i);

        PIECE_SIZES = new int[NUM_PIECES];
        CANONICAL_CELLS = new int[NUM_PIECES][][][];
        CANONICAL_TOKENS = new int[NUM_PIECES][];
        NUM_CANONICAL = new int[NUM_PIECES];
        TOKEN_TO_CANONICAL = new int[NUM_PIECES][NUM_ORIENTATIONS];

        for (int p = 0; p < NUM_PIECES; p++) {
            int[][] base = PIECE_BASE[p];
            PIECE_SIZES[p] = base.length;

            int maxR = 0, maxC = 0;
            for (int[] cell : base) {
                maxR = Math.max(maxR, cell[0]);
                maxC = Math.max(maxC, cell[1]);
            }

            int[][][] allTrans = new int[NUM_ORIENTATIONS][][];
            String[] allKeys = new String[NUM_ORIENTATIONS];

            for (int t = 0; t < NUM_ORIENTATIONS; t++) {
                int[][] tc = new int[base.length][2];
                for (int i = 0; i < base.length; i++) {
                    int r = base[i][0], c = base[i][1];
                    switch (t) {
                        case 0: tc[i][0] = r;     
                           tc[i][1] = c;        break;
                        case 1: tc[i][0] = c;    
                            tc[i][1] = maxR - r; break;
                        case 2: tc[i][0] = maxR - r; 
                            tc[i][1] = maxC - c; break;
                        case ROT_270: tc[i][0] = maxC - c; 
                            tc[i][1] = r;        break;
                        case MIR_0:   tc[i][0] = r;    
                            tc[i][1] = maxC - c; break;
                        case MIR_90:  tc[i][0] = c;     
                           tc[i][1] = r;        break;
                        case MIR_180: tc[i][0] = maxR - r;
                             tc[i][1] = c;        break;
                        case MIR_270: tc[i][0] = maxC - c; 
                            tc[i][1] = maxR - r; break;
                    }
                }

                int minR2 = Integer.MAX_VALUE, minC2 = Integer.MAX_VALUE;
                for (int[] cell : tc) {
                    minR2 = Math.min(minR2, cell[0]);
                    minC2 = Math.min(minC2, cell[1]);
                }
                for (int[] cell : tc) {
                    cell[0] -= minR2;
                    cell[1] -= minC2;
                }

                Arrays.sort(tc, (a, b) -> a[0] != b[0] 
                ? a[0] - b[0] : a[1] - b[1]);
                allTrans[t] = tc;
                allKeys[t] = Arrays.deepToString(tc);
            }

            List<int[][]> cList = new ArrayList<>();
            List<Integer> tList = new ArrayList<>();
            Map<String, Integer> seen = new LinkedHashMap<>();

            for (int t = 0; t < NUM_ORIENTATIONS; t++) {
                if (!seen.containsKey(allKeys[t])) {
                    seen.put(allKeys[t], cList.size());
                    cList.add(allTrans[t]);
                    tList.add(t);
                }
                TOKEN_TO_CANONICAL[p][t] = seen.get(allKeys[t]);
            }

            NUM_CANONICAL[p] = cList.size();
            CANONICAL_CELLS[p] = cList.toArray(new int[0][][]);
            CANONICAL_TOKENS[p] = new int[tList.size()];
            for (int i = 0; i < tList.size(); i++)
                CANONICAL_TOKENS[p][i] = tList.get(i);
        }
    }

    private int[][] board = new int[SIZE + 1][SIZE + 1];
    private int currentPlayer = PLAYER_1;
    private boolean[][] hands = new boolean[2][NUM_PIECES];
    private int consecutivePasses = 0;
    private List<BlokusMove> movesHistory = new ArrayList<>();

    public BlokusBoard() {
        for (int i = 0; i < NUM_PIECES; i++) {
            hands[0][i] = true;
            hands[1][i] = true;
        }
    }

    public static class BlokusMove implements Board.Move {
        int pieceIndex;
        int oriIndex;
        int row, col;
        boolean isPass;
        int prevPassCount;

        public BlokusMove() {}

        BlokusMove(int p, int o, int r, int c) {
            pieceIndex = p;
            oriIndex = o;
            row = r;
            col = c;
        }

        static BlokusMove pass() {
            BlokusMove m = new BlokusMove();
            m.isPass = true;
            return m;
        }

        @Override
        public void fromString(String s) throws IOException {
            s = s.trim();
            if (s.equalsIgnoreCase("pass")) {
                isPass = true;
                return;
            }
            isPass = false;

            int po = s.indexOf('(');
            int pc = s.indexOf(')');
            if (po < 0 || pc < 0)
                throw new IOException("Invalid move format");

            String before = s.substring(0, po).trim();
            String coords = s.substring(po + 1, pc).trim();

            int sp = before.indexOf(' ');
            if (sp < 0)
                throw new IOException("Invalid move format");

            String name = before.substring(0, sp);
            String ori = before.substring(sp + 1).trim();

            Integer pIdx = NAME_TO_INDEX.get(name);
            if (pIdx == null)
                throw new IOException("Unknown piece: " + name);
            pieceIndex = pIdx;

            int rawTok = -1;
            for (int i = 0; i < ORI_TOKENS.length; i++)
                if (ORI_TOKENS[i].equals(ori)) { rawTok = i; break; }
            if (rawTok < 0)
                throw new IOException("Unknown orientation: " + ori);

            oriIndex = TOKEN_TO_CANONICAL[pieceIndex][rawTok];

            String[] cp = coords.split(",");
            if (cp.length != 2)
                throw new IOException("Invalid coordinates");
            row = Integer.parseInt(cp[0].trim());
            col = Integer.parseInt(cp[1].trim());
        }

        @Override
        public String toString() {
            if (isPass) return "pass";
            if (pieceIndex < 0 || pieceIndex >= NUM_PIECES || oriIndex < 0)
                return "";
            return PIECE_NAMES[pieceIndex] + " "
                + ORI_TOKENS[CANONICAL_TOKENS[pieceIndex][oriIndex]]
                + " (" + row + "," + col + ")";
        }

        @Override
        public int compareTo(Board.Move o) {
            BlokusMove other = (BlokusMove) o;
            if (this.isPass && other.isPass) return 0;
            if (this.isPass) return 1;
            if (other.isPass) return -1;

            int cmp = Integer.compare(other.pieceIndex, this.pieceIndex);
            if (cmp != 0) return cmp;

            cmp = Integer.compare(
                CANONICAL_TOKENS[this.pieceIndex][this.oriIndex],
                CANONICAL_TOKENS[other.pieceIndex][other.oriIndex]);
            if (cmp != 0) return cmp;

            if (this.row != other.row) 
                return Integer.compare(this.row, other.row);
            return Integer.compare(this.col, other.col);
        }
    }

    @Override
    public Board.Move createMove() { return new BlokusMove(); }

    @Override
    public int getCurrentPlayer() { return currentPlayer; }

    @Override
    public boolean isGameOver() { 
        return consecutivePasses >= CONSECUTIVE_PASS_LIMIT; }

    @Override
    public List<BlokusMove> getMoveHistory() { 
        return new ArrayList<>(movesHistory); }

    @Override
    public void applyMove(Board.Move m) throws InvalidMoveException {
        if (isGameOver())
            throw new InvalidMoveException("Game is over");

        BlokusMove move = (BlokusMove) m;
        int hi = currentPlayer == PLAYER_1 ? 0 : 1;

        BlokusMove hist = new BlokusMove();
        hist.prevPassCount = consecutivePasses;

        if (move.isPass) {
            hist.isPass = true;
            consecutivePasses++;
            currentPlayer = -currentPlayer;
            movesHistory.add(hist);
            return;
        }

        if (move.pieceIndex < 0 || move.pieceIndex >= NUM_PIECES)
            throw new InvalidMoveException("Invalid piece");
        if (!hands[hi][move.pieceIndex])
            throw new InvalidMoveException("Piece not in hand");

        int[][] cells = CANONICAL_CELLS[move.pieceIndex][move.oriIndex];

        for (int[] cell : cells) {
            int r = move.row + cell[0], c = move.col + cell[1];
            if (r < 1 || r > SIZE || c < 1 || c > SIZE)
                throw new InvalidMoveException("Out of bounds");
            if (board[r][c] != EMPTY)
                throw new InvalidMoveException("Overlap");
        }

        for (int[] cell : cells)
            board[move.row + cell[0]][move.col + cell[1]] = currentPlayer;

        hands[hi][move.pieceIndex] = false;

        hist.isPass = false;
        hist.pieceIndex = move.pieceIndex;
        hist.oriIndex = move.oriIndex;
        hist.row = move.row;
        hist.col = move.col;

        consecutivePasses = 0;
        currentPlayer = -currentPlayer;
        movesHistory.add(hist);
    }

    @Override
    public void undoMove() {
        if (movesHistory.isEmpty()) return;

        BlokusMove last = movesHistory.remove(movesHistory.size() - 1);
        currentPlayer = -currentPlayer;
        consecutivePasses = last.prevPassCount;

        if (!last.isPass) {
            int[][] cells = CANONICAL_CELLS[last.pieceIndex][last.oriIndex];
            for (int[] cell : cells)
                board[last.row + cell[0]][last.col + cell[1]] = EMPTY;
            int hi = currentPlayer == PLAYER_1 ? 0 : 1;
            hands[hi][last.pieceIndex] = true;
        }
    }

    @Override
    public List<BlokusMove> getValidMoves() {
        if (isGameOver()) return new ArrayList<>();

        List<BlokusMove> moves = new ArrayList<>();
        int hi = currentPlayer == PLAYER_1 ? 0 : 1;
        boolean first = isFirstMove(currentPlayer);

        for (int p = NUM_PIECES - 1; p >= 0; p--) {
            if (!hands[hi][p]) continue;

            for (int oi = 0; oi < NUM_CANONICAL[p]; oi++) {
                int[][] cells = CANONICAL_CELLS[p][oi];
                int maxDr = 0, maxDc = 0;
                for (int[] cell : cells) {
                    maxDr = Math.max(maxDr, cell[0]);
                    maxDc = Math.max(maxDc, cell[1]);
                }

                for (int ar = 1; ar <= SIZE - maxDr; ar++)
                    for (int ac = 1; ac <= SIZE - maxDc; ac++)
                        if (isLegalPlacement
                            (cells, ar, ac, currentPlayer, first))
                            moves.add(new BlokusMove(p, oi, ar, ac));
            }
        }

        if (moves.isEmpty())
            moves.add(BlokusMove.pass());

        return moves;
    }

    @Override
    public int getValue() {
        int xSq = 0, oSq = 0;
        for (int r = 1; r <= SIZE; r++)
            for (int c = 1; c <= SIZE; c++) {
                if (board[r][c] == PLAYER_1) xSq++;
                else if (board[r][c] == PLAYER_2) oSq++;
            }

        if (isGameOver()) {
            if (xSq > oSq) return WIN;
            if (oSq > xSq) return -WIN;
            return 0;
        }

        int xMob = computeMobility(PLAYER_1);
        int oMob = computeMobility(PLAYER_2);
        return SQUARE_VALUE * xSq + xMob - SQUARE_VALUE * oSq - oMob;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        sb.append("   ");
        for (int c = 1; c <= SIZE; c++) {
            if (c == COL_LABEL_WRAP) sb.append("t");
            else if (c > COL_LABEL_WRAP) sb.append(c - COL_LABEL_WRAP);
            else sb.append(c);
            if (c < SIZE) sb.append(" ");
        }
        sb.append("\n");

        for (int r = 1; r <= SIZE; r++) {
            sb.append(String.format("%2d", r));
            for (int c = 1; c <= SIZE; c++) {
                sb.append(" ");
                if (board[r][c] == PLAYER_1) sb.append("X");
                else if (board[r][c] == PLAYER_2) sb.append("O");
                else if ((r == START_R1 && c == START_C1) 
                    || (r == START_R2 && c == START_C2)) sb.append("s");
                else sb.append(".");
            }
            if (r == SIZE - 1) {
                sb.append(" X's hand:");
                for (int p = 0; p < NUM_PIECES; p++)
                    if (hands[0][p]) sb.append(" ").append(PIECE_NAMES[p]);
            }
            if (r == SIZE) {
                sb.append(" O's hand:");
                for (int p = 0; p < NUM_PIECES; p++)
                    if (hands[1][p]) sb.append(" ").append(PIECE_NAMES[p]);
            }
            sb.append(" \n");
        }
        return sb.toString();
    }

    // ---- Helpers ----

    private boolean isFirstMove(int player) {
        int hi = player == PLAYER_1 ? 0 : 1;
        boolean allInHand = true;
        for (int i = 0; i < NUM_PIECES; i++)
            if (!hands[hi][i]) { allInHand = false; break; }
        if (allInHand) return true;
        int startR = player == PLAYER_1 ? START_R2 : START_R1;
        int startC = player == PLAYER_1 ? START_C2 : START_C1;
        return board[startR][startC] == EMPTY;
    }

    private boolean isLegalPlacement(int[][] cells, int ar, int ac,
                                     int player, boolean firstMove) {
        
        boolean coversStart = false;
        boolean hasDiag = false;

        for (int[] cell : cells) {
            int r = ar + cell[0], c = ac + cell[1];

            if (r < 1 || r > SIZE || c < 1 || c > SIZE) return false;
            if (board[r][c] != EMPTY) return false;

            if (r > 1    && board[r - 1][c] == player) return false;
            if (r < SIZE && board[r + 1][c] == player) return false;
            if (c > 1    && board[r][c - 1] == player) return false;
            if (c < SIZE && board[r][c + 1] == player) return false;

            if (!hasDiag) {
                if (r > 1    && c > 1    && board[r-1][c-1] == player)
                     hasDiag = true;
                else if (r > 1    && c < SIZE && board[r-1][c+1] == player) 
                    hasDiag = true;
                else if (r < SIZE && c > 1    && board[r+1][c-1] == player) 
                    hasDiag = true;
                else if (r < SIZE && c < SIZE && board[r+1][c+1] == player) 
                    hasDiag = true;
            }

            if ((r == START_R1 && c == START_C1) 
                || (r == START_R2 && c == START_C2))
                coversStart = true;
        }

        return firstMove ? coversStart : hasDiag;
    }

    private int computeMobility(int player) {
        int mobility = 0;
        int hi = player == PLAYER_1 ? 0 : 1;
        boolean first = isFirstMove(player);

        for (int p = 0; p < NUM_PIECES; p++) {
            if (!hands[hi][p]) continue;

            for (int oi = 0; oi < NUM_CANONICAL[p]; oi++) {
                int[][] cells = CANONICAL_CELLS[p][oi];
                int maxDr = 0, maxDc = 0;
                for (int[] cell : cells) {
                    maxDr = Math.max(maxDr, cell[0]);
                    maxDc = Math.max(maxDc, cell[1]);
                }
                for (int ar = 1; ar <= SIZE - maxDr; ar++)
                    for (int ac = 1; ac <= SIZE - maxDc; ac++)
                        if (isLegalPlacement(cells, ar, ac, player, first))
                            mobility += PIECE_SIZES[p];
            }
        }
        return mobility;
    }
}
