import java.util.concurrent.Callable;

/**
 * The LookAheadMoveThread objects are what is launched for each move the current player has.
 * They implement the minimax algorithm as well as the alpha-beta pruning.  They have timeouts
 * and will return the best move they've seen so far if it expires.
 */
public class LiteMoveThread implements Callable<BestMove> {
    private final boolean maximize;
    private final LiteMinimax minimax;
    private final LiteBoard board;
    private final LiteBoard origBoard;
    private final int depth;
    private final Move move;
    private final BestMove best;

    LiteMoveThread(final LiteBoard orig, final LiteMinimax minimax, boolean maximize, final Move move, int depth) {
        this.best = new BestMove(maximize);
        this.minimax = minimax;
        this.maximize = maximize;
        this.depth = depth;
        this.move = move;
        this.origBoard = orig;

        this.board = new LiteBoard(orig);
        this.board.executeMove(move);
        this.board.advanceTurn();
        this.minimax.addNumMovesExamined(1);

        // See if that move left the other player with no moves and return it if so:
        best.endGameFound = board.numMoves1 == 0;
        if (best.endGameFound) {
            best.value = minimax.evaluate(board);
            best.move = move;
            best.move.setValue(best.value);
        }
    }

    // This call is made to us when we are launched
    @Override
    public BestMove call() {
        if (best.endGameFound) {
            minimax.cachedMoves.addMoveValue(this.origBoard.board, maximize, best.move, best.value, 1);
            return best;
        }

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
