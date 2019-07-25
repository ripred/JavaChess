import java.util.concurrent.Callable;

/**
 * The LookAheadMoveThread objects are what is launched for each move the current player has.
 * They implement the minimax algorithm as well as the alpha-beta pruning.  They have timeouts
 * and will return the best move they've seen so far if it expires.
 */
public class LiteMoveThread implements Callable<BestMove> {
    private final LiteMinimax minimax;
    private final LiteBoard origBoard;
    private final boolean maximize;
    private final LiteBoard board;
    private final BestMove best;
    private final int depth;
    private final Move move;

    LiteMoveThread(final LiteBoard orig, final LiteMinimax minimax, boolean maximize, final Move move, int depth) {
        this.best = new BestMove(maximize);
        this.maximize = maximize;
        this.minimax = minimax;
        this.origBoard = orig;
        this.depth = depth;
        this.move = move;

        this.board = new LiteBoard(orig);
        this.board.executeMove(move);
        this.board.advanceTurn();
        this.minimax.addNumMovesExamined(1);
    }

    // This call is made to us when we are launched
    @Override
    public BestMove call() {
        Thread.yield();

        int lookAheadVal = minimax.minmax(board, LiteUtil.MIN_VALUE, LiteUtil.MAX_VALUE,
                depth - 1, !maximize);

        if ((maximize && lookAheadVal >= best.value) || (!maximize && lookAheadVal <= best.value)) {
            best.value = lookAheadVal;
            best.move = move;
            best.move.setValue(best.value);
            minimax.cachedMoves.addMoveValue(origBoard.board, maximize, best.move, best.value, 1);
        }

        return best;
    }
}
