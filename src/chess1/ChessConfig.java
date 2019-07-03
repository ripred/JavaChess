package chess1;

import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Properties;

import static main.Ansi.bg24b;
import static main.Ansi.fg24b;

public class ChessConfig {
    private String configFilePath;
    private Properties props;

    // General Game Settings:
    public int     maxDrawReps;         // number of repeated moves to call draw

    // AI Settings:
    public boolean humanPlayer;         // human player 1 if true
    public boolean humanMovesFirst;     // human player plays white if true
    public int     maxThreads;          // maximum number of threads in thread pool
    public int     maxSeconds;          // maximum number of seconds AI player is allowed per move
    public int     maxDepth;            // maximum ply depth AI searches ahead

    // Interface Settings:
    public boolean showTargetPaths;     // color the board to show opponents possible moves if true
    public boolean showVictimPaths;     // color the board to show current players possible moves if true
    public boolean showCapturesOnly;    // limit board coloring to only show possible captures if true

    // The player labels
    public String player1;
    public String player2;

    // The  colors of the background squares
    public String blkBack;
    public String whtBack;

    // The colors of the pieces
    public String blkForeB;
    public String blkForeW;
    public String whtForeB;
    public String whtForeW;

    // The colors of the last moved piece
    public String blkMoved;
    public String whtMoved;

    // The colors of the kings in check
    public String blkCheck;
    public String whtCheck;

    // The color influences on victim and target positions
    public String targetsShade;
    public String victimsShade;

    public ChessConfig(final String filename) {
        configFilePath = filename;
        props = new Properties();
    }

    public boolean loadConfiguration() {
        // If the properties file is missing we need to create it with default values
        boolean fileMissing = false;

        try {
            props.load(new FileInputStream(configFilePath));
        } catch (IOException e) {
            fileMissing = true;
        }

        // Populate values with the various config values (properties)
        // from the file and set a default value for each one if it is missing

        maxDrawReps = Integer.valueOf(props.getProperty("numDrawReps", "3"));

        humanPlayer = Boolean.valueOf(props.getProperty("humanPlayer", "false"));
        humanMovesFirst = Boolean.valueOf(props.getProperty("humanMovesFirst", "true"));
        maxThreads = Integer.valueOf(props.getProperty("maxThreads", "100"));
        maxDepth = Integer.valueOf(props.getProperty("aiPlyDepth", "6"));
        maxSeconds = Integer.valueOf(props.getProperty("maxAISeconds", "30"));

        showTargetPaths = Boolean.valueOf(props.getProperty("showTargets", "true"));
        showVictimPaths = Boolean.valueOf(props.getProperty("showVictims", "true"));
        showCapturesOnly = Boolean.valueOf(props.getProperty("showCapturesOnly", "true"));

        player1 = props.getProperty("player1", "Player 1");
        player2 = props.getProperty("player2", "Player 2");

        blkBack  = props.getProperty("blkBack", "142, 142, 142");
        whtBack  = props.getProperty("whtBack", "204, 204, 204");

        blkForeB = props.getProperty("blkForeB", "64, 64, 64");
        blkForeW = props.getProperty("blkForeW", "0, 0, 0");
        whtForeB = props.getProperty("whtForeB", "255, 255, 255");
        whtForeW = props.getProperty("whtForeW", "255, 255, 255");

        blkMoved = props.getProperty("blkMoved", "128, 0, 0");
        whtMoved = props.getProperty("whtMoved", "192, 192, 0");

        blkCheck = props.getProperty("blkCheck", "192, 128, 0");
        whtCheck = props.getProperty("whtCheck", "192, 128, 0");

        targetsShade = props.getProperty("targetsShade", "0, 0, 64");
        victimsShade = props.getProperty("victimsShade", "64, 0, 0");

        return fileMissing;
    }

    public boolean saveConfiguration() {
        // update old values
        props.setProperty("numDrawReps", String.valueOf(maxDrawReps));

        props.setProperty("humanPlayer", String.valueOf(humanPlayer));
        props.setProperty("humanMovesFirst", String.valueOf(humanMovesFirst));
        props.setProperty("maxThreads", String.valueOf(maxThreads));
        props.setProperty("aiPlyDepth", String.valueOf(maxDepth));
        props.setProperty("maxAISeconds", String.valueOf(maxSeconds));

        props.setProperty("showTargets", String.valueOf(showTargetPaths));
        props.setProperty("showVictims", String.valueOf(showVictimPaths));
        props.setProperty("showCapturesOnly", String.valueOf(showCapturesOnly));

        props.setProperty("player1", player1);
        props.setProperty("player2", player2);

        props.setProperty("blkBack", blkBack);
        props.setProperty("whtBack", whtBack);

        // The colors of the pieces
        props.setProperty("blkForeB", blkForeB);
        props.setProperty("blkForeW", blkForeW);
        props.setProperty("whtForeB", whtForeB);
        props.setProperty("whtForeW", whtForeW);

        // The colors of the last moved piece
        props.setProperty("blkMoved", blkMoved);
        props.setProperty("whtMoved", whtMoved);

        // The colors of the kings in check
        props.setProperty("blkCheck", blkCheck);
        props.setProperty("whtCheck", whtCheck);

        // The color influences on victim and target positions
        props.setProperty("targetsShade", targetsShade);
        props.setProperty("victimsShade", victimsShade);

        boolean successful = true;
        try {
            FileWriter writer = new FileWriter(configFilePath);

            String usage = "\n"
                    + " Usage:\n"
                    + " \n"
                    + " General Game Settings:\n"
                    + " numDrawReps         number of repeated moves to call game a draw\n"
                    + " \n"
                    + " AI Settings:\n"
                    + " humanPlayer:        set to true to play either side\n"
                    + " humanMovesFirst:    set to true to play as player 1 (white)\n"
                    + " maxThreads:         maximum number of threads for AI to run simultaneously\n"
                    + " aiPlyDepth:         maximum number of moves for AI to look ahead\n"
                    + " maxAISeconds:       maximum number of seconds to allow AI to think (0 for no time limit)\n"
                    + " \n"
                    + " UI Settings:\n"
                    + " showVictims:        color the board to show current players possible moves if true\n"
                    + " showTargets:        color the board to show opponents possible moves if true\n"
                    + " showCapturesOnly:   limit board coloring to only show possible captures if true\n"
                    + " player1             label to use for player 1\n"
                    + " player2             label to use for player 2\n"
                    + " \n"
                    + " ANSI Colors:\n"
                    + " blkBack             background color for dark squares\n"
                    + " whtBack             background color for light squares\n"
                    + " \n"
                    + " blkForeB            foreground color for black piece on dark square\n"
                    + " blkForeW            foreground color for black piece on light square\n"
                    + " \n"
                    + " whtForeB            foreground color for white piece on dark square\n"
                    + " whtForeW            foreground color for white piece on light square\n"
                    + " \n"
                    + " blkMoved            foreground color for last moved black piece\n"
                    + " whtMoved            foreground color for last moved white piece\n"
                    + " \n"
                    + " blkCheck            foreground color for black king in check\n"
                    + " whtCheck            foreground color for white king in check\n"
                    + " \n"
                    + " targetsShade        color influence for target positions\n"
                    + " victimsShade        color influence for victim positions\n"
                    + "\n";

            props.store(writer, usage);
        } catch (IOException e) {
            successful = false;
        }

        return successful;
    }
}

