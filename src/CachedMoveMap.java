import java.util.Arrays;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * The CachedMoveMap holds a map of board states to Moves.
 * It is used to see if we have already examined a given Move and it's results
 * and determined it is a good move for a given board state.  This allows us to
 * take Moves that have been through the time-expensive minmax examinations for that
 * move and store it away so it can be reused when this board is encountered again
 * by other threads.
 *
 * The map is implemented using a ConcurrentHashMap so no global locking is required
 * when it is accessed by different threads.
 */
public class CachedMoveMap extends ConcurrentHashMap<String, ConcurrentHashMap<Boolean, BestMove>> {

    // A static table of tokens for building 'board state' keys
    private static final char[] tokens = {' ', 'p', 'n', 'b', 'r', 'q', 'k', ' ', 'P', 'N', 'B', 'R', 'Q', 'K'};

    private ConcurrentMap<String, MoveStat> moveRisk;

    // Some useful metrics we keep track of like cache hits and cache misses.
    // Also a lock object to synchronize access to the values
    private final Object statisticsLock = new Object();
    long numMovesExamined;
    long numMovesOffered;
    long numMovesCached;
    long maxMovesCached;
    long numMovesUpdated;
    long numExaminationsSaved;
    long numCacheHits;
    long numCacheMisses;
    long numMovesTested;
    long numMovesImproved;

    public CachedMoveMap() {
        synchronized (statisticsLock) {
            numMovesExamined = 0;
            numMovesOffered = 0;
            numMovesCached = 0;
            maxMovesCached = 0;
            numMovesUpdated = 0;
            numExaminationsSaved = 0;
            numCacheHits = 0;
            numCacheMisses = 0;
            numMovesTested =  0;
            numMovesImproved = 0;
            moveRisk = new ConcurrentHashMap<>();
        }
    }

    private void addNumMovesExamined(final int num) {
        synchronized (statisticsLock) {
            numMovesExamined += num;
        }
    }

    private long getNumMovesExamined() {
        synchronized (statisticsLock) {
            return numMovesExamined;
        }
    }

    private void addNumMovesOffered(final int num) {
        synchronized (statisticsLock) {
            numMovesOffered += num;
        }
    }

    private long getNumMovesOffered() {
        synchronized (statisticsLock) {
            return numMovesOffered;
        }
    }

    private void addNumMovesCached(final int num) {
        synchronized (statisticsLock) {
            numMovesCached += num;
        }
    }

    private long getNumMovesCached() {
        synchronized (statisticsLock) {
            return numMovesCached;
        }
    }


    // static method to return the map key to be used for a given board state
    public static String getBoardKey(byte[] boardBytes) {
        assert Main.useCache : "cache is not in use.  this should not be called.";

        char[] hash = {
                ' ', ' ', ' ', ' ', ' ', ' ', ' ', ' ',
                ' ', ' ', ' ', ' ', ' ', ' ', ' ', ' ',
                ' ', ' ', ' ', ' ', ' ', ' ', ' ', ' ',
                ' ', ' ', ' ', ' ', ' ', ' ', ' ', ' ',
                ' ', ' ', ' ', ' ', ' ', ' ', ' ', ' ',
                ' ', ' ', ' ', ' ', ' ', ' ', ' ', ' ',
                ' ', ' ', ' ', ' ', ' ', ' ', ' ', ' ',
                ' ', ' ', ' ', ' ', ' ', ' ', ' ', ' '
        };

        // create a hash token for this board with the pieces where they are
        for (int ndx = 0; ndx < LiteBoard.BOARD_SIZE; ndx++) {
            byte piece = boardBytes[ndx];
            int type = LiteUtil.getType(piece);
            if (type == LiteBoard.Empty) continue;
            int side = LiteUtil.getSide(piece);
            hash[ndx] = tokens[type + 7 * side];
        }
        return String.valueOf(hash, 0, LiteBoard.BOARD_SIZE);
    }

    /**
     * Offer a 'best move' for this board state for this side (maximizing or !maximizing) with a value.
     * If this board exists in the map and we have a move for this side we will replace the existing
     * one with this move/value if they are a better value than what is already there and this will
     * become the new suggestion for this board state.
     *
     * @param b             an array of 64 bytes representing a board piece arrangement (state)
     * @param maximize      true if we are the maximizing side (white) or false if we are not (black side)
     * @param move          the move to offer as the best move for this board state for this min or max goal
     * @param value         the value of this move
     * @param movesExamined the number of moves examined to find this move
     */
    public void addMoveValue(byte[] b, boolean maximize, Move move, Integer value, int movesExamined) {
        if (!Main.useCache) {
            return;
        }

        try {
            String key = getBoardKey(b);

            // These moves were examined so add them to our total count
            // even if we ultimately don't keep this move as the 'best'.
            // The moves were still examined and thus should be counted.
            synchronized (statisticsLock) {
                numMovesExamined += movesExamined;
                numMovesOffered += 1;
            }

            // add the board, max/min, and move if all are new:
            if (!containsKey(key)) {
                put(key, new ConcurrentHashMap<>());
                BestMove best = new BestMove(maximize);
                best.value = value;
                best.move = move;
                best.move.setValue(best.value);
                best.movesExamined = movesExamined;
                get(key).put(maximize, best);
                synchronized (statisticsLock) {
                    numMovesCached++;
                    if (numMovesCached > maxMovesCached) {
                        maxMovesCached = numMovesCached;
                    }
                }
                return;
            }

            // Get the board map and its descendants
            ConcurrentMap<Boolean, BestMove> bestMap = get(key);

            // see if this maximize/move combo exists and add both if not
            if (!bestMap.containsKey(maximize)) {
                BestMove best = new BestMove(maximize);
                best.value = value;
                best.move = move;
                best.move.setValue(best.value);
                best.movesExamined = movesExamined;
                get(key).put(maximize, best);
                synchronized (statisticsLock) {
                    this.numMovesCached++;
                    if (numMovesCached > maxMovesCached) {
                        maxMovesCached = numMovesCached;
                    }
                }
                return;
            }

            // get the best move for this side (maximize or !maximize) for this board setup
            //
            BestMove best = bestMap.get(maximize);

            // see if the value param passed to us is better and replace this move if so
            //
            if ((!maximize && value < best.value) || (maximize && value > best.value)) {
                int alreadyExamined = best.movesExamined;
                best.value = value;
                best.move = move;
                best.move.setValue(best.value);
                best.movesExamined = movesExamined + alreadyExamined;
                synchronized (statisticsLock) {
                    numMovesUpdated++;
                }
            }
        } catch (NullPointerException e) {
            e.printStackTrace();
        }
    }

    /**
     * See if we have a move cached away for this board setup and maximize/minimize goal.
     * Get the move and return it if we do.
     *
     * @param b        an array of 64 bytes representing a board piece arrangement (state)
     * @param maximize true if we are the maximizing side (white) or false if we are not (black side)
     * @return the best move seen so far for this board state if we have one other side returns null.
     */
    public BestMove lookupBestMove(byte[] b, boolean maximize) {

        if (!Main.useCache) {
            return null;
        }

        String key = getBoardKey(b);

        // See if we have this board state
        if (containsKey(key) && get(key).contains(maximize)) {

            // We do.  Get the maximize/minimize Move associated with it (if any)
            ConcurrentMap<Boolean, BestMove> bestMap = get(key);
            if (bestMap.containsKey(maximize)) {

                // We have a move stored away.  Do a few sanity checks
                // on the move before we suggest it
                BestMove bm = bestMap.get(maximize);

                // bad BestMove object
                assert bm != null : "Ooops";

                // bad move stored inside
                assert bm.move != null : "Ooops";

                // get the 'from' spot of this move
                int fi = bm.move.getFrom();

                // make sure it points within a board
                assert ((fi >= 0) && (fi <= 63)) : "Oooops";

                // Make sure there is a still a piece on the board in
                // the spot this move points to.
                assert !LiteUtil.isEmpty(b[fi]);

                addNumMovesExamined(bm.movesExamined);

                synchronized (statisticsLock) {
                    numExaminationsSaved += bm.movesExamined;
                    numCacheHits++;
                }

                return bm;
            }
        }

        synchronized (statisticsLock) {
            numCacheMisses++;
        }

        return null;
    }

    public void deletePieceTaken(byte[] b, int index) {

        if (!Main.useCache) {
            return;
        }

        if (true) return;

        String key = getBoardKey(b);
        char takenChar = key.charAt(index);
        key = key.substring(0, index) + ' ' + key.substring(index + 1);

        // sort the key by letters
        char[] tempArray = key.toCharArray();
        Arrays.sort(tempArray);
        key = new String(tempArray);

        int lastSpace = key.lastIndexOf(' ');
        key = key.substring(lastSpace + 1);

        int firstChar = key.indexOf(takenChar);
        int lastChar = key.lastIndexOf(takenChar);
        int byteLen = lastChar - firstChar;

        Set<String> mapKeys = keySet();
        for (String item : mapKeys) {
            tempArray = item.toCharArray();
            Arrays.sort(tempArray);
            String newItem = new String(tempArray);
            lastSpace = newItem.lastIndexOf(' ');
            newItem = newItem.substring(lastSpace + 1);

            int firstItemChar = newItem.indexOf(takenChar);
            int lastItemChar = newItem.lastIndexOf(takenChar);
            int byteItemLen = lastItemChar - firstItemChar;

            if (byteItemLen > byteLen) {
                remove(item);
                moveRisk.remove(item);
            }
        }
    }

    public double getMoveRisk(byte[] b) {
        String key = getBoardKey(b);

        if (!moveRisk.containsKey(key)) {
            return 1.0f;
        } else {
            MoveStat moveStat = moveRisk.get(key);
            if (moveStat.numRetries == 0) {
                return 1.0f;
            } else {
                return ((double) moveStat.numBetter / (double) moveStat.numRetries);
            }
        }
    }

    public void increaseMoveUsedCount(byte[] b) {
        String key = getBoardKey(b);
        moveRisk.computeIfAbsent(key, (s) -> new MoveStat()).numRetries++;
        synchronized (statisticsLock) {
            numMovesTested++;
        }
    }

    public void increaseMoveImprovedCount(byte[] b) {
        String key = getBoardKey(b);
        moveRisk.computeIfAbsent(key, (s) -> new MoveStat()).numBetter++;
        synchronized (statisticsLock) {
            numMovesImproved++;
        }
    }

    private class MoveStat {
        int numRetries;
        int numBetter;

        MoveStat() {
            numRetries = 0;
            numBetter = 0;
        }
    }
}
