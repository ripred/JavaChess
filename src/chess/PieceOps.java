package Chess;

public class PieceOps {
    final private static int MAX_VALUE = Integer.MAX_VALUE - (Integer.MAX_VALUE / 4);
    final private static int MIN_VALUE = 0 - MAX_VALUE;
    final private static int[] values = {0, 10000, 30000, 30000, 50000, 90000, MAX_VALUE};

    final private static byte Type   = (byte) 0b00000111;
    final private static byte Side   = (byte) 0b00001000;
    final private static byte Moved  = (byte) 0b00010000;
    final private static byte Check  = (byte) 0b00100000;
    final private static byte Unused = (byte) 0b11000000;

    final public static byte Empty  = (byte) 0b00000000;
    final public static byte Pawn   = (byte) 0b00000001;
    final public static byte Rook   = (byte) 0b00000010;
    final public static byte Knight = (byte) 0b00000011;
    final public static byte Bishop = (byte) 0b00000100;
    final public static byte Queen  = (byte) 0b00000101;
    final public static byte King   = (byte) 0b00000110;
    final public static byte Marker = (byte) 0b00000111;

    public static int getValue(final byte b) {
        return values[getType(b)];
    }
    public static byte getType(final byte b) {
        return (byte)(Type & b);
    }
    public static byte getSide(final byte b) {
        return (byte)(Side & b);
    }
    public static boolean hasMoved(final byte b) {
        return (Moved & b) == Moved;
    }
    public static boolean inCheck(final byte b) {
        return (Check & b) == Check;
    }
    public static byte setType(final byte b, final byte type) {
        return (byte) ((b & ~Type) | (type & Type));
    }
    public static byte setSide(final byte b, final byte type) {
        return (byte) ((b & ~Side) | (type & Side));
    }
    public static byte setMoved(final byte b, final boolean hasMoved) {
        return (byte) ((b & ~Moved) | (hasMoved ? Moved : 0));
    }
    public static byte setCheck(final byte b, final boolean inCheck) {
        return (byte) ((b & ~Check) | (inCheck ? Check : 0));
    }
}