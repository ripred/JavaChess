package chess1;

public interface AIMoveSelector {
    Move bestMove(final Board board);
    int getNumMovesExamined();
}

