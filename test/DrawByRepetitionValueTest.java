import org.junit.Test;
import static org.junit.Assert.*;

public class DrawByRepetitionValueTest {
    @Test
    public void testDifferentMoveValuesStillDraw() {
        LiteBoard board = new LiteBoard();
        for (int i = 0; i < 6; i++) {
            board.history[i] = new Move(0,0,0,1,i);
        }
        board.numHist = 6;
        Move last = new Move(0,0,0,1,999);
        board.lastMove = last;
        assertTrue("Draw should be detected despite differing move values",
                   board.checkDrawByRepetition(last, 3));
    }
}
