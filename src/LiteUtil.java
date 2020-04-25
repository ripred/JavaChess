/**
 * The LiteUtil class is used to interpret and/or set the attributes
 * of a chess piece using one byte to represent the piece
 */
public class LiteUtil {
    final public  static int MAX_VALUE = Integer.MAX_VALUE - (Integer.MAX_VALUE / 4);
    final public  static int MIN_VALUE = 0 - MAX_VALUE;

    final private static int[] values = {0, 10000, 30000, 30000, 50000, 90000, MAX_VALUE};

    // Masks
    final public static byte Type   = (byte) 0x07;
    final public static byte Unused = (byte) 0x88;
    final public static byte Side   = (byte) 0x10;
    final public static byte Moved  = (byte) 0x20;
    final public static byte Check  = (byte) 0x40;

    final private static byte  Empty   = (byte) 0;
    final private static byte  Pawn    = (byte) 1;
    final private static byte  Knight  = (byte) 2;
    final private static byte  Bishop  = (byte) 3;
    final private static byte  Rook    = (byte) 4;
    final private static byte  Queen   = (byte) 5;
    final private static byte  King    = (byte) 6;
    final private static byte  Marker  = (byte) 7;

    public static byte makeSpot(int type, int side, boolean moved, boolean inCheck) {
        byte b = 0;
        b = setType(b, type);
        b = setSide(b, side);
        b = setMoved(b, moved);
        b = setCheck(b, inCheck);
        return b;
    }


    public static byte getType(int b) {
        return (byte) (Type & b);
    }

    public static boolean isEmpty(int b) {
        return getType(b) == Empty;
    }

    public static int getValue(int b) {
        return values[getType(b)];
    }

    public static byte getSide(int b) {
        return (byte) ((Side & b) >> 4);
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
        return (byte) ((b & ~Side) | ((side << 4) & Side));
    }

    public static byte setMoved(int b, boolean hasMoved) {
        return (byte) ((b & ~Moved) | (hasMoved ? Moved : 0));
    }

    public static byte setCheck(byte b, boolean inCheck) {
        return (byte) ((b & ~Check) | (inCheck ? Check : 0));
    }
}