import org.junit.Test;
import static org.junit.Assert.*;

public class DrawByRepetitionTest {
    @Test
    public void testCheckDrawByRepetition() {
        LiteBoard board = new LiteBoard();
        // create a simple move
        Move move = new Move(0,0,0,1,0);
        board.lastMove = move;
        // simulate repeating the same move three times
        for (int i = 0; i < 6; i++) {
            board.history[i] = move;
        }
        board.numHist = 6;
        boolean result = board.checkDrawByRepetition(board.lastMove, 3);
        assertTrue("Draw by repetition should be detected", result);
    }
}
