package chess;

/**
 * The Move class represents the move of a piece from
 * one spot to another and the value of that move.
 *
 */
public class Move {
    private int fromCol;
    private int fromRow;
    private int toCol;
    private int toRow;
    private int value;

    Move(int fromCol, int fromRow, int toCol, int toRow, int value) {
        this.fromCol = fromCol;
        this.fromRow = fromRow;
        this.toCol = toCol;
        this.toRow = toRow;
        this.value = value;
    }

    int  getFromCol()           { return fromCol; }
    int  getFromRow()           { return fromRow; }
    int  getToCol()             { return toCol; }
    int  getToRow()             { return toRow; }
    int  getValue()             { return value; }

    @Override
    public String toString() {
        return String.format("[Move from:%d,%d to %d,%d value:%d]",
                fromCol, fromRow, toCol, toRow, value);
    }
}

