package framework;

//import org.junit.jupiter.api.Test;
//import static org.junit.jupiter.api.Assertions.assertEquals;
//import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 *
 * @author Zachary Paul Faltersack
 * @version 0.95
 */
public class DatumTest {
    //region Constructor Tests
    @Test
    public void constructorNullStatisticThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> new Datum(null, 1));
        assertThrows(IllegalArgumentException.class, () -> new Datum(null, 1.5));
        assertThrows(IllegalArgumentException.class, () -> new Datum(null, "1"));
    }

    @Test
    public void constructorEmptyStatisticThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> new Datum("", 1));
        assertThrows(IllegalArgumentException.class, () -> new Datum("", 1.5));
        assertThrows(IllegalArgumentException.class, () -> new Datum("", "1"));
    }

    @Test
    public void constructDatumWithInteger() {
        Datum datum = new Datum("statistic", 1);
        assertEquals("statistic", datum.getStatistic());
        assertEquals("1", datum.getDatum());
    }

    @Test
    public void constructDatumWithDouble() {
        Datum datum = new Datum("statistic", 1.5);
        assertEquals("statistic", datum.getStatistic());
        assertEquals("1.5", datum.getDatum());
    }

    @Test
    public void constructDatumWithString() {
        Datum datum = new Datum("statistic", "Hello");
        assertEquals("statistic", datum.getStatistic());
        assertEquals("Hello", datum.getDatum());
    }

    @Test
    public void constructDatumWithStringNullSetsEmpty() {
        Datum datum = new Datum("statistic", null);
        assertEquals("statistic", datum.getStatistic());
        assertEquals("", datum.getDatum());
    }

    @Test
    public void constructDatumWithStringCommaThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> new Datum("", "Hello, World!"));
    }
    //endregion
}
