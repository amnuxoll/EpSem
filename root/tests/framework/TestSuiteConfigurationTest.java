package framework;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 *
 * @author Zachary Paul Faltersack
 * @version 0.95
 */
public class TestSuiteConfigurationTest {
    //region Constructor Tests
    @Test
    public void constructorNumberOfIterationsLessThanOneThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> new TestSuiteConfiguration(0, 1));
    }

    @Test
    public void constructorNumberOfGoalsLessThanOneThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> new TestSuiteConfiguration(1, 0));
    }
    //endregion

    //region getNumberOfIterations Tests
    @Test
    public void getNumberOfIterations()
    {
        TestSuiteConfiguration configuration = new TestSuiteConfiguration(13, 14);
        assertEquals(13, configuration.getNumberOfIterations());
    }
    //endregion

    //region getNumberOfGoals Tests
    @Test
    public void getNumberOfGoals()
    {
        TestSuiteConfiguration configuration = new TestSuiteConfiguration(13, 14);
        assertEquals(14, configuration.getNumberOfGoals());
    }
    //endregion
}
