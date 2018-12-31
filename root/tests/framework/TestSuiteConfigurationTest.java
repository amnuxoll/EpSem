package tests.framework;

import framework.TestSuiteConfiguration;

import tests.EpSemTest;
import tests.EpSemTestClass;
import static tests.Assertions.*;

/**
 *
 * @author Zachary Paul Faltersack
 * @version 0.95
 */
@EpSemTestClass
public class TestSuiteConfigurationTest {
    //region Constructor Tests
    @EpSemTest
    public void constructorNumberOfIterationsLessThanOneThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> new TestSuiteConfiguration(0, 1));
    }

    @EpSemTest
    public void constructorNumberOfGoalsLessThanOneThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> new TestSuiteConfiguration(1, 0));
    }
    //endregion

    //region getNumberOfIterations Tests
    @EpSemTest
    public void getNumberOfIterations()
    {
        TestSuiteConfiguration configuration = new TestSuiteConfiguration(13, 14);
        assertEquals(13, configuration.getNumberOfIterations());
    }
    //endregion

    //region getNumberOfGoals Tests
    @EpSemTest
    public void getNumberOfGoals()
    {
        TestSuiteConfiguration configuration = new TestSuiteConfiguration(13, 14);
        assertEquals(14, configuration.getNumberOfGoals());
    }
    //endregion
}
