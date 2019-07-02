package chess1;

import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Properties;

public class ChessConfig {
    private String configFilePath;
    private Properties props;

    // AI Settings:
    public boolean humanPlayer;         // human player 1 if true
    public int     maxThreads;          // maximum number of threads in thread pool
    public int     maxSeconds;          // maximum number of seconds AI player is allowed per move
    public int     maxDepth;            // maximum ply depth AI searches ahead

    // Interface Settings:
    public boolean showTargetPaths;     // color the board to show opponents possible moves if true
    public boolean showVictimPaths;     // color the board to show current players possible moves if true
    public boolean showCapturesOnly;    // limit board coloring to only show possible captures if true

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

        humanPlayer = Boolean.valueOf(props.getProperty("humanPlayer", "false"));
        maxThreads = Integer.valueOf(props.getProperty("maxThreads", "100"));
        maxDepth = Integer.valueOf(props.getProperty("aiPlyDepth", "6"));
        maxSeconds = Integer.valueOf(props.getProperty("maxAISeconds", "30"));

        showTargetPaths = Boolean.valueOf(props.getProperty("showTargets", "true"));
        showVictimPaths = Boolean.valueOf(props.getProperty("showVictims", "true"));
        showCapturesOnly = Boolean.valueOf(props.getProperty("showCapturesOnly", "true"));

        return fileMissing;
    }

    public boolean saveConfiguration() {
        // update old values
        props.setProperty("humanPlayer", String.valueOf(humanPlayer));
        props.setProperty("maxThreads", String.valueOf(maxThreads));
        props.setProperty("aiPlyDepth", String.valueOf(maxDepth));
        props.setProperty("maxAISeconds", String.valueOf(maxSeconds));

        props.setProperty("showTargets", String.valueOf(showTargetPaths));
        props.setProperty("showVictims", String.valueOf(showVictimPaths));
        props.setProperty("showCapturesOnly", String.valueOf(showCapturesOnly));

        boolean successful = true;
        try {
            FileWriter writer = new FileWriter(configFilePath);

            String usage = "\n"
                    + " Usage:\n"
                    + " \n"
                    + " humanPlayer:        set to true to play as player 1 (white)\n"
                    + " maxThreads:         maximum number of threads for AI to run simultaneously\n"
                    + " aiPlyDepth:         maximum number of moves for AI to look ahead\n"
                    + " maxAISeconds:       maximum number of seconds to allow AI to think\n"
                    + " showVictims:        color the board to show current players possible moves if true\n"
                    + " showTargets:        color the board to show opponents possible moves if true\n"
                    + " showCapturesOnly:   limit board coloring to only show possible captures if true\n"
                    ;

            props.store(writer, usage);
        } catch (IOException e) {
            successful = false;
        }

        return successful;
    }
}
