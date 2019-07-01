package chess1.AI;

import chess1.Move;
import chess1.Piece;

/**
 * The BestMove objects are a transparent structure to have places to keep
 * track of the best moves seen by each thread as they search
 *
 */
class BestMove {
    Move move;
    int value;

    BestMove(boolean maximize) {
        value = maximize ? Piece.MIN_VALUE : Piece.MAX_VALUE;
        move = null;
    }

    @Override
    public String toString() {
        String string = "Move: ";

        if (move == null) {
            string += "null";
        } else {
            string += String.format("%c%d to %c%d",
                    move.getFromCol() + 'a',
                    8 - move.getFromRow(),
                    move.getToCol() + 'a',
                    8 - move.getToRow());
        }
        string += String.format(" Value: %,12d", value);

        return string;
    }
}
