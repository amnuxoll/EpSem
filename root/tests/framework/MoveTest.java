package framework;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 *
 * @author Zachary Paul Faltersack
 * @version 0.95
 */
public class MoveTest {
    //region Constructor Tests
    @Test
    public void testConstructorNullNameThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> new Move(null));
    }

    @Test
    public void testConstructorEmptyNameThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> new Move(""));
    }
    //endregion

    //region getName Tests
    @Test
    public void testGetName() {
        Move move = new Move("move");
        assertEquals("move", move.getName());
    }
    //endregion

    //region equals Tests
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
    //endregion

    //region hashCode Tests
    @Test
    public void testHashCodeAreEqual() {
        Move move1 = new Move("move");
        Move move2 = new Move("move");
        assertEquals(move1.hashCode(), move2.hashCode());
    }
    //endregion

    //region toString Tests
    @Test
    public void toStringIsName()
    {
        Move move = new Move("move");
        assertEquals("move", move.toString());
    }
    //endregion
}
