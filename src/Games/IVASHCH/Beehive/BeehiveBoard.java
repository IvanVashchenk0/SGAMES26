package Games.IVASHCH.Beehive;

import Games.Board;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * Beehive (hex connection game) on an 11×11 rhombus grid.
 */
public class BeehiveBoard implements Board {

    public static final int SIZE = 11;
    public static final int EMPTY = 0;
    private static final int FIRST_INDEX = 0;
    private static final int INDEX_BASE_OFFSET = 1;
    private static final int LAST_INDEX = SIZE - INDEX_BASE_OFFSET;
    private static final int ROW_INDEX = 0;
    private static final int COL_INDEX = 1;
    private static final int SWAP_PLACEHOLDER_COORD = FIRST_INDEX;
    private static final int MOVE_PART_COUNT = 2;
    private static final int BRIDGE_COMMON_CELL_COUNT = 2;
    private static final int SECOND_COMMON_CELL_INDEX = FIRST_INDEX + INDEX_BASE_OFFSET;
    private static final int FIRST_MOVE_COUNT = 1;
    private static final int NO_SPAN = 0;
    private static final int HASH_ROW_MULTIPLIER = 31;
    private static final int COMPARE_EQUAL = 0;
    private static final int COMPARE_LESS = -1;
    private static final int COMPARE_GREATER = 1;
    private static final int TEN_LABEL_INDEX = LAST_INDEX - INDEX_BASE_OFFSET;
    private static final int ELEVEN_LABEL_INDEX = LAST_INDEX;
    private static final String TEN_LABEL = "T";
    private static final String ELEVEN_LABEL = "E";

    // Row/column deltas for the six neighboring hexes in this coordinate system.
    static final int[][] ADJ_OFFSETS = {
            {-1, 0}, {-1, 1},
            {0, -1}, {0, 1},
            {1, -1}, {1, 0}
    };

    // Row/column deltas for the six possible two-step bridge endpoints.
    static final int[][] BRIDGE_OFFSETS = {
        {-2,  1},
        {-1, -1},
        {-1,  2},
        { 1, -2},
        { 1,  1},
        { 2, -1}
    };

    // --- Location ---

    static final class Location {
        final int row;
        final int col;

        Location(int row, int col) {
            this.row = row;
            this.col = col;
        }

        Location add(Location o) {
            return new Location(row + o.row, col + o.col);
        }

        Location add(int dr, int dc) {
            return new Location(row + dr, col + dc);
        }

        boolean inBounds() {
            return row >= FIRST_INDEX && row < SIZE && col >= FIRST_INDEX && col < SIZE;
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof Location)) return false;
            Location other = (Location) o;
            return row == other.row && col == other.col;
        }

        @Override
        public int hashCode() {
            return HASH_ROW_MULTIPLIER * row + col;
        }

        @Override
        public String toString() {
            return "(" + row + "," + col + ")";
        }
    }

    // --- BridgeGroup ---

    static final class BridgeGroup {
        final int owner;
        final List<Cell> members = new ArrayList<>();
        int minRow = Integer.MAX_VALUE;
        int maxRow = Integer.MIN_VALUE;
        int minCol = Integer.MAX_VALUE;
        int maxCol = Integer.MIN_VALUE;

        BridgeGroup(int owner) {
            this.owner = owner;
        }

        void addMember(Cell c) {
            members.add(c);
            c.group = this;
            minRow = Math.min(minRow, c.loc.row);
            maxRow = Math.max(maxRow, c.loc.row);
            minCol = Math.min(minCol, c.loc.col);
            maxCol = Math.max(maxCol, c.loc.col);
        }

        int spanPlayer1() {
            return maxCol - minCol + INDEX_BASE_OFFSET;
        }

        int spanPlayer2() {
            return maxRow - minRow + INDEX_BASE_OFFSET;
        }

        boolean winsPlayer1() {
            return owner == PLAYER_1 && minCol == FIRST_INDEX && maxCol == LAST_INDEX;
        }

        boolean winsPlayer2() {
            return owner == PLAYER_2 && minRow == FIRST_INDEX && maxRow == LAST_INDEX;
        }
    }

    // --- Cell ---

    static final class Cell {
        final Location loc;
        int owner = EMPTY;
        final List<Cell> adjacentCells = new ArrayList<>();
        final List<Cell> bridgedCells = new ArrayList<>();
        BridgeGroup group;

        Cell(Location loc) {
            this.loc = loc;
        }
    }

    // --- Move ---

    public static class BeehiveMove implements Board.Move {
        boolean isSwap;
        int row;
        int col;

        public BeehiveMove() {}

        BeehiveMove(boolean isSwap, int row, int col) {
            this.isSwap = isSwap;
            this.row = row;
            this.col = col;
        }

        BeehiveMove(BeehiveMove other) {
            this.isSwap = other.isSwap;
            this.row = other.row;
            this.col = other.col;
        }

        static BeehiveMove swapMove() {
            return new BeehiveMove(true, SWAP_PLACEHOLDER_COORD, SWAP_PLACEHOLDER_COORD);
        }

        @Override
        public void fromString(String s) throws IOException {
            s = s.trim();
            if (s.equalsIgnoreCase("swap")) {
                isSwap = true;
                row = col = SWAP_PLACEHOLDER_COORD;
                return;
            }
            String[] parts = s.split(",", MOVE_PART_COUNT);
            if (parts.length != MOVE_PART_COUNT) {
                throw new IOException("Expected \"row, col\" or swap");
            }
            try {
                int r = Integer.parseInt(parts[ROW_INDEX].trim());
                int c = Integer.parseInt(parts[COL_INDEX].trim());
                if (r < INDEX_BASE_OFFSET || r > SIZE || c < INDEX_BASE_OFFSET || c > SIZE) {
                    throw new IOException("Coordinates out of range");
                }
                isSwap = false;
                row = r - INDEX_BASE_OFFSET;
                col = c - INDEX_BASE_OFFSET;
            } catch (NumberFormatException e) {
                throw new IOException("Invalid number in move");
            }
        }

        @Override
        public String toString() {
            if (isSwap) return "swap";
            return (row + INDEX_BASE_OFFSET) + "," + (col + INDEX_BASE_OFFSET);
        }

        @Override
        public int compareTo(Board.Move o) {
            BeehiveMove other = (BeehiveMove) o;
            if (!this.isSwap && !other.isSwap) {
                int cr = Integer.compare(this.row, other.row);
                if (cr != COMPARE_EQUAL) return cr;
                return Integer.compare(this.col, other.col);
            }
            if (this.isSwap && other.isSwap) return COMPARE_EQUAL;
            if (!this.isSwap) return COMPARE_LESS;
            return COMPARE_GREATER;
        }
    }

    private final Cell[][] cells = new Cell[SIZE][SIZE];
    private int currentPlayer = PLAYER_1;
    private final List<BeehiveMove> moveHistory = new ArrayList<>();
    private boolean swapUsed;
    private BridgeGroup winningGroup;
    private final List<BridgeGroup> lastGroups = new ArrayList<>();

    public BeehiveBoard() {
        for (int r = FIRST_INDEX; r < SIZE; r++) {
            for (int c = FIRST_INDEX; c < SIZE; c++) {
                cells[r][c] = new Cell(new Location(r, c));
            }
        }
        for (int r = FIRST_INDEX; r < SIZE; r++) {
            for (int c = FIRST_INDEX; c < SIZE; c++) {
                Cell cell = cells[r][c];
                for (int[] off : ADJ_OFFSETS) {
                    Location nloc = cell.loc.add(off[ROW_INDEX], off[COL_INDEX]);
                    if (nloc.inBounds()) {
                        cell.adjacentCells.add(cells[nloc.row][nloc.col]);
                    }
                }
                for (int[] off : BRIDGE_OFFSETS) {
                    Location nloc = cell.loc.add(off[ROW_INDEX], off[COL_INDEX]);
                    if (nloc.inBounds()) {
                        cell.bridgedCells.add(cells[nloc.row][nloc.col]);
                    }
                }
            }
        }
    }

    private static List<Cell> commonAdjacent(Cell a, Cell b) {
        List<Cell> common = new ArrayList<>(BRIDGE_COMMON_CELL_COUNT);
        for (Cell x : a.adjacentCells) {
            if (b.adjacentCells.contains(x)) common.add(x);
        }
        return common;
    }

    private static boolean bridgeIsActive(Cell a, Cell b) {
        if (a.owner == EMPTY || a.owner != b.owner) return false;
        List<Cell> mid = commonAdjacent(a, b);
        if (mid.size() != BRIDGE_COMMON_CELL_COUNT) return false;
        return mid.get(FIRST_INDEX).owner == EMPTY && mid.get(SECOND_COMMON_CELL_INDEX).owner == EMPTY;
    }

    private void recomputeGroupsAndWinner() {
        for (int r = FIRST_INDEX; r < SIZE; r++) {
            for (int c = FIRST_INDEX; c < SIZE; c++) {
                cells[r][c].group = null;
            }
        }
        lastGroups.clear();
        winningGroup = null;

        for (int r = FIRST_INDEX; r < SIZE; r++) {
            for (int c = FIRST_INDEX; c < SIZE; c++) {
                Cell start = cells[r][c];
                if (start.owner == EMPTY || start.group != null) continue;

                BridgeGroup g = new BridgeGroup(start.owner);
                Deque<Cell> dq = new ArrayDeque<>();
                dq.add(start);
                g.addMember(start);

                while (!dq.isEmpty()) {
                    Cell u = dq.removeFirst();
                    for (Cell v : u.adjacentCells) {
                        if (v.owner == u.owner && v.owner != EMPTY && v.group == null) {
                            g.addMember(v);
                            dq.addLast(v);
                        }
                    }
                    for (Cell v : u.bridgedCells) {
                        if (v.owner == u.owner && v.owner != EMPTY && v.group == null
                                && bridgeIsActive(u, v)) {
                            g.addMember(v);
                            dq.addLast(v);
                        }
                    }
                }
                lastGroups.add(g);
            }
        }

        winningGroup = findActualWinningGroup(PLAYER_1);
        if (winningGroup == null) {
            winningGroup = findActualWinningGroup(PLAYER_2);
        }
    }

    private BridgeGroup findActualWinningGroup(int player) {
        boolean[][] seen = new boolean[SIZE][SIZE];
        Deque<Cell> dq = new ArrayDeque<>();

        if (player == PLAYER_1) {
            for (int r = FIRST_INDEX; r < SIZE; r++) {
                Cell edge = cells[r][FIRST_INDEX];
                if (edge.owner == player) {
                    seen[r][FIRST_INDEX] = true;
                    dq.addLast(edge);
                }
            }
        } else {
            for (int c = FIRST_INDEX; c < SIZE; c++) {
                Cell edge = cells[FIRST_INDEX][c];
                if (edge.owner == player) {
                    seen[FIRST_INDEX][c] = true;
                    dq.addLast(edge);
                }
            }
        }

        while (!dq.isEmpty()) {
            Cell u = dq.removeFirst();
            if ((player == PLAYER_1 && u.loc.col == LAST_INDEX)
                    || (player == PLAYER_2 && u.loc.row == LAST_INDEX)) {
                return u.group;
            }
            for (Cell v : u.adjacentCells) {
                if (v.owner == player && !seen[v.loc.row][v.loc.col]) {
                    seen[v.loc.row][v.loc.col] = true;
                    dq.addLast(v);
                }
            }
        }
        return null;
    }

    private int countStones() {
        int n = FIRST_INDEX;
        for (int r = FIRST_INDEX; r < SIZE; r++) {
            for (int c = FIRST_INDEX; c < SIZE; c++) {
                if (cells[r][c].owner != EMPTY) n++;
            }
        }
        return n;
    }

    private boolean canOfferSwap() {
        return !swapUsed && moveHistory.size() == FIRST_MOVE_COUNT && currentPlayer == PLAYER_2;
    }

    @Override
    public Move createMove() {
        return new BeehiveMove();
    }

    @Override
    public void applyMove(Move m) throws InvalidMoveException {
        if (isGameOver()) {
            throw new InvalidMoveException("Game is over");
        }
        BeehiveMove move = (BeehiveMove) m;

        if (move.isSwap) {
            if (!canOfferSwap()) {
                throw new InvalidMoveException("Swap is not legal");
            }
            if (countStones() != FIRST_MOVE_COUNT) {
                throw new InvalidMoveException("Swap requires exactly one stone");
            }
            outer:
            for (int r = FIRST_INDEX; r < SIZE; r++) {
                for (int c = FIRST_INDEX; c < SIZE; c++) {
                    Cell cell = cells[r][c];
                    if (cell.owner != EMPTY) {
                        cell.owner = PLAYER_2;
                        break outer;
                    }
                }
            }
            swapUsed = true;
            moveHistory.add(new BeehiveMove(move));
            currentPlayer = PLAYER_1;
            recomputeGroupsAndWinner();
            return;
        }

        if (move.row < FIRST_INDEX || move.row >= SIZE || move.col < FIRST_INDEX || move.col >= SIZE) {
            throw new InvalidMoveException("Coordinates out of range");
        }

        Cell cell = cells[move.row][move.col];
        if (cell.owner != EMPTY) {
            throw new InvalidMoveException("Cell occupied");
        }
        cell.owner = currentPlayer;
        moveHistory.add(new BeehiveMove(move));
        recomputeGroupsAndWinner();
        currentPlayer = -currentPlayer;
    }

    @Override
    public List<? extends Move> getValidMoves() {
        if (isGameOver()) {
            return new ArrayList<>();
        }
        List<BeehiveMove> moves = new ArrayList<>();
        for (int r = FIRST_INDEX; r < SIZE; r++) {
            for (int c = FIRST_INDEX; c < SIZE; c++) {
                if (cells[r][c].owner == EMPTY) {
                    moves.add(new BeehiveMove(false, r, c));
                }
            }
        }
        if (canOfferSwap()) {
            moves.add(BeehiveMove.swapMove());
        }
        return moves;
    }

    @Override
    public int getCurrentPlayer() {
        return currentPlayer;
    }

    @Override
    public int getValue() {
        if (winningGroup != null) {
            return winningGroup.owner == PLAYER_1 ? WIN : -WIN;
        }
        int maxBlue = NO_SPAN;
        int maxRed = NO_SPAN;
        for (BridgeGroup g : lastGroups) {
            if (g.owner == PLAYER_1) {
                maxBlue = Math.max(maxBlue, g.spanPlayer1());
            } else if (g.owner == PLAYER_2) {
                maxRed = Math.max(maxRed, g.spanPlayer2());
            }
        }
        return maxBlue - maxRed;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(' ');
        for (int c = FIRST_INDEX; c < SIZE; c++) {
            sb.append(axisLabel(c));
            if (c < LAST_INDEX) sb.append(' ');
        }
        sb.append('\n');
        for (int r = FIRST_INDEX; r < SIZE; r++) {
            for (int i = FIRST_INDEX; i < r; i++) sb.append(' ');
            sb.append(axisLabel(r));
            for (int c = FIRST_INDEX; c < SIZE; c++) {
                sb.append(cellToken(cells[r][c]));
            }
            sb.append(' ');
            sb.append('\n');
        }
        return sb.toString();
    }

    private static String axisLabel(int index) {
        if (index == TEN_LABEL_INDEX) return TEN_LABEL;
        if (index == ELEVEN_LABEL_INDEX) return ELEVEN_LABEL;
        return Integer.toString(index + INDEX_BASE_OFFSET);
    }

    private String cellToken(Cell cell) {
        int o = cell.owner;
        if (o == EMPTY) return " .";

        boolean highlight = winningGroup != null && cell.group == winningGroup;
        if (o == PLAYER_1) {
            return highlight ? " B" : " b";
        }
        return highlight ? " R" : " r";
    }

    @Override
    public List<? extends Move> getMoveHistory() {
        return new ArrayList<>(moveHistory);
    }

    @Override
    public void undoMove() {
        if (moveHistory.isEmpty()) return;
        BeehiveMove last = moveHistory.remove(moveHistory.size() - INDEX_BASE_OFFSET);
        if (last.isSwap) {
            swapUsed = false;
            currentPlayer = PLAYER_2;
            outer:
            for (int r = FIRST_INDEX; r < SIZE; r++) {
                for (int c = FIRST_INDEX; c < SIZE; c++) {
                    Cell cell = cells[r][c];
                    if (cell.owner != EMPTY) {
                        cell.owner = PLAYER_1;
                        break outer;
                    }
                }
            }
        } else {
            currentPlayer = -currentPlayer;
            cells[last.row][last.col].owner = EMPTY;
        }
        recomputeGroupsAndWinner();
    }

    @Override
    public boolean isGameOver() {
        return winningGroup != null;
    }
}
