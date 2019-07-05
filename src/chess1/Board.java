package chess1;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import static java.lang.Math.abs;

public class Board {
    private List<Move> currentPlayerMoves;
    private List<Spot> board;
    private List<Integer> piecesTaken0;
    private List<Integer> piecesTaken1;
    private List<Move> moveHistory;
    private final int verbose;
    private int turn;
    private int passes;
    private Move lastMove;
    private int maxAllowedRepetitions;

    /**
     * The default constructor for a Board object.
     *
     * Initializes the board to a new game state and sets the verbose level to none.
     */
    public Board() {
        verbose = 0;
        init();
    }

    /**
     * Constructor for a new Board object that allows setting the verbose level.
     *
     * @param verbose A flag indicating the level of debug information to output to the console.
     *                Higher values for 'verbose' indicate more debug information.
     *                If this flag is 0 no debug output will be written to the console.
     */
    public Board(int verbose) {
        this.verbose = verbose;
        init();
    }

    /**
     * Copy-constructor for a Board object.
     *
     * @param ref The Board object to copy into this new Board.
     */
    public Board(final Board ref) {
        board = ref.copyBoard(false);
        verbose = ref.verbose;
        turn = ref.turn;
        passes = ref.passes;
        lastMove = new Move(ref.lastMove);
        if (ref.currentPlayerMoves == null) {
            currentPlayerMoves = null;
        } else {
            currentPlayerMoves = new ArrayList<>(ref.currentPlayerMoves.size());
            currentPlayerMoves.addAll(ref.currentPlayerMoves);
        }
        piecesTaken0 = new ArrayList<>();
        piecesTaken0.addAll(ref.piecesTaken0);
        piecesTaken1 = new ArrayList<>();
        piecesTaken1.addAll(ref.piecesTaken1);
        moveHistory = new ArrayList<>();
        moveHistory.addAll(ref.moveHistory);
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

        passes = 1;
        turn = Side.White;
        maxAllowedRepetitions = 3;
        lastMove = new Move(-1, -1, -1, -1, -1);
        piecesTaken0 = new ArrayList<>();
        piecesTaken1 = new ArrayList<>();
        moveHistory = new ArrayList<>();
        currentPlayerMoves = getMovesSorted(turn);
    }

    /**
     * Test board
     */
    public void initTest() {
        initBoardArray();

        putPiece(Piece.King,  Side.Black, 2, 4);
        getSpot(2, 4).setMoved(true);

        putPiece(Piece.Queen, Side.White, 3, 6);
        putPiece(Piece.King,  Side.White, 2, 6);
        getSpot(2, 6).setMoved(true);

        passes = 1;
        turn = Side.White;
        maxAllowedRepetitions = 3;
        lastMove = new Move(-1, -1, -1, -1, -1);
        piecesTaken0 = new ArrayList<>();
        piecesTaken1 = new ArrayList<>();
        moveHistory = new ArrayList<>();
        currentPlayerMoves = getMovesSorted(turn);
    }

    /**
     * @return Returns a new copy of the current List<Spot> of the spots on the board.
     */
    private List<Spot> copyBoard(boolean reverse) {
        List<Spot> newBoard = new ArrayList<>();
        for (Spot s:board) {
            if (reverse)
                newBoard.add(0, new Spot(s));
            else
                newBoard.add(new Spot(s));
        }
        return newBoard;
    }

    /**
     * @return Returns the list of spots on the board for the current game state.
     */
    public List<Spot> getBoard() {
        return board;
    }

    /**
     * @return Returns 1 if the current side is white or 0 if the current side is black.
     *
     */
    public int getTurn() {
        return turn;
    }

    public int getNumTurns() { return passes; }

    public Move getLastMove() {
        return new Move(lastMove);
    }

    public int getMaxAllowedRepetitions() {
        return maxAllowedRepetitions;
    }

    public void setMaxAllowedRepetitions(int maxRepetitions) {
        maxAllowedRepetitions = maxRepetitions;
    }

    int getVerbose() { return verbose; }

    public final List<Integer> getPiecesTaken0() {
        List<Integer> list = new ArrayList<>(piecesTaken0.size());
        list.addAll(piecesTaken0);
        return list;
    }

    public final List<Integer> getPiecesTaken1() {
        List<Integer> list = new ArrayList<>(piecesTaken1.size());
        list.addAll(piecesTaken1);
        return list;
    }

    public final List<Move> getMoveHistory() {
        List<Move> list = new ArrayList<>();
        moveHistory.forEach(m -> {
            list.add(new Move(m));
        });
        return list;
    }

    public boolean checkDrawByRepetition() {
        // Check for draw-by-repetition (same made too many times in a row by a player)
        List<Move> history = getMoveHistory();
        return history.size() >= Math.pow(2.0, (float) maxAllowedRepetitions + 1)
                && Collections.frequency(
                history.subList(history.size() - (int) Math.pow(2.0, (float) maxAllowedRepetitions + 1), history.size()),
                getLastMove()) >= maxAllowedRepetitions;
    }

    public boolean checkDrawByRepetition(final Move move, final int maxRepetitions) {
        // Check for draw-by-repetition (same made too many times in a row by a player)
        List<Move> history = getMoveHistory();
        return history.size() >= Math.pow(2.0, (float) maxRepetitions + 1)
                && Collections.frequency(
                history.subList(history.size() - (int) Math.pow(2.0, (float) maxRepetitions + 1), history.size()), move) >= maxRepetitions;
    }

    /**
     * Advance the turn to the next player and create a new list of available moves for that player.
     *
     */
    public void advanceTurn() {
        passes++;
        turn = (turn == Side.White) ? Side.Black : Side.White;
        currentPlayerMoves = getMovesSorted(turn);
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
    private void fillRow(final int type, final int side, final int row) {
        for (int col=0; col <= 7; ++col) {
            board.set(row * 8 + col, new Spot(side, type, col, row));
        }
    }

    /**
     * Initialize the list of Spots on the board to be a new list of empty spots
     *
     */
    private void initBoardArray() {
        final int BOARD_SIZE = 64;
        board = new ArrayList<>(BOARD_SIZE);
        for (int pos=0; pos < BOARD_SIZE; ++pos) {
            board.add(new Spot(Side.Black, Piece.Empty, pos % 8, pos / 8));
        }
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
     * Get the Spot on the board at the specified location.
     *
     * @param col the column location of the desired spot
     * @param row the row location of the desired spot
     * @return returns the Spot at the specified location
     * @throws IllegalArgumentException if the specified location is invalid
     */
    public Spot getSpot(final int col, final int row) throws IllegalArgumentException {
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
    public void executeMove(final Move move) {
        lastMove = new Move(move);

        int fx = move.getFromCol();
        int fy = move.getFromRow();
        int tx = move.getToCol();
        int ty = move.getToRow();
        int fi = fy * 8 + fx;
        int ti = ty * 8 + tx;

        // If a piece is being captured then add it to the list:
        if (board.get(ti).getType() != Piece.Empty) {
            if (board.get(ti).getSide() == Side.Black) {
                piecesTaken0.add(board.get(ti).getType());
            } else {
                piecesTaken1.add(board.get(ti).getType());
            }
        }

        board.set(ti, board.get(fi));
        board.set(fi, new Spot(Side.Black, Piece.Empty, fx, fy));
        board.get(ti).setCol(tx);
        board.get(ti).setRow(ty);
        board.get(ti).setMoved(true);

        // See if this is a Castling move:
        if (board.get(ti).getType() == Piece.King) {
            // get the number of columns being moved. Only castling moves 2 column positions
            int delta = tx - fx;
            if (abs(delta) == 2) {
                // This is a castling move so we need to move to rook as well:
                int rfi;    // Rook from index
                int rti;    // Rook to index
                if (delta < 0) {
                    // castling is on queen's side.
                    rfi = fy * 8;       // rook from-index
                    rti = fy * 8 + 3;   // rook to-index
                } else {
                    // castling is on king's side
                    rfi = fy * 8 + 7;   // rook from-index
                    rti = fy * 8 + 5;   // rook to-index
                }
                board.set(rti, board.get(rfi));
                board.get(rti).setCol(rti % 8);
                board.get(rti).setRow(fy);
                board.get(rti).setMoved(true);
                board.set(rfi, new Spot(Side.Black, Piece.Empty, rfi % 8, rfi / 8));
            }
        } else if (board.get(ti).getType() == Piece.Pawn) {
            if (ty == 0 || ty == 7) {
                // Pawn made it to back row = Promote to Queen
                board.get(ti).setType(Piece.Queen);
            }
        }
        moveHistory.add(move);
    }

    /**
     * Get the list of all available moves for the current player.
     * Note: For speed we only generate the list of moves for the current player
     * when a move is made and the state of the board has been changed.
     * This method simply returns the existing Move list.
     *
     * @return Returns The list of all available moves for the current player
     */
    public List<Move> getCurrentPlayerMoves() {
        return currentPlayerMoves;
    }

    /**
     * Checks the board state to see if the specified side is in check
     *
     * @param board The board to check
     * @param side The side to examine to see if it is in check
     * @return true if the current player is in check otherwise false.
     */
    public List<Move> kingInCheck(final Board board, final int side) {
        List<Move> moves = new ArrayList<>();
        int otherSide = (side == Side.White) ? Side.Black : Side.White;
        List<Move> opponentMoves = board.getMoves(otherSide, false);

        for (Spot s : board.getBoard()) {
            if (s.getType() != Piece.King || s.getSide() != side || s.isEmpty()) continue;

            for (Move m : opponentMoves) {
                if (m.getToCol() == s.getCol() && m.getToRow() == s.getRow()) {
                    moves.add(new Move(m));
                }
            }
            break;
        }
        return moves;
    }

    /**
     * Create a new list of all possible moves for the specified side.
     *
     * @param side The side to get all available moves for
     * @return Returns a new List<Move> of all possible moves for the specified side
     */
    public List<Move> getMoves(final int side, final boolean checkKing) {
        final long startTime = System.nanoTime();
        final List<Move> moves = new ArrayList<>();
        List<Move> pMoves;

        int start = 0;
        int end = 64;
        int delta = 1;

        if (turn == Side.Black) {
            start = 63;
            end = -1;
            delta = -1;
        }

        for (int spotLoop = start; spotLoop != end; spotLoop += delta) {
            Spot spot = board.get(spotLoop);
            if (spot.isEmpty() || spot.getSide() != side) {
                continue;
            }
            int col = spot.getCol();
            int row = spot.getRow();
            switch (spot.getType()) {
                case Piece.Pawn:
                    pMoves = getPawnMoves(col, row);
                    moves.addAll(pMoves);
                    break;
                case Piece.Rook:
                    pMoves = getRookMoves(col, row);
                    moves.addAll(pMoves);
                    break;
                case Piece.Knight:
                    pMoves = getKnightMoves(col, row);
                    moves.addAll(pMoves);
                    break;
                case Piece.Bishop:
                    pMoves = getBishopMoves(col, row);
                    moves.addAll(pMoves);
                    break;
                case Piece.Queen:
                    pMoves = getQueenMoves(col, row);
                    moves.addAll(pMoves);
                    break;
                case Piece.King:
                    pMoves = getKingMoves(col, row);
                    moves.addAll(pMoves);
                    break;
            }
        }

        if (checkKing) {
            return cleanupMoves(moves, side);
        } else {
            return moves;
        }
    }

    /**
     * Get a list of all possible moves for a given side sorted by their values.
     *
     * @param side The side to get the moves for.
     * @return Returns a List<Move> of all possible moves sorted in descending order
     *         based on the value of each move.
     */
    public List<Move> getMovesSorted(final int side) {
        Comparator<Move> sortByValue = Comparator.comparing(Move::getValue).reversed();
        List<Move> moves = getMoves(side, true);
        moves.sort(sortByValue);
        return moves;
    }

    /**
     * Take a list of moves and create a new list that does not contain
     * any moves that would place the specified side in check.
     *
     * @param moves The list of moves to clean up
     * @return A new List<Move> that does not include any moves that would place the current player in check.
     */
    private List<Move> cleanupMoves(final List<Move> moves, final int side) {
        List<Move> valid = new ArrayList<>();
        for (Move m:moves) {
            Board current = new Board(this);
            current.executeMove(m);
            if (kingInCheck(current, side).size() == 0) {
                valid.add(m);
            }
        }
        return valid;
    }

    /**
     * Utility method to add a new Move to a list of moves if the Move is valid.
     *
     * This method help eliminate repetitious blocks of code that perform the same checks
     * in the move generator code for each of the individual pieces.
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
    private List<Move> getPawnMoves(int col, int row) throws IllegalArgumentException {
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
    private List<Move> getRookMoves(int col, int row) throws IllegalArgumentException {
        if (!isValidSpot(col, row)) {
            throw new IllegalArgumentException("Invalid col: " + col + " or row: " + row);
        }
        Spot startSpot = getSpot(col, row);

        if (startSpot.isEmpty()) {
            throw new IllegalArgumentException("No piece at spot col: " + col + " or row: " + row);
        }

        List<Move> moves = new ArrayList<>();

        for (int x=col - 1; x >= 0; --x) {
            if (!isValidSpot(x, row))
                break;
            addMoveIfValid(moves, col, row, x, row);
            Spot spot = getSpot(x, row);
            if (!spot.isEmpty())
                break;
        }
        for (int x=col + 1; x <= 7; ++x) {
            if (!isValidSpot(x, row))
                break;
            addMoveIfValid(moves, col, row, x, row);
            Spot spot = getSpot(x, row);
            if (!spot.isEmpty())
                break;
        }
        for (int y=row - 1; y >= 0; --y) {
            if (!isValidSpot(col, y))
                break;
            addMoveIfValid(moves, col, row, col, y);
            Spot spot = getSpot(col, y);
            if (!spot.isEmpty())
                break;
        }
        for (int y=row + 1; y <= 7; ++y) {
            if (!isValidSpot(col, y))
                break;
            addMoveIfValid(moves, col, row, col, y);
            Spot spot = getSpot(col, y);
            if (!spot.isEmpty())
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
    private List<Move> getKnightMoves(int col, int row) throws IllegalArgumentException {
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
    private List<Move> getBishopMoves(int col, int row) throws IllegalArgumentException {
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
    private List<Move> getQueenMoves(int col, int row) throws IllegalArgumentException {
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
    private List<Move> getKingMoves(int col, int row) throws IllegalArgumentException {
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