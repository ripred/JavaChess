package chess1.AI;

import chess1.LiteBoard;
import chess1.LiteUtil;
import chess1.Move;
import chess1.Side;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 *
 * Implementation of the Minimax algorithm with alpha-beta pruning.
 *
 * Minimax algorithm overview: https://en.wikipedia.org/wiki/Minimax
 *
 */
public class LiteMinimax  {
    private final ExecutorService pool = Executors.newFixedThreadPool(1000);
    private final LiteEval evaluator = new LiteEval();
    private final Object processedLock = new Object();
    private Consumer<String> callback;
    private FutureTask[] threadStack;
    private Map<Integer, Integer> movesProcessed;
    private boolean maximize;
    private final long maxSeconds;
    private int maxThreads;
    private int numThreads;
    private int startDepth;
    private long stopNanos;
    public BestMove best;
    private int throttle;
    private Thread currentSearch;

    public LiteMinimax(int depth, int maxSeconds) {
        this.best = new BestMove(false);
        this.maxSeconds = maxSeconds;
        this.currentSearch = null;
        this.startDepth = depth;
        this.movesProcessed = null;
        this.threadStack = null;
        this.maximize = false;
        this.callback = null;
        this.maxThreads = 0;
        this.numThreads = 0;
    }

    public boolean moveSearchIsDone() {
        if (currentSearch == null) return true;
        if (currentSearch.isAlive()) {
            return false;
        } else {
            try {
                currentSearch.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            } finally {
                currentSearch = null;
            }
        }
        return true;
    }

    public Move getBestMove() {
        return best.move;
    }

    public void registerDisplayCallback(Consumer<String> cb) {
        callback = cb;
    }

    public int getMaxThreads() {
        return maxThreads;
    }

    public int getNumMovesExamined() {
        synchronized (processedLock) {
        int num = 0;
        for (int v: movesProcessed.values())
            num += v;
        return num;
        }
    }

    public Map<Integer, Integer> getNumMovesExaminedMap() {
        synchronized (processedLock) {
            return new HashMap<>(movesProcessed);
        }
    }

    public void addNumMovesExamined(int depth, int num) {
        synchronized (processedLock) {
            if (movesProcessed.containsKey(depth))
                movesProcessed.put(depth, movesProcessed.get(depth) + num);
            else
                movesProcessed.put(depth, num);
        }
    }

    public int evaluate(final LiteBoard board) {
        return evaluator.evaluate(board);
    }

    public void setThrottle(int nanos) {
        throttle = nanos;
    }

    private void printMsg(String msg) {
        if (callback != null) {
            callback.accept(msg);
        }
    }

    /**
     * Implementation of the bestMove() function of the
     * AIMoveSelector interface using the minimax algorithm.
     *
     * @param board The board state to find the best move for the current player for
     * @param returnImmediate the call will launch the search on a background thread and return immediately
     * @return the best move for the current player or null if there are no moves.
     */
    public Move bestMove(final LiteBoard board, boolean returnImmediate) {
        movesProcessed = Collections.synchronizedMap(new HashMap<>());
        final int side = board.turn;
        maximize = (side == Side.White);
        int numMoves = board.numMoves1;
        if (numMoves == 1) {
            // We have only one move so nothing the other side can do in response will change
            // what move we return so just return it now and save the recursive depth cost.
            addNumMovesExamined(startDepth, 1);
            return board.moves1[0];
        }

        // cancel any existing thread stack and create a new array to keep the new threads in
        cancelThreadStack();
        threadStack = new FutureTask[numMoves];
        numThreads = 0;

        stopNanos = System.nanoTime() + (maxSeconds * 1_000_000_000L);
        best = new BestMove(maximize);


        // Loop through all of the moves launching a thread for each one so each can go explore
        // what good board valuations we have in the future of this move and keep track of the best one
        for (int i=0; i < numMoves; i++) {
            Move move = board.moves1[i];
            // If we are past our allowed time then return the best move so far
            if (maxSeconds > 0 && System.nanoTime() >= stopNanos) {
                break;
            }

            LiteMoveThread lookAheadThread = new LiteMoveThread(board, this, maximize, move, startDepth);
//            if (lookAheadThread.foundEndMove()) {
//                best = lookAheadThread.getBestMove();
//                break;
//            }

            FutureTask<BestMove> task = new FutureTask<>(lookAheadThread);
            threadStack[numThreads++] = task;
            maxThreads = Integer.max(maxThreads, numThreads);
            Thread t = new Thread(task);
            t.start();
        }

        if (returnImmediate) {
            if (currentSearch != null) {
                currentSearch.interrupt();
            }
            currentSearch = new Thread(() -> finishCurrentSearch(board));
            currentSearch.start();
            return best.move;
        }

        return finishCurrentSearch(board);
    }

    private Move finishCurrentSearch(final LiteBoard board) {
        // Now we wait on all of the threads to finish so we can see which has the best score
        for (int i = 0; i < numThreads; ++i) {
            BestMove threadResult = null;

            try {
                threadResult = (BestMove) threadStack[i].get();
            } catch (Exception e) {
                System.out.println("\nWe're having problems waiting for move threads to finish..\n");
                e.printStackTrace();
            }

            if (threadResult != null && threadResult.endGameFound) {
                best = threadResult;
                cancelThreadStack();
                break;
            }

            if (best.endGameFound) {
                cancelThreadStack();
                break;
            }

            Thread.yield();

            if (threadResult != null) {
                if (maximize && threadResult.value > best.value) {
                    best = threadResult;
                } else if (!maximize && threadResult.value < best.value) {
                    best = threadResult;
                }
            }

            best.move = checkEndGameCornerCases(board, best.move);
        }

//        threadStack = null;
//        numThreads = 0;
        return best.move;
    }

    private void cancelThreadStack() {
        if (threadStack == null) return;
        stopNanos = System.nanoTime();
    }

    private Move checkEndGameCornerCases(final LiteBoard board, Move bestMove) {
        //
        // Map our moves by type
        //
        Map<Integer, List<Move>> ourMoveMap = mapMovesByType(board, board.moves1, board.numMoves1);

        // If we are getting repetitive and possibly stuck at a local maxima then
        // see if we have any possible pawn advances available and pick one
        // of those instead
        if (bestMove != null && board.checkDrawByRepetition(bestMove, board.maxRep - 1)) {
            printMsg("Attempting to end repetition..");

            if (ourMoveMap.get(LiteUtil.Pawn).size() > 0) {
                bestMove = ourMoveMap.get(LiteUtil.Pawn).get(0);
            } else if (ourMoveMap.get(LiteUtil.Queen).size() > 0) {
                bestMove = ourMoveMap.get(LiteUtil.Queen).get(0);
            } else if (ourMoveMap.get(LiteUtil.Rook).size() > 0) {
                bestMove = ourMoveMap.get(LiteUtil.Rook).get(0);
            } else if (ourMoveMap.get(LiteUtil.Bishop).size() > 0) {
                bestMove = ourMoveMap.get(LiteUtil.Bishop).get(0);
            } else if (ourMoveMap.get(LiteUtil.Knight).size() > 0) {
                bestMove = ourMoveMap.get(LiteUtil.Knight).get(0);
            }  else if (ourMoveMap.get(LiteUtil.King).size() > 0) {
                bestMove = ourMoveMap.get(LiteUtil.King).get(0);
            }

        }

        if (bestMove == null) {
            return null;
        }

        int bestNdx = bestMove.getFromCol() + bestMove.getFromRow() * 8;

        // Additional corner-case:
        // If the best move we think we have is the king and we are not in check:
        //     Check to see if we have any pawn moves left and use one of those
        //     to see if it can get promoted before we move this king

        // Only do this if we are not in check:
        if (board.kingInCheck(board.turn)) {
            if (board.getType(bestNdx) == LiteUtil.King) {
                // See if all we have left are pawn and king moves
                if (ourMoveMap.get(LiteUtil.Pawn).size() > 0 &&
                        ourMoveMap.get(LiteUtil.Rook).size()  == 0 &&
                        ourMoveMap.get(LiteUtil.Queen).size() == 0) {
                    bestMove = ourMoveMap.get(LiteUtil.Pawn).get(0);
                }
            }
        }

        // See if our opponent only has their king left and get more aggressive if so:

        if (!board.kingInCheck(board.turn)) {
            Map<Integer, List<Move>> theirMoveMap = mapMovesByType(board, board.moves2, board.numMoves2);

            // If all they have left is their king..
            if (theirMoveMap.get(LiteUtil.Pawn).size() == 0 &&
                    theirMoveMap.get(LiteUtil.Knight).size() == 0 &&
                    theirMoveMap.get(LiteUtil.Bishop).size() == 0 &&
                    theirMoveMap.get(LiteUtil.Rook).size() == 0 &&
                    theirMoveMap.get(LiteUtil.Queen).size() == 0 &&
                    theirMoveMap.get(LiteUtil.King).size() > 0) {

                // if we have pawns march them towards the end row
                if (ourMoveMap.get(LiteUtil.Pawn).size() > 0) {
                    bestMove = ourMoveMap.get(LiteUtil.Pawn).get(0);
                } else {

                    // If we have at least one queen and one rook then look further to find a game ending move
                    if (ourMoveMap.get(LiteUtil.Queen).size() > 0 &&
                            ourMoveMap.get(LiteUtil.Rook).size() > 0 &&
                            ourMoveMap.get(LiteUtil.King).size() > 0 &&
                            startDepth < 6) {

                        // Extend the depth and search again for a game ending move
                        startDepth = 6;
                        printMsg("Looking for game-ending moves...");

                    } else if ((ourMoveMap.get(LiteUtil.Queen).size() > 0)
                            || (ourMoveMap.get(LiteUtil.Rook).size() > 0) &&
                            ourMoveMap.get(LiteUtil.King).size() > 0 &&
                            startDepth < 7) {

                        // We have (at minimum) a queen or a rook along with our king so we should
                        // be able to back them into a corner if we search even further ahead..
                        startDepth = 7;
                        printMsg("Looking even further for a game ending move..");
                    }
                }
            }
        }
        return bestMove;
    }

    private static Map<Integer, List<Move>> mapMovesByType(LiteBoard board, Move[] moves, int numMoves) {
        Map<Integer, List<Move>> movesByPiece = new HashMap<>(6);
        movesByPiece.put(LiteUtil.Pawn, new ArrayList<>());
        movesByPiece.put(LiteUtil.Rook, new ArrayList<>());
        movesByPiece.put(LiteUtil.Knight, new ArrayList<>());
        movesByPiece.put(LiteUtil.Bishop, new ArrayList<>());
        movesByPiece.put(LiteUtil.Queen, new ArrayList<>());
        movesByPiece.put(LiteUtil.King, new ArrayList<>());
        for (int i=0; i < numMoves; i++) {
            Move move = moves[i];
            int pieceType = board.getType(move.getFromCol() + move.getFromRow() * 8);
            List<Move> list = movesByPiece.get(pieceType);
            list.add(move);
        }
        return movesByPiece;
    }

    int minmax(final LiteBoard board, int alpha, int beta, int depth, boolean maximize) {
        int value = maximize ? LiteUtil.MIN_VALUE : LiteUtil.MAX_VALUE;
        int movesExamined = 0;

        if (throttle > 0) {
            try {
                Thread.sleep(0, throttle);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        for (int i=0; i < board.numMoves1; i++) {
            Move move = board.moves1[i];
            if (depth <= 0) {
                if ((move.getValue() ==  0) || depth < -2) {
                    return evaluator.evaluate(board);
                }
            }

            LiteBoard currentBoard = new LiteBoard(board);
            currentBoard.executeMove(move);
            currentBoard.advanceTurn();

            ++movesExamined;

            // See if the move we just made leaves the other player with
            // no moves and return the best value we've seen if so:
            if (currentBoard.numMoves1 == 0) {
                value = maximize ?
                        LiteUtil.MAX_VALUE - (100 - depth) :
                        LiteUtil.MIN_VALUE + (100 - depth);
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
        addNumMovesExamined(depth, movesExamined);
        return value;
    }

    // Called when our context is being destroyed

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
//            Thread.currentThread().interrupt();
        }
    }
}