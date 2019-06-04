package framework;

/**
 * The set of configurations that can be used for a {@link TestSuite}.
 *
 * @author Zachary Paul Faltersack
 * @version 0.95
 */
public class TestSuiteConfiguration {

    //region Static Configurations

    /** Defines a short debug configuration. */
    public static final TestSuiteConfiguration QUICK = new TestSuiteConfiguration(10, 100);

    /** Defines a medium sniff-test configuration. */
    public static final TestSuiteConfiguration MEDIUM = new TestSuiteConfiguration(25, 500);

    /** Provides comprehensive data-gathering configuration. */
    public static final TestSuiteConfiguration FULL = new TestSuiteConfiguration(50, 1000);

    //endregion

    //region Class Variables

    /** The number of machines in which an agent should run. */
    private int numberOfIterations;

    /** The number of goals the agent should find in a given machine. */
    private int numberOfGoals;

    //endregion

    //region Constructors

    /**
     * Creates an instance of a {@link TestSuiteConfiguration}.
     *
     * @param numberOfIterations the number of "machines" to run an agent in.
     * @param numberOfGoals the number of goals the agent should find in a given machine.
     */
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

    /**
     * Gets the number of iterations for an environment.
     *
     * @return the number of iterations an agent should execute in a given environment.
     */
    public int getNumberOfIterations() {
        return this.numberOfIterations;
    }

    /**
     * The number of goals to find in a given instance of an environment.
     *
     * @return the number of goals to find.
     */
    public int getNumberOfGoals() {
        return this.numberOfGoals;
    }

    //endregion
}
