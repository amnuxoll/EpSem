package framework;

/**
 *
 * @author Zachary Paul Faltersack
 * @version 0.95
 */
public class TestSuiteConfiguration {
    //region Static Configurations
    public static final TestSuiteConfiguration QUICK = new TestSuiteConfiguration(10, 100);
    public static final TestSuiteConfiguration MEDIUM = new TestSuiteConfiguration(25, 500);
    public static final TestSuiteConfiguration FULL = new TestSuiteConfiguration(50, 1000);
    //endregion

    //region Class Variables
    private int numberOfIterations;
    private int numberOfGoals;
    //endregion

    //region Constructors
    public TestSuiteConfiguration(int numberOfIterations, int numberOfGoals) {
        if (numberOfIterations < 1)
            throw new IllegalArgumentException("numberOfIterations cannot be less than 1.");
        if (numberOfGoals < 1)
            throw new IllegalArgumentException("numberOfGoals cannot be less than 1.");
        this.numberOfIterations = numberOfIterations;
        this.numberOfGoals = numberOfGoals;
    }
    //endregion

    //region Public Methods
    public int getNumberOfIterations() {
        return this.numberOfIterations;
    }

    public int getNumberOfGoals() {
        return this.numberOfGoals;
    }
    //endregion
}
