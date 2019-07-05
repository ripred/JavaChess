package chess1.AI;

import chess1.Board;
import chess1.Side;

import java.util.List;

/**
 * The TotalMoveCounts class is an implementation of the
 * BoardEvaluator interface.
 *
 * The implementation scores the board by summing the count
 * of each player's available moves.
 */
public class TotalMoveCounts implements BoardEvaluator {

    @Override
    public int evaluate(final Board board, List<Integer> statList) {
        final int moveCountWhite = board.getMovesSorted(Side.White).size();
        final int moveCountBlack = board.getMovesSorted(Side.Black).size();
        return moveCountWhite - moveCountBlack;
    }
}

