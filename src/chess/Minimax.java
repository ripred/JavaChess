package chess;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * Implementation of the Minimax algorithm with alpha-beta pruning
 *
 * Minimax algorithm overview: https://en.wikipedia.org/wiki/Minimax
 *
 */
public class Minimax implements AIMoveSelector {
    private final Object processedLock = new Object();
    private BoardEvaluator evaluator;
    private Integer movesProcessed;
    private long maxSeconds;
    private int startDepth;
    private long stopNanos;

    Minimax(final int depth, final int maxSeconds) {
        this.maxSeconds = maxSeconds;
        this.startDepth = depth;
        this.movesProcessed = 0;

        // instantiate a BoardEvaluator object to get an identity function for the board state
//        evaluator = new TotalMoveCounts();
//        evaluator = new PieceValuesPlusPos();
        evaluator = new TotalPieceValues();
    }

    @Override
    public int getNumMovesExamined() {
        synchronized (processedLock) {
            int result = movesProcessed;
            movesProcessed = 0;
            return result;
        }
    }

    /**
     * Implementation of the bestMove() function of the
     * AIMoveSelector interface using the minimax algorithm.
     *
     * @param board The board state to find the best move for the current player for
     * @return Returns the best move for the current player or null if there are no moves.
     */
    @Override
    public Move bestMove(final Board board) {
        int lowest = Piece.MAX_VALUE;
        int highest = Piece.MIN_VALUE;
        final int side = board.getTurn();
        final boolean maximizing = side == Side.Black;
        int depth = startDepth;
        stopNanos = System.nanoTime() + (maxSeconds * 1_000_000_000L);
        boolean gameEndFound = false;
        Move bestMove = null;

        if (board.getCurrentPlayerMoves().size() == 1) {
            // We have only one move so nothing the other side can do in response will change
            // what move we return so just return it now and save the recursive depth cost.
            synchronized (processedLock) {
                ++movesProcessed;
            }
            return board.getCurrentPlayerMoves().get(0);
        }

        List<Thread> threads = new ArrayList<>();
        List<Object> results = new ArrayList<>();
        Object resultsLock = new Object();

        for (Move move:board.getCurrentPlayerMoves()) {
            Board currentBoard = new Board(board);
            currentBoard.executeMove(move);
            currentBoard.advanceTurn();
            synchronized (processedLock) {
                ++movesProcessed;
            }

            // See if that move leaves the other player with no moves and return it if so:
            if (currentBoard.getCurrentPlayerMoves().isEmpty()) {
                gameEndFound = true;
                bestMove = move;
                break;
            }

            // If we are past our allowed time then return the best move so far
            if (maxSeconds > 0 && System.nanoTime() >= stopNanos) {
                break;
            }

            Thread t = new Thread(() -> {
                int currentValue = minmax(
                        currentBoard, Piece.MIN_VALUE, Piece.MAX_VALUE, depth - 1, maximizing);

                synchronized (resultsLock) {
                    results.add(currentValue);
                    results.add(move);
                }
            });
            threads.add(t);
            t.start();
        }

        for (Thread t:threads) {
            try {
                t.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        if (!gameEndFound) {
            synchronized (resultsLock) {
                while (!results.isEmpty()) {
                    int value = ((int) results.remove(0));
                    Move move = ((Move) results.remove(0));
                    if (side == Side.White) {
                        if (value >= highest) {
                            highest = value;
                            bestMove = move;
                        }
                    } else {
                        if (value <= lowest) {
                            lowest = value;
                            bestMove = move;
                        }
                    }
                }
            }
        }
        return bestMove;
    }

    private int minmax(final Board board, int alpha, int beta, final int depth, final boolean maximizing) {
        if (depth <= 0) {
            return evaluator.evaluate(board);
        }

        int lowestOrHighest = maximizing ? Piece.MIN_VALUE : Piece.MAX_VALUE;
        for (final Move m:board.getCurrentPlayerMoves()) {
            Board currentBoard = new Board(board);
            currentBoard.executeMove(m);
            currentBoard.advanceTurn();

            synchronized (processedLock) {
                ++movesProcessed;
            }

            Thread.yield();

            // See if the move we just made leaves the other player with
            // no moves and return the best value possible if so:
            if (currentBoard.getCurrentPlayerMoves().size() == 0) {
                if (maximizing)
                    lowestOrHighest = Piece.MAX_VALUE - (100 - depth);
                else
                    lowestOrHighest = Piece.MIN_VALUE + (100 - depth);
                break;
            }

            // If we are past our allowed time then return the lowest so far
            if (maxSeconds > 0 && System.nanoTime() >= stopNanos) {
                break;
            }

            final int currentValue = minmax(currentBoard, alpha, beta, depth - 1, !maximizing);
            if ((!maximizing && currentValue <= lowestOrHighest) || (maximizing && currentValue >= lowestOrHighest)) {
                lowestOrHighest = currentValue;
            }

            if (maximizing)
                alpha = Integer.max(alpha, currentValue);
            else
                beta = Integer.min(beta, currentValue);

            if (alpha > beta) {
                break;
            }
        }

        return lowestOrHighest;
    }
}
