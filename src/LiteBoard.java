// Lightweight class to be used during minimax traversals.
// Has just what is needed to keep board state and moves and nothing more
// so it can be passed and cloned faster.  All straight arrays.
//
// No setters/getters.  All free love.  We're all adults here.  No hand holding. Don't screw up.

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static java.lang.Math.abs;

public class LiteBoard {
    public static final int BOARD_SIZE = 64;

    public byte[] board;
    public Move[] moves1;
    public Move[] moves2;
    public Move[] history;
    public byte[] taken1;
    public byte[] taken2;
    public int numTaken1;
    public int numTaken2;
    public int numMoves1;
    public int numMoves2;
    public int numHist;
    public int maxRep;
    public int turns;
    public int turn;
    public Move lastMove;
    public int blkKingLoc;
    public int whtKingLoc;
    public int numPieces1;
    public int numPieces2;
    public byte[] pieces1;
    public byte[] pieces2;


    // utility functions for board location attributes like piece side, piece type, etc
    final public static int  Empty  = 0b00000000;
    final public static int  Pawn   = 0b00000001;
    final public static int  Knight = 0b00000010;
    final public static int  Bishop = 0b00000011;
    final public static int  Rook   = 0b00000100;
    final public static int  Queen  = 0b00000101;
    final public static int  King   = 0b00000110;
    final public static int  Marker = 0b00000111;

    public boolean  isEmpty(int ndx) {
        return (board[ndx] & LiteUtil.Type) == Empty;
    }

    public int      getType(int ndx) {
        return board[ndx] & LiteUtil.Type;
    }

    public int      getSide(int ndx) {
        int result = (((board[ndx] & LiteUtil.Side) >>> 4) & 0x01);
        return result;
    }

    public boolean  hasMoved(int ndx) {
        return (board[ndx] & LiteUtil.Moved) != 0;
    }

    public int      getValue(int ndx) {
        return LiteUtil.getValue(board[ndx]);
    }

    public boolean  inCheck(int ndx) {
        return LiteUtil.inCheck(board[ndx]);
    }

    public void     setType(int ndx, int type) {
        board[ndx] = LiteUtil.setType(board[ndx], type);
    }

    public void     setSide(int ndx, int side) {
        board[ndx] = LiteUtil.setSide(board[ndx], side);
    }

    public void     setMoved(int ndx, boolean hasMoved) {
        board[ndx] = LiteUtil.setMoved(board[ndx], hasMoved);
    }

    public void     setCheck(byte ndx, boolean inCheck) {
        board[ndx] = LiteUtil.setCheck(board[ndx], inCheck);
    }


    /**
     * Copy-constructor for new LiteBoard objects
     *
     * @param orig the LiteBoard object to copy from to create this new board instance
     */
    public LiteBoard(LiteBoard orig) {
        board   = new byte[BOARD_SIZE];
        moves1  = new Move[256];
        moves2  = new Move[256];
        pieces1 = new byte[16];
        pieces2 = new byte[16];
        taken1  = new byte[16];
        taken2  = new byte[16];
        history = new Move[256];

        numMoves1  = orig.numMoves1;
        numMoves2  = orig.numMoves2;

        numPieces1 = orig.numPieces1;
        numPieces2 = orig.numPieces2;

        numTaken1  = orig.numTaken1;
        numTaken2  = orig.numTaken2;

        numHist    = orig.numHist;

        turn       = orig.turn;
        turns      = orig.turns;
        maxRep     = orig.maxRep;
        lastMove   = new Move(orig.lastMove);

        blkKingLoc = orig.blkKingLoc;
        whtKingLoc = orig.whtKingLoc;

        System.arraycopy(orig.board,   0, board,   0, BOARD_SIZE);

        for (int ndx=0; ndx < numMoves1; ndx++) {
            moves1[ndx] = new Move(orig.moves1[ndx]);
        }

        for (int ndx=0; ndx < numMoves2; ndx++) {
            moves2[ndx] = new Move(orig.moves2[ndx]);
        }

        System.arraycopy(orig.pieces1,   0, pieces1, 0, numPieces1);
        System.arraycopy(orig.pieces2,   0, pieces2, 0, numPieces2);

        System.arraycopy(orig.taken1,   0, taken1,   0, numTaken1);
        System.arraycopy(orig.taken2,   0, taken2,   0, numTaken2);

        System.arraycopy(orig.history, 0, history, 0, numHist);
    }


    /**
     * Default constructor for a new LiteBoard object.
     * Note: This initializes the contents of the board to contain the pieces for
     * the two sides arranged for a new game.
     */
    public LiteBoard() {
        board   = new byte[BOARD_SIZE];
        moves1  = new Move[256];
        moves2  = new Move[256];
        pieces1 = new byte[16];
        pieces2 = new byte[16];
        taken1  = new byte[16];
        taken2  = new byte[16];
        history = new Move[256];

        lastMove = new Move(8, 8, 8, 8, 0);
        maxRep = 3;
        turn = Side.White;
        turns = 1;

        numMoves1 = moves1.length;
        for (int i = 0; i < numMoves1; i++) {
            moves1[i] = new Move(0, 0, 0, 0, 0);
        }
        numMoves1 = 0;

        numMoves2 = moves2.length;
        for (int i = 0; i < numMoves2; i++) {
            moves2[i] = new Move(0, 0, 0, 0, 0);
        }
        numMoves2 = 0;

        numHist = history.length;
        for (int i = 0; i < numHist; i++) {
            history[i] = new Move(0, 0, 0, 0, 0);
        }
        numHist = 0;

        numTaken1 = 0;
        numTaken2 = 0;

        numPieces1 = 0;
        numPieces2 = 0;

        blkKingLoc = 4 + 0 * 8;
        whtKingLoc = 4 + 7 * 8;

        board[0 + 0 * 8] = LiteUtil.newSpot(LiteBoard.Rook,     Side.Black, false, false);
        board[1 + 0 * 8] = LiteUtil.newSpot(LiteBoard.Knight,   Side.Black, false, false);
        board[2 + 0 * 8] = LiteUtil.newSpot(LiteBoard.Bishop,   Side.Black, false, false);
        board[3 + 0 * 8] = LiteUtil.newSpot(LiteBoard.Queen,    Side.Black, false, false);
        board[4 + 0 * 8] = LiteUtil.newSpot(LiteBoard.King,     Side.Black, false, false);
        board[5 + 0 * 8] = LiteUtil.newSpot(LiteBoard.Bishop,   Side.Black, false, false);
        board[6 + 0 * 8] = LiteUtil.newSpot(LiteBoard.Knight,   Side.Black, false, false);
        board[7 + 0 * 8] = LiteUtil.newSpot(LiteBoard.Rook,     Side.Black, false, false);
        board[0 + 1 * 8] = LiteUtil.newSpot(LiteBoard.Pawn,     Side.Black, false, false);
        board[1 + 1 * 8] = LiteUtil.newSpot(LiteBoard.Pawn,     Side.Black, false, false);
        board[2 + 1 * 8] = LiteUtil.newSpot(LiteBoard.Pawn,     Side.Black, false, false);
        board[3 + 1 * 8] = LiteUtil.newSpot(LiteBoard.Pawn,     Side.Black, false, false);
        board[4 + 1 * 8] = LiteUtil.newSpot(LiteBoard.Pawn,     Side.Black, false, false);
        board[5 + 1 * 8] = LiteUtil.newSpot(LiteBoard.Pawn,     Side.Black, false, false);
        board[6 + 1 * 8] = LiteUtil.newSpot(LiteBoard.Pawn,     Side.Black, false, false);
        board[7 + 1 * 8] = LiteUtil.newSpot(LiteBoard.Pawn,     Side.Black, false, false);

        board[0 + 6 * 8] = LiteUtil.newSpot(LiteBoard.Pawn,     Side.White, false, false);
        board[1 + 6 * 8] = LiteUtil.newSpot(LiteBoard.Pawn,     Side.White, false, false);
        board[2 + 6 * 8] = LiteUtil.newSpot(LiteBoard.Pawn,     Side.White, false, false);
        board[3 + 6 * 8] = LiteUtil.newSpot(LiteBoard.Pawn,     Side.White, false, false);
        board[4 + 6 * 8] = LiteUtil.newSpot(LiteBoard.Pawn,     Side.White, false, false);
        board[5 + 6 * 8] = LiteUtil.newSpot(LiteBoard.Pawn,     Side.White, false, false);
        board[6 + 6 * 8] = LiteUtil.newSpot(LiteBoard.Pawn,     Side.White, false, false);
        board[7 + 6 * 8] = LiteUtil.newSpot(LiteBoard.Pawn,     Side.White, false, false);
        board[0 + 7 * 8] = LiteUtil.newSpot(LiteBoard.Rook,     Side.White, false, false);
        board[1 + 7 * 8] = LiteUtil.newSpot(LiteBoard.Knight,   Side.White, false, false);
        board[2 + 7 * 8] = LiteUtil.newSpot(LiteBoard.Bishop,   Side.White, false, false);
        board[3 + 7 * 8] = LiteUtil.newSpot(LiteBoard.Queen,    Side.White, false, false);
        board[4 + 7 * 8] = LiteUtil.newSpot(LiteBoard.King,     Side.White, false, false);
        board[5 + 7 * 8] = LiteUtil.newSpot(LiteBoard.Bishop,   Side.White, false, false);
        board[6 + 7 * 8] = LiteUtil.newSpot(LiteBoard.Knight,   Side.White, false, false);
        board[7 + 7 * 8] = LiteUtil.newSpot(LiteBoard.Rook,     Side.White, false, false);

        generateMoveLists();
        int a = 1;
    }

//    public static void moveGenTest() {
//        LiteBoard test = new LiteBoard();
//        LinkedHashMap<String, List<Double>> results = new LinkedHashMap<>();
//
//        List<String> types = new ArrayList<>(Arrays.asList("Pawn", "Rook", "Knight", "Bishop", "Queen", "King"));
//
//        UnaryOperator<LiteBoard> pawnSetup = (s) -> {
//            // set up our pawn with two captures waiting, anso a push and double push
//            s.board[3 + 3 * 8] = LiteUtil.newSpot(Pawn, Side.White, false, false);
//            s.board[2 + 2 * 8] = LiteUtil.newSpot(Pawn, Side.Black, true, false);
//            s.board[4 + 2 * 8] = LiteUtil.newSpot(Pawn, Side.Black, true, false);
//
//            // Also set up En passant capture on the left
//            s.board[2 + 3 * 8] = LiteUtil.newSpot(Pawn, Side.Black, true, false);
//
//            s.lastMove = new Move(2, 1, 2, 3, 0);
//
//            // Also set up En passant capture on the right
//            s.board[4 + 3 * 8] = LiteUtil.newSpot(Pawn, Side.Black, true, false);
//            s.lastMove = new Move(4, 1, 4, 3, 0);
//
//            return s;
//        };
//        UnaryOperator<LiteBoard> genericSetup = (s) -> {
//            s.board[3 + 3 * 8] = LiteUtil.newSpot(Rook, Side.Black, true, false);
//            return s;
//        };
//        UnaryOperator<LiteBoard> kingSetup1 = (s) -> {
//            // place an unused king king
//            s.board[0 + 0 * 8] = LiteUtil.newSpot(LiteUtil.King, Side.Black, true, false);
//
//            // place a king
//            s.board[3 + 3 * 8] = LiteUtil.newSpot(LiteUtil.King, Side.White, true, false);
//
//            // now pin the row above with a rook:
//            s.board[0 + 2 * 8] = LiteUtil.newSpot(LiteUtil.Rook, Side.Black, true, false);
//
//            // now pin the row below with a rook:
//            s.board[0 + 4 * 8] = LiteUtil.newSpot(LiteUtil.Rook, Side.Black, true, false);
//
//            // now pin the column to the left with a queen:
//            s.board[2 + 7 * 8] = LiteUtil.newSpot(LiteUtil.Queen, Side.Black, true, false);
//
//            s.turn = Side.White;
//            s.generateMoveLists();
//            s.numMoves1 = 0;
//            s.turn = Side.Black;
//
//            // only choice should be 3,3 -> 4,3
//
//            return s;
//        };
//
////        results.put(types.get(0), runMoveGenTest(test, pawnSetup, test::getPawnMoves));
//
//        // reset board for tests on other piece types
//        test = new LiteBoard();
//
////        results.put(types.get(1), runMoveGenTest(test, genericSetup, test::getRookMoves));
////        results.put(types.get(2), runMoveGenTest(test, genericSetup, test::getKnightMoves));
////        results.put(types.get(3), runMoveGenTest(test, genericSetup, test::getBishopMoves));
////        results.put(types.get(4), runMoveGenTest(test, genericSetup, test::getQueenMoves));
//
//
//        test = new LiteBoard();
//        results.put(types.get(5), runMoveGenTest(test, kingSetup1, test::getKingMoves));
//
//
//        String lbl = "          TotTime    Moves    Moves/s";
//        String fmt = "%8s  %7.2f  %7.2f  %7.2f  \n";
//
//        List<Pair<String, List<Double>>> data = new ArrayList<>(
//                Arrays.asList(
//                        new Pair<>(types.get(0), results.get(types.get(0))),
//                        new Pair<>(types.get(1), results.get(types.get(1))),
//                        new Pair<>(types.get(2), results.get(types.get(2))),
//                        new Pair<>(types.get(3), results.get(types.get(3))),
//                        new Pair<>(types.get(4), results.get(types.get(4))),
//                        new Pair<>(types.get(5), results.get(types.get(5))))
//        );
//
//        Comparator<Pair<String, List<Double>>> byTime = new Comparator<Pair<String, List<Double>>>() {
//            @Override
//            public int compare(Pair<String, List<Double>> lhs, Pair<String, List<Double>> rhs) {
//                // -1 - less than, 1 - greater than, 0 - equal, all inversed for descending
//                return lhs.getValue().get(0) > rhs.getValue().get(0) ? -1 : (lhs.getValue().get(0) < rhs.getValue().get(0)) ? 1 : 0;
//            }
//        };
//
//        Comparator<Pair<String, List<Double>>> byMove = new Comparator<Pair<String, List<Double>>>() {
//            @Override
//            public int compare(Pair<String, List<Double>> lhs, Pair<String, List<Double>> rhs) {
//                // -1 - less than, 1 - greater than, 0 - equal, all inversed for descending
//                return lhs.getValue().get(1) > rhs.getValue().get(1) ? -1 : (lhs.getValue().get(1) < rhs.getValue().get(1)) ? 1 : 0;
//            }
//        };
//
//        Comparator<Pair<String, List<Double>>> byMpS = new Comparator<Pair<String, List<Double>>>() {
//            @Override
//            public int compare(Pair<String, List<Double>> lhs, Pair<String, List<Double>> rhs) {
//                // -1 - less than, 1 - greater than, 0 - equal, all inversed for descending
//                if (lhs.getValue() == null || rhs.getValue() == null)
//                    return 0;
//
//                return lhs.getValue().get(2) > rhs.getValue().get(2) ? -1 : (lhs.getValue().get(2) < rhs.getValue().get(2)) ? 1 : 0;
//            }
//        };
//
//        System.out.println();
//        System.out.println();
//
//        Collections.sort(data, byMpS);
//
//        System.out.println(lbl);
//        for (Pair<String, List<Double>> line : data) {
//            if (line.getValue() == null)
//                continue;
//            System.out.printf(fmt, line.getKey(), line.getValue().get(0), line.getValue().get(1), line.getValue().get(2));
//        }
//        System.out.println();
//        System.out.println();
//
//        dumpBoard(test);
//        List<Move> moves = test.getMoves(Side.White);
//        for (Move move :moves) {
//            System.out.print(move.toString());
//            for (Move o : test.moves2) {
//                if (o.getToCol() == move.getToCol() && o.getToRow() == move.getToRow())
//                    System.out.print("*");
//            }
//
//
//            System.out.println();
//        }
//    }

//    private static List<Double> runMoveGenTest(LiteBoard board, UnaryOperator<LiteBoard>setup,
//                                               BiFunction<Integer, Integer, List<Move>> moveGen) {
//        int RESOLUTION = 1_000_000;
////        int PASSES = 5_000_000;
//        int PASSES = 1;
//
////        long totalTime = (System.nanoTime() - startTime) / RESOLUTION;
//        long totalTime = 0;
//
//        List<Move> moves;
//        List<Move> lastList = null;
//        for (int i = 0; i < PASSES; i++) {
//            // call the test setup:
//            setup.apply(board);
//
//            long startTime = System.nanoTime();
//            moves = moveGen.apply(3, 3);
//            totalTime += System.nanoTime() - startTime;
//            lastList = moves;
//        }
//        totalTime /= RESOLUTION;
//        double movesPerSec = totalTime / (double) lastList.size();
//
//        return new ArrayList<>(Arrays.asList((double) totalTime, (double) lastList.size(), movesPerSec));
//    }

    public static void dumpBoard(LiteBoard board) {
        String pieces = " pnbrqk";
        for (int i = 0; i < BOARD_SIZE; i++) {
            if (i >= 8 && i % 8 == 0) System.out.println();

            int type = board.getType(i);
            int side = board.getSide(i);

            if (side == Side.White)
                pieces = pieces.toUpperCase();
            else
                pieces = pieces.toLowerCase();

            if (i % 8 == 0) {
                System.out.print((8 - (i / 8)) + " ");
            }
            System.out.print(" ");

            switch (type) {
                case Pawn:
                case Knight:
                case Bishop:
                case Rook:
                case Queen:
                case King:
                    System.out.print(pieces.charAt(type));
                    break;
                default:
                    System.out.print(((i / 8) + (i % 8)) % 2 == 1 ? " " : ".");
            }
            System.out.print(" ");
        }
        System.out.println("\n   A  B  C  D  E  F  G  H");
    }

    public int getNumTaken1() {
        return numTaken1;
    }
    public int getNumTaken2() {
        return numTaken2;
    }
    public int getTaken1(int ndx) {
        return taken1[ndx];
    }
    public int getTaken2(int ndx) {
        return taken2[ndx];
    }


    private void generateMoveLists() {
        List<Move> moves = getMovesSorted(turn);
        numMoves1 = moves.size();
        for (int i = 0; i < numMoves1; i++) {
            moves1[i] = moves.get(i);
        }

        moves = getMovesSorted((turn+1)%2);
        numMoves2 = moves.size();
        for (int i = 0; i < numMoves2; i++) {
            moves2[i] = moves.get(i);
        }

        // at this point the Side (Black or White) constant is in the .turn value
        // for the board.  Whichever color is to move next, it's moves are in
        // moves1 and the non-moving players moves are in moves2.  Also both
        // pieces1 and pieces2 are updated to contain the pieces with the same
        // logic, pieces1 is for whoever's color is in .turn, the other in pieces2.
    }


    public boolean checkDrawByRepetition(final Move move, final int maxRepetitions) {
        // Check for draw-by-repetition (same made too many times in a row by a player)
        int need = (int) Math.pow(2.0, (maxRepetitions + 1));
        if (numHist < need) return false;
        int count = 0;
        for (int i = numHist - need; i < numHist; i++) {
            if (history[i].equals(move)) {
                count++;
                if (count >= maxRepetitions) return true;
            }
        }
        return false;
    }


    public boolean kingInCheck(final int side) {
        int otherSide = (side + 1) % 2;
        List<Move> opponentMoves = getMoves(otherSide, false);
        int numMoves = opponentMoves.size();

        for (int ndx = 0; ndx < BOARD_SIZE; ndx++) {
            if (getType(ndx) != King || getSide(ndx) != side) continue;
            for (Move move : opponentMoves) {
                if (move.getTo() == ndx) {
                    return true;
                }
            }
            break;
        }
        return false;
    }


    public void executeMove(final Move move) {
        int fx = move.getFromCol();
        int fy = move.getFromRow();
        int tx = move.getToCol();
        int ty = move.getToRow();
        int fi = move.getFrom();
        int ti = move.getTo();

        // do some quick debug/sanity checks:
        if (move.equals(lastMove)) {
            String dbgMsg = String.format("say wha?! move seems to be the same move we last made: %s", move);
            Main.log(Main.LogLevel.DEBUG, dbgMsg);
            Main.bailOnInternalError(dbgMsg);
        }

        if (getType(fi) == Empty) {
            String dbgMsg = String.format("move seems to start at an empty spot: %s", move);
            Main.log(Main.LogLevel.DEBUG, dbgMsg);
            Main.bailOnInternalError(dbgMsg);
        }

        if (!isEmpty(ti)) {
            int sideToMove = getSide(fi);
            int sideAtDest = getSide(ti);
            if (sideToMove == sideAtDest) {
                String dbgMsg = String.format("move seems to capture piece of its own side: %s", move);
                Main.log(Main.LogLevel.DEBUG, dbgMsg);
                Main.bailOnInternalError(dbgMsg);
            }
        }

        // special check for en passant
        int type = getType(fi);
        int toType = getType(ti);
        if (toType == Empty && type == Pawn && fx != tx) { // en-passant capture
            if (turn != Side.Black)
                taken2[numTaken2++] = (byte) Pawn;
            else
                taken1[numTaken1++] = (byte) Pawn;
            board[tx + fy * 8] = LiteUtil.newSpot(Empty, Side.Black, false, false);
        } else {
            if (toType != Empty) {
                if (turn != Side.Black)
                    taken2[numTaken2++] = (byte) toType;
                else
                    taken1[numTaken1++] = (byte) toType;
            }
        }

        board[ti] = board[fi];
        board[fi] = LiteUtil.newSpot(Empty, Side.Black, false, false);
        setMoved(ti, true);

        // See if this is a Castling move:
        if (type == King) {
            int delta = tx - fx;
            if (abs(delta) == 2) {
                int rfi;    // Rook from index
                int rti;    // Rook to index
                if (delta < 0) {
                    rfi = fy * 8;       // rook from-index
                    rti = fy * 8 + 3;   // rook to-index
                } else {
                    rfi = fy * 8 + 7;   // rook from-index
                    rti = fy * 8 + 5;   // rook to-index
                }
                board[rti] = board[rfi];
                setMoved(rti, true);
                board[rfi] = LiteUtil.newSpot(Empty, Side.Black, false, false);
            }

            if (getSide(fi) == Side.Black)
                blkKingLoc = ti;
            else
                whtKingLoc = ti;

        } else if (type == Pawn) {
            if (ty == 0 || ty == 7) {
                setType(ti, Queen);
            }
        }

        if (numHist >= history.length) {
            numHist -= history.length / 4;
            System.arraycopy(history, history.length / 4, history, 0, numHist);
        }
        history[numHist++] = move;

        lastMove = new Move(move);
    }


    /**
     * Advance the total number of moves in the game.
     * Also toggle which players turn it is, and generates
     * the following changes in the board:
     *
     *      + .turn  contains the color of the new player to move next
     *      + .turns contains the total number of moves made so far
     *
     *      + move list for current     moving player are in moves1     (turn's Side color: Black or White)
     *      + move list for current non-moving player are in moves2     ( ((turn + 1) % 2)'s Side color: Black or White)
     *      + piece list for current     moving player are in pieces1
     *      + piece list for current non-moving player are in pieces2
     *
     */
    public void advanceTurn() {
        turns++;
        turn = ((turn + 1) % 2);
        generateMoveLists();
    }


    public List<Move> getMovesSorted(final int side) {
        List<Move> moves = getMoves(side, true);
        Comparator<Move> sortByValue;
//        if (side == Side.Black) {
//            sortByValue = Comparator.comparing(Move::getValue);
//        } else {
            sortByValue = Comparator.comparing(Move::getValue).reversed();
//        }
        moves.sort(sortByValue);
        return moves;
    }


    private List<Move> getMoves(final int side, boolean checkKing) {
        List<Move> moves = new ArrayList<>();

        // We also update the pieces array and numPieces for this side
        byte[] pieces = new byte[16];
        int numPieces = 0;

        for (int ndx = 0; ndx < BOARD_SIZE; ndx++) {
            if (isEmpty(ndx) || getSide(ndx) != side) continue;

            // add piece to list of pieces for this side
            byte b = board[ndx];
            pieces[numPieces++] = b;

            int col = ndx % 8;
            int row = ndx / 8;
            switch (getType(ndx)) {
                case Pawn:
                    moves.addAll(getPawnMoves(col, row));
                    break;
                case Rook:
                    moves.addAll(getRookMoves(col, row));
                    break;
                case Knight:
                    moves.addAll(getKnightMoves(col, row));
                    break;
                case Bishop:
                    moves.addAll(getBishopMoves(col, row));
                    break;
                case Queen:
                    moves.addAll(getQueenMoves(col, row));
                    break;
                case King:
                    moves.addAll(getKingMoves(col, row));
                    break;
            }
        }

        if (side == turn) {
            pieces1 = pieces;
            numPieces1 = numPieces;
        } else {
            pieces2 = pieces;
            numPieces2 = numPieces;
        }

        if (checkKing) {
            moves = cleanupMoves(moves, side);
        }

        return moves;
    }


    private List<Move> cleanupMoves(final List<Move> moves, final int side) {
        List<Move> valid = new ArrayList<>();
        for (Move move : moves) {
            LiteBoard current = new LiteBoard(this);
            current.executeMove(move);
            if (!current.kingInCheck(side)) {
                valid.add(move);
            }
        }
        return valid;
    }


    private static boolean isValidSpot(final int col, final int row) {
        return (col >= 0) && (col <= 7) && (row >= 0) && (row <= 7);
    }


    private void addMoveIfValid(final List<Move> moves,
                                final int fromCol, final int fromRow,
                                final int toCol, final int toRow) {

        if (!isValidSpot(fromCol, fromRow)) {
            return;
        }
        if (!isValidSpot(toCol, toRow)) {
            return;
        }
        int fi = fromCol + fromRow * 8;
        int ti = toCol + toRow * 8;

// I don't *think* any pieces ask to move from anywhere but their spot
//        if (isEmpty(fi)) {
//            return;
//        }

        int value = 0;
        int pieceType = getType(fi);
        int pieceSide = getSide(fi);
        if (!isEmpty(ti)) {
            if (getSide(fi) == getSide(ti)) {
                return;
            }
            value = getValue(ti);
        }

        // extra checks if moving a pawn...
        if (pieceType == Pawn) {
            int forward = (pieceSide == Side.Black) ? 1 : -1;
            // if double push
            if (abs(fromRow - toRow) == 2) {
                // not allowed if the pawn has already moved
                if (hasMoved(fi) || !isEmpty(fromCol + (fromRow + forward) * 8)) {
                    return;
                }
            }
            // if advancing in same column
            if (fromCol == toCol) {
                // not allowed if a piece is in the way
                if (!isEmpty(ti)) {
                    return;
                }
            } else {
                // pawns cannot move diagonally unless
                if (isEmpty(ti)) {
                    if (lastMove.getToRow() != (toRow - forward) || lastMove.getToCol() != toCol) {
                        return;
                    }
                    // capturing en passant
                    value = LiteUtil.getValue(Pawn);
                } else {
                    // capturing normal
                    value = getValue(ti);
                }
            }
        }

//        if (pieceSide == Side.Black)
//            value *= -1;

        moves.add(new Move(fromCol, fromRow, toCol, toRow, value));
    }

    /**
     * Get a list of all possible moves for a pawn at the given location on the board.
     *
     * @param col The column on the board to get moves from
     * @param row The row on the board to get moves from
     * @return A new List<Move> containing all possible moves a pawn could make from the given spot
     * @throws IllegalArgumentException if the specified location is invalid or if it does not contain a piece to move.
     */
    private List<Move> getPawnMoves(int col, int row) {
        int ndx = col + row * 8;
        int forward = getSide(ndx) == Side.White ? -1 : 1;
        List<Move> moves = new ArrayList<>();

        addMoveIfValid(moves, col, row, col, row + forward);
        if (!hasMoved(col + row * 8)) {
            addMoveIfValid(moves, col, row, col, row + forward + forward);
        }

        if (isValidSpot(col - 1, row + forward) && !isEmpty((col - 1) + (row + forward) * 8)) {
            addMoveIfValid(moves, col, row, col - 1, row + forward);
        }

        if (isValidSpot(col + 1, row + forward) && !isEmpty((col + 1) + (row + forward) * 8)) {
            addMoveIfValid(moves, col, row, col + 1, row + forward);
        }

        // en-passant! on the left
        int ep1x = col - 1;
        if (isValidSpot(ep1x, row) && getSide(ep1x + row * 8) != getSide(ndx)) {
            if (lastMove.getToCol() == ep1x && lastMove.getToRow() == row) {
                if (abs(lastMove.getFromRow() - lastMove.getToRow()) == 2) {
                    if (getType(ep1x + row * 8) == Pawn) {
                        addMoveIfValid(moves, col, row, ep1x, row + forward);
                    }
                }
            }
        }

        // en-passant! on the right
        ep1x = col + 1;
        if (isValidSpot(ep1x, row) && getSide(ep1x + row * 8) != getSide(ndx)) {
            if (lastMove.getToCol() == ep1x && lastMove.getToRow() == row) {
                if (abs(lastMove.getFromRow() - lastMove.getToRow()) == 2) {
                    if (getType(ep1x + row * 8) == Pawn) {
                        addMoveIfValid(moves, col, row, ep1x, row + forward);
                    }
                }
            }
        }

        return moves;
    }

    private boolean addSlider(List<Move> moves, int col, int row, int x, int y) {
        if (!isValidSpot(x, y)) {
            return false;
        }
        if (!isEmpty(x + y * 8)) {
            if (getSide(col + row * 8) == getSide(x + y * 8))
                return false;
            addMoveIfValid(moves, col, row, x, y);
            return false;
        }
        addMoveIfValid(moves, col, row, x, y);
        return true;
    }

    /**
     * Get a list of all possible moves for a rook at the given location on the board.
     *
     * @param col The column on the board to get moves from
     * @param row The row on the board to get moves from
     * @return A new List<Move> containing all possible moves a rook could make from the given spot
     * @throws IllegalArgumentException if the specified location is invalid or if it does not contain a piece to move.
     */
    protected List<Move> getRookMoves(int col, int row) {
        List<Move> moves = new ArrayList<>();

        for (int offset = 1; offset <= 7; offset++) {
            int x = col - offset;
            int y = row;
            if (!addSlider(moves, col, row, x, y))
                break;
        }
        for (int offset = 1; offset <= 7; offset++) {
            int x = col + offset;
            int y = row;
            if (!addSlider(moves, col, row, x, y))
                break;
        }
        for (int offset = 1; offset <= 7; offset++) {
            int x = col;
            int y = row - offset;
            if (!addSlider(moves, col, row, x, y))
                break;
        }
        for (int offset = 1; offset <= 7; offset++) {
            int x = col;
            int y = row + offset;
            if (!addSlider(moves, col, row, x, y))
                break;
        }

//        for (int x = col - 1; x >= 0; --x) {
//            if (!isEmpty(x + row * 8))
//                if (getSide(col + row * 8) == getSide(x + row * 8))
//                    break;
//            addMoveIfValid(moves, col, row, x, row);
//        }
//        for (int x = col + 1; x <= 7; ++x) {
//            if (!isEmpty(x + row * 8))
//                if (getSide(col + row * 8) == getSide(x + row * 8))
//                    break;
//            addMoveIfValid(moves, col, row, x, row);
//        }
//        for (int y = row - 1; y >= 0; --y) {
//            if (!isEmpty(col + y * 8))
//                if (getSide(col + row * 8) == getSide(col + y * 8))
//                    break;
//            addMoveIfValid(moves, col, row, col, y);
//        }
//        for (int y = row + 1; y <= 7; ++y) {
//            if (!isEmpty(col + y * 8))
//                if (getSide(col + row * 8) == getSide(col + y * 8))
//                    break;
//            addMoveIfValid(moves, col, row, col, y);
//        }
//
        return moves;
    }


    /**
     * Get a list of all possible moves for a knight at the given location on the board.
     *
     * @param col The column on the board to get moves from
     * @param row The row on the board to get moves from
     * @return A new List<Move> containing all possible moves a knight could make from the given spot
     * @throws IllegalArgumentException if the specified location is invalid or if it does not contain a piece to move.
     */
    protected List<Move> getKnightMoves(int col, int row) {
        List<Move> moves = new ArrayList<>();

        addMoveIfValid(moves, col, row, col - 1, row - 2);
        addMoveIfValid(moves, col, row, col + 1, row - 2);
        addMoveIfValid(moves, col, row, col - 1, row + 2);
        addMoveIfValid(moves, col, row, col + 1, row + 2);
        addMoveIfValid(moves, col, row, col - 2, row - 1);
        addMoveIfValid(moves, col, row, col + 2, row - 1);
        addMoveIfValid(moves, col, row, col - 2, row + 1);
        addMoveIfValid(moves, col, row, col + 2, row + 1);

        return moves;
    }

    /**
     * Get a list of all possible moves for a bishop at the given location on the board.
     *
     * @param col The column on the board to get moves from
     * @param row The row on the board to get moves from
     * @return A new List<Move> containing all possible moves a bishop could make from the given spot
     * @throws IllegalArgumentException if the specified location is invalid or if it does not contain a piece to move.
     */
    protected List<Move> getBishopMoves(int col, int row) {
        List<Move> moves = new ArrayList<>();

        for (int offset = 1; offset <= 7; offset++) {
            int x = col - offset;
            int y = row - offset;
            if (!addSlider(moves, col, row, x, y))
                break;
        }
        for (int offset = 1; offset <= 7; offset++) {
            int x = col + offset;
            int y = row - offset;
            if (!addSlider(moves, col, row, x, y))
                break;
        }
        for (int offset = 1; offset <= 7; offset++) {
            int x = col - offset;
            int y = row + offset;
            if (!addSlider(moves, col, row, x, y))
                break;
        }
        for (int offset = 1; offset <= 7; offset++) {
            int x = col + offset;
            int y = row + offset;
            if (!addSlider(moves, col, row, x, y))
                break;
        }

        return moves;
    }

    /**
     * Get a list of all possible moves for a queen at the given location on the board.
     *
     * @param col The column on the board to get moves from
     * @param row The row on the board to get moves from
     * @return A new List<Move> containing all possible moves a queen could make from the given spot
     * @throws IllegalArgumentException if the specified location is invalid or if it does not contain a piece to move.
     */
    protected List<Move> getQueenMoves(int col, int row) {
        List<Move> moves;
        moves = getRookMoves(col, row);
        moves.addAll(getBishopMoves(col, row));
        return moves;
    }

    /**
     * Get a list of all possible moves for a king at the given location on the board.
     *
     * @param col The column on the board to get moves from
     * @param row The row on the board to get moves from
     * @return A new List<Move> containing all possible moves a king could make from the given spot
     * @throws IllegalArgumentException if the specified location is invalid or if it does not contain a piece to move.
     */
    protected List<Move> getKingMoves(int col, int row) {
        int ndx = col + row * 8;
        List<Move> moves = new ArrayList<>();

        addMoveIfValid(moves, col, row, col - 1, row - 1);
        addMoveIfValid(moves, col, row, col + 1, row - 1);
        addMoveIfValid(moves, col, row, col - 1, row + 1);
        addMoveIfValid(moves, col, row, col + 1, row + 1);
        addMoveIfValid(moves, col, row, col, row - 1);
        addMoveIfValid(moves, col, row, col, row + 1);
        addMoveIfValid(moves, col, row, col - 1, row);
        addMoveIfValid(moves, col, row, col + 1, row);

        // check for king side castling:
        if (!hasMoved(ndx)
                && isEmpty((col + 1) + row * 8)
                && isEmpty((col + 2) + row * 8)
                && getType((col + 3) + row * 8) == Rook
                && !hasMoved((col + 3) + row * 8)) {
            addMoveIfValid(moves, col, row, col + 2, row);
        }

        // check for queen side castling:
        if (!hasMoved(ndx)
                && isEmpty((col - 1) + row * 8)
                && isEmpty((col - 2) + row * 8)
                && isEmpty((col - 3) + row * 8)
                && getType((col - 4) + row * 8) == Rook
                && !hasMoved((col - 4) + row * 8)) {
            addMoveIfValid(moves, col, row, col - 2, row);
        }

        return moves;
    }
}
