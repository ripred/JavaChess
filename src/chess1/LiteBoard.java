package chess1;

// Lightweight class to be used during minimax traversals.
// Has just what is needed to keep board state and moves and nothing more
// so it can be passed and cloned faster.  All straight arrays.
//
// No setters/getters.  All free love.  We're all adults here.  No hand holding. Don't screw up.

import javafx.util.Pair;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.function.BiFunction;

import static java.lang.Math.abs;

public class LiteBoard {
    public static final int BOARD_SIZE = 64;

    public byte[] board;
    public Move[] moves1;
    public Move[] moves2;
    public Move[] history;
    public int numMoves1;
    public int numMoves2;
    public int numHist;
    public int maxRep;
    public int turn;
    public Move lastMove;


    // accessors for board spots
    public boolean isEmpty(int ndx) {
        return (board[ndx] & LiteUtil.Type) == LiteUtil.Empty;
    }
    public int getType(int ndx) {
        return board[ndx] & LiteUtil.Type;
    }
    public int getSide(int ndx) {
        return (board[ndx] & LiteUtil.Side) >> 3;
    }
    public boolean hasMoved(int ndx) {
        return (board[ndx] & LiteUtil.Moved) != 0;
    }
    public int getValue(int ndx) {
        return LiteUtil.getValue(board[ndx]);
    }
    public boolean inCheck(int ndx) {
        return LiteUtil.inCheck(board[ndx]);
    }
    public void setType(int ndx, int type) {
        board[ndx] = LiteUtil.setType(board[ndx], type);
    }
    public void setSide(int ndx, int side) {
        board[ndx] = LiteUtil.setSide(board[ndx], side);
    }
    public void setMoved(int ndx, boolean hasMoved) {
        board[ndx] |= LiteUtil.Moved;
    }
    public void setCheck(byte ndx, boolean inCheck) {
        board[ndx] = LiteUtil.setCheck(board[ndx], inCheck);
    }

    // Can only be instantiated by cloning Board object or another LiteBoard object:
    public LiteBoard(Board orig) {
        copyBoard(orig, false);
    }
    public LiteBoard(Board orig, boolean generateNewMoves) {
        copyBoard(orig, generateNewMoves);
    }

    private void copyBoard(Board orig, boolean generateNewMoves) {
        board = new byte[BOARD_SIZE];
        moves1 = new Move[256];
        moves2 = new Move[256];
        history = new Move[256];

        maxRep = orig.getMaxAllowedRepetitions();
        turn = orig.getTurn();
        lastMove = orig.getLastMove();

        for (int ndx=0; ndx < BOARD_SIZE; ndx++) {
            Spot spot = orig.getSpot(ndx % 8, ndx / 8);
            board[ndx] = 0;

            setType(ndx, spot.getType());
            setSide(ndx, spot.getSide());
            setMoved(ndx, spot.hasMoved());
        }

        if (generateNewMoves) {
            updateMoveLists();
        } else {
            numMoves1 = orig.getCurrentPlayerMoves().size();
            for (int i = 0; i < numMoves1; i++) {
                moves1[i] = new Move(orig.getCurrentPlayerMoves().get(i));
            }

            numMoves2 = orig.getOtherPlayerMoves().size();
            for (int i = 0; i < numMoves2; i++) {
                moves2[i] = new Move(orig.getOtherPlayerMoves().get(i));
            }
        }
        numHist = orig.getMoveHistory().size();
        for (int i=0; i < numHist; i++) {
            history[i] = new Move(orig.getMoveHistory().get(i));
        }
    }

    public LiteBoard(LiteBoard orig) {
        board = new byte[BOARD_SIZE];
        moves1 = new Move[256];
        moves2 = new Move[256];
        history = new Move[256];

        maxRep = orig.maxRep;
        turn = orig.turn;
        lastMove = orig.lastMove;

        System.arraycopy((byte[])orig.board, 0, (byte[])this.board, 0, orig.board.length);

        numMoves1 = orig.numMoves1;
        for (int i=0; i < numMoves1; i++) {
            moves1[i] = new Move(orig.moves1[i]);
        }

        numMoves2 = orig.numMoves2;
        for (int i=0; i < numMoves2; i++) {
            moves2[i] = new Move(orig.moves2[i]);
        }

        numHist = orig.numHist;
        for (int i=0; i < numHist; i++) {
            history[i] = new Move(orig.history[i]);
        }
    }

    private LiteBoard() {
        board = new byte[BOARD_SIZE];
        moves1 = new Move[256];
        moves2 = new Move[256];
        history = new Move[256];

        maxRep = 3;
        turn = Side.White;
        lastMove = new Move(0, 0, 7, 7, 0);

        numMoves1 = moves1.length;
        for (int i=0; i < numMoves1; i++) {
            moves1[i] = new Move(i % 8, i / 8, i % 8, i / 8, 0);
        }

        numMoves2 = moves2.length;
        for (int i=0; i < numMoves2; i++) {
            moves2[i] = new Move(i % 8, i / 8, i % 8, i / 8, 0);
        }

        numHist = history.length;
        for (int i=0; i < numHist; i++) {
            history[i] = new Move(i % 8, i / 8, i % 8, i / 8, 0);
        }

        updateMoveLists();
    }

    public static void moveGenTest() {
        LiteBoard test = new LiteBoard();
        LinkedHashMap<String, List<Double>> results = new LinkedHashMap<>();

        List<String> types = new ArrayList<>(Arrays.asList("Pawn", "Rook", "Knight", "Bishop", "Queen", "King"));

        // set up our pawn with two captures waiting, anso a push and double push
        test.board[3 + 3 * 8] = LiteUtil.newSpot(Side.White, LiteUtil.Pawn, true, false);
        test.board[2 + 2 * 8] = LiteUtil.newSpot(Side.Black, LiteUtil.Pawn, true, false);
        test.board[4 + 2 * 8] = LiteUtil.newSpot(Side.Black, LiteUtil.Pawn, true, false);

        // Also set up En passant capture on the left
        test.board[2 + 3 * 8] = LiteUtil.newSpot(Side.Black, LiteUtil.Pawn, true, false);

        test.lastMove = new Move(2, 1, 2, 3, 0);

        // Also set up En passant capture on the right
        test.board[4 + 3 * 8] = LiteUtil.newSpot(Side.Black, LiteUtil.Pawn, true, false);
        test.lastMove = new Move(4, 1, 4, 3, 0);

        results.put(types.get(0), runMoveGenTest(test::getPawnMoves   ));

        // reset board for tests on other piece types
        test = new LiteBoard();
        test.board[3 + 3 * 8] = LiteUtil.newSpot(Side.Black, LiteUtil.Rook, true, false);

        results.put(types.get(1), runMoveGenTest(test::getRookMoves   ));
        results.put(types.get(2), runMoveGenTest(test::getKnightMoves ));
        results.put(types.get(3), runMoveGenTest(test::getBishopMoves ));
        results.put(types.get(4), runMoveGenTest(test::getQueenMoves  ));
        results.put(types.get(5), runMoveGenTest(test::getKingMoves   ));


        String lbl = "          TotTime    Moves    Moves/s";
        String fmt = "%8s  %7.2f  %7.2f  %7.2f  \n";

        List<Pair<String, List<Double>>> data = new ArrayList<>(
                Arrays.asList(
                        new Pair<>(types.get(0), results.get(types.get(0))),
                        new Pair<>(types.get(1), results.get(types.get(1))),
                        new Pair<>(types.get(2), results.get(types.get(2))),
                        new Pair<>(types.get(3), results.get(types.get(3))),
                        new Pair<>(types.get(4), results.get(types.get(4))),
                        new Pair<>(types.get(5), results.get(types.get(5))))
        );

        Comparator<Pair<String, List<Double>>> byTime = new Comparator<Pair<String, List<Double>>>() {
            @Override
            public int compare(Pair<String, List<Double>> lhs, Pair<String, List<Double>> rhs) {
                // -1 - less than, 1 - greater than, 0 - equal, all inversed for descending
                return lhs.getValue().get(0) > rhs.getValue().get(0) ? -1 : (lhs.getValue().get(0) < rhs.getValue().get(0)) ? 1 : 0;
            }
        };

        Comparator<Pair<String, List<Double>>> byMove = new Comparator<Pair<String, List<Double>>>() {
            @Override
            public int compare(Pair<String, List<Double>> lhs, Pair<String, List<Double>> rhs) {
                // -1 - less than, 1 - greater than, 0 - equal, all inversed for descending
                return lhs.getValue().get(1) > rhs.getValue().get(1) ? -1 : (lhs.getValue().get(1) < rhs.getValue().get(1)) ? 1 : 0;
            }
        };

        Comparator<Pair<String, List<Double>>> byMpS = new Comparator<Pair<String, List<Double>>>() {
            @Override
            public int compare(Pair<String, List<Double>> lhs, Pair<String, List<Double>> rhs) {
                // -1 - less than, 1 - greater than, 0 - equal, all inversed for descending
                return lhs.getValue().get(2) > rhs.getValue().get(2) ? -1 : (lhs.getValue().get(2) < rhs.getValue().get(2)) ? 1 : 0;
            }
        };

        System.out.println();
        System.out.println();

        Collections.sort(data, byMpS);

        System.out.println(lbl);
        for (Pair<String, List<Double>> line : data) {
            System.out.printf(fmt, line.getKey(), line.getValue().get(0), line.getValue().get(1), line.getValue().get(2));
        }
        System.out.println();
        System.out.println();


    }
    private static List<Double> runMoveGenTest(BiFunction<Integer, Integer, List<Move>> moveGen) {
        int RESOLUTION = 1_000_000;
        int PASSES = 5_000_000;

        long startTime = System.nanoTime();
        List<Move> moves;
        List<Move> lastList = null;
        for (int i=0; i < PASSES; i++) {
            moves = moveGen.apply(3, 3);
            lastList = moves;
        }
        long totalTime = (System.nanoTime() - startTime) / RESOLUTION;
        double movesPerSec = totalTime / (double) lastList.size();

        return new ArrayList<>(Arrays.asList((double)totalTime, (double)lastList.size(), movesPerSec));
    }

    private void updateMoveLists() {
        List<Move> moves = getMovesSorted(turn);
        numMoves1 = moves.size();
        for (int i=0; i < numMoves1; i++) {
            moves1[i] = moves.get(i);
        }

        moves = getMovesSorted((turn + 1) % 2);
        numMoves2 = moves.size();
        for (int i=0; i < numMoves2; i++) {
            moves2[i] = moves.get(i);
        }
    }


    public boolean checkDrawByRepetition(final Move move, final int maxRepetitions) {
        // Check for draw-by-repetition (same made too many times in a row by a player)
        int need = (int) Math.pow(2.0, maxRepetitions);
        if (numHist < need) return false;
        int count = 0;
        for (int i=numHist - need; i < numHist; i++) {
            if (history[i].equals(move)) {
                count++;
                if (count >= maxRepetitions) return true;
            }
        }
        return false;
    }


    public boolean kingInCheck(final int side) {
        int otherSide = ((side + 1) % 2);
        List<Move> opponentMoves = getMoves(otherSide, false);
        for (int ndx=0; ndx < BOARD_SIZE; ndx++) {
            if (getType(ndx) != LiteUtil.King || getSide(ndx) != side) continue;
            for (Move move : opponentMoves) {
                if ( (move.getToCol() + move.getToRow() * 8) == ndx) {
                    return true;
                }
            }
            break;
        }
        return false;
    }


    public void executeMove(final Move move) {
        lastMove = new Move(move);

        int fx = move.getFromCol();
        int fy = move.getFromRow();
        int tx = move.getToCol();
        int ty = move.getToRow();
        int fi = fy * 8 + fx;
        int ti = ty * 8 + tx;

        // special check for en passant
        int toType = getType(ti);
        if (toType == Piece.Empty && getType(fi) == LiteUtil.Pawn && fx != tx) { // en-passant capture
            board[tx + fy * 8] = LiteUtil.newSpot(Side.Black, Piece.Empty, false, false);
        }

        board[ti] = board[fi];
        board[fi] = LiteUtil.newSpot(Side.Black, Piece.Empty, false, false);
        setMoved(ti, true);

        // See if this is a Castling move:
        if (toType == LiteUtil.King) {
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
                board[rfi] = LiteUtil.newSpot(Side.Black, Piece.Empty, false, false);
            }
        } else if (toType == LiteUtil.Pawn) {
            if (ty == 0 || ty == 7) {
                setType(ti, LiteUtil.Queen);
            }
        }

        history[numHist++] = move;
    }

    public void advanceTurn() {
        turn = (turn + 1) % 2;
        updateMoveLists();
    }


    public List<Move> getMovesSorted(final int side) {
        Comparator<Move> sortByValue = Comparator.comparing(Move::getValue).reversed();
        List<Move> moves = getMoves(side, true);
        moves.sort(sortByValue);
        return moves;
    }


    private List<Move>  getMoves(final int side, final boolean checkKing) {
        List<Move> moves = new ArrayList<>();

        for (int ndx=0; ndx < BOARD_SIZE; ndx++) {
            if (isEmpty(ndx) || getSide(ndx) != side) {
                continue;
            }

            int col = ndx % 8;
            int row = ndx / 8;
            switch (getType(ndx)) {
                case LiteUtil.Pawn:
                    moves.addAll(getPawnMoves(col, row));
                    break;
                case LiteUtil.Rook:
                    moves.addAll(getRookMoves(col, row));
                    break;
                case LiteUtil.Knight:
                    moves.addAll(getKnightMoves(col, row));
                    break;
                case LiteUtil.Bishop:
                    moves.addAll(getBishopMoves(col, row));
                    break;
                case LiteUtil.Queen:
                    moves.addAll(getQueenMoves(col, row));
                    break;
                case LiteUtil.King:
                    moves.addAll(getKingMoves(col, row));
                    break;
            }
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
        return col >= 0 && col <= 7 && row >= 0 && row <= 7;
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

        if (isEmpty(fi)) {
            return;
        }

        int value = 0;
        if (!isEmpty(ti)) {
            if (getSide(fi) == getSide(ti)) {
                return;
            } else {
                value = Piece.values[getType(ti)];
            }
        }

        // extra checks if moving a pawn...
        if (getType(fi) == LiteUtil.Pawn) {
            if (abs(fromRow - toRow) == 2) {
                // not allowed if the pawn has already moved
                if (hasMoved(fi) || !isEmpty(fromCol + (fromRow + 1) * 8)) {
                    return;
                }
            }
            int forward = (turn == Side.Black) ? 1 : -1;
            // if advancing in same column
            if (fromCol == toCol) {
                // if double push
                // not allowed if a piece is in the way
                if (!isEmpty(ti)) {
                    return;
                }
            } else {
                // pawns cannot move diagonally unless
                if (isEmpty(ti)) {
                    if (lastMove.getToRow() != (toRow - forward) || lastMove.getToCol() != toCol) {
                        return;
                    } else {
                        // capturing en passant
                        value = Piece.values[LiteUtil.Pawn];
                    }
                } else {
                    // capturing normal
                    value = Piece.values[getType(ti)];
                }
            }
        }

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
        addMoveIfValid(moves, col, row, col, row + forward + forward);

        // en-passant! on the left
        int ep1x = col - 1;
        if (isValidSpot(ep1x, row) && getSide(ep1x + row * 8) != getSide(ndx)) {
            if (lastMove.getToCol() == ep1x && lastMove.getToRow() == row) {
                if (abs(lastMove.getFromRow() - lastMove.getToRow()) == 2) {
                    if (getType(ep1x + row * 8) == LiteUtil.Pawn) {
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
                    if (getType(ep1x + row * 8) == LiteUtil.Pawn) {
                        addMoveIfValid(moves, col, row, ep1x, row + forward);
                    }
                }
            }
        }

        if (isValidSpot(col - 1, row + forward) && getType((col - 1) + (row + forward) * 8) != Piece.Empty) {
            addMoveIfValid(moves, col, row, col - 1, row + forward);
        }
        if (isValidSpot(col + 1, row + forward) && getType((col + 1) + (row + forward) * 8) != Piece.Empty) {
            addMoveIfValid(moves, col, row, col + 1, row + forward);
        }

        return moves;
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

        for (int x = col - 1; x >= 0; --x) {
            if (!isEmpty(x + row * 8))
                break;
            addMoveIfValid(moves, col, row, x, row);
        }
        for (int x = col + 1; x <= 7; ++x) {
            if (!isEmpty(x + row * 8))
                break;
            addMoveIfValid(moves, col, row, x, row);
        }
        for (int y = row - 1; y >= 0; --y) {
            if (!isEmpty(col + y * 8))
                break;
            addMoveIfValid(moves, col, row, col, y);
        }
        for (int y = row + 1; y <= 7; ++y) {
            if (!isEmpty(col + y * 8))
                break;
            addMoveIfValid(moves, col, row, col, y);
        }

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

        for (int offset = 1; offset < 7; offset++) {
            if (isValidSpot(col - offset, row - offset)) {
                if (!isEmpty((col - offset) + (row - offset) * 8))
                    break;
                addMoveIfValid(moves, col, row, col - offset, row - offset);
            }

            if (isValidSpot(col + offset, row - offset)) {
                if (!isEmpty((col + offset) + (row - offset) * 8))
                    break;
                addMoveIfValid(moves, col, row, col + offset, row - offset);
            }

            if (isValidSpot(col - offset, row + offset)) {
                if (!isEmpty((col - offset) + (row + offset) * 8))
                    break;
                addMoveIfValid(moves, col, row, col - offset, row + offset);
            }

            if (isValidSpot(col + offset, row + offset)) {
                if (!isEmpty((col + offset) + (row + offset) * 8))
                    break;
                addMoveIfValid(moves, col, row, col + offset, row + offset);
            }
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
        List<Move> moves = null;
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
                && getType((col + 3) + row * 8) == LiteUtil.Rook
                && !hasMoved((col + 3) + row * 8)) {
            addMoveIfValid(moves, col, row, col + 2, row);
        }

        // check for queen side castling:
        if (!hasMoved(ndx)
                && isEmpty((col - 1) + row * 8)
                && isEmpty((col - 2) + row * 8)
                && isEmpty((col - 3) + row * 8)
                && getType((col - 4) + row * 8) == LiteUtil.Rook
                && !hasMoved((col - 4) + row * 8)) {
            addMoveIfValid(moves, col, row, col - 2, row);
        }
        return moves;
    }


}
