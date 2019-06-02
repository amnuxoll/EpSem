package tests.framework;

import framework.Datum;

import tests.EpSemTest;
import tests.EpSemTestClass;
import static tests.Assertions.*;

/**
 *
 * @author Zachary Paul Faltersack
 * @version 0.95
 */
@EpSemTestClass
public class DatumTest {
    //region Constructor Tests

    @EpSemTest
    public void constructorNullStatisticThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> new Datum(null, 1));
        assertThrows(IllegalArgumentException.class, () -> new Datum(null, 1.5));
    }

    @EpSemTest
    public void constructorEmptyStatisticThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> new Datum("", 1));
        assertThrows(IllegalArgumentException.class, () -> new Datum("", 1.5));
    }

    @EpSemTest
    public void constructDatumWithInteger() {
        Datum datum = new Datum("statistic", 1);
        assertEquals("statistic", datum.getStatistic());
        assertEquals("1", datum.getDatum());
    }

    @EpSemTest
    public void constructDatumWithDouble() {
        Datum datum = new Datum("statistic", 1.5);
        assertEquals("statistic", datum.getStatistic());
        assertEquals("1.5", datum.getDatum());
    }
    //endregion
}
