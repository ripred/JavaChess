import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.io.IOException;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.FutureTask;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.Future;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.stream.Stream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Arrays;
import java.util.List;
import java.util.Map;


/**
 * Implementation of the Minimax algorithm with alpha-beta pruning.
 * <p>
 * <p>
 * Minimax algorithm overview: https://en.wikipedia.org/wiki/Minimax
 */
public class LiteMinimax implements Serializable {
    private static final long serialVersionUID = 7249069248361182397L;

    private ExecutorService executorForMainSearch = null;
    private ExecutorService pool = null;
    private Object processedLock = null;
    public  Future<Move> currentSearch;
    private FutureTask<BestMove>[] threadStack;
    private double acceptableRiskLevel;
    private Consumer<String> callback;
    private String serDeserFilename;
    private long searchTimeLimit;
    private long movesProcessed;
    private long gameDuration;
    private boolean maximize;
    private long maxSeconds;
    private int maxThreads;
    private int numThreads;
    private int startDepth;
    private long gameTime;
    private int throttle;
    public BestMove best;


    // Board state and move maps
    //
    CachedMoveMap cachedMoves;


    /**
     * Create a LiteMinimax object
     *
     * @param filename   the filename to deserialize the move map from
     * @param depth      The ply depth this minimax engine will search to
     * @param maxSeconds The maximum number of seconds allowed to find
     *                   a best move.  If it times out it will return
     *                   the best move found so far.
     */
    public LiteMinimax(String filename, int depth, int maxSeconds) {
        this.acceptableRiskLevel = Main.riskLevel;
        this.best = new BestMove(false);
        this.cachedMoves = new CachedMoveMap();
        this.gameTime = System.nanoTime();
        this.acceptableRiskLevel = 0.25f;
        this.serDeserFilename = filename;
        this.maxSeconds = maxSeconds;
        this.currentSearch = null;
        this.startDepth = depth;
        this.movesProcessed = 0L;
        this.threadStack = null;
        this.maximize = false;
        this.callback = null;
        this.maxThreads = 0;
        this.numThreads = 0;
    }


    private void initThreadSupport() {
        if (executorForMainSearch == null) {
            executorForMainSearch = Executors.newSingleThreadExecutor();
            pool = Executors.newFixedThreadPool(100);
            processedLock = new Object();
            currentSearch = null;
            threadStack = null;
            maxThreads = 0;
            numThreads = 0;
        }
    }


    /**
     * See if a previous search launched in the background has completed or not
     *
     * @return true if the last background search initiated has completed
     */
    public boolean moveSearchIsDone() {
        if (!Main.useThreads) return true;
        if (currentSearch == null) return true;
        if (!currentSearch.isDone()) {
            return false;
        } else {
            Move move = null;
            try {
                move = currentSearch.get();
                if (move != null) {
                    best.move = move;
                    best.value = move.getValue();
                } else {
                    best = null;
                }
                currentSearch = null;
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            } finally {
                currentSearch = null;
            }
            currentSearch = null;
        }
        return true;
    }


    /**
     * Get the best move found by the last search
     *
     * @return the best move found by the last search performed or null if no move was found
     */
    public Move getBestMove() {
        return best.move;
    }


    /**
     * Get the maximum number of threads launched during a search
     *
     * @return
     */
    public int getMaxThreads() {
        return maxThreads;
    }


    /**
     * Get the number of moves examined so far by the last search that was started.
     * If the search was launched in the background then repeated calls to
     * this method will return higher values indicating the progress of the
     * move examinations.
     *
     * @return the number of moves examined for the last search started
     */
    public long getNumMovesExamined() {
        if (processedLock != null) {
            synchronized (processedLock) {
                return movesProcessed;
            }
        } else {
            return movesProcessed;
        }
    }


    /**
     * Add to the number of moves examined so far during this search
     *
     * @param num   the number of moves examined
     */
    public void addNumMovesExamined(int num) {
        if (processedLock != null) {
            synchronized (processedLock) {
                movesProcessed += num;
            }
        } else {
            movesProcessed += num;
        }
    }


    /**
     * Register a String message consumer that can receive
     * any asynchronous this object needs to send.
     *
     * @param cb an instance of a functional object to send
     *           our messages to if needed.
     */
    public void registerDisplayCallback(Consumer<String> cb) {
        callback = cb;
    }


    /**
     * Ask the currently associated board evaluator object
     * to give a score for this board state.
     * <p>
     *
     * Scores above 0 indicate an advantage for the white
     * player.  Scores below 0 indicate a more favorable
     * board for the black player.
     *
     * @param board the board state to examine
     * @return the score for the evaluation of this board
     */
    public int evaluate(final LiteBoard board) {
        return LiteEval.evaluate(board);
    }


    /**
     * Set a throttle on the worker threads used in the search.  The threads
     * will all sleep the specified number of nanoseconds (billionths of a second)
     * after each move is examined in order to reduce CPU consumption by this
     * process if needed.
     *
     * @param nanos the number of nanoseconds to sleep after each move is examined
     */
    public void setThrottle(int nanos) {
        throttle = nanos;
    }

    private int fileNum = 1;
    public void saveMoveMap(String filename) throws IOException {

        Main.logWriter.close();
        Main.logWriter = null;

        Main.logFile = "chess" + (fileNum++) + ".log";
        Main.log("Saving moves to disk..");
        log(null);
        Main.log("");
        Main.logWriter.close();
        Main.logWriter = null;

        int outerSize = cachedMoves.size();
        int outerCount = 0;
        int innerCount = 0;
        int innerSize = 0;

        CachedMoveMap temp = new CachedMoveMap();
        for (String key : cachedMoves.keySet()) {
            if (!temp.containsKey(key)) {

                innerSize = cachedMoves.get(key).size();

                temp.put(key, new ConcurrentHashMap<>(innerSize, 0.1f, 1));
            }

            ConcurrentMap<Boolean, BestMove> bestMap = cachedMoves.get(key);

            System.out.print('\r' + Ansi.clearEOL);

            for (Boolean b : bestMap.keySet()) {
                int num = (++innerCount + outerCount * innerSize);
                if ((num % 100_000) == 0) {
                    System.out.printf("\rCopying move %,d of %,d%s",
                            num,
                            (outerSize * innerSize),
                            Ansi.clearEOL);
                }

                BestMove best = bestMap.get(b);
                if (!temp.get(key).containsKey(b)) {
                    temp.get(key).put(b, best);
                }
            }
            innerCount = 0;
            outerCount++;
        }
        System.out.print('\r' + Ansi.clearEOL);
        System.out.print("\rSerializing to disk.." + Ansi.clearEOL);

        ObjectOutputStream objectOutputStream = null;
        try {
            FileOutputStream fos = new FileOutputStream(filename);
            BufferedOutputStream bos = new BufferedOutputStream(fos, 1024 * 1024 * 128);
            objectOutputStream = new ObjectOutputStream(bos);
            objectOutputStream.writeObject(temp);
            objectOutputStream.flush();
        } finally {
            if (objectOutputStream != null) {
                objectOutputStream.close();
            }
        }
        System.out.print('\r' + Ansi.clearEOL);
        System.out.flush();

        temp = null;

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.gc();
    }

    public void readMoveMap(String filename) throws IOException, ClassNotFoundException {
        FileInputStream fileInputStream
                = new FileInputStream(filename);
        BufferedInputStream bis = new BufferedInputStream(fileInputStream, 1024 * 1024 * 128);
        ObjectInputStream objectInputStream
                = new ObjectInputStream(bis);
        cachedMoves = (CachedMoveMap) objectInputStream.readObject();
        objectInputStream.close();

        System.out.print("Loaded move map!!");
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }


    public void log(LiteBoard board) {
        gameDuration = (System.nanoTime() - gameTime) / 1_000_000_000L;
        int hours = 0;
        int minutes = 0;
        int seconds;
        while (gameDuration > 360) {
            hours++;
            gameDuration /= 360;
        }
        while (gameDuration > 60) {
            minutes++;
            gameDuration /= 60;
        }
        seconds = (int) gameDuration;

        int totalCaches = 2 * cachedMoves.size();
        long totalMapChecks = cachedMoves.numCacheHits + cachedMoves.numCacheMisses;

        Main.setLogLevel(Main.LogLevel.DEBUG);

        List<String> logLines = Arrays.asList(
                "Minimax Statistics = ",
                "",
                String.format("Ply Depth for game: %d", Main.maxDepth),
                String.format("Game Duration: %02d:%02d:%02d", hours, minutes, seconds),
                (board == null) ? "" : String.format("Turn: %d", board.turns),
                "",
                String.format("Total Boards stored in Cache:      %,14d", cachedMoves.size()),
                String.format("Max Boards stored in Cache:        %,14d", cachedMoves.maxMovesCached),
                String.format("Total Moves in Cache:              %,14d", totalCaches),
                "",
                String.format("Number Cached Moves Offered:       %,14d", cachedMoves.numMovesOffered),
                String.format("Number Cached Moves Added:         %,14d", cachedMoves.numMovesCached),
                String.format("Number of times Replaced Existing: %,14d", cachedMoves.numMovesUpdated),
                "",
                String.format("Number of Total Cache Checks:      %,14d", totalMapChecks),
                String.format("Number of Cache hits:              %,14d", cachedMoves.numCacheHits),
                String.format("Number Cache misses:               %,14d", cachedMoves.numCacheMisses),
                String.format("Number of Saved Move examinations: %,14d", cachedMoves.numExaminationsSaved),
                "",
                String.format("Number time Ran Anyway:            %,14d", cachedMoves.numMovesTested),
                String.format("Number Ran Anyway Better:          %,14d", cachedMoves.numMovesImproved),
                "",
                "Minimax Statistics = ");
        Main.log(Main.LogLevel.DEBUG, logLines);
    }


    /**
     * Private helper function to send a message to a
     * message consumer (observer) that has registered with us.
     *
     * @param msg the message to send
     */
    private void printMsg(String msg) {
        if (callback != null) {
            callback.accept(msg);
        }
    }


    class PieceList extends ArrayList<Integer> {
    }

    class SidePieceMap extends HashMap<Integer, PieceList> {
        public PieceList pawns;
        public PieceList knights;
        public PieceList bishops;
        public PieceList queen;
        public PieceList king;

        SidePieceMap() {
            super();
            for (int type = LiteBoard.Pawn; type <= LiteBoard.King; type++)
                put(type, new PieceList());
            pawns = get(LiteBoard.Pawn);
            knights = get(LiteBoard.Knight);
            bishops = get(LiteBoard.Bishop);
            queen = get(LiteBoard.Queen);
            king = get(LiteBoard.King);
        }
    }

    class PieceMap extends HashMap<Integer, SidePieceMap> {
    }


    /**
     * Search the specified board for the best move for the player who moves next.
     *
     * @param board           The board state to find the best move for the current player for
     * @param returnImmediate if set to true then this call will start the search in the a background
     *                        and return immediately.
     * @return the best move for the current player or null if there are no moves found so far
     * (If 'returnImmediate' is false and this method returns null then no move could
     * be found.
     */
    public Move bestMove(final LiteBoard board, boolean returnImmediate) {
        final int side = board.turn;
        maximize = (side == Side.White);
        movesProcessed = 0L;

        // Clear the best move we have for this search and set the time limit for them to finish.
        // If maxSeconds == 0 then the threads ignore the time limit and run to completion.
        searchTimeLimit = (maxSeconds == 0) ? 0 : System.nanoTime() + (maxSeconds * 1_000_000_000L);
        best = new BestMove(maximize);

        if (board.numMoves1 == 1) {
            // We have only one move so nothing the other side can do in response will change
            // what move we make so just return it now and save the recursive depth cost.
            addNumMovesExamined(1);
            return board.moves1[0];
        }

        if (board.numMoves1 <= 0) {
            String playerName = (side == Side.Black) ? Main.config.player2 : Main.config.player1;
            Main.log(Main.LogLevel.DEBUG, "bestMove(...) called and no moves are available for ", playerName);
            return null;
        }

        // create a map of the pieces on the board by side, and then by type, and then their locations
        SidePieceMap piecesOurs = new SidePieceMap();
        SidePieceMap piecesTheirs = new SidePieceMap();
        PieceMap pieceMap = new PieceMap();
        pieceMap.put(side, piecesOurs);
        pieceMap.put((side + 1) % 2, piecesTheirs);
        for (int ndx = 0; ndx < LiteBoard.BOARD_SIZE; ndx++) {
            if (board.isEmpty(ndx)) continue;
            pieceMap.get((int) LiteUtil.getSide(board.board[ndx])).get((int) LiteUtil.getType(board.board[ndx])).add(ndx);
        }

        // If threads are not enabled then we search here and now on the current thread:
        if (!Main.useThreads) {
            return searchWithNoThreads(board, pieceMap);
        }

        initThreadSupport();

        // Cancel any existing parent search thread (and its child threads) before we start a new one
        cancelSearchAndWait();

        // Start the search threads, one for each one of our moves:
        launchMoveThreads(board);

        // Now that we have started threads exploring all moves available for the current
        // player, see if the caller wants us to wait for them to finish or put the current
        // thread into its own background thread and return immediately.

        if (returnImmediate) {
            currentSearch = executorForMainSearch.submit(() -> finishCurrentSearch(board, pieceMap));
            return best.move;
        } else {
            // Otherwise wait here for the search to complete and return the move it suggests:
            currentSearch = null;
            return finishCurrentSearch(board, pieceMap);
        }
    }


    /**
     * Iterate over all available moves for the current player and decide which move is the best.
     * This executes on the current thread and is a blocking call.
     *
     * @param board the board state to examine each move on
     * @param pieceMap board pieces mapped by type and side
     * @return the best move for this board or null if no legal move is available
     */
    private Move searchWithNoThreads(final LiteBoard board, PieceMap pieceMap) {
        // We are not using threads.  Walk through all moves and find the best and return it in this calling thread.

        for (int index = 0; index < board.numMoves1; index++) {
            Move move = board.moves1[index];
            LiteBoard currentBoard = new LiteBoard(board);
            currentBoard.executeMove(move);
            currentBoard.advanceTurn();

            // See if we have a best move already stored away for this board arrangement:
            if (currentBoard.numPieces1 > 5) {     // we force moves to be manually evaluated via minmax when we
                // get down to the end game
                BestMove check = cachedMoves.lookupBestMove(currentBoard.board, maximize);
                if (check != null && check.move != null) {
                    double confidence = cachedMoves.getMoveRisk(currentBoard.board);
                    if (confidence <= acceptableRiskLevel) {
                        if ((!maximize && check.value < best.value) || (maximize && check.value > best.value)) {
                            best = check;
                            continue;
                        }
                    }
                }
            }

            int lookAheadVal = minmax(currentBoard, LiteUtil.MIN_VALUE, LiteUtil.MAX_VALUE,
                    startDepth, !maximize);

            if ((maximize && lookAheadVal > best.value) || (!maximize && lookAheadVal < best.value)) {
                best.value = lookAheadVal;
                best.move = move;
                best.move.setValue(best.value);
                cachedMoves.addMoveValue(board.board, maximize, best.move, best.value, best.movesExamined);
            }

            if (best.move != null) {
                cachedMoves.addMoveValue(board.board, maximize, best.move, best.value, best.movesExamined);
            }

            // Check for specific corner cases when we might want to make
            // specific kinds of moves for
            Move check = checkEndGameCornerCases(board, pieceMap, best.move);
            if (check != null) {
                best.move = check;
                best.move.setValue(best.value);
            }

            if (best.move != null) {
                cachedMoves.addMoveValue(board.board, maximize, best.move, best.value, best.movesExamined);
            }
        }
        return best.move;
    }

    /**
     * Launch a separate thread for each move available to the current player to move.
     * All threads are stored in the threadStack[] array with numThreads threads in it.
     *
     * @param board the board to find the best move on.
     */
    private void launchMoveThreads(final LiteBoard board) {
        // Loop through all of the moves available to the current player and launch a
        // thread for each one so each can go explore what good board valuations we
        // have in the future of this move and keep track of the best one
        //
        threadStack = new FutureTask[board.numMoves1];
        numThreads = 0;

        for (int i = 0; i < board.numMoves1; i++) {
            Move move = board.moves1[i];

            // See if we have a best move already stored away for this board arrangement:
            if (board.numPieces1 > 5) {     // we force moves to be manually evaluated via minmax when we get down to the end game
                BestMove check = cachedMoves.lookupBestMove(board.board, maximize);
                if (check != null && check.move != null) {
                    double confidence = cachedMoves.getMoveRisk(board.board);
                    if (confidence <= acceptableRiskLevel) {
                        if ((!maximize && check.value < best.value) || (maximize && check.value > best.value)) {
                            best = check;
                            continue;
                        }
                    }
                }
            }

            // Create a Futures object to represent the eventual result of the move we give it to explore
            LiteMoveThread lookAheadThread = new LiteMoveThread(board, this, maximize, move, startDepth);

            // Create a background thread for this move search and start it
            FutureTask<BestMove> task = new FutureTask<>(lookAheadThread);
            threadStack[numThreads++] = task;
            maxThreads = Integer.max(maxThreads, numThreads);
            Thread t = new Thread(task);
            t.setName(String.format("Turn-%03d:Move-%02d", board.turns, numThreads));
            t.start();
        }
    }

    /**
     * Call the get() method on the executor of the threads of an existing background search and gather their
     * results as they complete, keeping track of which move was the best one returned.
     *
     * @param board the board state the search is for
     * @return the best move found for this board state or null if no legal move was found
     */
    private Move finishCurrentSearch(final LiteBoard board, PieceMap pieceMap) {
        // Now we wait on all of the threads to finish so we can see which has the best score

        for (int index=0; index < numThreads; index++) {
            BestMove threadResult;

            try {
                threadResult = (BestMove) threadStack[index].get();
            } catch (Exception e) {
                Main.log(Main.LogLevel.DEBUG, "We're having problems waiting for move threads to finish");
                StackTraceElement[] stack = e.getStackTrace();
                for (StackTraceElement elem : stack) {
                    Main.log(Main.LogLevel.DEBUG, elem.toString());
                }
                continue;
            }

            if (threadResult == null) {
                // Not sure what to do here.  The thread completed but did not return
                // any useful move suggestions.
                //
                // I guess we just loop back and continue gathering other threads as
                // they complete until we're done with all of them
                continue;
            }

            // Continue on to the next thread if this one found no moves at all
            if (threadResult.move == null) {
                continue;
            }

            // save the number of moves it examined (even if it's not the best
            // move we keep, it still examined those moves and their results):
            addNumMovesExamined(threadResult.movesExamined);

            // See if the results of this thread's search are a better move than
            // we have so far and keep it if so:
            //
            if ((maximize && threadResult.value >= best.value) || (!maximize && threadResult.value <= best.value)) {
                best = threadResult;
                cachedMoves.addMoveValue(board.board, maximize, best.move, best.value, best.movesExamined);
            }

            // play nice with the other processes on this cpu
            Thread.yield();

            // Check for specific corner cases when we might want to make
            // specific kinds of moves for
            Move check = checkEndGameCornerCases(board, pieceMap, best.move);
            if (check != null) {
                best.move = check;
                best.value = check.getValue();
                cachedMoves.addMoveValue(board.board, maximize, best.move, best.value, best.movesExamined);
            }
        }

        // We are finished making all available moves for the current player for
        // this board setup and gathered the result of each.  If we have a best move
        // to suggest for this board it should have been set in the loop above as it
        // examined each search thread's results

        // Return the best move found for this board setup:
        return best.move;
    }


    /**
     * Cancel any background search threads that may be still
     * running from any last search we may have started
     */
    public void cancelThreadStack(int wait) {
        if (threadStack == null) return;
        searchTimeLimit = System.nanoTime();
        try {
            Thread.sleep(wait);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void cancelSearchAndWait() {
        if (currentSearch != null) {
            searchTimeLimit = System.nanoTime();
            boolean completed = moveSearchIsDone();
            while (!completed) {
                try {
                    Thread.sleep(1);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                completed = moveSearchIsDone();
            }
            currentSearch = null;
            threadStack = null;
            numThreads = 0;
        }
    }

    /**
     * See if this board state contains any specific 'corner-cases'
     * that we can direct special attention to
     *
     * @param board    the board state to examine
     * @param bestMove the best move so far for this board
     * @return a new best move if necessary, or the one already passed to us
     * if we found no reason to change it.
     */
    private Move checkEndGameCornerCases(final LiteBoard board, PieceMap pieceMap, Move bestMove) {

        // Follow the best move if we are in check..
        if (board.kingInCheck(board.turn)) {
            return bestMove;
        }

        //
        // Map our moves by type
        //
        Map<Integer, List<Move>> ourMoveMap = mapMovesByType(board, board.moves1, board.numMoves1);

        BiPredicate<Integer, List<Move>> pawnIsBlocked =
                (location, moves) -> {
                    for (Move move : moves) {
                        if ((move.getFrom() == location))
                            return false;
                    }
                    if (board.turn == Side.Black) {
                        if (location / 8 < 7)
                            return true;
                    } else {
                        if (location / 8 > 0)
                            return true;
                    }
                    return false;
                };

        SidePieceMap ourPieces = pieceMap.get(board.turn);
        SidePieceMap theirPieces = pieceMap.get((board.turn + 1) % 2);

        Stream<SidePieceMap> ourPieceStream = Stream.of(ourPieces);

        int totalPieces = ourPieceStream.flatMap(s -> s.values().stream())
                .reduce(0, (sum, list) -> sum + list.size(), Integer::sum);

        boolean tryAPawn = false;
        if (totalPieces - ourPieces.get(LiteBoard.Pawn).size() < 4) {
            if (!board.kingInCheck(board.turn)) {
                tryAPawn = true;
            }
        }

        boolean endRepetition = false;
        if (bestMove != null && board.checkDrawByRepetition(bestMove, 2)) {
            endRepetition = true;
        }

        // If we are getting repetitive and possibly stuck at a local maxima then
        // see if we have any possible pawn advances available and pick one
        // of those instead
        if (tryAPawn || endRepetition) {

            if (endRepetition) {
                printMsg("Attempting to end repetition..");
            }
            if (tryAPawn) {
                printMsg("looking at pawn strategy..");
            }

            // see if we have any pawn moves
            if (ourMoveMap.get(LiteBoard.Pawn).size() > 0) {
                return ourMoveMap.get(LiteBoard.Pawn).get(0);
            }

            // see if we have any pawns at all
            PieceList ourPawns = ourPieces.get(LiteBoard.Pawn);
            if (!ourPawns.isEmpty()) {
                // See which of them are blocked
                for (int ndx = 0; ndx < ourPawns.size(); ndx++) {
                    int location = ourPawns.get(ndx);
                    if (pawnIsBlocked.test(location, ourMoveMap.get(LiteBoard.Pawn))) {
                        // This pawn has no moves but it isn't on the last row, so it is
                        // blocked by another piece.  See if that piece is ours
                        int forward = (board.turn == Side.Black) ? 1 : -1;
                        int spotInFront = ((location % 8) + ((location / 8) + forward) * 8);
                        if (board.getSide(spotInFront) == board.turn) {
                            // The piece in the way is ours.  See if it can move
                            int typeOfPieceInTheWay = board.getType(spotInFront);
                            if (typeOfPieceInTheWay == LiteBoard.Empty) {
                                // FixMe!
                                break;
                            }
                            for (Move move : ourMoveMap.get(typeOfPieceInTheWay)) {
                                if (move.getFrom() == spotInFront) {
                                    // the piece in the way of our pawn has moves. Choose one
                                    // to be our next move
                                    return move;
                                }
                            }
                        }
                    }
                }
            }

            Move endRepMove = null;

            if (ourMoveMap.get(LiteBoard.Queen).size() > 0) {
                endRepMove = ourMoveMap.get(LiteBoard.Queen).get(0);
            } else if (ourMoveMap.get(LiteBoard.Rook).size() > 0) {
                endRepMove = ourMoveMap.get(LiteBoard.Rook).get(0);
            } else if (ourMoveMap.get(LiteBoard.Bishop).size() > 0) {
                endRepMove = ourMoveMap.get(LiteBoard.Bishop).get(0);
            } else if (ourMoveMap.get(LiteBoard.Knight).size() > 0) {
                endRepMove = ourMoveMap.get(LiteBoard.Knight).get(0);
            } else if (ourMoveMap.get(LiteBoard.King).size() > 0) {
                endRepMove = ourMoveMap.get(LiteBoard.King).get(0);
            }
            if (endRepMove != null) {
                return endRepMove;
            }
        }

        if (bestMove == null) {
            return null;
        }

        int bestNdx = bestMove.getFrom();

        // Additional corner-case:
        // If the best move we think we have is the king and we are not in check:
        //     Check to see if we have any pawn moves left and use one of those
        //     to see if it can get promoted before we move this king

        // Only do this if we are not in check:
        if (!board.kingInCheck(board.turn)) {
            if (board.getType(bestNdx) == LiteBoard.King) {
                // See if all we have left are pawn and king moves
                if (ourMoveMap.get(LiteBoard.Rook).size() == 0 &&
                        ourMoveMap.get(LiteBoard.Queen).size() == 0) {
                    if (ourMoveMap.get(LiteBoard.Pawn).size() > 0) {
                        return ourMoveMap.get(LiteBoard.Pawn).get(0);
                    }

                    // see if we have any pawns at all
                    PieceList ourPawns = ourPieces.get(LiteBoard.Pawn);
                    if (!ourPawns.isEmpty()) {
                        // See which of them are blocked
                        for (int ndx = 0; ndx < ourPawns.size(); ndx++) {
                            int location = ourPawns.get(ndx);
                            if (pawnIsBlocked.test(location, ourMoveMap.get(LiteBoard.Pawn))) {
                                // This pawn has no moves but it isn't on the last row, so it is
                                // blocked by another piece.  See if that piece is ours
                                int forward = (board.turn == Side.Black) ? 1 : -1;
                                int spotInFront = ((location % 8) + ((location / 8) + forward) * 8);
                                if (board.getSide(spotInFront) == board.turn) {
                                    // The piece in the way is ours.  See if it can move
                                    int typeOfPieceInTheWay = board.getType(spotInFront);
                                    if (typeOfPieceInTheWay == LiteBoard.Empty) {
                                        // FixMe!
                                        break;
                                    }
                                    for (Move move : ourMoveMap.get(typeOfPieceInTheWay)) {
                                        if (move.getFrom() == spotInFront) {
                                            // the piece in the way of our pawn has moves. Choose one
                                            // to be our next move
                                            return move;
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // See if our opponent only has their king left and get more aggressive if so:

        if (!board.kingInCheck(board.turn)) {
            Map<Integer, List<Move>> theirMoveMap = mapMovesByType(board, board.moves2, board.numMoves2);

            // If all they have left is their king..
            if (theirMoveMap.get(LiteBoard.Pawn).size() == 0 &&
                    theirMoveMap.get(LiteBoard.Knight).size() == 0 &&
                    theirMoveMap.get(LiteBoard.Bishop).size() == 0 &&
                    theirMoveMap.get(LiteBoard.Rook).size() == 0 &&
                    theirMoveMap.get(LiteBoard.Queen).size() == 0 &&
                    theirMoveMap.get(LiteBoard.King).size() > 0) {

                // if we have pawns march them towards the end row
                if (ourMoveMap.get(LiteBoard.Pawn).size() > 0) {
                    bestMove = ourMoveMap.get(LiteBoard.Pawn).get(0);
                } else {

                    // If we have at least one queen and one rook then look further to find a game ending move
                    if (ourMoveMap.get(LiteBoard.Queen).size() > 0 &&
                            ourMoveMap.get(LiteBoard.Rook).size() > 0 &&
                            ourMoveMap.get(LiteBoard.King).size() > 0 &&
                            startDepth < 3) {

                        // Extend the depth and search again for a game ending move
                        startDepth = 3;
                        printMsg("Looking for game-ending moves...");

                    } else if ((ourMoveMap.get(LiteBoard.Queen).size() > 0)
                            || (ourMoveMap.get(LiteBoard.Rook).size() > 0) &&
                            ourMoveMap.get(LiteBoard.King).size() > 0 &&
                            startDepth < 4) {

                        // We have (at minimum) a queen or a rook along with our king so we should
                        // be able to back them into a corner if we search even further ahead..
                        startDepth = 4;
                        printMsg("Looking even further for a game ending move..");
                    }
                }
            }
        }
        return bestMove;
    }


    /**
     * Private helper function to create a map of available
     * moves, indexed by their pieces types.
     *
     * @param board    the board state the moves are for
     * @param moves    an array of moves to add to the map
     * @param numMoves the number of moves to add
     * @return the new Map<Integer, List<Move>> containing
     * the moves mapped to the types of pieces that
     * can make them.
     */
    private static Map<Integer, List<Move>> mapMovesByType(LiteBoard board, Move[] moves, int numMoves) {
        Map<Integer, List<Move>> movesByPiece = new HashMap<>(6);
        movesByPiece.put(LiteBoard.Pawn, new ArrayList<>());
        movesByPiece.put(LiteBoard.Rook, new ArrayList<>());
        movesByPiece.put(LiteBoard.Knight, new ArrayList<>());
        movesByPiece.put(LiteBoard.Bishop, new ArrayList<>());
        movesByPiece.put(LiteBoard.Queen, new ArrayList<>());
        movesByPiece.put(LiteBoard.King, new ArrayList<>());

        for (int ndx = 0; ndx < numMoves; ndx++) {
            int fi = moves[ndx].getFrom();
            byte b = board.board[fi];
            int pieceType = LiteUtil.getType(b);

            if (pieceType == LiteBoard.Empty) {
                List<String> logLines = new ArrayList<>(Arrays.asList(
                        "",
                        "Yet another attempt to map a move that seems to point to an empty spot:",
                        String.format("fi = %d:", fi),
                        String.format("b = %d:", b),
                        String.format("pieceType = %d:", pieceType),
                        String.format("board.board[fi] = %d:", board.board[fi]),
                        ""));
                StackTraceElement[] stack = Thread.currentThread().getStackTrace();
                for (StackTraceElement s : stack) {
                    logLines.add(s.toString());
                }
                logLines.add("");
                Main.log(Main.LogLevel.ERROR, logLines);

                continue;
            }

            movesByPiece.get(pieceType).add(moves[ndx]);
        }
        return movesByPiece;
    }


    /**
     * The awesome, one and only, minimax algorithm method which recursively searches
     * for the best moves up to a certain number of moves ahead (plies) or until a
     * move timeout occurs (if any timeouts are in effect).
     *
     * @param origBoard the board state to examine all moves for
     * @param alpha     the lower bounds of the best move and score found so far
     * @param beta      the upper bounds of the best move and score found so far
     * @param depth     the number of turns to search ahead.  ply is a "half-turn'
     *                  where one player has moved but the responding move has not
     *                  been made.  A full turn for fair evaluation usually requires
     *                  a balanced number of exchanges.
     * @param maximize  true if we are looking for a board state with the maximum score (white player's turn)
     *                  false if we are looking for a board state with the lowest score (black player's turn)
     * @return the best score this move (and all consequential response/exchanges up to the allowed
     *         look-ahead depth or time limit for searching).
     */
    int minmax(final LiteBoard origBoard, int alpha, int beta, int depth, boolean maximize) {
        BestMove mmBest = new BestMove(maximize);
        int lookAheadValue = 0;
        int cachedValue = 0;
        boolean gotCacheHit;
        BestMove check;

        if (throttle > 0) {
            try {
                Thread.sleep(0, throttle);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        for (int i = 0; i < origBoard.numMoves1; i++) {
            // Get the next move available for this board and this player
            Move move = origBoard.moves1[i];

            ///////////////////////////////////////////////////////////////////
            // See if we are at the end of our allowed depth to search and if so,
            // continue search if we still have made capture moves that we want to
            // see the responses for.  That way we don't take a pawn with our queen
            // at the end of a ply, and not know that in response we lose our queen!
            //
            // This is known as quiescent searching.

            if (depth <= 0) {
                if ((move.getValue() == 0) || depth < -2) {
                    addNumMovesExamined(mmBest.movesExamined);
                    return LiteEval.evaluate(origBoard);
                }
            }

            ///////////////////////////////////////////////////////////////////
            // Before we try to find our own best move for this board state, see
            // if one is already cached:

            gotCacheHit = false;
            check = null;

            if (origBoard.numPieces1 > 5) {     // we force moves to be manually evaluated via minmax when we get down to the end game
                check = cachedMoves.lookupBestMove(origBoard.board, maximize);
            }

            if (check != null) {
                if (check.move != null) {
                    gotCacheHit = true;
                    cachedValue = check.value;
                    lookAheadValue = check.value;
                } else {
                    check = null;
                }
            }

            if (check != null) {
                double moveRisk = cachedMoves.getMoveRisk(origBoard.board);
                if (moveRisk > acceptableRiskLevel) {
                    // The risk is too high so we will do this manually and increase the count
                    // of how many times we have rechecked this move for this board
                    cachedMoves.increaseMoveUsedCount(origBoard.board);
                    check = null;
                }
            }

            if (check == null) {
                LiteBoard currentBoard = new LiteBoard(origBoard);
                currentBoard.executeMove(move);
                currentBoard.advanceTurn();
                mmBest.movesExamined++;

                // See if the move we just made leaves the other player with no moves
                // and if so, return it as the best value we'll ever see on this search:
                if (currentBoard.numMoves1 == 0) {
                    mmBest.move = move;
                    mmBest.value = maximize ?
                            LiteUtil.MAX_VALUE - (100 - depth) :
                            LiteUtil.MIN_VALUE + (100 - depth);
                    break;
                }

                Thread.yield();

                // The recursive minimax step
                // While we have the depth keep looking ahead to see what this move accomplishes
                lookAheadValue = minmax(currentBoard, alpha, beta, depth - 1, !maximize);

                // See if this move is better than any we've seen for this board:
                //
                if ((!maximize && lookAheadValue < mmBest.value) || (maximize && lookAheadValue > mmBest.value)) {
                    mmBest.value = lookAheadValue;
                    mmBest.move = move;

                    cachedMoves.addMoveValue(origBoard.board, maximize, move, lookAheadValue, mmBest.movesExamined);
                }

                // See if we had a cache hit but ran it anyway, and whether this improved the existing move
                if (((maximize && lookAheadValue > cachedValue) || (!maximize && lookAheadValue < cachedValue)) &&
                        gotCacheHit) {
                    cachedMoves.increaseMoveImprovedCount(origBoard.board);
                }
            } else {
                mmBest.movesExamined += check.movesExamined;
            }

            // The alpha-beta pruning step
            // Continually tighten the low and high range of what we see this accomplishes
            // in the future and stop now if all future results are worse than the best move
            // we already have.
            if (maximize)
                alpha = Integer.max(alpha, lookAheadValue);
            else
                beta = Integer.min(beta, lookAheadValue);

            if (alpha >= beta) {
                break;
            }

            // If we are out of time then return the best outcome we've seen this move accomplish
            if (searchTimeLimit > 0 && System.nanoTime() >= searchTimeLimit) {
                break;
            }

            if (Thread.currentThread().isInterrupted()) {
                break;
            }
        }

        addNumMovesExamined(mmBest.movesExamined);
        return mmBest.value;
    }


    /**
     * Called when our object is being torn down.
     * Originally written to ensure all threads were collected
     * and finalised if the program was exiting.  It would seem
     * now that the program does fine on cleaning them all up
     * without this method needing to be specifically called.
     * <p>
     * May be deprecated in the future.
     *
     * @throws IOException
     */
    public void close() throws IOException {
        // Disable new tasks from being submitted
        pool.shutdown();
        executorForMainSearch.shutdown();

        try {
            // Wait a while for existing tasks to terminate
            if (!pool.awaitTermination(5, TimeUnit.SECONDS)) {
                // Cancel currently executing tasks
                pool.shutdownNow();

                // Wait a while for tasks to respond to being cancelled
                if (!pool.awaitTermination(5, TimeUnit.SECONDS))
                    System.err.println("some look-ahead threads did not terminate");
            }

            // same for the other executor:
            // Wait a while for existing tasks to terminate
            if (!executorForMainSearch.awaitTermination(5, TimeUnit.SECONDS)) {
                // Cancel currently executing tasks
                executorForMainSearch.shutdownNow();

                // Wait a while for tasks to respond to being cancelled
                if (!executorForMainSearch.awaitTermination(5, TimeUnit.SECONDS))
                    System.err.println("some look-ahead threads did not terminate");
            }
        } catch (InterruptedException ie) {
            // (Re-)Cancel if current thread also interrupted
            pool.shutdownNow();
            executorForMainSearch.shutdown();

            // Preserve interrupt status
//          Thread.currentThread().interrupt();
        }
    }
}