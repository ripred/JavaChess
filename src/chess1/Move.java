package chess1;

import java.util.Objects;

/**
 * The Move class represents the move of a piece from
 * one spot to another and the value of that move.
 *
 */
public class Move {
    private final int fromCol;
    private final int fromRow;
    private final int toCol;
    private final int toRow;
    private final int value;

    public Move(int fromCol, int fromRow, int toCol, int toRow, int value) {
        this.fromCol = fromCol;
        this.fromRow = fromRow;
        this.toCol = toCol;
        this.toRow = toRow;
        this.value = value;
    }

    Move(final Move ref) {
        this.fromCol = ref.fromCol;
        this.fromRow = ref.fromRow;
        this.toCol = ref.toCol;
        this.toRow = ref.toRow;
        this.value = ref.value;
    }

    public int  getFromCol()    { return fromCol; }
    public int  getFromRow()    { return fromRow; }
    public int  getToCol()      { return toCol; }
    public int  getToRow()      { return toRow; }
    public int  getValue()      { return value; }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Move move = (Move) o;
        return fromCol == move.fromCol
            && toCol   == move.toCol
            && fromRow == move.fromRow
            && toRow   == move.toRow
            && value   == move.value;
    }

    @Override
    public int hashCode() {
        return Objects.hash(fromCol, toCol, fromRow, toRow, value);
    }

    @Override
    public String toString() {
        return String.format("[Move from:%d,%d to %d,%d value:%d]",
                fromCol, fromRow, toCol, toRow, value);
    }
}

