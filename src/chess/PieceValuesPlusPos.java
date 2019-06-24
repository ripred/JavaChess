package chess;

/**
 * The PieceValuesPlusPos class is an implementation of the
 * BoardEvaluator interface.
 *
 * The implementation scores the board by summing the values
 * of each player's available pieces plus a bonus for the best
 * pieces controlling the center of the board.
 */
public class PieceValuesPlusPos implements BoardEvaluator {

    @Override
    public int evaluate(final Board board) {
        int value = 0;
        for (final Spot spot:board.getBoard()) {
            if (spot.isEmpty()) continue;
            final int sValue = Piece.values[spot.getType()];
            if (spot.getSide() == Side.Black)
                value -= sValue;
            else
                value += sValue;

            // Add bonus for pieces being closer to the board center.
            // Higher power pieces get bigger bonus.

            if (spot.getType() == Piece.King) continue;

            int dx = spot.getCol();
            if (dx > 3) dx = 7 - dx;
            int dy = spot.getRow();
            if (dy > 3) dy = 7 - dy;
            if (spot.getSide() == Side.Black)
                value -= (dx + dy) * spot.getType() * 10;
            else
                value += (dx + dy) * spot.getType() * 10;
        }
        return value;
    }
}