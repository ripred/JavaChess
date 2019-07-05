package main;

import chess1.*;
import chess1.AI.*;
import static main.Ansi.*;

import sun.misc.SignalHandler;
import sun.misc.Signal;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;
import java.util.List;
import java.util.Map;

import java.io.IOException;

import static java.lang.Math.abs;


public class Main {

    private static ChessConfig config = null;
    private static AIMoveSelector moveAgent = null;

    private static void onAppExit() {
        if (moveAgent != null) {
            try {
                // Reset display attributes
                System.out.print("\r" + resetAll);

                // clear display to end of line
                System.out.print("\r" + clearEOL);

                // Turn the cursor back on
                System.out.println(cursOn);

                moveAgent.close();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                moveAgent = null;
                System.out.println("Goodbye \uD83D\uDE0E");
            }
        }

        if (config != null) {
            config.saveConfiguration();
        }
    }

    public static void main(String[] args) {

        SignalHandler sigHandler = sig -> {
            // handle SIGINT

            onAppExit();
            System.exit(0);
        };
        try {
            Signal.handle(new Signal("INT"), sigHandler);
        } catch (Exception e) {
            e.printStackTrace();
        }

        config = new ChessConfig("chess.properties");
        config.loadConfiguration();
        moveAgent = new Minimax(config.maxThreads, config.maxDepth, config.maxSeconds);

        playGame(moveAgent, config.humanPlayer);

        onAppExit();
    }

    private static void playGame(final AIMoveSelector moveAgent, final boolean isHuman) {
        for (int n=0; n < 20; ++n) {
            System.out.println("\n");
        }

        final Board board = new Board(1);

        board.setMaxAllowedRepetitions(config.maxDrawReps);

        moveAgent.registerDisplayCallback(new ThreadMsgConsumer(board, moveAgent));

        int humanSide = config.humanMovesFirst ? Side.White : Side.Black;
        long gameStart = System.nanoTime();
        long moveStart;
        Move move;

//        board.initTest();

        System.out.println();
        showBoard(board, null, moveAgent, 0);

        System.out.print(getTurnPrompt(board));
        moveStart = System.nanoTime();
        move = getNextPlayersMove(board, moveAgent);

        while (move != null) {
            String moveDesc = getMoveDesc(board, move);

            board.executeMove(move);
            board.advanceTurn();

            System.out.println();
            showBoard(board, moveDesc, moveAgent, moveStart);

            // Check for draw-by-repetition (same made too many times in a row by a player)
            if (board.checkDrawByRepetition()) {
                break;
            }

            System.out.print(getTurnPrompt(board));
            moveStart = System.nanoTime();
            move = getNextPlayersMove(board, moveAgent);
        }

        showGameSummary(board);
        showTotalGameTime(gameStart);
    }

    private static Move getNextPlayersMove(Board board, AIMoveSelector agent) {
        if (config.humanPlayer) {
            if ((board.getTurn() == Side.White) ^ !config.humanMovesFirst) {
                return getHumanMove(board);
            }
        }
        return moveAgent.bestMove(board);
    }

//    static final String[] charSetAscii = {"   "," p "," n "," b "," r "," q "," k "};
    private static final String[] charSetUnicodeWhite = {"   "," ♙ "," ♞ "," ♝ "," ♜ "," ♛ "," ♚ "};

    static void showBoard(final Board board, final String moveDesc, final AIMoveSelector agent, final long startTime) {
        // The screen row to begin displaying on
        int topRow = 1;

        // The background colors of the squares
        String blkBack  = bg24b(142, 142, 142);
        String whtBack  = bg24b(204, 204, 204);

        // The colors of the pieces
        String blkForeB = fg24b( 64,  64,  64);
        String blkForeW = fg24b(  0,   0,   0);
        String whtForeB = fg24b(255, 255, 255);
        String whtForeW = fg24b(255, 255, 255);

        // The colors of the last moved piece
        String blkMoved = fg24b( 128,   0,  0);
        String whtMoved = fg24b( 192, 192,  0);

        // The colors of the kings in check
        String blkCheck = fg24b( 192, 128,  0);
        String whtCheck = fg24b( 192, 128,  0);

        // The color influences on victim and target positions
        String targetsShade = bg24b(  0,    0, 64);
        String victimsShade = bg24b(  64,   0,   0);

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
        final String blkTakePath = mergeColors24(targetsShade, blkBack, false);
        final String whtTakePath = mergeColors24(targetsShade, whtBack, false);

        // The colors of opponent paths to our pieces that they can take
        // opponent moves tends towards reds
        final String blkGivePath = mergeColors24(victimsShade, blkBack, false);
        final String whtGivePath = mergeColors24(victimsShade, whtBack, false);

        final long timeSpent = (System.nanoTime() - startTime) / 1_000_000_000L;
        final int numProcessed = agent.getNumMovesExamined();
        final long numPerSec = (timeSpent == 0) ? numProcessed : (numProcessed / timeSpent);

        final BoardEvaluator evaluator = new PieceValuesPlusPos();

        final int value = evaluator.evaluate(board);

        StringBuilder sb = new StringBuilder("    " + config.player2 + " taken:");
        for (final int type:board.getPiecesTaken1()) {
            sb.append(whtForeB);
            if (type == Piece.Pawn) {
                sb.append(boldAttr);
            }
            sb.append(" " + charSetUnicodeWhite[type].trim());
        }
        String takenMsg1 = sb.toString();

        sb = new StringBuilder("    " + config.player1 + " taken:");
        for (final int type:board.getPiecesTaken0()) {
            sb.append(blkForeB);
            if (type == Piece.Pawn) {
                sb.append(boldAttr);
            }
            sb.append(" " + charSetUnicodeWhite[type].trim());
        }
        String takenMsg0 = sb.toString();

        String stat0 = String.format("    Board value:        %,7d : %s's favor",
                abs(value), ((value < 0) ? config.player2 : ((value == 0) ? "No one" : config.player1)));
        stat0 += clearEOL;

        String stat1 = "";
        String stat2 = "";
        String stat3 = "";

        if (startTime != 0) {
            stat1 = String.format("    Time spent:      %,10ds%s",
                    timeSpent,
                    (timeSpent == config.maxSeconds && timeSpent != 0) ? " (hit cfg limit)" : "");
            stat2 = String.format("    Moves examined:  %,10d", numProcessed);
            stat3 = String.format("    (per second):    %,10d", numPerSec);
        }
        stat1 += clearEOL;

        int otherSide = board.getTurn() == Side.Black ? Side.White :Side.Black;

        // get maps of pieces under attack for both sides
        Map<Move, List<Spot>> targets = createTargetMap(board, board.getTurn());
        Map<Move, List<Spot>> victims = createTargetMap(board, otherSide);

        // ====================================================================

        // Move the cursor to the specified row
        System.out.print(cursPos(1, topRow));

        // Turn off the cursor
        System.out.print(cursOff);

        if (moveDesc != null) {
            System.out.print(moveDesc);
            System.out.print(clearEOL); // clear display to end of line
        }
        System.out.println();

        List<Move> checkMoves = board.kingInCheck(board, board.getTurn());
        if (checkMoves.size() > 0) {
            Move move = checkMoves.get(0);
            System.out.print((board.getTurn() == Side.Black) ? config.player2 : config.player1);
            System.out.print(" is in check from ");
            System.out.println(String.format("%s", posString(move.getFromCol(), move.getFromRow())));
        } else {
            System.out.println(clearEOL);
        }

        for (int row=0; row < 8; ++row) {
            System.out.print(" " + (8-row) + " ");

            for (int col=0; col < 8; ++col) {
                final boolean blkSq = (((row + col) % 2) == 0);
                final String back = blkSq ? blkBack : whtBack;
                final Spot spot = board.getSpot(col, row);
                final int type = spot.getType();
                final boolean blkPiece = (spot.getSide() == Side.Black);
                final String fore = blkPiece ? blkForeW : whtForeW;

                String fmtClrBack = back;
                String fmtClrFore = fore;
                String fmtAttr = "";

                if (type == Piece.Pawn) {
                    fmtAttr = boldAttr;
                }

                Move lastMove = board.getLastMove();
                if (lastMove.getToCol() == col && lastMove.getToRow() == row) {
                    fmtClrFore = blkPiece ? blkMoved : whtMoved;
                }

                if (type == Piece.King
                        && board.getTurn() == spot.getSide()
                        && board.kingInCheck(board, board.getTurn()).size() > 0) {
                    fmtClrFore = ((spot.getSide() == Side.Black) ? blkCheck : whtCheck);
                    fmtAttr = boldAttr;
                }

                String clrTake = (blkSq ? blkTakePath : whtTakePath);
                String clrGive = (blkSq ? blkGivePath : whtGivePath);

                boolean spotIsTarget = isSpotATarget(targets, col, row);
                boolean spotIsVictim = isSpotATarget(victims, col, row);

                boolean showTargets = config.showTargetPaths;
                boolean showVictims = config.showVictimPaths;
                boolean flipPerspective = (board.getTurn() == Side.Black);

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
                System.out.print(fmtClrBack + fmtClrFore + fmtAttr + charSetUnicodeWhite[type]);
            }
            System.out.print(resetAll);
            String[] rowStrings = {takenMsg0, takenMsg1, "", "", stat0, stat1, stat2, stat3};
            System.out.println(rowStrings[row]);
        }
        System.out.println("    A  B  C  D  E  F  G  H ");
        System.out.println();
    }

    private static void showGameSummary(final Board board) {
        if (board.checkDrawByRepetition()) {
            Move lastMove = board.getLastMove();
            System.out.println(String.format("Draw by-repetition!  Move from %s to %s made %d times in a row!",
                    posString(lastMove.getFromCol(), lastMove.getFromRow()),
                    posString(lastMove.getToCol(), lastMove.getToRow()),
                    board.getMaxAllowedRepetitions()));
        }

        List<Move> checkMateBlack = board.kingInCheck(board, Side.Black);
        List<Move> checkMateWhite = board.kingInCheck(board, Side.White);
        int numMoveBlack = board.getMoves(Side.Black, true).size();
        int numMoveWhite = board.getMoves(Side.White, true).size();

        assert numMoveBlack > 0 || numMoveWhite > 0 : "Internal error: Both players have 0 moves";

        if (checkMateBlack.size() > 0 && checkMateWhite.size() > 0) {
            System.out.println("Internal error: Both players are in check");
            System.out.println(config.player1 + "'s King in check from:");
            for (Move m:checkMateWhite)
                System.out.println("    " + m.toString());
            System.out.println();

            System.out.println(config.player2 + "'s King in check from:");
            for (Move m:checkMateBlack)
                System.out.println("    " + m.toString());
            System.out.println();
        }

        System.out.println();

        if (numMoveWhite == 0) {
            System.out.println(((checkMateWhite.size() > 0) ? "Checkmate!  " : "Stalemate!  ") + config.player2 + " Wins!");
        } else if (numMoveBlack == 0) {
            System.out.println(((checkMateBlack.size() > 0) ? "Checkmate!  " : "Stalemate!  ") + config.player1 + " Wins!");
        }
    }
    private static void showTotalGameTime(long startTime) {
        long totalTime = (System.nanoTime() - startTime) / 1_000_000_000L;
        int hours   = (int)(totalTime / 3600L);
        totalTime  -= hours * 3600L;
        int minutes = (int)(totalTime / 60L);
        totalTime  -= minutes * 60L;
        System.out.println(String.format("Total Game Time: %02d:%02d:%02d", hours, minutes, totalTime));
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
        List<Move> ourMoves = (side == board.getTurn()) ? board.getCurrentPlayerMoves() : board.getMovesSorted(side);

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
        StringBuilder sb = new StringBuilder();

        if (move != null) {
            Spot from = board.getSpot(move.getFromCol(), move.getFromRow());
            Spot to = board.getSpot(move.getToCol(), move.getToRow());
            int type = from.getType();
            String pieceName = getPieceName(type);
            String cap = to.isEmpty() ? "" : " capturing " + getPieceName(to.getType());
            boolean isCastle = (type == Piece.King) && abs(move.getFromCol() - move.getToCol()) == 2;

            if (isCastle) {
                sb.append("Castle on ").append((from.getCol() == 4) ? "king" : "queen").append("'s side. ");
            } else {
                sb.append(pieceName)
                        .append(" from ").append(posString(move.getFromCol(), move.getFromRow()))
                        .append(" to ").append(posString(move.getToCol(), move.getToRow()))
                        .append(cap);
            }
        }

        Board board1 = new Board(board);
        board1.executeMove(move);

        if (board1.kingInCheck(board1, board1.getTurn()).size() > 0) {
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
                + clearEOL;
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

