package chess;

public interface AIMoveSelector {
    Move bestMove(final Board board);
    int getNumMovesExamined();
}

