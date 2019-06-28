package ChessTest;

import Chess.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MoveTest {

    private Move move;
    private Move move1;
    private Move move2;

    @BeforeEach
    void beforeEach() {
        move  = new Move(1, 2, 3, 4, true);
        move1 = new Move(1 + 2 * 8, 3 + 4 * 8, false);
        move2 = new Move(move);
    }

    @AfterEach
    void afterEach() {
        move  = null;
        move1 = null;
        move2 = null;
    }

    @Test
    void Move(/* int, int */) {
        assertEquals(move.getFromCol() + move.getFromRow() * 8, move1.getFrom());
    }

    @Test
    void MoveCopy(/* Move move2 */) {
        assertEquals(move.getFromCol(), move2.getFromCol());
        assertEquals(move.getFromRow(), move2.getFromRow());
        assertEquals(move.getToCol(),   move2.getToCol());
        assertEquals(move.getToRow(),   move2.getToRow());

        assertEquals(move1.getFrom(),   move2.getFromCol() + move2.getFromRow() * 8);
        assertEquals(move1.getTo(),     move2.getToCol()   + move2.getToRow() * 8);

        assertEquals(move.isCapture(), move2.isCapture());
    }

    @Test
    void getFrom() {
        assertEquals(move.getFrom(), 1 + 2 * 8);
    }

    @Test
    void getTo() {
        assertEquals(move.getTo(),3 + 4 * 8);
    }

    @Test
    void getFromCol() {
        assertEquals(move.getFromCol(), 1);
    }

    @Test
    void getFromRow() {
        assertEquals(move.getFromRow(), 2);
    }

    @Test
    void getToCol() {
        assertEquals(move.getToCol(), 3);
    }

    @Test
    void getToRow() {
        assertEquals(move.getToRow(), 4);
    }

    @Test
    void isCapture() {
        assertTrue(move.isCapture());
        assertFalse(move1.isCapture());
    }

    @Test
    void Equals() {
        assertTrue(move.equals(move2));
    }

    @Test
    void HashCode() {
        assertEquals(move.hashCode(), move2.hashCode());
    }
}