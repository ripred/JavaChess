package Chess;

import java.util.Objects;

public class Move {
    private byte from;
    private byte to;

    private final static byte Col    = (byte) 0b00000111;
    private final static byte Row    = (byte) 0b00111000;
    private final static byte Cap    = (byte) 0b01000000;
    private final static byte Unused = (byte) 0b10000000;
    private final static byte Index  = (byte) (Col | Row);

    public Move(final int fromCol, final int fromRow, final int toCol, final int toRow, final boolean isCapture) {
        this.from = (byte) (((byte)fromCol & Col) | (((byte)fromRow) << 3));
        this.to   = (byte) (((byte)toCol   & Col) | (((byte)toRow)   << 3) | (isCapture ? Cap : 0));
    }
    public Move(final int from, final int to, final boolean isCapture) {
        this.from = (byte)((from) & Index);
        this.to   = (byte)(((to)   & Index) | (isCapture ? Cap : 0));
    }
    public Move(final Move move) {
        from = move.from;
        to   = move.to;
    }
    public final byte getFrom() {
        return (byte)(from & Index);
    }
    public final byte getTo() {
        return (byte)(to & Index);
    }
    public final byte getFromCol() {
        return (byte)(from & Col);
    }
    public final byte getFromRow() {
        return (byte)((from & Row) >> 3);
    }
    public final byte getToCol() {
        return (byte)(to & Col);
    }
    public final byte getToRow() {
        return (byte)((to & Row) >> 3);
    }
    public final boolean isCapture() {
        return (to & Cap) == Cap;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Chess.Move move = (Chess.Move) o;
        return (from & Index) == (move.from & Index)
            && (to & Index)   == (move.to & Index)
            && (from & Cap)   == (move.from & Cap)
            && (from & Cap)   == (move.from & Cap)
            && (to & Cap)     == (move.to & Cap);
    }

    @Override
    public int hashCode() {
        return Objects.hash(from, to);
    }

    @Override
    public String toString() {
        return String.format("[Move from:%d,%d to %d,%d capture:%d]",
            from % 8, from / 8, to % 8, to / 8, (to & Cap) == Cap);
    }
}
