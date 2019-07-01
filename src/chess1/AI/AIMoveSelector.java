package chess1.AI;

import chess1.Board;
import chess1.Move;

import java.io.Closeable;
import java.io.IOException;
import java.util.function.Consumer;

public abstract class AIMoveSelector implements Closeable {
    private int maxThreads;

    public AIMoveSelector(int maxThreads) {
        this.maxThreads = maxThreads;
    }

    public abstract Move bestMove(final Board board);

    public abstract int getNumMovesExamined();
    public abstract void addNumMovesExamined(int n);

    public abstract int getMaxThreads();

    public abstract void registerDisplayCallback(Consumer<String> cb);

    @Override
    public abstract void close() throws IOException;
}

