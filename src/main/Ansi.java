package main;

public class Ansi {

    //////////////////////////////////////////////////////////
    // CSI Ansi code prefix:
    public static final String CSI = new String(new byte[]{0x1b, '['});

    //////////////////////////////////////////////////////////
    // Resets all colors and attributes to normal
    public static final String resetAll = CSI + "0m";

    //////////////////////////////////////////////////////////
    // 4-bit Foreground and Background Colors
    // n = 0 to 15
    public static String fg4b(int n) { return CSI + "38:5:" + n + "m"; }
    public static String bg4b(int n) { return CSI + "48:5:" + n + "m"; }

    //////////////////////////////////////////////////////////
    // 6-bit Foreground and Background Colors
    // n = 0 to 215
    public static String fg6b(int n) { return fg4b(16+n); }
    public static String bg6b(int n) { return bg4b(16+n); }

    //////////////////////////////////////////////////////////
    // 24-bit Foreground and Background Colors
    // r,g,b = 0 to 255
    public static String fg24b(int r, int g, int b) { return CSI + "38;2;"+r+";"+g+";"+b+"m"; }
    public static String bg24b(int r, int g, int b) { return CSI + "48;2;"+r+";"+g+";"+b+"m"; }

    //////////////////////////////////////////////////////////
    // 7-bit Grayscale Foreground and Background Colors
    // n = 0 to 23
    public static String fgGray6b(int n) { return CSI + "38;2;"+232+n+"m"; }
    public static String bgGray6b(int n) { return CSI + "48;2;"+232+n+"m"; }


    //////////////////////////////////////////////////////////
    // Non-color attributes

    // Bold
    public static final String boldAttr = CSI + "1m";
    // Faint
    public static final String faintAttr = CSI + "2m";
    // Underline
    public static final String underAttr = CSI + "4m";
    // Reverse Video
    public static final String reverseAttr = CSI + "7m";
    // Strike-through
    public static final String strikeAttr = CSI + "9m";


    //////////////////////////////////////////////////////////
    // Cursor and erasing Controls

    //////////////////////////////////////////////////////////
    // Turn the cursor Off:
    public static final String cursOff = CSI + "?25l";

    //////////////////////////////////////////////////////////
    // Turn the cursor On:
    public static final String cursOn = CSI + "?25h";

    //////////////////////////////////////////////////////////
    // Clear the display from cursor to end of the line:
    public static final String clearEOL = CSI + "K";

    //////////////////////////////////////////////////////////
    // Position the cursor to a column and row:
    public static String cursPos(int col, int row) { return CSI + row+";"+col+"f"; }
}
