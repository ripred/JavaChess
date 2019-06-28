package main;

import chess1.*;

import sun.misc.Signal;
import sun.misc.SignalHandler;

import static java.lang.Math.abs;
import static java.lang.Math.sqrt;
import static java.lang.System.exit;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.function.Consumer;

class MyCallback implements Consumer<String> {
    static Board board;
    static AIMoveSelector agent;

    MyCallback(Board board, AIMoveSelector agent) {
        this.board = board;
        this.agent = agent;
    }

    @Override
    public void accept(String s) {
        Main.show(board, s, agent, 0);
    }
}

public class Main {
    private static final String csi = new String(new byte[]{0x1b, '['});

    // Playing with JNI !
    //
    public native void CGateway();

    static {
        System.loadLibrary("native");
    }


    public static void main(String[] args) {
        SignalHandler sigHandler = sig -> {
            // handle SIGINT

            // Reset display attributes
            System.out.print("\r" + csi + "0m");

            // clear display to end of line
            System.out.print("\r" + csi + "K");

            // Turn the cursor on
            System.out.println(csi + "?25h");

            System.out.println("Goodbye");
            exit(0);
        };
        Signal.handle(new Signal("INT"), sigHandler);

        // call our C++ code just for fun: 😎
        new Main().CGateway();

        final boolean isHuman = false;



        while (true) {
            playGame(isHuman);
            if (!isHuman) {
                try {
                    if (System.in.available() > 0)
                        System.in.readAllBytes();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                print("New game starts in 5 seconds - press any key to exit...");
                for (int i = 0; i < 500; ++i) {
                    try {
                        try {
                            if (System.in.available() > 0)
                                break;
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        Thread.sleep(10);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                try {
                    if (System.in.available() > 0)
                        break;
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        print();
        print("Goodbye");

        // Turn the cursor on
        System.out.print(csi + "?25h");
    }

    private static void playGame(final boolean isHuman) {
        for (int i = 0; i < 25; ++i) {
            System.out.println();
        }

        final int depth = 4;
        final int maxSeconds = 10 * 60;
        final Board board = new Board(1);
        final Minimax moveAgent = new Minimax(depth, maxSeconds);

        final MyCallback callback = new MyCallback(board, moveAgent);
        moveAgent.registerDisplayCallback(callback);

        final int humanSide = Side.White;

        boolean showHistory = false;
        final int maxRepetitions = 3;
        boolean drawByRepetition = false;
        long gameTime = System.nanoTime();
        ;
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
        int seconds = 0;
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
        print(csi + "0m");

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
        print(csi + "0m");

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
                print(csi + "0m");
            }
        }
        print();

        print(csi + "1m Bold             Rendition" + csi + "0m");
        print(csi + "2m Faint            Rendition" + csi + "0m");
        print(csi + "4m Underline        Rendition" + csi + "0m");
        print(csi + "22m Neither          Rendition" + csi + "0m");
        print(csi + "7m Reverse Video    Rendition" + csi + "0m");
        print(csi + "9m Crossed Out      Rendition" + csi + "0m");

        print();
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
        System.out.print(csi + "?25h");

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

    private static String makeFore24(final int r, final int g, final int b) {
        return csi + "38;2;" + r + ";" + g + ";" + b + "m";
    }
    private static String makeBack24(final int r, final int g, final int b) {
        return csi + "48;2;" + r + ";" + g + ";" + b + "m";
    }
    private static String mergeColors24(String clr1, String clr2, final boolean fore) {
        String[] parts1 = clr1.split(";");
        String[] parts2 = clr2.split(";");

        int r1 = Integer.valueOf(parts1[2]);
        int g1 = Integer.valueOf(parts1[3]);
        int b1 = Integer.valueOf(parts1[4].substring(0, parts1[4].length() - 1));

        int r2 = Integer.valueOf(parts2[2]);
        int g2 = Integer.valueOf(parts2[3]);
        int b2 = Integer.valueOf(parts2[4].substring(0, parts2[4].length() - 1));

        double pr1 = (double) r1 / 256.0;
        double pg1 = (double) g1 / 256.0;
        double pb1 = (double) b1 / 256.0;

        double pr2 = (double) r2 / 256.0;
        double pg2 = (double) g2 / 256.0;
        double pb2 = (double) b2 / 256.0;

        double r3 = ((pr1 + pr2) / 2.0);
        double g3 = ((pg1 + pg2) / 2.0);
        double b3 = ((pb1 + pb2) / 2.0);

        if (fore) {
            return makeFore24((int)(r3 * 256.0), (int)(g3 * 256.0), (int)(b3 * 256.0));
        } else {
            return makeBack24((int)(r3 * 256.0), (int)(g3 * 256.0), (int)(b3 * 256.0));
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

        final String rstAttr   = csi + "0m";
        final String boldAttr  = csi + "1m";
//        final String faintAttr = csi + "2m";
//        final String underAttr = csi + "4m";
//        final String noneAttr  = csi + "22m";
//        final String revAttr   = csi + "7m";
//        final String crossAttr = csi + "9m";

        // The screen row to begin displaying on
        int topRow = 1;

        // The background colors of the squares
        final String blkBack  = makeBack24(142, 142, 142);
        final String whtBack  = makeBack24(204, 204, 204);

        // The colors of the pieces
        final String blkForeB = makeFore24( 64,  64,  64);
        final String blkForeW = makeFore24(  0,   0,   0);
        final String whtForeB = makeFore24(255, 255, 255);
        final String whtForeW = makeFore24(255, 255, 255);

        // The colors of the last moved piece
        final String blkMoved = makeFore24( 128,   0,  0);
        final String whtMoved = makeFore24( 192, 192,  0);

        // The colors of the kings in check
        final String blkCheck = makeFore24( 192, 128,  0);
        final String whtCheck = makeFore24( 192, 128,  0);

        // The colors of paths to opponent pieces that we can take
        // current player moves tends towards blues
        //
        final String blueInfluence = makeBack24(  0,    0, 64);
        final String blkTakePath = mergeColors24(blueInfluence, blkBack, false);
        final String whtTakePath = mergeColors24(blueInfluence, whtBack, false);

        // The colors of opponent paths to our pieces that they can take
        // opponent moves tends towards reds
        final String redInfluence = makeBack24(  64,   0,   0);
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
        System.out.print(csi + String.format("%d;1f", topRow));

        // Turn off the cursor
        System.out.print(csi + "?25l");

        if (moveDesc != null) {
            System.out.print(moveDesc);
            System.out.print(csi + "K"); // clear display to end of line
        }
        System.out.println();
        System.out.println();

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
            System.out.print(rstAttr);

            if (row == 0) {
                System.out.print(takenMsg1);
            } else if (row == 1) {
                System.out.print(takenMsg0);
            } else if (row == 4) {
                System.out.print(stat0);
                System.out.print(csi + "K"); // clear display to end of line
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
