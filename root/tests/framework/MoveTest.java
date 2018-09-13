package framework;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class MoveTest {

    // Constructor Tests
    @Test
    public void testConstructorNullNameThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> new Move(null));
    }

    @Test
    public void testConstructorEmptyNameThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> new Move(""));
    }

    // equals Tests
    @Test
    public void testEqualsAreEqual() {
        Move move1 = new Move("move");
        Move move2 = new Move("move");
        assertEquals(move1, move2);
    }

    @Test
    public void testEqualsAreNotEqual() {
        Move move1 = new Move("move1");
        Move move2 = new Move("move2");
        assertNotEquals(move1, move2);
    }

    // hashCode Tests
    @Test
    public void testHashCodeAreEqual() {
        Move move1 = new Move("move");
        Move move2 = new Move("move");
        assertEquals(move1.hashCode(), move2.hashCode());
    }

    // getName Tests
    @Test
    public void testGetName() {
        Move move = new Move("move");
        assertEquals("move", move.getName());
    }
}
