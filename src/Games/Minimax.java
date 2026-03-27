package Games;

import java.util.ArrayList;
import java.util.List;
import Games.Board.Move;

public class Minimax {

    public static class ValueMove {
        int value;
        Board.Move move;
    }
    public static void minimax(Board board, int min, int max, int level, ValueMove result) throws Board.InvalidMoveException {
        List<? extends Board.Move> validMoves = new ArrayList<>(board.getValidMoves());

        if (level == 0 || validMoves.isEmpty() || board.isGameOver()) {
            result.value = board.getValue();
            result.move = null;
            return;
        }
    if (board.getCurrentPlayer() == Board.PLAYER_1) {
        result.value = Integer.MIN_VALUE;
        for (Move move : validMoves) {
            board.applyMove(move);
            ValueMove childResult = new ValueMove();
            minimax(board, result.value, max, level - 1, childResult);
            if (childResult.value > result.value) {
                result.value = childResult.value;
                result.move = move;
            }
            board.undoMove();
            if (result.value >= max) {
                return;
            }
        }
    } else {
        result.value = Integer.MAX_VALUE;
        for (Move move : validMoves) {
            board.applyMove(move);
            ValueMove childResult = new ValueMove();
            minimax(board, min, result.value, level - 1, childResult);
            if (childResult.value < result.value) {
                result.value = childResult.value;
                result.move = move;
            }
            board.undoMove();
            if (result.value <= min) {
                return;
            }
        }
    }
}


}
