import java.io.Serializable;

/**
 * The BestMove objects are a transparent structure to have places to keep
 * track of the best moves seen by each thread as they search
 *
 */
public class BestMove implements Serializable {
    private static final long serialVersionUID = 7218269248347381737L;

    public Move move;
    public int value;
    public int movesExamined;

    public BestMove(boolean maximize) {
        value = maximize ? LiteUtil.MIN_VALUE : LiteUtil.MAX_VALUE;
        movesExamined = 0;
        move = null;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder("Move: ");
        if (move == null) {
            builder.append("null");
        } else {
            builder.append(
            String.format("%c%d to %c%d",
                    move.getFromCol() + 'a', 8 - move.getFromRow(),
                    move.getToCol()   + 'a', 8 - move.getToRow()));
        }
        builder.append(String.format(" Value: %,12d", value));
        return builder.toString();
    }
}
