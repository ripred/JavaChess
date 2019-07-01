package main;

import static main.Ansi.*;

//import chess1.*;
import chess1.ChessConfig;
import chess1.Board;
import chess1.Piece;
import chess1.Move;
import chess1.Spot;
import chess1.Side;

//import chess1.AI.*;
import chess1.AI.PieceValuesPlusPos;
import chess1.AI.AIMoveSelector;
import chess1.AI.BoardEvaluator;
import chess1.AI.Minimax;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;
import java.util.List;
import java.util.Map;

import java.util.function.Consumer;
import java.io.IOException;

import sun.misc.Signal;
import sun.misc.SignalHandler;

import static java.lang.Math.abs;

class MyCallback implements Consumer<String> {
    private static Board board;
    private static AIMoveSelector agent;

    MyCallback(Board board, AIMoveSelector agent) {
        MyCallback.board = board;
        MyCallback.agent = agent;
    }

    @Override
    public void accept(String s) {
        Main.show(board, s, agent, 0);
    }
}


public class Main {

    // Playing with JNI !
    //
    public native void CGateway();

    static {
        System.loadLibrary("native");
    }

    private static AIMoveSelector moveAgent = null;

    private static ChessConfig config;

    private static void onExit() {
        if (moveAgent != null) {
            try {
                // Reset display attributes
                System.out.print("\r" + resetAll);

                // clear display to end of line
                System.out.print("\r" + clearEOL);

                // Turn the cursor back on
                System.out.println(cursOn);

                System.out.println("Max threads used at once: " + moveAgent.getMaxThreads());

                moveAgent.close();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                moveAgent = null;
                System.out.println("Goodbye");
            }
        }

        if (config != null) {
            config.saveConfiguration();
        }
    }

    public static void main(String[] args) {

        config = new ChessConfig("chess.properties");
        config.loadConfiguration();

        final int depth = config.maxDepth;
//        final int maxSeconds = 7 * 60;
        final int maxSeconds = config.maxSeconds;
        final int maxThreads = config.maxThreads;

        moveAgent = new Minimax(maxThreads, depth, maxSeconds);

        SignalHandler sigHandler = sig -> {
            // handle SIGINT

            onExit();
            System.exit(0);
        };

        try {
            Signal.handle(new Signal("INT"), sigHandler);
        } catch (Exception e) {
            e.printStackTrace();
        }


        // call our C++ code just for fun: 😎
        new Main().CGateway();

        final boolean isHuman = config.humanPlayer;

        while (true) {
            playGame(moveAgent, isHuman);
            if (!isHuman) {
                try {
                    while (System.in.available() > 0) System.in.readAllBytes();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                print("New game starts in 5 seconds - press any key to exit...");
                for (int i = 0; i < 500; ++i) {
                    try {
                        try {
                            if (System.in.available() > 0) break;
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        Thread.sleep(10);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                try {
                    if (System.in.available() > 0) break;
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            System.out.print(Ansi.cursPos(1, 12));
            System.out.println(clearEOL);
            System.out.println(clearEOL);
            System.out.println(clearEOL);
            System.out.println(clearEOL);
            System.out.println(clearEOL);
        }

        onExit();
    }

    private static void playGame(AIMoveSelector moveAgent, final boolean isHuman) {
        System.out.println("\n".repeat(20));

        final Board board = new Board(1);

        final MyCallback callback = new MyCallback(board, moveAgent);
        moveAgent.registerDisplayCallback(callback);

        final int humanSide = Side.White;

        boolean showHistory = false;
        final int maxRepetitions = 3;
        boolean drawByRepetition = false;
        long gameTime = System.nanoTime();

        long startTime;
        Move move;

//        board.initTest();

        print();
        show(board, null, moveAgent, 0);

        String prompt = getTurnPrompt(board);
        System.out.print(prompt);

        startTime = System.nanoTime();

        if (isHuman && board.getTurn() == humanSide) {
            move = getHumanMove(board);
        } else {
            move = moveAgent.bestMove(board);
        }

        while (move != null) {
            String moveDesc = getMoveDesc(board, move);

            board.executeMove(move);
            if (board.kingInCheck(board, board.getTurn()).size() > 0) {
                print("Internal error.  Somehow we moved into check!");
            }
            board.advanceTurn();
            print();
            show(board, moveDesc, moveAgent, startTime);

            // Check for draw-by-repetition (same made too many times in a row by a player)
            drawByRepetition = board.checkDrawByRepetition(maxRepetitions);
            if (drawByRepetition) {
                break;
            }

            prompt = getTurnPrompt(board);
            System.out.print(prompt);

            startTime = System.nanoTime();

            if (isHuman && board.getTurn() == humanSide) {
                move = getHumanMove(board);
            } else {
                move = moveAgent.bestMove(board);
            }
        }

        if (drawByRepetition) {
            Move lastMove = board.getLastMove();
            print(String.format("Draw by-repetition!  Move from %c%d to %c%d made %d time in a row!",
                    lastMove.getFromCol() + 'a',
                    8 - lastMove.getFromRow(),
                    lastMove.getToCol() + 'a',
                    8 - lastMove.getToRow(),
                    maxRepetitions));
        }

        List<Move> checkMateBlack = board.kingInCheck(board, Side.Black);
        List<Move> checkMateWhite = board.kingInCheck(board, Side.White);
        int numMoveBlack = board.getMoves(0, true).size();
        int numMoveWhite = board.getMoves(1, true).size();

        assert numMoveBlack > 0 || numMoveWhite > 0 : "Internal error: Both players have 0 moves";

        if (checkMateBlack.size() > 0 && checkMateWhite.size() > 0) {
            print("Internal error: Both players are in check");
            print("White King in check from:");
            for (Move m:checkMateWhite)
                print("    " + m.toString());
            print();

            print("Black King in check from:");
            for (Move m:checkMateBlack)
                print("    " + m.toString());
            print();
        }

        print();

        if (numMoveWhite == 0) {
            if (checkMateWhite.size() > 0) {
                print("Checkmate!  Black Wins!");
            } else {
                print("Stalemate!  Black Wins!");
            }
        } else if (numMoveBlack == 0) {
            if (checkMateBlack.size() > 0) {
                print("Checkmate!  White Wins!");
            } else {
                print("Stalemate!  White Wins!");
            }
        }

        if (showHistory) {
            List<Move> history = board.getMoveHistory();
            int numMoves = history.size();
            print("Game History:");
            print(" White         Black");
            for (int i = 0; i < numMoves; ++i) {
                Move m = history.get(i);
                if (i % 2 == 0)
                    System.out.print(String.format("%3d ", 1 + i));
                System.out.print(String.format("%c%d to %c%d     ",
                        m.getFromCol() + 'a',
                        8 - m.getFromRow(),
                        m.getToCol() + 'a',
                        8 - m.getToRow()));
                if (i % 2 == 1)
                    System.out.println();
            }
            print();
        }

        int hours = 0;
        int minutes = 0;
        int seconds;
        long totalTime = (System.nanoTime() - gameTime) / 1_000_000_000L;
        while (totalTime >= 60 * 60) {
            hours++;
            totalTime -= 60 * 60;
        }
        while (totalTime >= 60) {
            minutes++;
            totalTime -= 60;
        }
        seconds = (int) totalTime;
        print(String.format("Total Game Time: %02d:%02d:%02d", hours, minutes, seconds));
    }

    private static void print() {
        System.out.println();
    }
    private static void print(String s) {
        System.out.println(s);
    }
    private static String getPieceName(final int type) {
        switch (type) {
            case Piece.Empty:
                return "Empty";
            case Piece.Pawn:
                return "Pawn";
            case Piece.Knight:
                return "Knight";
            case Piece.Bishop:
                return "Bishop";
            case Piece.Rook:
                return "Rook";
            case Piece.Queen:
                return "Queen";
            case Piece.King:
                return "King";
            default:
                return "Unknown";
        }
    }
    private static String getTurnPrompt(Board board) {
        return String.format("%d: %s's Turn: ",
                board.getNumTurns(),
                (board.getTurn() == Side.White) ? "White" : "Black");
    }
    private static String getMoveDesc(final Board board, final Move move) {
        StringBuilder sb = new StringBuilder();

        if (move != null) {
            Spot from = board.getSpot(move.getFromCol(), move.getFromRow());
            Spot to = board.getSpot(move.getToCol(), move.getToRow());
            int type = from.getType();
            String pieceName = getPieceName(type);
            String cap = to.isEmpty() ? "" : " capturing " + getPieceName(to.getType());
            boolean isCastle = type == Piece.King
                    && (move.getFromCol() - move.getToCol() == 2 || move.getFromCol() - move.getToCol() == -2);

            if (isCastle) {
                if (from.getCol() == 4) {
                    sb.append("Castle on king's side. ");
                } else {
                    sb.append("Castle on queen's side. ");
                }
            } else {
                sb.append(String.format("%s from %c%d to %c%d%s. ",
                        pieceName,
                        move.getFromCol() + 'A',
                        8 - move.getFromRow(),
                        move.getToCol() + 'A',
                        8 - move.getToRow(),
                        cap));
            }
        }

        Board board1 = new Board(board);
        board1.executeMove(move);

        if (board1.kingInCheck(board1, board1.getTurn()).size() > 0) {
            if (board1.getTurn() == Side.Black) {
                sb.append("White is in check! ");
            } else {
                sb.append("Black is in check! ");
            }
        }

        return sb.toString();
    }
    private static Move getHumanMove(final Board board) {
        String prompt = "Enter your move (ex: a2 a3): ";
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

    private static Map<Move, List<Spot>> createTargetMap(final Board board, final int side, List<Integer> types) {
        // ====================================================================
        // create a map of the current player moves which could
        // take an opponent piece
        Map<Move, List<Spot>> targets = new HashMap<>();

        List<Move> ourMoves;

        if (side == board.getTurn()) {
            ourMoves = board.getCurrentPlayerMoves();
        } else {
            ourMoves = board.getMovesSorted(side);
        }

        for (Move move:ourMoves) {
            Spot start = board.getSpot(move.getFromCol(), move.getFromRow());
            if (!types.contains(start.getType()))
                continue;

//            if (move.getValue() == 0) continue;

            List<Spot> spots = new ArrayList<>();
            int deltaX;
            int deltaY;
            int curX = start.getCol();
            int curY = start.getRow();
            int toCol = move.getToCol();
            int toRow = move.getToRow();

            switch (start.getType()) {
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
                        Spot next = new Spot(start.getSide(), start.getType(), curX, curY);
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

    public static void show(final Board board, final String moveDesc, final AIMoveSelector agent, final long startTime) {
//        final String[] charSetAscii = {"?","p","k","b","r","q","k"};
//        final String tmpWhitePawn = "♟";
        final String[] charSetUnicodeWhite = {"?","♙","♞","♝","♜","♛","♚"};
//        final String[] charSetUnicodeBlack = {"?","♙","♘","♗","♖","♕","♔"};
//        final boolean useUnicode = true;

        // The screen row to begin displaying on
        int topRow = 1;

        // The background colors of the squares
        final String blkBack  = bg24b(142, 142, 142);
        final String whtBack  = bg24b(204, 204, 204);

        // The colors of the pieces
        final String blkForeB = fg24b( 64,  64,  64);
        final String blkForeW = fg24b(  0,   0,   0);
        final String whtForeB = fg24b(255, 255, 255);
        final String whtForeW = fg24b(255, 255, 255);

        // The colors of the last moved piece
        final String blkMoved = fg24b( 128,   0,  0);
        final String whtMoved = fg24b( 192, 192,  0);

        // The colors of the kings in check
        final String blkCheck = fg24b( 192, 128,  0);
        final String whtCheck = fg24b( 192, 128,  0);

        // The colors of paths to opponent pieces that we can take
        // current player moves tends towards blues
        //
        final String blueInfluence = bg24b(  0,    0, 64);
        final String blkTakePath = mergeColors24(blueInfluence, blkBack, false);
        final String whtTakePath = mergeColors24(blueInfluence, whtBack, false);

        // The colors of opponent paths to our pieces that they can take
        // opponent moves tends towards reds
        final String redInfluence = bg24b(  64,   0,   0);
        final String blkGivePath = mergeColors24(redInfluence, blkBack, false);
        final String whtGivePath = mergeColors24(redInfluence, whtBack, false);

        final long timeSpent = (System.nanoTime() - startTime) / 1_000_000_000L;
        final int numProcessed = agent.getNumMovesExamined();
        final long numPerSec = (timeSpent == 0) ? numProcessed : (numProcessed / timeSpent);

        final BoardEvaluator evaluator = new PieceValuesPlusPos();

        final int value = evaluator.evaluate(board);

        StringBuilder sb = new StringBuilder("    Black player taken: ");
        for (final int type:board.getPiecesTaken1()) {
            sb.append(whtForeB);
            if (type == Piece.Pawn) {
                sb.append(boldAttr);
            }
            sb.append(charSetUnicodeWhite[type]).append(" ");
        }
        final String takenMsg1 = sb.toString();

        sb = new StringBuilder("    White player taken: ");
        for (final int type:board.getPiecesTaken0()) {
            sb.append(blkForeB);
            if (type == Piece.Pawn) {
                sb.append(boldAttr);
            }
            sb.append(charSetUnicodeWhite[type]).append(" ");
        }
        final String takenMsg0 = sb.toString();

        final String stat0 = String.format("    Board value:         %,7d : %s's favor",
                abs(value), ((value < 0) ? "Black" : ((value == 0) ? "No one" : "White")));

        String stat1 = "";
        String stat2 = "";
        String stat3 = "";

        if (startTime != 0) {
            stat1 = String.format("    Time Spent:       %,10d seconds", timeSpent);
            stat2 = String.format("    Moves examined:   %,10d", numProcessed);
            stat3 = String.format("    Moves per second: %,10d", numPerSec);
        }

        int otherSide = board.getTurn() == Side.Black ? Side.White :Side.Black;

        // get a maps of pieces under attack
        List<Integer> showOurTypes = new ArrayList<>();
        int[] ourTypes = {
                Piece.Pawn,
                Piece.Rook,
                Piece.Knight,
                Piece.Bishop,
                Piece.Queen,
                Piece.King
        };
        for (int t:ourTypes) {
            showOurTypes.add(t);
        }

        List<Integer> showOpponentTypes = new ArrayList<>();
        int[] opponentTypes = {
                Piece.Pawn,
                Piece.Rook,
                Piece.Knight,
                Piece.Bishop,
                Piece.Queen,
                Piece.King
        };
        for (int t:opponentTypes) {
            showOpponentTypes.add(t);
        }

        Map<Move, List<Spot>> targets = createTargetMap(board, board.getTurn(), showOurTypes);
        Map<Move, List<Spot>> victims = createTargetMap(board, otherSide, showOpponentTypes);

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
            System.out.print((board.getTurn() == Side.Black) ? "Black" : "White");
            System.out.print(" is in check from ");
            System.out.println(String.format("%c%d",
                    move.getFromCol() + 'a',
                    8 - move.getFromRow()));
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
                    if (blkPiece) {
                        fmtClrFore = blkMoved;
                        fmtAttr = boldAttr;
                    } else {
                        fmtClrFore = whtMoved;
                        fmtAttr = boldAttr;
                    }
                }

                if (type == Piece.King
                    && board.getTurn() == spot.getSide()
                    && board.kingInCheck(board, board.getTurn()).size() > 0) {
                        fmtClrFore = (spot.getSide() == Side.Black ? blkCheck : whtCheck);
                        fmtAttr = boldAttr;
                }

                String clrTake = (blkSq ? blkTakePath : whtTakePath);
                String clrGive = (blkSq ? blkGivePath : whtGivePath);

                boolean spotIsTarget = isSpotATarget(targets, col, row);
                boolean spotIsVictim = isSpotATarget(victims, col, row);

                boolean showTargets = true;
                boolean showVictims = true;
                boolean keepPerspective = board.getTurn() == Side.Black;

                if (spotIsTarget && !spotIsVictim) {
                    if (showTargets) {
                        if (keepPerspective)
                            fmtClrBack = clrGive;
                        else
                            fmtClrBack = clrTake;
                    }
                } else if (!spotIsTarget && spotIsVictim) {
                    if (showVictims) {
                        if (keepPerspective)
                            fmtClrBack = clrTake;
                        else
                            fmtClrBack = clrGive;
                    }
                } else if (spotIsTarget && spotIsVictim) {
                    if (showTargets || showVictims) {
                        if (keepPerspective)
                            fmtClrBack = mergeColors24(clrGive, clrTake, false);
                        else
                            fmtClrBack = mergeColors24(clrTake, clrGive, false);
                    }
                }

                final String chr = spot.isEmpty() ? " " : charSetUnicodeWhite[type];
                final String txt = (fmtClrBack + fmtClrFore + fmtAttr + " ") + chr + " ";
                System.out.print(txt);
            }
            System.out.print(resetAll);

            if (row == 0) {
                System.out.print(takenMsg1);
            } else if (row == 1) {
                System.out.print(takenMsg0);
            } else if (row == 4) {
                System.out.print(stat0);
                System.out.print(clearEOL); // clear display to end of line
            } else if (row == 5) {
                System.out.print(stat1);
            } else if (row == 6) {
                System.out.print(stat2);
            } else if (row == 7) {
                System.out.print(stat3);
            }
            System.out.println();
        }
        System.out.println("    A  B  C  D  E  F  G  H ");
        print();
    }
}

