package Chess;

/**
 * The PieceOps class is used to interpret and/or set the attributes
 * of a chess piece using one byte to represent the piece
 */
public class PieceOps {
    final private static int MAX_VALUE = Integer.MAX_VALUE - (Integer.MAX_VALUE / 4);
    final private static int MIN_VALUE = 0 - MAX_VALUE;
    final private static int[] values = {0, 10000, 30000, 30000, 50000, 90000, MAX_VALUE};

    // Masks
    final private static byte Type   = (byte) 0b00000111;
    final private static byte Side   = (byte) 0b00001000;
    final private static byte Moved  = (byte) 0b00010000;
    final private static byte Check  = (byte) 0b00100000;
    final private static byte Unused = (byte) 0b11000000;

    final public static byte Empty   = (byte) 0b00000000;
    final public static byte Pawn    = (byte) 0b00000001;
    final public static byte Knight  = (byte) 0b00000010;
    final public static byte Bishop  = (byte) 0b00000011;
    final public static byte Rook    = (byte) 0b00000100;
    final public static byte Queen   = (byte) 0b00000101;
    final public static byte King    = (byte) 0b00000110;
    final public static byte Marker  = (byte) 0b00000111;

    public static byte newSpot(int type, int side, boolean moved, boolean check) {
        byte b = 0;
        setType(b, type);
        setSide(b, side);
        setMoved(b, moved);
        setCheck(b, check);
        return b;
    }

    public static int getValue(int b) {
        return values[getType(b)];
    }

    public static byte getType(int b) {
        return (byte) (Type & b);
    }

    public static byte getSide(int b) {
        return (byte) ((Side & b) >> 3);
    }

    public static boolean hasMoved(int b) {
        return (Moved & b) == Moved;
    }

    public static boolean inCheck(int b) {
        return (Check & b) == Check;
    }

    public static byte setType(int b, int type) {
        return (byte) ((b & ~Type) | (type & Type));
    }

    public static byte setSide(int b, int side) {
        return (byte) ((b & ~Side) | ((side << 3) & Side));
    }

    public static byte setMoved(int b, boolean hasMoved) {
        return (byte) ((b & ~Moved) | (hasMoved ? Moved : 0));
    }

    public static byte setCheck(byte b, boolean inCheck) {
        return (byte) ((b & ~Check) | (inCheck ? Check : 0));
    }
}