package main;

import static java.lang.Math.sqrt;

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


    //////////////////////////////////////////////////////////
    // Merge two color strings into single (foreground or background) color average of the two:
    public static String mergeColors24(String clr1, String clr2, final boolean fore) {
        String[] parts1 = clr1.split(";");
        String[] parts2 = clr2.split(";");

        float r1 = Float.valueOf(parts1[2]);
        float g1 = Float.valueOf(parts1[3]);
        float b1 = Float.valueOf(parts1[4].substring(0, parts1[4].length() - 1));

        float r2 = Float.valueOf(parts2[2]);
        float g2 = Float.valueOf(parts2[3]);
        float b2 = Float.valueOf(parts2[4].substring(0, parts2[4].length() - 1));

        int r = (int)((((r1 / 256.0f) + (r2 / 256.0f)) / 2.0) * 256.0);
        int g = (int)((((g1 / 256.0f) + (g2 / 256.0f)) / 2.0) * 256.0);
        int b = (int)((((b1 / 256.0f) + (b2 / 256.0f)) / 2.0) * 256.0);

        return fore ? fg24b(r, g, b) : bg24b(r, g, b);
    }



    private static void testAnsi() {
        String csi = new String(new byte[]{0x1b, '['});
        String fg = csi + "38:5:";
        String bg = csi + "48:5:";
        int loop, i;

        // Standard and high-intensity colors
        for (i = 0; i < 16; ++i) {
            String out = csi + (i < 8 ? "97m" : "30m") + bg + i + "m" + String.format("%6d   ", i);
            System.out.print(out);
        }
        System.out.println(csi + "0m");

        // 216 colors
        for (loop = 0, i = 16; i < 232; ++i) {
            String out = csi + ((loop % 36) < (18) ? "97m" : "30m") + bg + i + "m" + String.format("%3d ", i);
            System.out.print(out);
            if ((++loop) % 36 == 0) System.out.println(csi + "40m");
        }

        // Grayscale colors
        for (loop = 0, i = 232; i < 256; ++i) {
            String out = csi + ((loop) < (12) ? "97m" : "30m") + bg + i + "m" + String.format("%4d  ", i);
            System.out.print(out);
            loop++;
        }
        System.out.println(csi + "0m");

        // 24-bit colors
        final int redStep = 48;
        final int greenStep = 32;
        final int blueStep = 16;
        for (int r = 0; r < 256; r += redStep) {
            for (int g = 0; g < 256; g += greenStep) {
                for (int b = 0; b < 256; b += blueStep) {
                    String fg24 = csi + "38;2;";
                    String bg24 = csi + "48;2;";
                    double brightness = sqrt((r * g) + (g * b) + (b * r));
                    String contrastFg = csi + ((brightness >= 192.0) ? "30m" : "97m");
                    String out = (contrastFg)
                            + (bg24 + (g) + ";" + (b) + ";" + (r) + "m")
                            + String.format("%02X%02X%02X ", r, g, b);
                    System.out.print(out);
                }
                System.out.println(csi + "0m");
            }
        }
        System.out.println();

        System.out.println(csi + "1m Bold             Rendition" + csi + "0m");
        System.out.println(csi + "2m Faint            Rendition" + csi + "0m");
        System.out.println(csi + "4m Underline        Rendition" + csi + "0m");
        System.out.println(csi + "22m Neither          Rendition" + csi + "0m");
        System.out.println(csi + "7m Reverse Video    Rendition" + csi + "0m");
        System.out.println(csi + "9m Crossed Out      Rendition" + csi + "0m");

        System.out.println();
    }
}
