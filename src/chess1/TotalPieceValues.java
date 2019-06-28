package chess1;

/**
 * The TotalPieceValues class is an implementation of the
 * BoardEvaluator interface.
 *
 * The implementation scores the board by summing the values
 * of each player's available pieces.
 */
public class TotalPieceValues implements BoardEvaluator {

    @Override
    public int evaluate(final Board board) {
        int value = 0;
        for (Spot s:board.getBoard()) {
            if (s.isEmpty()) continue;
            final int sValue = Piece.values[s.getType()];
            if (s.getSide() == Side.Black)
                value -= sValue;
            else
                value += sValue;
        }
        return value;
    }
}

