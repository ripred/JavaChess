package chess;

/**
 * The BoardEvaluator interface has one method: evaluate()
 * which returns a score for the state of the Board.
 *
 * An evaluation = 0 means the sides are evenly matched.
 * An evaluation > 0 means that white has an advantage.
 * An evaluation < 0 means that black has an advantage.
 */
public interface BoardEvaluator {
    int evaluate(final Board board);
}