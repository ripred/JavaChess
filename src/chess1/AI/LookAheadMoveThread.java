package chess1.AI;

import chess1.Board;
import chess1.Move;
import chess1.Piece;

import java.util.concurrent.Callable;

/**
 * The LookAheadMoveThread objects are what is launched for each move the current player has.
 * They implement the minimax algorithm as well as the alpha-beta pruning.  They have timeouts
 * and will return the best move they've seen so far if it expires.
 *
 */
public class LookAheadMoveThread implements Callable<BestMove> {
    private boolean maximize;
    private Minimax minimax;
    private Board board;
    private int depth;
    private Move move;
    private BestMove best;

    LookAheadMoveThread(final Board board, final Minimax minimax, boolean maximize, final Move move, int depth) {
        this.best = new BestMove(maximize);
        this.minimax = minimax;
        this.maximize = maximize;
        this.depth = depth;
        this.move = move;

        this.board = new Board(board);
        this.board.executeMove(move);
        this.board.advanceTurn();
        minimax.addNumMovesExamined(1);

        // See if that move left the other player with no moves and return it if so:
        best.endGameFound = this.board.getCurrentPlayerMoves().isEmpty();
        if (best.endGameFound) {
            best.move = move;
            best.value = minimax.evaluate(this.board);
        }
    }

    boolean foundEndMove() {
        return best.endGameFound;
    }

    BestMove getBestMove() {
        return best;
    }

    // This call is made to us when we are launched
    @Override
    public BestMove call() {
        if (best.endGameFound) {
            return best;
        }

        Thread.yield();

        int lookAheadVal = minimax.minmax(board, Piece.MIN_VALUE, Piece.MAX_VALUE,
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
