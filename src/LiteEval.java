
import java.util.ArrayList;
import java.util.List;

// Class to implement high-performance map/reduce algorithm using parallel processing and Java Streams
// Used for high speed game score evaluation of millions of board states
// (c) 2019 Trent M. Wyatt

public class LiteEval {

    // Filters to decide what to include in our evaluation
    public static int MATERIAL = 0x01;  // favor moves that leave more pieces for our side aftewards
    public static int CENTER   = 0x02;  // favor moves that occupy the center of the board
    public static int MOBILE   = 0x04;  // favor moves that leave us the most moves afterwards


    // a material values plugin:
    private static pieceEvalExt materialEvaluator = p ->
            LiteUtil.getValue(p) * ((LiteUtil.getSide(p) == Side.Black) ? -1 : 1);


    // a center location bonus plugin:
    private static locationEvalExt centerEvaluator = (ndx, p) -> {
        int type = LiteUtil.getType(p);
        if (type == LiteBoard.King) return 0;

        int dx = ndx % 8;
        if (dx > 3) dx = 7 - dx;
        int dy = ndx / 8;
        if (dy > 3) dy = 7 - dy;

        return (dx + dy) * type * ((LiteUtil.getSide(p) == Side.Black) ? -1 : 1);
    };


    //
    // Available evaluation plugin interfaces:
    //

    // 'evaluate by pieces' extension interface
    @FunctionalInterface
    public interface pieceEvalExt {
        int eval(byte b);
    }

    // 'evaluate by locations' extension interface
    @FunctionalInterface
    public interface locationEvalExt {
        int eval(int ndx, byte b);
    }

    // Include all evaluations is not specified
    public static int evaluate(final LiteBoard board) {
        return evaluate(board, MATERIAL | CENTER | MOBILE);
    }

    public static int evaluate(final LiteBoard board, final int using) {
        int score = 0;

        int mobilityBonus = 3;
        int centerBonus = 5;

        if ((using & MATERIAL) != 0) {
            score += sum(transformAllPieces(board.board, materialEvaluator));
        }

        if ((using & CENTER) != 0) {
            score += sum(transformAllLocations(board.board, centerEvaluator)) * centerBonus;
        }

        if ((using & MOBILE) != 0) {
            int factor = (board.turn == Side.Black) ? -1 : 1;

            score += board.numMoves1 * mobilityBonus * factor;
            score -= board.numMoves2 * mobilityBonus * factor;
        }

        return score;
    }

    ////////////////////////////////////////////////////////////////
    // extension mapping methods

    // apply an 'all pieces' extension:
    static private List<Integer> transformAllPieces(byte[] pieces, pieceEvalExt ext) {
        List<Integer> transformed = new ArrayList<>();
        for (byte b : pieces) transformed.add(ext.eval(b));
        return transformed;
    }

    // apply an 'all locations' extension:
    static private List<Integer> transformAllLocations(byte[] pieces, locationEvalExt ext) {
        List<Integer> transformed = new ArrayList<>();
        for (int ndx=0; ndx < pieces.length; ndx++)
            transformed.add(ext.eval(ndx, pieces[ndx]));
        return transformed;
    }

    // an aggregator for evaluation results
    static private int sum(List<Integer> list) {
        return list.parallelStream().reduce(0, Integer::sum);
    }
}

