import java.io.Serializable;
import java.util.Objects;

/**
 * The Move class represents the move of a piece from
 * one spot to another and the value of that move.
 *
 */
public class Move implements Serializable {
    private static final long serialVersionUID = 5918469248348323737L;

    private final int fromCol;
    private final int fromRow;
    private final int toCol;
    private final int toRow;
    private final int from;
    private final int to;
    private int value;

    public Move(int fromCol, int fromRow, int toCol, int toRow, int value) {
        this.fromCol = fromCol;
        this.fromRow = fromRow;
        this.toCol   = toCol;
        this.toRow   = toRow;
        this.from    = fromCol + fromRow * 8;
        this.to      = toCol + toRow * 8;
        this.value   = value;
    }

    public Move(final Move ref) {
        this.fromCol = ref.fromCol;
        this.fromRow = ref.fromRow;
        this.toCol   = ref.toCol;
        this.toRow   = ref.toRow;
        this.from    = ref.from;
        this.to      = ref.to;
        this.value   = ref.value;
    }

    public int  getFromCol()    { return fromCol; }
    public int  getFromRow()    { return fromRow; }
    public int  getToCol()      { return toCol; }
    public int  getToRow()      { return toRow; }
    public int  getFrom()       { return from; }
    public int  getTo()         { return to; }
    public int  getValue()      { return value; }
    public void setValue(int value) {
        this.value = value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Move move = (Move) o;
        return from == move.from
            && to   == move.to;
    }

    @Override
    public int hashCode() {
        return Objects.hash(from, to);
    }

    @Override
    public String toString() {
        String chessNotation = String.format("%c%d to %c%d",
                fromCol + 'a', 8 - fromRow,
                toCol   + 'a', 8 - toRow);

        return String.format("[Move from:%d,%d to %d,%d (%s) value:%d]",
                fromCol, fromRow, toCol, toRow, chessNotation, value);
    }
}

