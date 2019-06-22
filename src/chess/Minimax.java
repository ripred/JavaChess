package chess;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * Implementation of the Minimax algorithm
 *
 * Minimax algorithm overview: https://en.wikipedia.org/wiki/Minimax
 *
 * Example code: https://github.com/ykaragol/checkersmaster/blob/master/CheckersMaster/src/checkers/algorithm/MinimaxAlgorithm.java
 *
 * Example chess minimax explanation video: https://www.youtube.com/watch?v=ILI31U3UMI4&list=PLOJzCFLZdG4zk5d-1_ah2B4kqZSeIlWtt&index=46
 *
 * DONE: Added alpha-beta pruning
 *
 */
public class Minimax implements AIMoveSelector {
    private final Object alphaLock = new Object();
    private final Object betaLock = new Object();
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
        this.stopNanos = 0L;

//        evaluator = new TotalMoveCounts();
//        evaluator = new TotalPieceValues();
        evaluator = new PieceValuesPlusPos();
    }

    @Override
    public int getNumMovesExamined() {
        int result;
        synchronized (processedLock) {
            result = movesProcessed;
            movesProcessed = 0;
        }
        return result;
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
        int lowest = Integer.MAX_VALUE;
        int highest = Integer.MIN_VALUE;
        int alpha = Integer.MIN_VALUE;
        int beta = Integer.MAX_VALUE;
        final int side = board.getTurn();
        int depth = startDepth;
        stopNanos = System.nanoTime() + (maxSeconds * 1_000_000_000L);
        Move bestMove = null;

        if (board.getCurrentPlayerMoves().size() == 1) {
            // We have only one move.  Nothing the other side can do in response will change
            // what our best move is so just return it now and save the depth cost.
            return board.getCurrentPlayerMoves().get(0);
        }

        List<Thread> threads = new ArrayList<>();
        List<Object> results = new ArrayList<>();
        Object resultsLock = new Object();

        for (Move m:board.getCurrentPlayerMoves()) {
            Board currentBoard = new Board(board);
            currentBoard.executeMove(m);
            currentBoard.advanceTurn();
            synchronized (processedLock) {
                ++movesProcessed;
            }

            // See if that move leaves the other player with no moves and return it if so:
            if (currentBoard.getCurrentPlayerMoves().isEmpty()) {
                bestMove = m;
                break;
            }

            // If we are past our allowed time then return the best move so far
            if (maxSeconds > 0 && System.nanoTime() >= stopNanos) {
                break;
            }

            Thread t = new Thread(() -> {
                int currentValue = (side == Side.White) ?
                        this.min(currentBoard, alpha, beta, depth - 1) :
                        this.max(currentBoard, alpha, beta, depth - 1);

                synchronized (resultsLock) {
                    results.add(currentValue);
                    results.add(m);
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

        if (board.getVerbose() >= 4) {
            System.out.print(" ".repeat((6 - depth) * 2));
            System.out.print(String.format(
                    "bestMove(): %s's side, Depth=%d, lowest = %d, highest = %d, best move = ",
                    side == 1 ? "White" : "Black",
                    depth,
                    lowest,
                    highest
            ));
            if (bestMove == null) {
                System.out.println("null");
            } else {
                System.out.println(String.format("%d,%d to %d, %d",
                        bestMove.getFromCol(),
                        bestMove.getFromRow(),
                        bestMove.getToCol(),
                        bestMove.getToRow()));
            }
        }
        return bestMove;
    }

    private int min(final Board board, int alpha, int beta, final int depth) {

        if (depth <= 0) {
            int value = evaluator.evaluate(board);
            if (board.getVerbose() >= 4) {
                System.out.print(" ".repeat((6 - depth) * 2));
                System.out.println(String.format(
                        "min(): %s's turn, Depth=%d, evaluate = %d",
                        board.getTurn() == 0 ? "White" : "Black",
                        depth,
                        value));
            }
            return value;
        }
        int lowest = Integer.MAX_VALUE;
        for (final Move m:board.getCurrentPlayerMoves()) {
            Board currentBoard = new Board(board);
            currentBoard.executeMove(m);
            currentBoard.advanceTurn();

            synchronized (processedLock) {
                ++movesProcessed;
            }

            Thread.yield();

            // See if that move leaves the other player with no moves and return the lowest value possible if so:
            if (currentBoard.getCurrentPlayerMoves().size() == 0) {
                lowest = Integer.MIN_VALUE + (100 - depth);
                break;
            }

            // If we are past our allowed time then return the lowest so far
            if (maxSeconds > 0 && System.nanoTime() >= stopNanos) {
                break;
            }

            final int currentValue = max(currentBoard, alpha, beta, depth - 1);
            if (currentValue <= lowest) {
                lowest = currentValue;
            }

            synchronized (alphaLock) {
                alpha = Integer.max(alpha, currentValue);
                if (alpha > beta) {
                    break;
                }
            }
        }

        if (board.getVerbose() >= 4) {
            System.out.print(" ".repeat((6 - depth) * 2));
            System.out.println(String.format(
                    "min(): %s's turn, Depth=%d, lowest = %d",
                    board.getTurn() == 0 ? "White" : "Black",
                    depth,
                    lowest));
        }
        return lowest;
    }

    private int max(final Board board, int alpha, int beta, final int depth) {
        if (depth <= 0) {
            int value = evaluator.evaluate(board);
            if (board.getVerbose() >= 4) {
                System.out.print(" ".repeat((6 - depth) * 2));
                System.out.println(String.format(
                        "max(): %s's turn, Depth=%d, evaluate = %d",
                        board.getTurn() == 0 ? "White" : "Black",
                        depth,
                        value));
            }
            return value;
        }
        int highest = Integer.MIN_VALUE;
        for (Move m:board.getCurrentPlayerMoves()) {
            Board currentBoard = new Board(board);
            currentBoard.executeMove(m);
            currentBoard.advanceTurn();

            synchronized (processedLock) {
                ++movesProcessed;
            }

            Thread.yield();

            // See if that move leaves the other player with no moves and return the lowest value possible if so:
            if (currentBoard.getCurrentPlayerMoves().size() == 0) {
                highest = Integer.MAX_VALUE - (100 - depth);
                break;
            }

            // If we are past our allowed time then return the highest so far
            if (maxSeconds > 0 && System.nanoTime() >= stopNanos) {
                break;
            }

            final int currentValue = min(currentBoard, alpha, beta, depth - 1);
            if (currentValue >= highest) {
                highest = currentValue;
            }

            synchronized (betaLock) {
                beta = Integer.min(beta, currentValue);
                if (alpha > beta) {
                    break;
                }
            }
        }

        if (board.getVerbose() >= 4) {
            System.out.print(" ".repeat((6 - depth) * 2));
            System.out.println(String.format(
                    "max(): %s's turn, Depth=%d, highest = %d",
                    board.getTurn() == 0 ? "White" : "Black",
                    depth,
                    highest));
        }
        return highest;
    }
}
