package chess;

import java.lang.Math;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import static java.lang.Math.abs;

public class Board {
    private List<Move> currentPlayerMoves;
    private List<Spot> board;
    private List<Integer> piecesTaken0;
    private List<Integer> piecesTaken1;
    private int verbose;
    private int turn;
    private int passes;

    static long timeInGetMoves;

    /**
     * The default constructor for a Board object.
     *
     * Initializes the board to a new game state and sets the verbose level to none.
     */
    Board() {
        verbose = 0;
        timeInGetMoves = 0L;
        init();
    }

    /**
     * Constructor for a new Board object that allows setting the verbose level.
     *
     * @param verbose A flag indicating the level of debug information to output to the console.
     *                Higher values for 'verbose' indicate more debug information.
     *                If this flag is 0 no debug output will be written to the console.
     */
    Board(int verbose) {
        this.verbose = verbose;
        init();
    }

    /**
     * Copy-constructor for a Board object.
     *
     * @param ref The Board object to copy into this new Board.
     */
    Board(final Board ref) {
        board = ref.copyBoard();
        verbose = ref.verbose;
        turn = ref.turn;
        passes = ref.passes;
        currentPlayerMoves = ref.currentPlayerMoves;
        piecesTaken0 = new ArrayList<>();
        piecesTaken0.addAll(ref.piecesTaken0);
        piecesTaken1 = new ArrayList<>();
        piecesTaken1.addAll(ref.piecesTaken1);
    }

    /**
     * @return Returns a new copy of the current List<Spot> of the spots on the board.
     */
    private List<Spot> copyBoard() {
        List<Spot> newBoard = new ArrayList<>();
        for (Spot s:board) newBoard.add(new Spot(s));
        return newBoard;
    }

    /**
     * @return Returns the list of spots on the board for the current game state.
     */
    List<Spot> getBoard() {
        return board;
    }

    /**
     * @return Returns 1 if the current side is white or 0 if the current side is black.
     *
     */
    int getTurn() {
        return turn;
    }

    int getVerbose() { return verbose; }

    /**
     * Advance the turn to the next player and create a new list of available moves for that player.
     *
     */
    void advanceTurn() {
        passes++;
        turn = (turn == Side.White) ? Side.Black : Side.White;
        currentPlayerMoves = getMovesSorted(turn);
    }

    /**
     * Display the current board state on the output console.
     *
     */
    void show() {
        String[] charSetAscii = {"p","k","b","r","q","k"};
        String[] charSetUnicodeWhite = {"♟","♞","♝","♜","♛","♚"};
        String[] charSetUnicodeBlack = {"♙","♘","♗","♖","♕","♔"};
        final boolean useUnicode = true;

        for (int row=0; row <= 7; ++row) {
            System.out.print(String.format("%d ", 8 - row));
            for (int col=0; col <= 7; ++col) {
                String c;
                Spot spot = getSpot(col, row);
                if (spot.isEmpty()) {
                    c = ((col + row) % 2 == 0) ? "." : " ";
                } else {
                    if (spot.getSide() == Side.White) {
                        if (useUnicode)
                            c = charSetUnicodeWhite[spot.getType()];
                        else
                            c = charSetAscii[spot.getType()].toUpperCase();
                    } else {
                        if (useUnicode)
                            c = charSetUnicodeBlack[spot.getType()];
                        else
                            c = charSetAscii[spot.getType()];
                    }
                }
                System.out.print(c + " ");
            }

            // Show the pieces taken at the end of the first 2 rows:
            if (row == 0) {
                System.out.print("   Black pieces taken: ");
                for (int piece : piecesTaken0) {
                    System.out.print(charSetUnicodeWhite[piece] + " ");
                }
            } else if (row == 1) {
                System.out.print("   White pieces taken: ");
                for (int piece : piecesTaken1) {
                    System.out.print(charSetUnicodeBlack[piece] + " ");
                }
            }

            System.out.println();
        }
        System.out.println("  a b c d e f g h");
    }

    /**
     * Set all of the attributes of a spot on the board.
     *
     * @param type The piece type to make the spot
     * @param side The side to make the piece for (Unused if type == Piece.Empty)
     * @param col The column of the spot to change
     * @param row The row of the spot to change
     */
    void putPiece(final int type, final int side, final int col, final int row) {
        board.set(row * 8 + col, new Spot(side, type, col, row));
    }

    /**
     * Fill an entire row of the board with a specific set of Spot values.
     * This is used as a helper function when initializing the board state.
     *
     * @param type The piece type to make the spots
     * @param side The side to associate the spots with
     * @param row The row on the board to fill.
     */
    void fillRow(final int type, final int side, final int row) {
        for (int col=0; col <= 7; ++col) {
            board.set(row * 8 + col, new Spot(side, type, col, row));
        }
    }

    /**
     * Initialize the list of Spots on the board to be a new list of empty spots
     *
     */
    void initBoardArray() {
        board = new ArrayList<>(64);
        for (int n=0; n < 64; ++n) {
            board.add(new Spot(Side.Black, Piece.Empty, n % 8, n / 8));
        }
        passes = 0;
    }

    /**
     * Standard Game
     */
    void init() {
        initBoardArray();
        putPiece(Piece.Rook,   Side.Black, 0, 0);
        putPiece(Piece.Knight, Side.Black, 1, 0);
        putPiece(Piece.Bishop, Side.Black, 2, 0);
        putPiece(Piece.Queen,  Side.Black, 3, 0);
        putPiece(Piece.King,   Side.Black, 4, 0);
        putPiece(Piece.Bishop, Side.Black, 5, 0);
        putPiece(Piece.Knight, Side.Black, 6, 0);
        putPiece(Piece.Rook,   Side.Black, 7, 0);
        fillRow(Piece.Pawn,    Side.Black, 1);
        fillRow(Piece.Pawn,    Side.White, 6);
        putPiece(Piece.Rook,   Side.White, 0, 7);
        putPiece(Piece.Knight, Side.White, 1, 7);
        putPiece(Piece.Bishop, Side.White, 2, 7);
        putPiece(Piece.Queen,  Side.White, 3, 7);
        putPiece(Piece.King,   Side.White, 4, 7);
        putPiece(Piece.Bishop, Side.White, 5, 7);
        putPiece(Piece.Knight, Side.White, 6, 7);
        putPiece(Piece.Rook,   Side.White, 7, 7);

        turn = Side.White;
        timeInGetMoves = 0L;

        piecesTaken0 = new ArrayList<>();
        piecesTaken1 = new ArrayList<>();

        currentPlayerMoves = getMovesSorted(turn);
    }

    /**
     * Pawn vs Queen
     */
    void initTest1() {
        initBoardArray();

        putPiece(Piece.Pawn,   Side.Black, 3, 3);
        getSpot(3, 3).setMoved(true);
        putPiece(Piece.Queen,  Side.White, 4, 4);

        turn = Side.White;
        timeInGetMoves = 0L;
        currentPlayerMoves = getMovesSorted(turn);
    }

    /**
     * Queen vs Queen
     */
    void initTest2() {
        initBoardArray();

        putPiece(Piece.Queen,  Side.Black, 3, 3);
        putPiece(Piece.Queen,  Side.White, 4, 4);

        turn = Side.White;
        timeInGetMoves = 0L;
        currentPlayerMoves = getMovesSorted(turn);
    }

    /**
     * Pawn vs Queen
     */
    void initTest3() {
        initBoardArray();

        putPiece(Piece.Pawn,   Side.Black, 3, 3);
        getSpot(3, 3).setMoved(true);
        putPiece(Piece.Queen,  Side.White, 6, 5);

        turn = Side.White;
        timeInGetMoves = 0L;
        currentPlayerMoves = getMovesSorted(turn);
    }

    /**
     * Queen vs Queen
     */
    void initTest4() {
        initBoardArray();

        putPiece(Piece.Queen,   Side.Black, 3, 3);
        putPiece(Piece.Queen,  Side.White, 6, 5);

        turn = Side.White;
        timeInGetMoves = 0L;
        currentPlayerMoves = getMovesSorted(turn);
    }

    /**
     * Pawn vs Pawn
     */
    void initTest5() {
        initBoardArray();

        putPiece(Piece.Pawn,   Side.Black, 3, 2);
        getSpot(3, 2).setMoved(true);

        putPiece(Piece.Pawn,   Side.White, 4, 5);
        getSpot(4, 5).setMoved(true);

        turn = Side.White;
        timeInGetMoves = 0L;
        currentPlayerMoves = getMovesSorted(turn);
    }

    /**
     * Rook & Queen vs King
     */
    void initTest6() {
        initBoardArray();

        putPiece(Piece.King,    Side.Black, 4, 0);

        putPiece(Piece.Rook,    Side.White, 7, 7);
        putPiece(Piece.Queen,   Side.White, 3, 7);

        turn = Side.White;
        timeInGetMoves = 0L;
        currentPlayerMoves = getMovesSorted(turn);
    }

    /**
     * Alternating Pawns vs Alternating Pawns
     */
    void initTest7() {
        initBoardArray();

        for (int col=0; col <= 7; col +=2) {
            putPiece(Piece.Pawn, Side.Black, col, 1);
            putPiece(Piece.Pawn, Side.White, col + 1, 6);
        }

        turn = Side.White;
        timeInGetMoves = 0L;
        currentPlayerMoves = getMovesSorted(turn);
    }

    /**
     * Check to see if a column and row are a valid spot on the board or not.
     *
     * @param col The column to be checked
     * @param row The row to be checked
     * @return true if the specified column and row are valid, false if either are invalid
     */
    private static boolean isValidSpot(final int col, final int row) {
        return col >= 0 && col <= 7 && row >= 0 && row <= 7;
    }

    /**
     * Display a list of moves to the console.
     *
     * @param name The name of the piece to be used in the output message
     * @param col The column to be displayed in the output message
     * @param row The row to be displayed in the output message
     * @param moves The list of moves to be displayed to the output
     */
    private void showMoves(final String name, final int col, final int row, final List<Move> moves) {
        System.out.println(String.format("%s@%d,%d moves: %d", name, col, row, moves.size()));
        for (Move m:moves) {
            System.out.println(String.format("    %s", m));
        }
    }

    /**
     * Get the Spot on the board at the specified location.
     *
     * @param col the column location of the desired spot
     * @param row the row location of the desired spot
     * @return returns the Spot at the specified location
     * @throws IllegalArgumentException if the specified location is invalid
     */
    Spot getSpot(final int col, final int row) throws IllegalArgumentException {
        if (!isValidSpot(col, row)) {
            throw new IllegalArgumentException("Invalid column: " + col + " or row: " + row);
        }
        return board.get(row * 8 + col);
    }

    /**
     * Execute a move on the current board state and update the board state.
     *
     * The specified move will be executed on the current board state of the board
     * and the board state will be updated to reflect the new state after the move
     * has been executed.
     *
     * @param move The Move to be executed on the current board
     */
    void executeMove(final Move move) {
        int fx = move.getFromCol();
        int fy = move.getFromRow();
        int tx = move.getToCol();
        int ty = move.getToRow();
        int fi = fy * 8 + fx;
        int ti = ty * 8 + tx;

        // If a piece is being captured then add it to the list:
        if (board.get(ti).getType() != Piece.Empty) {
            if (board.get(ti).getSide() == Side.Black) {
                piecesTaken1.add(board.get(ti).getType());
            } else {
                piecesTaken0.add(board.get(ti).getType());
            }
        }

        board.set(ti, board.get(fi));
        board.set(fi, new Spot(Side.Black, Piece.Empty, fx, fy));
        board.get(ti).setCol(tx);
        board.get(ti).setRow(ty);
        board.get(ti).setMoved(true);

        // See if this is a Castling move:
        if (board.get(ti).getType() == Piece.King) {
            int delta = tx - fx;
            if (delta == 2 || delta == -2) {
                // castling:
                if (delta < 0) {
                    // castling on queen's side. Also move the rook:
                    int rfi = fy * 8;       // rook from-index
                    int rti = fy * 8 + 3;   // rook to-index
                    board.set(rti, board.get(rfi));
                    board.get(rti).setCol(3);
                    board.get(rti).setRow(fy);
                    board.get(rti).setMoved(true);
                    board.set(rfi, new Spot(Side.Black, Piece.Empty, rfi % 8, rfi / 8));
                } else {
                    // castling on king's side. Also move the rook:
                    int rfi = fy * 8 + 7;   // rook from-index
                    int rti = fy * 8 + 5;   // rook to-index
                    board.set(rti, board.get(rfi));
                    board.get(rti).setCol(5);
                    board.get(rti).setRow(fy);
                    board.get(rti).setMoved(true);
                    board.set(rfi, new Spot(Side.Black, Piece.Empty, rfi % 8, rfi / 8));
                }
            }
        } else if (board.get(ti).getType() == Piece.Pawn) {
            if (ty == 0 || ty == 7) {
                // Pawn made it to back row = Promote to Queen
                board.get(ti).setType(Piece.Queen);
            }
        }
    }

    /**
     * Get the list of all available moves for the current player.
     * Note: For speed we only generate the list of moves for the current player
     * when a move is made and the state of the board has been changed.
     * This method simply returns the existing Move list.
     *
     * @return Returns The list of all available moves for the current player
     */
    List<Move> getCurrentPlayerMoves() {
        return currentPlayerMoves;
    }

    /**
     * Checks the board state to see if the specified side is in check
     *
     * @param board The board to check
     * @param side The side to examine to see if it is in check
     * @return true if the current player is in check otherwise false.
     */
    boolean kingInCheck(final Board board, final int side) {
        int otherSide = (side == Side.Black) ? Side.White : Side.Black;
        List<Move> opponentMoves = board.getMoves(otherSide);
        for (Spot s:board.getBoard()) {
            if (s.isEmpty()
                || s.getSide() != side
                || s.getType() != Piece.King) continue;
            for (Move m:opponentMoves) {
                if (m.getToCol() == s.getCol() && m.getToRow() == s.getRow()) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Create a new list of all possible moves for the specified side.
     *
     * @param side The side to get all available moves for
     * @return Returns a new List<Move> of all possible moves for the specified side
     */
    List<Move> getMoves(final int side) {
        final long startTime = System.nanoTime();
        final List<Move> moves = new ArrayList<>();
        List<Move> pMoves;

        for (Spot spot : board) {
            if (spot.isEmpty() || spot.getSide() != side) {
                continue;
            }
            int col = spot.getCol();
            int row = spot.getRow();
            switch (spot.getType()) {
                case Piece.Pawn:
                    pMoves = getPawnMoves(col, row);
//                    if (verbose == 1) showMoves("Pawn", col, row, pMoves);
                    moves.addAll(pMoves);
                    break;
                case Piece.Rook:
                    pMoves = getRookMoves(col, row);
//                    if (verbose == 1) showMoves("Rook", col, row, pMoves);
                    moves.addAll(pMoves);
                    break;
                case Piece.Knight:
                    pMoves = getKnightMoves(col, row);
//                    if (verbose == 1) showMoves("Knight", col, row, pMoves);
                    moves.addAll(pMoves);
                    break;
                case Piece.Bishop:
                    pMoves = getBishopMoves(col, row);
//                    if (verbose == 1) showMoves("Bishop", col, row, pMoves);
                    moves.addAll(pMoves);
                    break;
                case Piece.Queen:
                    pMoves = getQueenMoves(col, row);
//                    if (verbose == 1) showMoves("Queen", col, row, pMoves);
                    moves.addAll(pMoves);
                    break;
                case Piece.King:
                    pMoves = getKingMoves(col, row);
//                    if (verbose == 1) showMoves("King", col, row, pMoves);
                    moves.addAll(pMoves);
                    break;
            }
        }

        timeInGetMoves += (System.nanoTime() - startTime) / 1000;

        return cleanupMoves(moves, side);
    }

    List<Move> getMovesNonParallel(final int side) {
        return null;
    }

    List<Move> getMovesParallel(final int side) {
        final long startTime = System.nanoTime();
        final List<Move> moves = new ArrayList<>();
        final List<Thread> threads = new ArrayList<>();
        Thread t;

        for (Spot spot : board) {
            if (spot.isEmpty() || spot.getSide() != side) {
                continue;
            }
            int col = spot.getCol();
            int row = spot.getRow();
            switch (spot.getType()) {
                case Piece.Pawn:
                    t = new Thread(() -> {
                        List<Move> pMoves = getPawnMoves(col, row);
//                        if (verbose == 1) showMoves("Pawn", col, row, pMoves);
                        synchronized (moves) {
                            moves.addAll(pMoves);
                        }
                    });
                    t.start();
                    threads.add(t);
                    break;
                case Piece.Rook:
                    t = new Thread(() -> {
                        List<Move> pMoves = getRookMoves(col, row);
//                        if (verbose == 1) showMoves("Rook", col, row, pMoves);
                        synchronized (moves) {
                            moves.addAll(pMoves);
                        }
                    });
                    threads.add(t);
                    break;
                case Piece.Knight:
                    t = new Thread(() -> {
                        List<Move> pMoves = getKnightMoves(col, row);
//                        if (verbose == 1) showMoves("Knight", col, row, pMoves);
                        synchronized (moves) {
                            moves.addAll(pMoves);
                        }
                    });
                    t.start();
                    threads.add(t);
                    break;
                case Piece.Bishop:
                    t = new Thread(() -> {
                        List<Move> pMoves = getBishopMoves(col, row);
//                        if (verbose == 1) showMoves("Bishop", col, row, pMoves);
                        synchronized (moves) {
                            moves.addAll(pMoves);
                        }
                    });
                    t.start();
                    threads.add(t);
                    break;
                case Piece.Queen:
                    t = new Thread(() -> {
                        List<Move> pMoves = getQueenMoves(col, row);
//                        if (verbose == 1) showMoves("Queen", col, row, pMoves);
                        synchronized (moves) {
                            moves.addAll(pMoves);
                        }
                    });
                    t.start();
                    threads.add(t);
                    break;
                case Piece.King:
                    t = new Thread(() -> {
                        List<Move> pMoves = getKingMoves(col, row);
//                        if (verbose == 1) showMoves("King", col, row, pMoves);
                        synchronized (moves) {
                            moves.addAll(pMoves);
                        }
                    });
                    t.start();
                    threads.add(t);
                    break;
            }
        }

        try {
            for (Thread thread:threads) {
                thread.join();
            }
        } catch (InterruptedException e) {
            System.out.println("InterruptedException in getMoves(): " + e.getMessage());
        }

        timeInGetMoves += (System.nanoTime() - startTime) / 1000;

        return cleanupMoves(moves, side);
    }

    /**
     * Get a list of all possible moves for a given side sorted by their values.
     *
     * @param side The side to get the moves for.
     * @return Returns a List<Move> of all possible moves sorted in descending order
     *         based on the value of each move.
     */
    List<Move> getMovesSorted(final int side) {
        Comparator<Move> sortByValue = Comparator.comparing(Move::getValue).reversed();
        List<Move> moves = getMoves(side);
        moves.sort(sortByValue);
        return moves;
    }

    private static boolean inCleanup = false;

    /**
     * Take a list of moves and create a new list that does not contain
     * any moves that would place the specified side in check.
     *
     * @param moves The list of moves to clean up
     * @return A new List<Move> that does not include any moves that would place the current player in check.
     */
    private List<Move> cleanupMoves(final List<Move> moves, final int side) {
        if (inCleanup)
            return moves;
        inCleanup = true;

        List<Move> valid = new ArrayList<>();
        for (Move m:moves) {
            Board current = new Board(this);
            current.executeMove(m);
            if (!kingInCheck(current, side)) {
                valid.add(m);
            }
        }
        inCleanup = false;
        return valid;
    }

    /**
     * Utility method to add a new Move to a list of moves if the Move is valid.
     *
     * This method help eliminate repetitious blocks of code that perform the same checks
     * in the move generator code for each of the individual pieces.
     *
     * @param moves The List<Move> of moves to add the move to if it is valid
     * @param fromCol The column of piece to be moved
     * @param fromRow The row of the piece to be moved
     * @param toCol The column to move the piece to
     * @param toRow The row to move the piece to
     */
    private void addMoveIfValid(final List<Move> moves,
                                final int fromCol, final int fromRow,
                                final int toCol, final int toRow) {
        if (!isValidSpot(fromCol, fromRow)) {
            return;
        }
        if (!isValidSpot(toCol, toRow)) {
            return;
        }
        Spot fromSpot = getSpot(fromCol, fromRow);
        if (fromSpot.isEmpty()) {
            return;
        }

        int value = 0;
        Spot toSpot = getSpot(toCol, toRow);
        if (!toSpot.isEmpty()) {
            value = Piece.values[toSpot.getType()];
            if (fromSpot.getSide() == toSpot.getSide()) {
                return;
            }
        }

        // extra checks if moving a pawn...
        if (fromSpot.getType() == Piece.Pawn) {
            // if advancing in same column
            if (fromCol == toCol) {
                // if moving 2 spots...
                if (abs(fromRow - toRow) == 2) {
                    // not allowed if the pawn has already moved
                    if (fromSpot.getMoved()) {
                        return;
                    }
                    // not allowed if a piece is in the way
                    if (fromSpot.getSide() == Side.White && !getSpot(fromCol, fromRow - 1).isEmpty()) {
                        return;
                    } else if (fromSpot.getSide() == Side.Black && !getSpot(fromCol, fromRow + 1).isEmpty()) {
                        return;
                    }
                }
                // pawns cannot capture when moving directly forward
                if (!toSpot.isEmpty()) {
                    return;
                }
            } else {
                // pawns cannot move diagonally unless capturing
                if (toSpot.isEmpty()) {
                    return;
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
    List<Move> getPawnMoves(int col, int row) throws IllegalArgumentException {
        if (!isValidSpot(col, row)) {
            throw new IllegalArgumentException("Invalid col: " + col + " or row: " + row);
        }
        Spot spot = getSpot(col, row);

        if (spot.isEmpty()) {
            throw new IllegalArgumentException("No piece at spot col: " + col + " or row: " + row);
        }

        List<Move> moves = new ArrayList<>();
        int forward = spot.getSide() == Side.White ? -1 : 1;

        addMoveIfValid(moves, col, row, col, row + forward);
        addMoveIfValid(moves, col, row, col, row + forward + forward);
        addMoveIfValid(moves, col, row, col - 1, row + forward);
        addMoveIfValid(moves, col, row, col + 1, row + forward);

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
    List<Move> getRookMoves(int col, int row) throws IllegalArgumentException {
        if (!isValidSpot(col, row)) {
            throw new IllegalArgumentException("Invalid col: " + col + " or row: " + row);
        }
        Spot spot = getSpot(col, row);

        if (spot.isEmpty()) {
            throw new IllegalArgumentException("No piece at spot col: " + col + " or row: " + row);
        }

        List<Move> moves = new ArrayList<>();

        for (int x=col - 1; x >= 0; --x) {
            if (!isValidSpot(x, row))
                break;
            addMoveIfValid(moves, col, row, x, row);
            Spot nextSpot = getSpot(x, row);
            if (!nextSpot.isEmpty())
                break;
        }
        for (int x=col + 1; x <= 7; ++x) {
            if (!isValidSpot(x, row))
                break;
            addMoveIfValid(moves, col, row, x, row);
            Spot nextSpot = getSpot(x, row);
            if (!nextSpot.isEmpty())
                break;
        }
        for (int y=row - 1; y >= 0; --y) {
            if (!isValidSpot(col, y))
                break;
            addMoveIfValid(moves, col, row, col, y);
            Spot nextSpot = getSpot(col, y);
            if (!nextSpot.isEmpty())
                break;
        }
        for (int y=row + 1; y <= 7; ++y) {
            if (!isValidSpot(col, y))
                break;
            addMoveIfValid(moves, col, row, col, y);
            Spot nextSpot = getSpot(col, y);
            if (!nextSpot.isEmpty())
                break;
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
    List<Move> getKnightMoves(int col, int row) throws IllegalArgumentException {
        if (!isValidSpot(col, row)) {
            throw new IllegalArgumentException("Invalid col: " + col + " or row: " + row);
        }
        Spot spot = getSpot(col, row);

        if (spot.isEmpty()) {
            throw new IllegalArgumentException("No piece at spot col: " + col + " or row: " + row);
        }

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
    List<Move> getBishopMoves(int col, int row) throws IllegalArgumentException {
        if (!isValidSpot(col, row)) {
            throw new IllegalArgumentException("Invalid col: " + col + " or row: " + row);
        }
        Spot spot = getSpot(col, row);

        if (spot.isEmpty()) {
            throw new IllegalArgumentException("No piece at spot col: " + col + " or row: " + row);
        }

        List<Move> moves = new ArrayList<>();

        // nw
        for (int offset=1; offset <= 7; ++offset) {
            if (!isValidSpot(col - offset, row - offset))
                break;
            addMoveIfValid(moves, col, row, col - offset, row - offset);
            Spot nextSpot = getSpot(col - offset, row - offset);
            if (!nextSpot.isEmpty())
                break;
        }
        // ne
        for (int offset=1; offset <= 7; ++offset) {
            if (!isValidSpot(col + offset, row - offset))
                break;
            addMoveIfValid(moves, col, row, col + offset, row - offset);
            Spot nextSpot = getSpot(col + offset, row - offset);
            if (!nextSpot.isEmpty())
                break;
        }
        // sw
        for (int offset=1; offset <= 7; ++offset) {
            if (!isValidSpot(col - offset, row + offset))
                break;
            addMoveIfValid(moves, col, row, col - offset, row + offset);
            Spot nextSpot = getSpot(col - offset, row + offset);
            if (!nextSpot.isEmpty())
                break;
        }
        // se
        for (int offset=1; offset <= 7; ++offset) {
            if (!isValidSpot(col + offset, row + offset))
                break;
            addMoveIfValid(moves, col, row, col + offset, row + offset);
            Spot nextSpot = getSpot(col + offset, row + offset);
            if (!nextSpot.isEmpty())
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
    List<Move> getQueenMoves(int col, int row) throws IllegalArgumentException {
        if (!isValidSpot(col, row)) {
            throw new IllegalArgumentException("Invalid col: " + col + " or row: " + row);
        }
        Spot spot = getSpot(col, row);

        if (spot.isEmpty()) {
            throw new IllegalArgumentException("No piece at spot col: " + col + " or row: " + row);
        }

        List<Move> moves = getRookMoves(col, row);
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
    List<Move> getKingMoves(int col, int row) throws IllegalArgumentException {
        if (!isValidSpot(col, row)) {
            throw new IllegalArgumentException("Invalid col: " + col + " or row: " + row);
        }
        Spot spot = getSpot(col, row);

        if (spot.isEmpty()) {
            throw new IllegalArgumentException("No piece at spot col: " + col + " or row: " + row);
        }

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
        if (!spot.getMoved()
                && getSpot(col + 1, row).isEmpty()
                && getSpot(col + 2, row).isEmpty()
                && getSpot(col + 3, row).getType() == Piece.Rook
                && !getSpot(col + 3, row).getMoved()) {
            addMoveIfValid(moves, col, row, col + 2, row);
        }

        // check for queen side castling:
        if (!spot.getMoved()
                && getSpot(col - 1, row).isEmpty()
                && getSpot(col - 2, row).isEmpty()
                && getSpot(col - 3, row).isEmpty()
                && getSpot(col - 4, row).getType() == Piece.Rook
                && !getSpot(col - 4, row).getMoved()) {
            addMoveIfValid(moves, col, row, col - 2, row);
        }

        return moves;
    }
}