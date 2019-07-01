package chess1;

import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Properties;

public class ChessConfig {
    private String configFilePath;
    private Properties props;

    // AI Settings:
    public boolean humanPlayer;     // human player 1 if true
    public int     maxThreads;      // maximum number of threads in thread pool
    public int     maxSeconds;      // maximum number of seconds AI player is allowed per move
    public int     maxDepth;        // maximum ply depth AI searches ahead

    // Interface Settings:
    public boolean showTargetPaths; // enable or disable displaying opponent pieces in check by current player
    public boolean showVictimPaths; // enable or disable displaying current player pieces in check by opponent

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

        boolean successful = true;
        try {
            FileWriter writer = new FileWriter(configFilePath);
            props.store(writer, null);
        } catch (IOException e) {
            successful = false;
        }

        return successful;
    }
}
