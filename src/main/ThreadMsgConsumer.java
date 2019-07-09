package main;

import java.util.function.Consumer;
import chess1.AI.AIMoveSelector;
import chess1.Board;

public class ThreadMsgConsumer implements Consumer<String> {
    private static Board board;
    private static AIMoveSelector agent;

    ThreadMsgConsumer(Board board, AIMoveSelector agent) {
        ThreadMsgConsumer.board = board;
        ThreadMsgConsumer.agent = agent;
    }

    @Override
    public void accept(String s) {
        Main.showBoard(board, s, agent, 0, 0);
    }
}

