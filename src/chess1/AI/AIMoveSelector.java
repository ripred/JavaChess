package chess1.AI;

import chess1.Board;
import chess1.Move;

import java.io.Closeable;
import java.io.IOException;
import java.util.function.Consumer;

public interface AIMoveSelector extends Closeable {
    Move bestMove(final Board board);

    int getNumMovesExamined();
    void addNumMovesExamined(int n);

    int getMaxThreads();

    void registerDisplayCallback(Consumer<String> cb);

    @Override
    void close() throws IOException;
}

