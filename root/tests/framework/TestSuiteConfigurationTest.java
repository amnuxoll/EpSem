package framework;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

public class TestSuiteConfigurationTest {

    // constructor Tests
    @Test
    public void constructorNumberOfIterationsLessThanOneThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> new TestSuiteConfiguration(0, 1));
    }

    @Test
    public void constructorNumberOfGoalsLessThanOneThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> new TestSuiteConfiguration(1, 0));
    }
}
