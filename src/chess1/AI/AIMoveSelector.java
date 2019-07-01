package chess1.AI;

import chess1.Board;
import chess1.Move;

public interface AIMoveSelector {
    Move bestMove(final Board board);
    int getNumMovesExamined();
    void addNumMovesExamined(int n);
}

