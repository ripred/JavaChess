import static java.lang.System.exit;
import static java.lang.Thread.yield;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Scanner;
import java.util.List;
import java.util.Map;
import java.util.function.BiPredicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import sun.misc.Signal;
import sun.misc.SignalHandler;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Main {

    public static LiteBoard liteBoard = null;
    public static Map<String, String> options = null;
    public static ChessConfig config = null;
    public static String serialFilename = "chessMoves.ser";

    private static LiteMinimax liteAgent = null;
    private static String configFile = "chess.properties";
    private static List<List<String>> bailedStackList = null;
    public static String logFile = "chess.log";
    public static BufferedWriter logWriter = null;
    public static LogLevel logLevel = LogLevel.DEBUG;
    public static int maxDepth = 2;
    public static int maxSeconds = 0;
    public static double riskLevel = 0.25;
    public static int refreshRate = 1000;
    public static boolean searchInBackground = true;
    public static boolean useCache = true;
    public static boolean useThreads = true;
    public static Thread mainThread = Thread.currentThread();

    public static void main(String[] args) throws InterruptedException, IllegalArgumentException {
        setLogLevel(LogLevel.DEBUG);

        // our signal handler - handle SIGINT (e.g. user hit ctrl-c) etc
        SignalHandler sigHandler = sig -> onAppExit();
        // Register our signal handler for the interrupt signal (SIGINT)
        Signal.handle(new Signal("INT"), sigHandler);

        parseCmdline(args);

        config = new ChessConfig(configFile);
        config.loadConfiguration();

        maxDepth = config.maxDepth;
        maxSeconds = config.maxSeconds;

        if (options.containsKey("ply")) {
            maxDepth = Integer.parseInt(options.get("ply"));
        }

        liteAgent = new LiteMinimax(serialFilename, maxDepth, maxSeconds);

        liteBoard = new LiteBoard();
        liteBoard.maxRep = config.maxDrawReps;

        if (options.containsKey("test")) {
            {
                EngineTuningTests.runTestSuites();
                System.exit(0);
            }
        }

        if (options.containsKey("profwait")) {
            Thread.sleep(Integer.parseInt(options.get("profwait")) * 1000);
        }

        if (options.containsKey("throttle")) {
            liteAgent.setThrottle(Integer.parseInt(options.get("throttle")));
        }

        playGame(liteAgent);

        onAppExit();
    }

    private static void playGame(final LiteMinimax liteAgent) throws InterruptedException {
        for (int n = 0; n < 20; ++n) {
            System.out.println("\n");
        }

        String moveDesc = "";
        long gameStart = System.nanoTime();
        long moveStart;
        Move move;

        liteAgent.registerDisplayCallback(s -> showBoard(liteBoard, s + Ansi.clearEOL, liteAgent, 0, gameStart, true));

        System.out.println();
        showBoard(liteBoard, null, liteAgent, 0, 0, true);

        moveStart = System.nanoTime();
        move = getNextPlayersMove(liteBoard, liteAgent, null, moveStart, gameStart);

        while (move != null) {
            moveDesc = getMoveDesc(liteBoard, move);

            int ndx = move.getTo();
            if (!liteBoard.isEmpty(ndx)) {
                // a piece is being taken.
                // throw away move maps that had it:
                liteAgent.cachedMoves.deletePieceTaken(liteBoard.board, ndx);
            }

            liteBoard.executeMove(move);
            liteBoard.advanceTurn();

            System.out.println();
            showBoard(liteBoard, moveDesc, liteAgent, moveStart, gameStart, true);

            liteAgent.log(liteBoard);

            moveStart = System.nanoTime();
            move = getNextPlayersMove(liteBoard, liteAgent, moveDesc, moveStart, gameStart);
        }

        String boardFinal = showBoardImpl(liteBoard, moveDesc, liteAgent, moveStart, gameStart, true, true);

        writeToScreenFile(boardFinal
                + getGameSummary(liteBoard)
                + getTotalGameTime(gameStart));
        System.out.println(getGameSummary(liteBoard) + getTotalGameTime(gameStart));
    }

    private static Move getNextPlayersMove(final LiteBoard board,
            LiteMinimax liteAgent,
            String moveDesc,
            long moveStart,
            long gameStart) throws InterruptedException {
        if (config.humanPlayer) {
            if ((board.turn == Side.White) ^ !config.humanMovesFirst) {
                return getHumanMove(board);
            }
        }

        // The single point where we launch (and own as the parent thread ourselves) the
        // search thread owner, and
        // it's child threads. The complimentary join() back together is noted below.
        Move move = liteAgent.bestMove(board, searchInBackground);

        while (move == null) {
            showBoard(board, moveDesc, liteAgent, moveStart, gameStart, false);
            Thread.sleep(refreshRate);

            // The single point where we join() with the existing search thread if it is
            // finished
            boolean searchCompleted = liteAgent.moveSearchIsDone();
            if (searchCompleted) {
                // we just join()ed back together with the completed search thread (and it's
                // child threads join()ed to
                // it before that).
            }

            if (searchCompleted) {
                move = liteAgent.getBestMove();
                if (move == null) {
                    // It is still null even after saying the search is complete so this is the end
                    // of the game. Remove
                    // the best move value from the agent so it doesn't get highlighted and
                    // re-display the final game board
                    liteAgent.best = null;
                    showBoard(board, moveDesc, liteAgent, moveStart, gameStart, searchCompleted);
                    break;
                }
            } else {
                showBoard(board, moveDesc, liteAgent, moveStart, gameStart, searchCompleted);
            }
        }

        // put a governor on how fast moves can fly through, so we don't do a move every
        // 5 seconds
        // and then suddenly fly through 8 moves that get decided quickly by the AI
        // faster than we can watch
        // and follow
        final long minMoveRate = 750_000_000L;
        if (System.nanoTime() - moveStart < minMoveRate) {
            long delayTime = System.nanoTime() + minMoveRate;
            while (System.nanoTime() < delayTime) {
                java.lang.Thread.yield();
            }
        }

        return move;
    }

    public enum LogLevel {
        DEBUG(0, "DEBUG"),
        INFO(1, "INFO"),
        WARN(2, "WARN"),
        ERROR(3, "ERROR");

        public int number;
        public String name;

        LogLevel(int num, String str) {
            number = num;
            name = str;
        }
    }

    public static void setLogLevel(LogLevel level) {
        logLevel = level;
    }

    public static void log(List<String> logLines) {
        logLines.forEach((line) -> log(line));
    }

    public static void log(LogLevel level, List<String> logLines) {
        logLines.forEach((line) -> log(level, line));
    }

    public static void log(String format, Object... args) {
        log(LogLevel.INFO, format, args);
    }

    public static void log(LogLevel level, String format, Object... args) {
        if (!options.containsKey("log"))
            return;

        if (level.number < logLevel.number)
            return;

        if (logWriter == null) {
            try {
                if (options.get("log").length() > 0) {
                    logFile = options.get("log");
                }

                logWriter = new BufferedWriter(new FileWriter(logFile));
            } catch (IOException e1) {
                // e1.printStackTrace();
                try {
                    logWriter.close();
                } catch (IOException e2) {
                    e2.printStackTrace();
                }
            }
            if (logWriter == null)
                return;
        }

        String output = createLogEntry(level, format, args);

        try {
            logWriter.write(output);
            logWriter.newLine();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static String createLogEntry(LogLevel level, String format, Object... args) {
        String formatDateTime;
        {
            LocalDateTime now = LocalDateTime.now();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            formatDateTime = now.format(formatter);
        }

        String output;
        if (args.length == 0) {
            output = String.format("%20s %5s - %s", formatDateTime, level.name, format);
        } else {
            String formattedStr = String.format(format, args);
            output = String.format("%20s %5s - %s", formatDateTime, level.name, formattedStr);
        }
        return output;
    }

    private static void parseCmdline(String[] args) {
        options = new HashMap<>();

        if (args.length > 0) {
            Pattern pat = Pattern.compile("(--?)([?a-zA-Z0-9]+)\\s*[=]?\\s*([^\\s]*)");
            for (String arg : args) {
                Matcher match = pat.matcher(arg);
                if (match.find()) {
                    String key = match.group(2);
                    String value = match.group(3);
                    options.put(key, value);
                }
            }

            if (containsAny.test(Arrays.asList("?", "help", "h"), options)) {
                outputUsage();
                System.exit(0);
            }

            if (options.containsKey("configfile")) {
                configFile = options.get("configfile");
            }

            if (options.containsKey("risk")) {
                riskLevel = Double.parseDouble(options.get("risk")) / 100.0f;
            }

            if (options.containsKey("maxtime")) {
                maxSeconds = Integer.parseInt(options.get("maxtime"));
            }

            if (options.containsKey("refresh")) {
                refreshRate = Integer.parseInt(options.get("refresh"));
            }

            if (options.containsKey("background")) {
                searchInBackground = options.get("background").toLowerCase().equals("true");
            }

            if (options.containsKey("cache")) {
                useCache = options.get("cache").toLowerCase().equals("true");
            }

            if (options.containsKey("threads")) {
                useThreads = options.get("threads").toLowerCase().equals("true");
            }
        }
    }

    private static void outputUsage() {
        System.out.println("chess [-option]");
        System.out.println("    -configfile=file            Sets the name of the properties file to load and use");
        System.out.println("    -risk=num                   Risk level.  Sets the percentage of times a move has\n" +
                "                                to be fully examined by the AI divided by the number of\n" +
                "                                times the extra evaluation found a better move. As the\n" +
                "                                move is run more times and continues to be the best move\n" +
                "                                this percentage goes down. num: 0-100.  Default: 25");
        System.out.println("                                Higher value mean less tested moves will be accepted.");
        System.out.println("    -log                        Activity and debug info will be written to chess.log");
        System.out.println(
                "    -log=file                   Activity and debug info will be written to specified log file");
        System.out
                .println("    -profwait=num               delay num seconds after startup to allow profiler to start");
        System.out.println("    -throttle=num               sleep num nanoseconds on each minmax test");
        System.out.println("    -refresh=num                update the display every num milliseconds");
        System.out.println("    -screenfile=file            write display output to file");
        System.out.println("    -test                       Run internal tests and exit");
        System.out.println("    -ply=num                    Sets the max number of look-ahead moves");
        System.out.println("    -maxtime=num                Limit AI thinking to num seconds");
        // System.out.println(" -key=value ");
    }

    private static void onAppExit() {
        // Reset display attributes
        System.out.print("\r" + Ansi.resetAll);

        // clear display to end of line
        System.out.print("\r" + Ansi.clearEOL);

        // Turn the cursor back on
        System.out.println(Ansi.cursOn);

        if (config != null) {
            config.saveConfiguration();
        }

        if (options.containsKey("log")) {
            liteAgent.log(null);
        }

        if (logWriter != null) {
            try {
                logWriter.close();
            } catch (IOException e) {
                e.printStackTrace();
                System.exit(1);
            }
            logWriter = null;
        }

        if (liteAgent != null) {
            if (!useThreads) {
                assert liteAgent.currentSearch == null : "background thread should be null on minimax search";
            } else {
                // stop any current search if running
                if (liteAgent.currentSearch != null) {
                    // attempt to join with background search threads
                    liteAgent.cancelThreadStack(1000);

                    long waitTime = 3_000_000_000L;
                    long stopTime = System.nanoTime() + waitTime;
                    while (!liteAgent.moveSearchIsDone() && System.nanoTime() < stopTime) {
                        java.lang.Thread.yield();
                    }
                }

                try {
                    liteAgent.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        // Turn the cursor back on
        System.out.println(Ansi.cursOn);

        System.out.println("Goodbye \uD83D\uDE0E");

        // Turn the cursor back on
        System.out.println(Ansi.cursOn);

        exit(0);
    }

    private static BiPredicate<List<String>, Map<String, String>> containsAny = (strList, optMap) -> {
        for (String key : strList) {
            if (optMap.containsKey(key))
                return true;
        }
        return false;
    };

    // private static final String[] charSetAscii = {" "," p "," n "," b "," r "," q
    // "," k "};
    private static final String[] charSetUnicodeWhite = { "   ", " ♙ ", " ♞ ", " ♝ ", " ♜ ", " ♛ ", " ♚ " };

    private static void writeToScreenFile(String output) {
        if (!options.containsKey("screenfile"))
            return;

        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(options.get("screenfile")));
            writer.write(output);
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    // output proxy for app for use with a LiteBoard and a LiteMinimax agent
    public static void showBoard(final LiteBoard board, final String moveDesc, final LiteMinimax agent, long moveStart,
            long gameStart, boolean searchDone) {
        // get the output formatted for this screen
        String output = showBoardImpl(board, moveDesc, agent, moveStart, gameStart, false, searchDone);
        System.out.print(output);

        // get the output formatted for storing in a file
        if (options.containsKey("screenfile")) {
            output = showBoardImpl(board, moveDesc, agent, moveStart, gameStart, true, searchDone);
            writeToScreenFile(output);
        }
    }

    private static String showBoardImpl(final LiteBoard board, final String moveDesc, final LiteMinimax agent,
            long moveStart, long gameStart, boolean forFile, boolean searchDone) {

        // The screen row to begin displaying on
        int topRow = 1;

        StringWriter writer = new StringWriter();

        // The background colors of the squares
        String blkBack = Ansi.bg24b(142, 142, 142);
        String whtBack = Ansi.bg24b(204, 204, 204);

        // The colors of the pieces
        String blkForeB = Ansi.fg24b(64, 64, 64);
        String blkForeW = Ansi.fg24b(0, 0, 0);
        String whtForeB = Ansi.fg24b(255, 255, 255);
        String whtForeW = Ansi.fg24b(255, 255, 255);

        // The colors of the last moved piece
        String blkMoved = Ansi.fg24b(128, 0, 0);
        String whtMoved = Ansi.fg24b(192, 192, 0);

        // The colors of the kings in check
        String blkCheck = Ansi.fg24b(192, 128, 0);
        String whtCheck = Ansi.fg24b(192, 128, 0);

        // The color influences on victim and target positions
        String targetsShade = Ansi.bg24b(0, 0, 64);
        String victimsShade = Ansi.bg24b(64, 0, 0);

        // Override with user-config values if valid:
        blkBack = cfgBg(config.blkBack, blkBack);
        whtBack = cfgBg(config.whtBack, whtBack);

        blkForeB = cfgFg(config.blkForeB, blkForeB);
        blkForeW = cfgFg(config.blkForeW, blkForeW);
        whtForeB = cfgFg(config.whtForeB, whtForeB);
        whtForeW = cfgFg(config.whtForeW, whtForeW);

        blkMoved = cfgFg(config.blkMoved, blkMoved);
        whtMoved = cfgFg(config.whtMoved, whtMoved);

        blkCheck = cfgFg(config.blkCheck, blkCheck);
        whtCheck = cfgFg(config.whtCheck, whtCheck);

        targetsShade = cfgBg(config.targetsShade, targetsShade);
        victimsShade = cfgBg(config.victimsShade, victimsShade);

        // The colors of paths to opponent pieces that we can take
        // current player moves tends towards blues
        //
        final String blkTakePath = Ansi.mergeColors24(targetsShade, blkBack, false);
        final String whtTakePath = Ansi.mergeColors24(targetsShade, whtBack, false);

        // The colors of opponent paths to our pieces that they can take
        // opponent moves tends towards reds
        final String blkGivePath = Ansi.mergeColors24(victimsShade, blkBack, false);
        final String whtGivePath = Ansi.mergeColors24(victimsShade, whtBack, false);

        StringBuilder sb = new StringBuilder("    " + config.player2 + " taken:");
        for (int ndx = 0; ndx < board.numTaken1; ndx++) {
            sb.append(whtForeB);
            int type = LiteUtil.getType(board.taken1[ndx]);
            if (type == LiteBoard.Pawn) {
                sb.append(Ansi.boldAttr);
            }
            sb.append(" ").append(charSetUnicodeWhite[type].trim());
        }
        String takenMsg1 = sb.toString();

        sb = new StringBuilder("    " + config.player1 + " taken:");
        for (int ndx = 0; ndx < board.numTaken2; ndx++) {
            sb.append(blkForeB);
            int type = LiteUtil.getType(board.taken2[ndx]);
            if (type == LiteBoard.Pawn) {
                sb.append(Ansi.boldAttr);
            }
            sb.append(" ").append(charSetUnicodeWhite[type].trim());
        }
        String takenMsg0 = sb.toString();

        int value = agent.evaluate(board);
        int score = Math.abs(value);

        String boardScore = String.format("    Board value:        %,7d", score);
        if (score != 0) {
            boardScore += " : ";
            boardScore += (value < 0) ? config.player2 : config.player1;
            boardScore += "'s favor";
        }

        String stat1 = "";
        String stat2 = "";
        String stat3 = "";

        if (moveStart != 0) {
            long timeSpent = (System.nanoTime() - moveStart) / 1_000_000_000L;

            long numProcessed = agent.getNumMovesExamined();

            long numPerSec = (timeSpent == 0) ? numProcessed : (numProcessed / timeSpent);

            String timeSuffix = (timeSpent == maxSeconds && timeSpent != 0) ? " (hit cfg limit)" : "";

            if (!forFile) {
                timeSuffix += Ansi.clearEOL;
            }

            stat1 = String.format("    Time spent:      %,12ds%s", timeSpent, timeSuffix);
            stat2 = String.format("    Moves examined:  %,12d", numProcessed);
            stat3 = String.format("    (per second):    %,12d", numPerSec);
        }

        int side = board.turn;
        int otherSide = (side + 1) % 2;

        // get maps of pieces under attack for both sides
        List<Integer> targets = createTargetMap(board, side);
        List<Integer> victims = createTargetMap(board, otherSide);

        // ====================================================================

        if (!forFile) {
            writer.write(Ansi.cursPos(1, topRow));

            // Turn off the cursor
            writer.write(Ansi.cursOff);
        }

        if (moveDesc != null) {
            writer.write(moveDesc);
            if (!moveDesc.contains("\n")) {
                writer.write("\n");
            }
            if (!forFile) {
                writer.write(Ansi.clearEOL);
            }
        } else {
            writer.write("\n");
            if (!forFile) {
                writer.write(Ansi.clearEOL);
            }
        }
        // writer.write("\n");

        // if (board.kingInCheck(board.turn)) {
        // writer.write((side == Side.Black) ? config.player2 : config.player1);
        // writer.write(" is in check");
        // }
        if (!forFile) {
            writer.write(Ansi.clearEOL);
        }
        writer.write("\n");

        // The color used to highlight the best move
        String bestFromBack = Ansi.bg24b(0, 160, 160);
        String bestToBack = Ansi.bg24b(0, 160, 160);

        int bestFromCol = 8;
        int bestFromRow = 8;
        int bestToCol = 8;
        int bestToRow = 8;

        if (!searchDone) {
            Move best = agent.getBestMove();
            if (best != null) {
                bestFromCol = best.getFromCol();
                bestFromRow = best.getFromRow();
                bestFromBack = ((bestFromCol + bestFromRow) % 2) == 0 ? Ansi.mergeColors24(bestFromBack, blkBack, false)
                        : Ansi.mergeColors24(bestFromBack, whtBack, false);
                bestToCol = best.getToCol();
                bestToRow = best.getToRow();
                bestToBack = ((bestToCol + bestToRow) % 2) == 0 ? Ansi.mergeColors24(bestToBack, blkBack, false)
                        : Ansi.mergeColors24(bestToBack, whtBack, false);
            }
        }

        for (int row = 0; row < 8; ++row) {
            writer.write(Ansi.resetAll + " " + (8 - row) + " ");

            for (int col = 0; col < 8; ++col) {
                boolean blkSq = (((row + col) % 2) == 0);
                String back = blkSq ? blkBack : whtBack;
                int piece = board.board[col + row * 8];
                int pieceSide = LiteUtil.getSide(piece);
                int pieceType = LiteUtil.getType(piece);
                boolean blkPiece = (pieceSide == Side.Black);
                String fore = blkPiece ? blkForeW : whtForeW;

                String fmtClrBack = back;
                String fmtClrFore = fore;
                String fmtAttr = "";

                if (pieceType == LiteBoard.Pawn) {
                    fmtAttr = Ansi.boldAttr;
                }

                if (board.lastMove.getToCol() == col && board.lastMove.getToRow() == row) {
                    fmtClrFore = blkPiece ? blkMoved : whtMoved;
                }

                if (pieceType == LiteBoard.King
                        && pieceSide == side
                        && board.kingInCheck(side)) {
                    fmtClrFore = blkPiece ? blkCheck : whtCheck;
                    fmtAttr = Ansi.boldAttr;
                }

                String clrTake = (blkSq ? blkTakePath : whtTakePath);
                String clrGive = (blkSq ? blkGivePath : whtGivePath);

                boolean spotIsTarget = targets.contains(col + row * 8);
                boolean spotIsVictim = victims.contains(col + row * 8);

                boolean showTargets = config.showTargetPaths;
                boolean showVictims = config.showVictimPaths;
                boolean flipPerspective = (side == Side.Black);

                if (spotIsTarget && !spotIsVictim) {
                    if (showTargets) {
                        fmtClrBack = flipPerspective ? clrGive : clrTake;
                    }
                } else if (!spotIsTarget && spotIsVictim) {
                    if (showVictims) {
                        fmtClrBack = flipPerspective ? clrTake : clrGive;
                    }
                } else if (spotIsTarget && spotIsVictim) {
                    if (showTargets || showVictims) {
                        fmtClrBack = flipPerspective
                                ? Ansi.mergeColors24(clrGive, clrTake, false)
                                : Ansi.mergeColors24(clrTake, clrGive, false);
                    }
                }

                if (bestFromCol == col && bestFromRow == row) {
                    fmtClrBack = bestFromBack;
                } else if (bestToCol == col && bestToRow == row) {
                    fmtClrBack = bestToBack;
                }

                writer.write(fmtClrBack + fmtClrFore + fmtAttr + charSetUnicodeWhite[pieceType]);
            }
            writer.write(Ansi.resetAll);
            String[] rowStrings = { boardScore, takenMsg0, takenMsg1, "", "", stat1, stat2, stat3 };

            writer.write(rowStrings[row] + "\r\n");

            if (!forFile) {
                writer.write(Ansi.clearEOL);
            }
        }
        writer.write("    A  B  C  D  E  F  G  H ");
        writer.write("\n");
        writer.write("\n");

        writer.write(getTurnPrompt(board.turns));

        try {
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return writer.toString();
    }

    private static String getGameSummary(final LiteBoard board) {
        String result = "";
        if (board.checkDrawByRepetition(board.lastMove, board.maxRep)) {
            Move lastMove = board.lastMove;
            result = String.format("Draw by-repetition!  Move from %s to %s made %d times in a row!\n",
                    posString(lastMove.getFromCol(), lastMove.getFromRow()),
                    posString(lastMove.getToCol(), lastMove.getToRow()),
                    board.maxRep);
        }

        boolean checkMateBlack = board.kingInCheck(Side.Black);
        boolean checkMateWhite = board.kingInCheck(Side.White);

        int numMoveBlack;
        int numMoveWhite;
        if (board.turn == Side.Black) {
            numMoveBlack = board.numMoves1;
            numMoveWhite = board.numMoves2;
        } else {
            numMoveBlack = board.numMoves2;
            numMoveWhite = board.numMoves1;
        }

        assert numMoveBlack > 0 || numMoveWhite > 0 : "Internal error: Both players have 0 moves";

        if (checkMateBlack && checkMateWhite) {
            result += "Internal error: Both players are in check\n";
        }

        result += "\n";

        if (numMoveWhite == 0) {
            result += (checkMateWhite ? "Checkmate!  " : "Stalemate!  ") + config.player2 + " Wins!\n";
        } else if (numMoveBlack == 0) {
            result += (checkMateBlack ? "Checkmate!  " : "Stalemate!  ") + config.player1 + " Wins!\n";
        }
        return result;
    }

    private static String getTotalGameTime(long startTime) {
        long totalTime = (System.nanoTime() - startTime) / 1_000_000_000L;
        int hours = (int) (totalTime / 3600L);
        totalTime -= hours * 3600L;
        int minutes = (int) (totalTime / 60L);
        totalTime -= minutes * 60L;
        return String.format("Total Game Time: %02d:%02d:%02d\n", hours, minutes, totalTime);
    }

    private static String cfgBg(final String key, final String def) {
        Integer[] cfgRGB = cfgClrStrToAnsi(key);
        return (cfgRGB.length == 3) ? Ansi.bg24b(cfgRGB[0], cfgRGB[1], cfgRGB[2]) : def;
    }

    private static String cfgFg(final String key, final String def) {
        Integer[] cfgRGB = cfgClrStrToAnsi(key);
        return (cfgRGB.length == 3) ? Ansi.fg24b(cfgRGB[0], cfgRGB[1], cfgRGB[2]) : def;
    }

    private static Integer[] cfgClrStrToAnsi(final String cfgStr) {
        String[] parts = cfgStr.split(",");
        if (parts.length < 3) {
            return new Integer[] {};
        } else {
            return new Integer[] {
                    Integer.valueOf(parts[0].trim()),
                    Integer.valueOf(parts[1].trim()),
                    Integer.valueOf(parts[2].trim()) };
        }
    }

    private static List<Integer> createTargetMap(final LiteBoard board, final int side) {
        // ====================================================================
        // create a map of the current player moves which could
        // take an opponent piece
        List<Integer> targets = new ArrayList<>();
        int numMoves = (side == board.turn) ? board.numMoves1 : board.numMoves2;
        Move[] moves = (side == board.turn) ? board.moves1 : board.moves2;

        for (int ndx = 0; ndx < numMoves; ndx++) {
            Move move = moves[ndx];
            if (config.showCapturesOnly && move.getValue() == 0) {
                continue;
            }
            if (config.showCapturesOnly && board.isEmpty(move.getToCol() + move.getToRow() * 8)) {
                continue;
            }

            int start = move.getFromCol() + move.getFromRow() * 8;
            int type = board.getType(start);
            int curX = start % 8;
            int curY = start / 8;
            int toCol = move.getToCol();
            int toRow = move.getToRow();
            int deltaX;
            int deltaY;

            switch (type) {
                // Single out the pieces that capture using only a
                // destination and not a path of spots to it...
                case LiteBoard.Pawn:
                case LiteBoard.Knight:
                case LiteBoard.King:
                    targets.add(toCol + toRow * 8);
                    break;
                case LiteBoard.Rook:
                case LiteBoard.Bishop:
                case LiteBoard.Queen:
                    deltaX = move.getToCol() - move.getFromCol();
                    deltaY = move.getToRow() - move.getFromRow();
                    deltaX = Integer.min(1, Integer.max(deltaX, -1));
                    deltaY = Integer.min(1, Integer.max(deltaY, -1));
                    do {
                        curX += deltaX;
                        curY += deltaY;
                        int next = curX + curY * 8;
                        targets.add(next);
                    } while (curX != toCol || curY != toRow);
                    break;
                default:
                    assert false : "We should not get here!! ?";
                    break;
            }
        }
        return targets;
    }

    private static String getMoveDesc(final LiteBoard board, final Move move) {
        return getMoveDesc(board, move, false);
    }

    private static String getMoveDesc(final LiteBoard board, final Move move, boolean forFile) {
        if (move == null)
            return "";

        StringBuilder sb = new StringBuilder();

        int from = board.board[move.getFrom()];
        int to = board.board[move.getTo()];
        int fromType = LiteUtil.getType(from);
        String pieceName = getPieceName(fromType);
        String suffix = LiteUtil.isEmpty(to) ? ""
                : " capturing "
                        + getPieceName(LiteUtil.getType(to));
        if (fromType == LiteBoard.Pawn) {
            if ((board.turn == Side.Black && move.getToRow() == 7) ||
                    (board.turn == Side.White && move.getToRow() == 0)) {
                if (!suffix.isEmpty())
                    suffix += " and ";
                suffix += " is promoted to queen! ";
            }
            if (LiteUtil.isEmpty(to) && move.getFromCol() != move.getToCol()) {
                suffix = " En passant capturing Pawn!";
            }
        }
        boolean isCastle = (fromType == LiteBoard.King) && (Math.abs(move.getFromCol() - move.getToCol()) == 2);

        if (isCastle) {
            sb.append("Castle on ").append((move.getToCol() == 6) ? "king" : "queen").append("'s side. ");
        } else {
            sb.append(pieceName)
                    .append(" from ").append(posString(move.getFromCol(), move.getFromRow()))
                    .append(" to ").append(posString(move.getToCol(), move.getToRow()))
                    .append(suffix);
        }
        if (!forFile) {
            sb.append(Ansi.clearEOL);
        }
        sb.append("\n");

        if (board.kingInCheck(board.turn)) {
            sb.append((board.turn == Side.Black) ? config.player2 : config.player1).append(" is in check! ");
            if (!forFile) {
                sb.append(Ansi.clearEOL);
            }
        }
        return sb.toString();
    }

    /**
     * A table of piece names
     */
    private static final String[] typeNames = { "Empty", "Pawn", "Knight", "Bishop", "Rook", "Queen", "King" };

    public static String getPieceName(int type) {
        return typeNames[(type & 0x7)];
    }

    private static String getTurnPrompt(int turns) {
        return String.format("%d: %s's Turn: ",
                turns,
                ((turns % 2) == Side.White) ? config.player1 : config.player2);
    }

    private static char toPos(int nibble) {
        return posDigit[(nibble & 0xF)];
    }

    // A table of position digits
    private static final char[] posDigit = {
            'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', '8', '7', '6', '5', '4', '3', '2', '1'
    };

    public static String posString(int col, int row) {
        return String.valueOf(new char[] { toPos(col), toPos(row + 8) });
    }

    private static Move getHumanMove(final LiteBoard board) {
        String prompt = "your move (ex: a2 a3): ";
        String response;

        // Turn the cursor on
        System.out.print(Ansi.cursOn);

        while (true) {
            System.out.print(prompt);
            Scanner input = new Scanner(System.in);
            response = input.nextLine();

            if (response.length() < 5) {
                System.out.println("Invalid format.");
                continue;
            }

            response = response.toLowerCase();
            String[] parts = response.split(" ");
            if (parts.length < 2) {
                System.out.println("Invalid format.");
                continue;
            }

            int col1 = parts[0].charAt(0) - 'a';
            int col2 = parts[1].charAt(0) - 'a';
            int row1 = 8 - Integer.parseInt(parts[0].substring(1));
            int row2 = 8 - Integer.parseInt(parts[1].substring(1));

            for (int ndx = 0; ndx < board.numMoves1; ndx++) {
                Move m = board.moves1[ndx];
                if (m.getFromCol() == col1 && m.getFromRow() == row1
                        && m.getToCol() == col2 && m.getToRow() == row2) {
                    int value = board.isEmpty(col2 + row2 * 8) ? 0 : board.getValue(col2 + row2 * 8);
                    return new Move(col1, row1, col2, row2, value);
                }
            }
            System.out.println("That is not a legal move.");
        }
    }
}
