package chess1.AI;

import chess1.LiteBoard;
import chess1.LiteUtil;
import chess1.Move;

import java.util.concurrent.Callable;

/**
 * The LookAheadMoveThread objects are what is launched for each move the current player has.
 * They implement the minimax algorithm as well as the alpha-beta pruning.  They have timeouts
 * and will return the best move they've seen so far if it expires.
 *
 */
public class LiteMoveThread implements Callable<BestMove> {
    private final boolean maximize;
    private final LiteMinimax minimax;
    private final LiteBoard board;
    private final int depth;
    private final Move move;
    private final BestMove best;

    LiteMoveThread(final LiteBoard orig, final LiteMinimax minimax, boolean maximize, final Move move, int depth) {
        this.best = new BestMove(maximize);
        this.minimax = minimax;
        this.maximize = maximize;
        this.depth = depth;
        this.move = move;

        this.board = new LiteBoard(orig);
        this.board.executeMove(move);
        this.board.advanceTurn();
        this.minimax.addNumMovesExamined(depth, 1);

        // See if that move left the other player with no moves and return it if so:
        best.endGameFound = board.numMoves1 == 0;
        if (best.endGameFound) {
            best.move = move;
            best.value = minimax.evaluate(board);
        }
    }

    // This call is made to us when we are launched
    @Override
    public BestMove call() {
        if (best.endGameFound) {
            return best;
        }

        Thread.yield();

        int lookAheadVal = minimax.minmax(board, LiteUtil.MIN_VALUE, LiteUtil.MAX_VALUE,
                depth - 1, !maximize);

        if (maximize && lookAheadVal >= best.value) {
            best.value = lookAheadVal;
            best.move = move;
        } else if (!maximize && lookAheadVal <= best.value) {
            best.value = lookAheadVal;
            best.move = move;
        }
        return best;
    }
}
