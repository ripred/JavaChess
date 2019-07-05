package chess1.AI;

import chess1.Board;
import chess1.Move;
import chess1.Piece;
import chess1.Side;
import chess1.Spot;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.FutureTask;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * Implementation of the Minimax algorithm with alpha-beta pruning.
 *
 * Minimax algorithm overview: https://en.wikipedia.org/wiki/Minimax
 *
 */
public class Minimax extends AIMoveSelector {
    private ExecutorService pool = Executors.newFixedThreadPool(threadPoolSize);
    private final BoardEvaluator evaluator = new PieceValuesPlusPos();
    private final Object processedLock = new Object();
    private Consumer<String> callback;
    private Integer movesProcessed;
    private long maxSeconds;
    private int maxThreads;
    private int startDepth;
    private long stopNanos;

    @Override
    public void registerDisplayCallback(Consumer<String> cb) {
        callback = cb;
    }

    private void printMsg(String msg) {
        if (callback != null) {
            callback.accept(msg);
        }
    }

    @Override
    public int getMaxThreads() {
        return maxThreads;
    }

    public Minimax(int maxThreads, int depth, int maxSeconds) {
        super(maxThreads);
        this.maxSeconds = maxSeconds;
        this.startDepth = depth;
        this.movesProcessed = 0;
        this.callback = null;
        this.maxThreads = 0;
    }

    @Override
    public int getNumMovesExamined() {
        synchronized (processedLock) {
            return movesProcessed;
        }
    }

    @Override
    public void addNumMovesExamined(int n) {
        synchronized (processedLock) {
            movesProcessed += n;
        }
    }

    int evaluate(final Board board) {
        return evaluator.evaluate(board);
    }

    public void setDepth(int n) {
        startDepth = n;
    }

    public int getDepth() {
        return startDepth;
    }

    /**
     * Implementation of the bestMove() function of the
     * AIMoveSelector interface using the minimax algorithm.
     *
     * @param board The board state to find the best move for the current player for
     * @return the best move for the current player or null if there are no moves.
     */
    @Override
    public Move bestMove(final Board board) {
        final int side = board.getTurn();
        final boolean maximize = (side == Side.White);
        int numMoves = board.getCurrentPlayerMoves().size();
        BestMove best = new BestMove(maximize);

        movesProcessed = 0;
        stopNanos = System.nanoTime() + (maxSeconds * 1_000_000_000L);

        if (numMoves == 1) {
            // We have only one move so nothing the other side can do in response will change
            // what move we return so just return it now and save the recursive depth cost.
            synchronized (processedLock) {
                ++movesProcessed;
            }
            return board.getCurrentPlayerMoves().get(0);
        }

        // Array to keep the threads in
        FutureTask[] lookAheadThreads = new FutureTask[numMoves];
        int numThreads = 0;

        // Loop through all of the moves launching a thread for each one so each can go explore
        // what good board valuations we have in the future of this move and keep track of the best one
        for (Move move : board.getCurrentPlayerMoves()) {
            // If we are past our allowed time then return the best move so far
            if (maxSeconds > 0 && System.nanoTime() >= stopNanos) {
                break;
            }

            LookAheadMoveThread lookAheadThread = new LookAheadMoveThread(board, this, maximize, move, startDepth);
            if (lookAheadThread.foundEndMove()) {
                best = lookAheadThread.getBestMove();
                break;
            }

            FutureTask<LookAheadMoveThread> task = new FutureTask<>(lookAheadThread);
            lookAheadThreads[numThreads++] = task;
            maxThreads = Integer.max(maxThreads, numThreads);
            Thread t = new Thread(task);
            t.setPriority(Thread.MIN_PRIORITY);
            t.start();
        }

        // Now we wait on all of the threads to finish so we can see which has the best score
        for (int i = 0; i < numThreads; ++i) {
            LookAheadMoveThread threadResult = null;
            try {
                threadResult = (LookAheadMoveThread) lookAheadThreads[i].get();
            } catch (Exception e) {
                System.out.println("\nWe're having problems waiting for move threads to finish..\n");
                e.printStackTrace();
            }

            if (threadResult != null && threadResult.foundEndMove()) {
                best = threadResult.getBestMove();

                for (int k = i + 1; k < numThreads; ++k) {
                    lookAheadThreads[k].cancel(true);
                }
                break;
            }

            Thread.yield();

            if (threadResult != null) {
                if (maximize && threadResult.getBestMove().value > best.value) {
                    best = threadResult.getBestMove();
                } else if (!maximize && threadResult.getBestMove().value < best.value) {
                    best = threadResult.getBestMove();
                }
            }

            best.move = checkEndGameCornerCases(board, best.move);
        }
        return best.move;
    }


    private Move checkEndGameCornerCases(final Board board, Move bestMove) {
        //
        // Map our moves by type
        //
        Map<Integer, List<Move>> ourMoveMap = mapMovesByType(board, board.getCurrentPlayerMoves());

        // If we are getting repetitive and possibly stuck at a local maxima then
        // see if we have any possible pawn advances available and pick one
        // of those instead
        if (bestMove != null && board.checkDrawByRepetition(bestMove, board.getMaxAllowedRepetitions())) {
            printMsg("Attempting to end repetition..");

            if (ourMoveMap.get(Piece.Pawn).size() > 0) {
                bestMove = ourMoveMap.get(Piece.Pawn).get(0);
            } else if (ourMoveMap.get(Piece.Queen).size() > 0) {
                bestMove = ourMoveMap.get(Piece.Queen).get(0);
            } else if (ourMoveMap.get(Piece.Rook).size() > 0) {
                bestMove = ourMoveMap.get(Piece.Rook).get(0);
            } else if (ourMoveMap.get(Piece.Bishop).size() > 0) {
                bestMove = ourMoveMap.get(Piece.Bishop).get(0);
            } else if (ourMoveMap.get(Piece.Knight).size() > 0) {
                bestMove = ourMoveMap.get(Piece.Knight).get(0);
            }  else if (ourMoveMap.get(Piece.King).size() > 0) {
                bestMove = ourMoveMap.get(Piece.King).get(0);
            }

        }

        if (bestMove == null) {
            return null;
        }

        Spot bestSpot = board.getSpot(bestMove.getFromCol(), bestMove.getFromRow());

        // Additional corner-case:
        // If the best move we think we have is the king and we are not in check:
        //     Check to see if we have any pawn moves left and use one of those
        //     to see if it can get promoted before we move this king

        // Only do this if we are not in check:
        if (board.kingInCheck(board, board.getTurn()).size() == 0) {
            if (bestSpot.getType() == Piece.King) {
                // See if all we have left are pawn and king moves
                if (ourMoveMap.get(Piece.Pawn).size() > 0 &&
                        ourMoveMap.get(Piece.Rook).size()  == 0 &&
                        ourMoveMap.get(Piece.Queen).size() == 0) {
                    bestMove = ourMoveMap.get(Piece.Pawn).get(0);
                }
            }
        }

        // See if our opponent only has their king left and get more aggressive if so:

        if (board.kingInCheck(board, board.getTurn()).size() == 0) {
            List<Move> theirMoveList = board.getMovesSorted(board.getTurn() == Side.White ? Side.Black : Side.White);
            Map<Integer, List<Move>> theirMoveMap = mapMovesByType(board, theirMoveList);

            // If all they have left is their king..
            if (theirMoveMap.get(Piece.Pawn).size() == 0 &&
                    theirMoveMap.get(Piece.Knight).size() == 0 &&
                    theirMoveMap.get(Piece.Bishop).size() == 0 &&
                    theirMoveMap.get(Piece.Rook).size() == 0 &&
                    theirMoveMap.get(Piece.Queen).size() == 0 &&
                    theirMoveMap.get(Piece.King).size() > 0) {

                // If we have at least one queen and one rook then look further to find a game ending move
                if (ourMoveMap.get(Piece.Queen).size() > 0 &&
                        ourMoveMap.get(Piece.Rook).size() > 0 &&
                        ourMoveMap.get(Piece.King).size() > 0 &&
                        startDepth < 6) {

                    // Extend the depth and search again for a game ending move
                    startDepth = 6;
                    printMsg("Looking for game-ending moves...");

                } else if ((ourMoveMap.get(Piece.Queen).size() > 0)
                        || (ourMoveMap.get(Piece.Rook).size() > 0) &&
                        ourMoveMap.get(Piece.King).size() > 0 &&
                        startDepth < 7) {

                    // We have (at minimum) a queen or a rook along with our king so we should
                    // be able to back them into a corner if we search even further ahead..
                    startDepth = 7;
                    printMsg("Looking even further for a game ending move..");
                }
            }
        }
        return bestMove;
    }

    private static Map<Integer, List<Move>> mapMovesByType(Board board, List<Move> moves) {
        Map<Integer, List<Move>> movesByPiece = new HashMap<>(6);
        movesByPiece.put(Piece.Pawn, new ArrayList<>());
        movesByPiece.put(Piece.Rook, new ArrayList<>());
        movesByPiece.put(Piece.Knight, new ArrayList<>());
        movesByPiece.put(Piece.Bishop, new ArrayList<>());
        movesByPiece.put(Piece.Queen, new ArrayList<>());
        movesByPiece.put(Piece.King, new ArrayList<>());
        for (Move move : moves) {
            int pieceType = board.getSpot(move.getFromCol(), move.getFromRow()).getType();
            movesByPiece.get(pieceType).add(move);
        }
        return movesByPiece;
    }

    int minmax(final Board board, int alpha, int beta, int depth, boolean maximize) {
        int value = maximize ? Piece.MIN_VALUE : Piece.MAX_VALUE;
        int movesExamined = 0;

        if (depth <= 0) {
            return evaluator.evaluate(board);
        }

        for (final Move move : board.getCurrentPlayerMoves()) {
            Board currentBoard = new Board(board);
            currentBoard.executeMove(move);
            currentBoard.advanceTurn();

            ++movesExamined;

            // See if the move we just made leaves the other player with
            // no moves and return the best value we've seen if so:
            if (currentBoard.getCurrentPlayerMoves().size() == 0) {
                value = maximize ?
                        Piece.MAX_VALUE - (100 - depth) :
                        Piece.MIN_VALUE + (100 - depth);
                break;
            }

            Thread.yield();

            // The minimax step
            // While we have the depth keep looking ahead to see what this move accomplishes
            int lookAheadValue = minmax(currentBoard, alpha, beta, depth - 1, !maximize);
            if ((!maximize && lookAheadValue < value) || (maximize && lookAheadValue > value)) {
                value = lookAheadValue;
            }

            // The alpha-beta pruning step
            // Continually tighten the low and high range of what we see this accomplishes
            // in the future and stop now if all future results are worse than the best move
            // we already have.
            if (maximize)
                alpha = Integer.max(alpha, lookAheadValue);
            else
                beta  = Integer.min(beta,  lookAheadValue);

            if (alpha >= beta) {
                break;
            }

            // If we are out of time then return the best outcome we've seen this move accomplish
            if (maxSeconds > 0 && System.nanoTime() >= stopNanos) {
                break;
            }

            if (Thread.currentThread().isInterrupted()) {
                break;
            }
        }
        synchronized (processedLock) {
            movesProcessed += movesExamined;
        }
        return value;
    }

    // Called when our context is being destroyed
    @Override
    public void close() throws IOException {
        // Disable new tasks from being submitted
        pool.shutdown();

        try {
            // Wait a while for existing tasks to terminate
            if (!pool.awaitTermination(60, TimeUnit.SECONDS)) {
                // Cancel currently executing tasks
                pool.shutdownNow();

                // Wait a while for tasks to respond to being cancelled
                if (!pool.awaitTermination(60, TimeUnit.SECONDS))
                    System.err.println("some look-ahead threads did not terminate");
            }
        } catch (InterruptedException ie) {
            // (Re-)Cancel if current thread also interrupted
            pool.shutdownNow();

            // Preserve interrupt status
            Thread.currentThread().interrupt();
        }
    }
}