package chess1;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 *
 * Implementation of the Minimax algorithm with alpha-beta pruning
 *
 * Minimax algorithm overview: https://en.wikipedia.org/wiki/Minimax
 *
 */
public class Minimax implements AIMoveSelector {
    private final Object processedLock = new Object();
    private final BoardEvaluator evaluator;
    private Integer movesProcessed;
    private final long maxSeconds;
    private int startDepth;
    private long stopNanos;
    private Consumer<String> callback;

    public Minimax(final int depth, final int maxSeconds) {
        this.maxSeconds = maxSeconds;
        this.startDepth = depth;
        this.movesProcessed = 0;
        this.callback = null;

        // instantiate a BoardEvaluator object to get an identity function for the board state
//        evaluator = new TotalMoveCounts();
//        evaluator = new TotalPieceValues();
        evaluator = new PieceValuesPlusPos();
    }

    public void registerDisplayCallback(Consumer<String> cb) {
        callback = cb;
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
        stopNanos = System.nanoTime() + (maxSeconds * 1_000_000_000L);
        boolean gameEndFound = false;
        int depth = startDepth;
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
                        currentBoard, Piece.MIN_VALUE, Piece.MAX_VALUE, startDepth - 1, maximizing);

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

        // Get the best move available by finding the best move result
        // returned by all of the threads we launched.  We don't do this
        // if we have found a game ending move already.
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

            // check additional end-game corner-cases
            bestMove = checkEndGameCornerCases(board, bestMove);
        }

        return bestMove;
    }

    Move checkEndGameCornerCases(final Board board, Move bestMove) {
        //
        // Map our moves by type
        //
        Map<Integer, List<Move>> ourMoveMap = mapMovesByType(board, board.getCurrentPlayerMoves());

        // if we are getting repetitive and possibly stuck at a local maxima then
        // see if we have any possible pawn advances available and pick one
        // of those instead
        if (board.checkDrawByRepetition(bestMove, 2)) {
            if (callback != null) {
                callback.accept("Attempting to end repetition...");
            }

            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

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
            }
        }

        if (bestMove == null) {
            return null;
        }

        Spot bestSpot = board.getSpot(bestMove.getFromCol(), bestMove.getFromRow());

        // Additional corner-case:
        // If the best move we think we have is the king and we are not moving him because we are in check:
        //     check to see if we have nothing but pawns moves left and pick
        //     one of those first to see if it can get promoted before we move this king

        // only do this if we are not in check:
        if (board.kingInCheck(board, board.getTurn()).size() == 0) {
            if (bestSpot.getType() == Piece.King) {
                // see if all we have left are pawn and king moves
                if (ourMoveMap.get(Piece.Pawn).size()        > 0 &&
                        ourMoveMap.get(Piece.Rook).size()   == 0 &&
                        ourMoveMap.get(Piece.Queen).size()  == 0) {
                    bestMove = ourMoveMap.get(Piece.Pawn).get(0);
                }
            }
        }

        if (board.kingInCheck(board, board.getTurn()).size() == 0) {
            List<Move> theirMoveList = board.getMovesSorted(board.getTurn() == Side.White ? Side.Black : Side.White);
            Map<Integer, List<Move>> theirMoveMap = mapMovesByType(board, theirMoveList);

            // see if all they have left are king moves
            if (theirMoveMap.get(Piece.Pawn).size()       == 0 &&
                    theirMoveMap.get(Piece.Knight).size() == 0 &&
                    theirMoveMap.get(Piece.Bishop).size() == 0 &&
                    theirMoveMap.get(Piece.Rook).size()   == 0 &&
                    theirMoveMap.get(Piece.Queen).size()  == 0 &&
                    theirMoveMap.get(Piece.King).size()    > 0) {

                if (ourMoveMap.get(Piece.Queen).size()    > 0 &&
                        ourMoveMap.get(Piece.Rook).size() > 0 &&
                        ourMoveMap.get(Piece.King).size() > 0 &&
                        startDepth < 6) {
                    // reset our start depth and it will boil out
                    int origDepth = startDepth;
                    startDepth = 6;
                    if (callback != null) {
                        callback.accept("Performing endgame search depth 6...");
                    }
                    Move result = bestMove(board);
                    if (callback != null) {
                        callback.accept("");
                    }
                    startDepth = origDepth;
                    return result;
                } else if (ourMoveMap.get(Piece.Queen).size() > 0 &&
                        ourMoveMap.get(Piece.King).size() > 0 &&
                        startDepth < 7) {
                    // reset our start depth and it will boil out
                    int origDepth = startDepth;
                    startDepth = 7;
                    if (callback != null) {
                        callback.accept("Performing endgame search depth 7...");
                    }
                    Move result = bestMove(board);
                    if (callback != null) {
                        callback.accept("");
                    }
                    startDepth = origDepth;
                    return result;
                } else if (ourMoveMap.get(Piece.Rook).size() > 0 &&
                        ourMoveMap.get(Piece.King).size() > 0 &&
                        startDepth < 7) {
                    // reset our start depth and it will boil out
                    int origDepth = startDepth;
                    startDepth = 7;
                    if (callback != null) {
                        callback.accept("Performing endgame search depth 7...");
                    }
                    Move result = bestMove(board);
                    if (callback != null) {
                        callback.accept("");
                    }
                    startDepth = origDepth;
                    return result;
                }
            }
        }

        return bestMove;
    }

    private static Map<Integer, List<Move>> mapMovesByType(Board board, List<Move> moves) {
        Map<Integer, List<Move>> movesByPiece = new HashMap<>(6);
        movesByPiece.put(Piece.Pawn,   new ArrayList<>());
        movesByPiece.put(Piece.Rook,   new ArrayList<>());
        movesByPiece.put(Piece.Knight, new ArrayList<>());
        movesByPiece.put(Piece.Bishop, new ArrayList<>());
        movesByPiece.put(Piece.Queen,  new ArrayList<>());
        movesByPiece.put(Piece.King,   new ArrayList<>());
        for (Move move:moves) {
            int pieceType = board.getSpot(move.getFromCol(), move.getFromRow()).getType();
            movesByPiece.get(pieceType).add(move);
        }
        return movesByPiece;
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

            if (alpha >= beta) {
                break;
            }
        }

        return lowestOrHighest;
    }
}
