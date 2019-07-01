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
public class LookAheadMoveThread implements Callable<LookAheadMoveThread> {
    boolean endMoveFound;
    boolean maximize;
    Minimax minimax;
    BestMove best;
    Board board;
    int depth;
    Move move;

    LookAheadMoveThread(final Board board, final Minimax minimax, boolean maximize, final Move move, int depth) {
        this.best = new BestMove(maximize);
        this.minimax = minimax;
        this.maximize = maximize;
        this.depth = depth;
        this.move = move;
        this.endMoveFound = false;

        this.board = new Board(board);
        this.board.executeMove(move);
        this.board.advanceTurn();
        minimax.addNumMovesExamined(1);

        // See if that move left the other player with no moves and return it if so:
        endMoveFound = this.board.getCurrentPlayerMoves().isEmpty();
        if (endMoveFound) {
            best.move = move;
            best.value = minimax.evaluate(this.board);
        }
    }

    // This call is made to us when we are launched
    @Override
    public LookAheadMoveThread call() {
        if (endMoveFound) {
            return this;
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
        return this;
    }
}
