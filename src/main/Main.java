package main;

import chess1.*;
import chess1.AI.*;

import static java.lang.System.exit;
import static main.Ansi.*;

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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import sun.misc.Signal;
import sun.misc.SignalHandler;


@FunctionalInterface
interface Predicate1<T> {

    public boolean test(T t);

    public default Predicate1<T> and(Predicate1<T> other) {
        return t -> test(t) && other.test(t);
    }

    public default Predicate1<T> or(Predicate1<T> other) {
        return t -> test(t) || other.test(t);
    }

    public static <U> Predicate1<U> isEqualTo(U u) {
        return s -> s.equals(u);
    }
}


public class Main {

    private static final String statsFile  ="stats.csv";

    private static ChessConfig config = null;
    private static Map<String, String> options = null;

    private static void writeStatsFile(String filename) {
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(filename));
            writer.write("\"MoveCount\",\"Standard\",\"Quiescent\"\n");
            for (int i=0; i < stats.size(); ++i) {
                List<Integer> list = stats.get(i);
                writer.write(String.format("%d,%d,%d\n", list.get(0), list.get(1), list.get(2)));
            }
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void onAppExit() {
        // Reset display attributes
        System.out.print("\r" + resetAll);

        // clear display to end of line
        System.out.print("\r" + clearEOL);

        // Turn the cursor back on
        System.out.println(cursOn);

        if (config != null) {
            config.saveConfiguration();
        }

        writeStatsFile(statsFile);

        System.out.println("Goodbye \uD83D\uDE0E");
    }

    static List<List<Integer>> stats;

    public static void main(String[] args) {

        SignalHandler sigHandler = sig -> {
            // handle SIGINT

            onAppExit();
            exit(0);
        };
        try {
            Signal.handle(new Signal("INT"), sigHandler);
        } catch (Exception e) {
            e.printStackTrace();
        }

        config = new ChessConfig("chess.properties");
        config.loadConfiguration();

        options = new HashMap<>();

        if (args.length > 0) {
            Pattern pat = Pattern.compile("(-[a-zA-Z0-9]+)\\s*[=]?\\s*([^\\s]*)");
            for (String arg:args) {
                Matcher match = pat.matcher(arg);
                if (match.find()) {
                    String key = match.group(1).substring(1);
                    String value = match.group(2);
                    options.put(key, value);
                }
            }
        }

        stats = new ArrayList<>();

        AIMoveSelector moveAgent = new Minimax(config.maxThreads, config.maxDepth, config.maxSeconds);
        LiteMinimax liteAgent = new LiteMinimax(config.maxDepth, config.maxSeconds);

        if (options.get("t") != null) {
            moveAgent.setThrottle(Integer.valueOf(options.get("t")));
            liteAgent.setThrottle(Integer.valueOf(options.get("t")));
        }

        playGame(moveAgent, liteAgent);

        onAppExit();
    }

    private static void playGame(final AIMoveSelector moveAgent, final LiteMinimax liteAgent) {
        for (int n=0; n < 20; ++n) {
            System.out.println("\n");
        }

        final Board board = new Board(1);

        board.setMaxAllowedRepetitions(config.maxDrawReps);

        moveAgent.registerDisplayCallback(new ThreadMsgConsumer(board, moveAgent));

        String moveDesc = null;
        long gameStart = System.nanoTime();
        long moveStart;
        Move move;

//        board.initTest();

        System.out.println();
        showBoard(board, null, liteAgent, 0, 0);

        moveStart = System.nanoTime();
        move = getNextPlayersMove(board, liteAgent, moveDesc, moveStart, gameStart);

        while (move != null) {
            moveDesc = getMoveDesc(board, move);

            board.executeMove(move);
            board.advanceTurn(true);

            System.out.println();
            showBoard(board, moveDesc, liteAgent, moveStart, gameStart);

            // Check for draw-by-repetition (same move made too many times in a row by a player)
            if (board.checkDrawByRepetition()) {
                break;
            }

            moveStart = System.nanoTime();
            move = getNextPlayersMove(board, liteAgent, moveDesc, moveStart, gameStart);
        }
        System.out.println(getGameSummary(board));
        System.out.println(getTotalGameTime(gameStart));
    }

    private static Move getNextPlayersMove(final Board board, LiteMinimax liteAgent, String moveDesc, long moveStart, long gameStart) {
        if (config.humanPlayer) {
            if ((board.getTurn() == Side.White) ^ !config.humanMovesFirst) {
                return getHumanMove(board);
            }
        }

        LiteBoard liteBoard = new LiteBoard(board, true);

        if (liteBoard.kingInCheck((liteBoard.turn + 1) % 2)) {
            showBoard(board, "* Somehow we allowed a move that put the king in check! *", liteAgent, moveStart, gameStart);
            return null;
        }


        Move move = liteAgent.bestMove(liteBoard, true);

//        Move move = agent.bestMove(board, true);
        while (move == null) {
            showBoard(board, moveDesc, liteAgent, moveStart, gameStart);





            Map<Integer, Integer> processedMap = liteAgent.getNumMovesExaminedMap();
            int std = 0;
            int quiescent = 0;
            for (int d=config.maxDepth; d >= -2; --d) {
                processedMap.putIfAbsent(d, 0);
                if (d >= 0)
                    std += processedMap.get(d);
                else
                    quiescent += processedMap.get(d);
            }

            int totalMoves = liteBoard.numMoves1 + liteBoard.numMoves2;

            List<Integer> stat = new ArrayList<>();

            stat.add(totalMoves);
            stat.add(std);
            stat.add(quiescent);
            stats.add(stat);





            writeStatsFile(statsFile);


            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
//            if (agent.moveSearchIsDone()) {
            if (liteAgent.moveSearchIsDone()) {
//                move = agent.getBestMove();
                move = liteAgent.getBestMove();

                if (move == null) {
                    // it's still null even after saying the search is
                    // complete so this is a loss.
                    return null;
                }
            }
        }

        return move;
    }

//    static final String[] charSetAscii = {"   "," p "," n "," b "," r "," q "," k "};
    private static final String[] charSetUnicodeWhite = {"   "," ♙ "," ♞ "," ♝ "," ♜ "," ♛ "," ♚ "};

    static void showBoard(final Board board, final String moveDesc, final LiteMinimax agent, long moveStart, long gameStart) {
        String output = showBoardImpl(board, moveDesc, agent, moveStart, gameStart, false);
        System.out.print(output);

        if (!options.containsKey("outfile"))
            return;

        String filename = "output.txt";
        output = showBoardImpl(board, moveDesc, agent, moveStart, gameStart, true);
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(filename));
            writer.write(output);
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    static String showBoardImpl(final Board board, final String moveDesc, final LiteMinimax agent, long moveStart, long gameStart, boolean forFile) {
        // The screen row to begin displaying on
        int topRow = 1;

        StringWriter writer = new StringWriter();

        // The background colors of the squares
        String blkBack = bg24b(142, 142, 142);
        String whtBack = bg24b(204, 204, 204);

        // The colors of the pieces
        String blkForeB = fg24b(64, 64, 64);
        String blkForeW = fg24b(0, 0, 0);
        String whtForeB = fg24b(255, 255, 255);
        String whtForeW = fg24b(255, 255, 255);

        // The colors of the last moved piece
        String blkMoved = fg24b(128, 0, 0);
        String whtMoved = fg24b(192, 192, 0);

        // The colors of the kings in check
        String blkCheck = fg24b(192, 128, 0);
        String whtCheck = fg24b(192, 128, 0);

        // The color influences on victim and target positions
        String targetsShade = bg24b(0, 0, 64);
        String victimsShade = bg24b(64, 0, 0);

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

        // The colors used to show spots adjacent to the king
        String blkClrAdjacent = mergeColors24(bg24b(128, 128, 0), blkBack, false);
        String whtClrAdjacent = mergeColors24(bg24b(142, 142, 0), whtBack, false);

        // The colors of paths to opponent pieces that we can take
        // current player moves tends towards blues
        //
        final String blkTakePath = mergeColors24(targetsShade, blkBack, false);
        final String whtTakePath = mergeColors24(targetsShade, whtBack, false);

        // The colors of opponent paths to our pieces that they can take
        // opponent moves tends towards reds
        final String blkGivePath = mergeColors24(victimsShade, blkBack, false);
        final String whtGivePath = mergeColors24(victimsShade, whtBack, false);

        StringBuilder sb = new StringBuilder("    " + config.player2 + " taken:");
        for (final int type : board.getPiecesTaken1()) {
            sb.append(whtForeB);
            if (type == Piece.Pawn) {
                sb.append(boldAttr);
            }
            sb.append(" ").append(charSetUnicodeWhite[type].trim());
        }
        String takenMsg1 = sb.toString();

        sb = new StringBuilder("    " + config.player1 + " taken:");
        for (final int type : board.getPiecesTaken0()) {
            sb.append(blkForeB);
            if (type == Piece.Pawn) {
                sb.append(boldAttr);
            }
            sb.append(" ").append(charSetUnicodeWhite[type].trim());
        }
        String takenMsg0 = sb.toString();

        LiteBoard liteBoard = new LiteBoard(board);
        int value = agent.evaluate(liteBoard);
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
            Map<Integer, Integer> processedMap = agent.getNumMovesExaminedMap();
            int numProcessed = 0;
            for (int v : processedMap.values())
                numProcessed += v;
            long numPerSec = (timeSpent == 0) ? numProcessed : (numProcessed / timeSpent);

            String timeLimit = (timeSpent == config.maxSeconds && timeSpent != 0) ? " (hit cfg limit)" : "";

            stat1 = String.format("    Time spent:      %,10ds%s", timeSpent, timeLimit);
            stat2 = String.format("    Moves examined:  %,10d", numProcessed);
            stat3 = String.format("    (per second):    %,10d", numPerSec);

            int std = 0;
            int quiescent = 0;
            for (int d=config.maxDepth; d >= -2; --d) {
                processedMap.putIfAbsent(d, 0);
                if (d >= 0)
                    std += processedMap.get(d);
                else
                    quiescent += processedMap.get(d);
            }
            stat2 += String.format(" = Std:%,9d + Quiescent: %,9d", std, quiescent);
        }

        int side = board.getTurn();
        int otherSide = (side + 1) % 2;

        // get maps of pieces under attack for both sides
        Map<Move, List<Spot>> targets = createTargetMap(board, side);
        Map<Move, List<Spot>> victims = createTargetMap(board, otherSide);

        // ====================================================================

        if (!forFile) {
            writer.write(cursPos(1, topRow));

            // Turn off the cursor
            writer.write(cursOff);
        }

        if (moveDesc != null) {
            writer.write(moveDesc);
            if (!forFile) {
                writer.write(clearEOL);
            }
        }
        writer.write("\n");


        if (board.getKingInCheck(board.getTurn())) {
            writer.write((side == Side.Black) ? config.player2 : config.player1);
            writer.write(" is in check");
        }
        if (!forFile) {
            writer.write(clearEOL);
        }
        writer.write("\n");

        Move lastMove = board.getLastMove();

        // The color used to highlight the best move
        String bestFromBack = bg24b(0, 160, 160);
        String bestToBack = bg24b(0, 160, 160);

        int bestFromCol = 8;
        int bestFromRow = 8;
        int bestToCol = 8;
        int bestToRow = 8;
        if (!agent.moveSearchIsDone()) {
            Move best = agent.getBestMove();
            if (best != null) {
                bestFromCol = best.getFromCol();
                bestFromRow = best.getFromRow();
                bestFromBack = ((bestFromCol + bestFromRow) % 2) == 0 ?
                        mergeColors24(bestFromBack, blkBack, false) :
                        mergeColors24(bestFromBack, whtBack, false);
                bestToCol = best.getToCol();
                bestToRow = best.getToRow();
                bestToBack = ((bestToCol + bestToRow) % 2) == 0 ?
                        mergeColors24(bestToBack, blkBack, false) :
                        mergeColors24(bestToBack, whtBack, false);
            }
        }

        for (int row = 0; row < 8; ++row) {
            writer.write(resetAll + " " + (8 - row) + " ");

            for (int col = 0; col < 8; ++col) {
                boolean blkSq = (((row + col) % 2) == 0);
                String back = blkSq ? blkBack : whtBack;
                Spot spot = board.getSpot(col, row);
                int type = spot.getType();
                boolean blkPiece = (spot.getSide() == Side.Black);
                String fore = blkPiece ? blkForeW : whtForeW;

                String fmtClrBack = back;
                String fmtClrFore = fore;
                String fmtAttr = "";

                if (type == Piece.Pawn) {
                    fmtAttr = boldAttr;
                }

                if (lastMove.getToCol() == col && lastMove.getToRow() == row) {
                    fmtClrFore = blkPiece ? blkMoved : whtMoved;
                }

                if (type == Piece.King
                        && side == spot.getSide()
                        && board.getKingInCheck(side)) {
                    fmtClrFore = blkPiece ? blkCheck : whtCheck;
                    fmtAttr = boldAttr;
                }

                if (board.getAdjacentBonus()) {
                    for (Spot adjSpot : board.getBlkKingAdjacent()) {
                        if (adjSpot.getCol() == col && adjSpot.getRow() == row) {
                            if (blkSq)
                                fmtClrBack = blkClrAdjacent;
                            else
                                fmtClrBack = whtClrAdjacent;
                            break;
                        }
                    }
                    for (Spot adjSpot : board.getWhtKingAdjacent()) {
                        if (adjSpot.getCol() == col && adjSpot.getRow() == row) {
                            if (blkSq)
                                fmtClrBack = blkClrAdjacent;
                            else
                                fmtClrBack = whtClrAdjacent;
                            break;
                        }
                    }
                }

                String clrTake = (blkSq ? blkTakePath : whtTakePath);
                String clrGive = (blkSq ? blkGivePath : whtGivePath);

                boolean spotIsTarget = isSpotATarget(targets, col, row);
                boolean spotIsVictim = isSpotATarget(victims, col, row);

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
                                ? mergeColors24(clrGive, clrTake, false)
                                : mergeColors24(clrTake, clrGive, false);
                    }
                }

                if (bestFromCol == col && bestFromRow == row) {
                    fmtClrBack = bestFromBack;
                } else if (bestToCol == col && bestToRow == row) {
                    fmtClrBack = bestToBack;
                }

                writer.write(fmtClrBack + fmtClrFore + fmtAttr + charSetUnicodeWhite[type]);
            }
            writer.write(resetAll);
            String[] rowStrings = {boardScore, takenMsg0, takenMsg1, "", "", stat1, stat2, stat3};

            writer.write(rowStrings[row] + "\r\n");

            if (!forFile) {
                writer.write(clearEOL);
            }
        }
        writer.write("    A  B  C  D  E  F  G  H ");
        writer.write("\n");
        writer.write("\n");

        writer.write(getTurnPrompt(board));

//        board.updateMoves();
        if (board.getCurrentPlayerMoves().size() == 0 ||
                board.getOtherPlayerMoves().size() == 0 ||
                board.checkDrawByRepetition()) {
            writer.write("\r" + getGameSummary(board));
            writer.write(getTotalGameTime(gameStart));
        }

        try {
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return writer.toString();
    }

    static void showBoard(final Board board, final String moveDesc, final AIMoveSelector agent, long moveStart, long gameStart) {
        String output = showBoardImpl(board, moveDesc, agent, moveStart, gameStart, false);
        System.out.print(output);

        if (!options.containsKey("outfile"))
            return;

        String filename = "output.txt";
        output = showBoardImpl(board, moveDesc, agent, moveStart, gameStart, true);
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(filename));
            writer.write(output);
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    static String showBoardImpl(final Board board, final String moveDesc, final AIMoveSelector agent, long moveStart, long gameStart, boolean forFile) {
        // The screen row to begin displaying on
        int topRow = 1;

        StringWriter writer = new StringWriter();

        // The background colors of the squares
        String blkBack = bg24b(142, 142, 142);
        String whtBack = bg24b(204, 204, 204);

        // The colors of the pieces
        String blkForeB = fg24b(64, 64, 64);
        String blkForeW = fg24b(0, 0, 0);
        String whtForeB = fg24b(255, 255, 255);
        String whtForeW = fg24b(255, 255, 255);

        // The colors of the last moved piece
        String blkMoved = fg24b(128, 0, 0);
        String whtMoved = fg24b(192, 192, 0);

        // The colors of the kings in check
        String blkCheck = fg24b(192, 128, 0);
        String whtCheck = fg24b(192, 128, 0);

        // The color influences on victim and target positions
        String targetsShade = bg24b(0, 0, 64);
        String victimsShade = bg24b(64, 0, 0);

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

        // The colors used to show spots adjacent to the king
        String blkClrAdjacent = mergeColors24(bg24b(128, 128, 0), blkBack, false);
        String whtClrAdjacent = mergeColors24(bg24b(142, 142, 0), whtBack, false);

        // The colors of paths to opponent pieces that we can take
        // current player moves tends towards blues
        //
        final String blkTakePath = mergeColors24(targetsShade, blkBack, false);
        final String whtTakePath = mergeColors24(targetsShade, whtBack, false);

        // The colors of opponent paths to our pieces that they can take
        // opponent moves tends towards reds
        final String blkGivePath = mergeColors24(victimsShade, blkBack, false);
        final String whtGivePath = mergeColors24(victimsShade, whtBack, false);

        StringBuilder sb = new StringBuilder("    " + config.player2 + " taken:");
        for (final int type : board.getPiecesTaken1()) {
            sb.append(whtForeB);
            if (type == Piece.Pawn) {
                sb.append(boldAttr);
            }
            sb.append(" ").append(charSetUnicodeWhite[type].trim());
        }
        String takenMsg1 = sb.toString();

        sb = new StringBuilder("    " + config.player1 + " taken:");
        for (final int type : board.getPiecesTaken0()) {
            sb.append(blkForeB);
            if (type == Piece.Pawn) {
                sb.append(boldAttr);
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
            int numProcessed = agent.getNumMovesExamined();
            long numPerSec = (timeSpent == 0) ? numProcessed : (numProcessed / timeSpent);

            String timeLimit = (timeSpent == config.maxSeconds && timeSpent != 0) ? " (hit cfg limit)" : "";

            stat1 = String.format("    Time spent:      %,10ds%s", timeSpent, timeLimit);
            stat2 = String.format("    Moves examined:  %,10d", numProcessed);
            stat3 = String.format("    (per second):    %,10d", numPerSec);
        }

        int side = board.getTurn();
        int otherSide = board.getTurn() == Side.Black ? Side.White : Side.Black;

        // get maps of pieces under attack for both sides
        Map<Move, List<Spot>> targets = createTargetMap(board, side);
        Map<Move, List<Spot>> victims = createTargetMap(board, otherSide);

        // ====================================================================

        if (!forFile) {
            writer.write(cursPos(1, topRow));

            // Turn off the cursor
            writer.write(cursOff);
        }

        if (moveDesc != null) {
            writer.write(moveDesc);
            if (!forFile) {
                writer.write(clearEOL);
            }
        }
        writer.write("\n");

        if (board.getKingInCheck(board.getTurn())) {
            writer.write((side == Side.Black) ? config.player2 : config.player1);
            writer.write(" is in check");
        }
        if (!forFile) {
            writer.write(clearEOL);
        }
        writer.write("\n");

        Move lastMove = board.getLastMove();

        // The color used to highlight the best move
        String bestFromBack = bg24b(0, 160, 160);
        String bestToBack = bg24b(0, 160, 160);

        int bestFromCol = 8;
        int bestFromRow = 8;
        int bestToCol = 8;
        int bestToRow = 8;
        if (!agent.moveSearchIsDone()) {
            Move best = agent.getBestMove();
            if (best != null) {
                bestFromCol = best.getFromCol();
                bestFromRow = best.getFromRow();
                bestFromBack = ((bestFromCol + bestFromRow) % 2) == 0 ?
                        mergeColors24(bestFromBack, blkBack, false) :
                        mergeColors24(bestFromBack, whtBack, false);
                bestToCol = best.getToCol();
                bestToRow = best.getToRow();
                bestToBack = ((bestToCol + bestToRow) % 2) == 0 ?
                        mergeColors24(bestToBack, blkBack, false) :
                        mergeColors24(bestToBack, whtBack, false);
            }
        }

        for (int row = 0; row < 8; ++row) {
            writer.write(resetAll + " " + (8 - row) + " ");

            for (int col = 0; col < 8; ++col) {
                boolean blkSq = (((row + col) % 2) == 0);
                String back = blkSq ? blkBack : whtBack;
                Spot spot = board.getSpot(col, row);
                int type = spot.getType();
                boolean blkPiece = (spot.getSide() == Side.Black);
                String fore = blkPiece ? blkForeW : whtForeW;

                String fmtClrBack = back;
                String fmtClrFore = fore;
                String fmtAttr = "";

                if (type == Piece.Pawn) {
                    fmtAttr = boldAttr;
                }

                if (lastMove.getToCol() == col && lastMove.getToRow() == row) {
                    fmtClrFore = blkPiece ? blkMoved : whtMoved;
                }

                if (type == Piece.King
                        && side == spot.getSide()
                        && board.getKingInCheck(side)) {
                    fmtClrFore = blkPiece ? blkCheck : whtCheck;
                    fmtAttr = boldAttr;
                }

                if (board.getAdjacentBonus()) {
                    for (Spot adjSpot : board.getBlkKingAdjacent()) {
                        if (adjSpot.getCol() == col && adjSpot.getRow() == row) {
                            if (blkSq)
                                fmtClrBack = blkClrAdjacent;
                            else
                                fmtClrBack = whtClrAdjacent;
                            break;
                        }
                    }
                    for (Spot adjSpot : board.getWhtKingAdjacent()) {
                        if (adjSpot.getCol() == col && adjSpot.getRow() == row) {
                            if (blkSq)
                                fmtClrBack = blkClrAdjacent;
                            else
                                fmtClrBack = whtClrAdjacent;
                            break;
                        }
                    }
                }

                String clrTake = (blkSq ? blkTakePath : whtTakePath);
                String clrGive = (blkSq ? blkGivePath : whtGivePath);

                boolean spotIsTarget = isSpotATarget(targets, col, row);
                boolean spotIsVictim = isSpotATarget(victims, col, row);

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
                                ? mergeColors24(clrGive, clrTake, false)
                                : mergeColors24(clrTake, clrGive, false);
                    }
                }

                if (bestFromCol == col && bestFromRow == row) {
                    fmtClrBack = bestFromBack;
                } else if (bestToCol == col && bestToRow == row) {
                    fmtClrBack = bestToBack;
                }

                writer.write(fmtClrBack + fmtClrFore + fmtAttr + charSetUnicodeWhite[type]);
            }
            writer.write(resetAll);
            String[] rowStrings = {boardScore, takenMsg0, takenMsg1, "", "", stat1, stat2, stat3};

            writer.write(rowStrings[row] + "\r\n");

            if (!forFile) {
                writer.write(clearEOL);
            }
        }
        writer.write("    A  B  C  D  E  F  G  H ");
        writer.write("\n");
        writer.write("\n");

        writer.write(getTurnPrompt(board));

        if (board.getCurrentPlayerMoves().size() == 0 ||
                board.getOtherPlayerMoves().size() == 0 ||
                board.checkDrawByRepetition()) {
            writer.write("\r" + getGameSummary(board));
            writer.write(getTotalGameTime(gameStart));
        }

        try {
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return writer.toString();
    }

    private static String getGameSummary(final Board board) {
        String result = "";
        if (board.checkDrawByRepetition()) {
            Move lastMove = board.getLastMove();
            result = String.format("Draw by-repetition!  Move from %s to %s made %d times in a row!\n",
                    posString(lastMove.getFromCol(), lastMove.getFromRow()),
                    posString(lastMove.getToCol(), lastMove.getToRow()),
                    board.getMaxAllowedRepetitions());
        }

        boolean checkMateBlack = board.getKingInCheck(Side.Black);
        boolean checkMateWhite = board.getKingInCheck(Side.White);

        int numMoveBlack = board.getMoves(Side.Black, true).size();
        int numMoveWhite = board.getMoves(Side.White, true).size();

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
        int hours   = (int)(totalTime / 3600L);
        totalTime  -= hours * 3600L;
        int minutes = (int)(totalTime / 60L);
        totalTime  -= minutes * 60L;
        return String.format("Total Game Time: %02d:%02d:%02d\n", hours, minutes, totalTime);
    }

    private static String cfgBg(final String key, final String def) {
        Integer[] cfgRGB = cfgClrStrToAnsi(key);
        return (cfgRGB.length == 3) ? bg24b(cfgRGB[0], cfgRGB[1], cfgRGB[2]) : def;
    }
    private static String cfgFg(final String key, final String def) {
        Integer[] cfgRGB = cfgClrStrToAnsi(key);
        return (cfgRGB.length == 3) ? fg24b(cfgRGB[0], cfgRGB[1], cfgRGB[2]) : def;
    }
    private static Integer[] cfgClrStrToAnsi(final String cfgStr) {
        String[] parts = cfgStr.split(",");
        if (parts.length < 3) {
            return new Integer[] {};
        } else {
            return new Integer[] {
                    Integer.valueOf(parts[0].trim()),
                    Integer.valueOf(parts[1].trim()),
                    Integer.valueOf(parts[2].trim())};
        }
    }

    private static Map<Move, List<Spot>> createTargetMap(final Board board, final int side) {
        // ====================================================================
        // create a map of the current player moves which could
        // take an opponent piece
        Map<Move, List<Spot>> targets = new HashMap<>();
        List<Move> ourMoves = (side == board.getTurn()) ? board.getCurrentPlayerMoves() : board.getOtherPlayerMoves();

        for (Move move:ourMoves) {
            if (config.showCapturesOnly && move.getValue() == 0) continue;

            Spot start = board.getSpot(move.getFromCol(), move.getFromRow());
            int type = start.getType();
            List<Spot> spots = new ArrayList<>();
            int curX = start.getCol();
            int curY = start.getRow();
            int toCol = move.getToCol();
            int toRow = move.getToRow();
            int deltaX;
            int deltaY;

            switch (type) {
                // Single out the pieces that capture using only a
                // destination and not a path of spots to it...
                case Piece.Pawn:
                case Piece.Knight:
                case Piece.King:
                    spots.add(new Spot(board.getSpot(toCol, toRow)));
                    targets.put(move, spots);
                    break;
                case Piece.Rook:
                case Piece.Bishop:
                case Piece.Queen:
                    deltaX = move.getToCol() - move.getFromCol();
                    deltaY = move.getToRow() - move.getFromRow();
                    deltaX = Integer.min(1, Integer.max(deltaX, -1));
                    deltaY = Integer.min(1, Integer.max(deltaY, -1));
                    do {
                        curX += deltaX;
                        curY += deltaY;
                        Spot next = new Spot(start.getSide(), type, curX, curY);
                        spots.add(next);
                    } while (curX != toCol || curY != toRow);
                    targets.put(move, spots);
                    break;
                default:
                    assert false : "We should not get here!! ?";
                    break;
            }
        }
        return targets;
    }

    private static boolean isSpotATarget(final Map<Move, List<Spot>> targets, final int col, final int row) {
        for (Move move:targets.keySet()) {
            List<Spot> path = targets.get(move);
            for (Spot spot:path) {
                if (spot.getCol() == col && spot.getRow() == row) {
                    return true;
                }
            }
        }
        return false;
    }

    private static String getMoveDesc(final Board board, final Move move) {
        if (move == null) return "";

        StringBuilder sb = new StringBuilder();

        Spot from = board.getSpot(move.getFromCol(), move.getFromRow());
        Spot to = board.getSpot(move.getToCol(), move.getToRow());
        int type = from.getType();
        String pieceName = getPieceName(type);
        String suffix = to.isEmpty() ? "" : " capturing " + getPieceName(to.getType());
        if (type == Piece.Pawn) {
            if ((board.getTurn() == Side.Black && move.getToRow() == 7)
             || (board.getTurn() == Side.White && move.getToRow() == 0)) {
                if (!suffix.isEmpty()) suffix += " and ";
                suffix += " is promoted to queen! ";
            }
            if (to.isEmpty() && move.getFromCol() != move.getToCol()) {
                suffix = " En passant capturing Pawn!";
            }
        }
        boolean isCastle = (type == Piece.King) && (Math.abs(move.getFromCol() - move.getToCol()) == 2);

        if (isCastle) {
            sb.append("Castle on ").append((to.getCol() == 6) ? "king" : "queen").append("'s side. ");
        } else {
            sb.append(pieceName)
                    .append(" from ").append(posString(move.getFromCol(), move.getFromRow()))
                    .append(" to ").append(posString(move.getToCol(), move.getToRow()))
                    .append(suffix);
        }

        Board board1 = new Board(board);
        board1.executeMove(move);

        if (board1.getKingInCheck(board1.getTurn())) {
            sb.append((board1.getTurn() == Side.Black) ? config.player1 : config.player2).append(" is in check! ");
        }
        return sb.toString();
    }

    /** A table of piece names */
    private static final String[] typeNames = {"Empty","Pawn","Knight","Bishop","Rook","Queen","King"};

    private static String getPieceName(int type) {
        return typeNames[(type & 0x7)];
    }

    private static String getTurnPrompt(Board board) {
        return String.format("%d: %s's Turn: ",
                board.getNumTurns(),
                (board.getTurn() == Side.White) ? config.player1 : config.player2)
//                + clearEOL
                ;
    }

    private static char toPos(int nibble) {
        return posDigit[(nibble & 0xF)];
    }

    /** A table of position digits */
    private static final char[] posDigit = {
            'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', '8', '7', '6', '5', '4', '3', '2', '1'
    };

    private static String posString(int col, int row) {
        return String.valueOf(new char[] {toPos(col), toPos(row+8)});
    }

    private static Move getHumanMove(final Board board) {
        String prompt = "your move (ex: a2 a3): ";
        String response;

        // Turn the cursor on
        System.out.print(cursOn);

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
            int row1 = 8 - Integer.valueOf(parts[0].substring(1));
            int row2 = 8 - Integer.valueOf(parts[1].substring(1));

            for (Move m:board.getCurrentPlayerMoves()) {
                if (m.getFromCol() == col1 && m.getFromRow() == row1
                    && m.getToCol() == col2 && m.getToRow() == row2) {
                    Spot spot = board.getSpot(col2, row2);
                    int value = spot.isEmpty() ? 0 : Piece.values[spot.getType()];
                    return new Move(col1, row1, col2, row2, value);
                }
            }
            System.out.println("That is not a legal move.");
        }
    }
}

