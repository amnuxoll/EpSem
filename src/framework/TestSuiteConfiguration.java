package framework;

/**
 * The set of configurations that can be used for a {@link TestSuite}.
 *
 * @author Zachary Paul Faltersack
 * @version 0.95
 */
public class TestSuiteConfiguration {

    //region Static Configurations
    /** Defines a temp debug configuration */
    public static final TestSuiteConfiguration TEST = new TestSuiteConfiguration(1, 100);

    /** Defines a one-FSM (very short) debug configuration. */
    public static final TestSuiteConfiguration ONCE = new TestSuiteConfiguration(1, 300);

    /** Defines a two-FSM (rather short) debug configuration. */
    public static final TestSuiteConfiguration TWICE = new TestSuiteConfiguration(2, 100);

    /** very short */
    public static final TestSuiteConfiguration VERY_QUICK = new TestSuiteConfiguration(5, 80);

    /** Defines a short debug configuration. */
    public static final TestSuiteConfiguration QUICK = new TestSuiteConfiguration(10, 500);
    public static final TestSuiteConfiguration QUICK_MULTI = new TestSuiteConfiguration(10, 100, 3);

    /** Defines a medium sniff-test configuration. */
    public static final TestSuiteConfiguration MEDIUM = new TestSuiteConfiguration(25, 500);
    public static final TestSuiteConfiguration MEDIUM_MULTI = new TestSuiteConfiguration(25, 500, 3);

    public static final TestSuiteConfiguration LONG = new TestSuiteConfiguration(25, 1000);
    public static final TestSuiteConfiguration LONG_MULTI = new TestSuiteConfiguration(25, 1000, 3);

    /** Provides comprehensive data-gathering configuration. */
    public static final TestSuiteConfiguration FULL = new TestSuiteConfiguration(50, 1000);
    public static final TestSuiteConfiguration FULL_MULTI = new TestSuiteConfiguration(50, 1000, 3);

    //endregion

    //region Class Variables

    /** The number of machines in which an agent should run. */
    private int numberOfIterations;

    /** The number of goals the agent should find in a given machine. */
    private int numberOfGoals;

    /** If this is a positive number then the suite will run in multi-thread mode with this timeout on the executor. */
    private int timeout;
    //endregion

    //region Constructors

    /**
     * Creates an instance of a {@link TestSuiteConfiguration}.
     *
     * @param numberOfIterations the number of "machines" to run an agent in.
     * @param numberOfGoals the number of goals the agent should find in a given machine.
     */
    public TestSuiteConfiguration(int numberOfIterations, int numberOfGoals) {
        this(numberOfIterations, numberOfGoals, 0);
    }

    /**
     * Creates an instance of a {@link TestSuiteConfiguration}.
     *
     * @param numberOfIterations the number of "machines" to run an agent in.
     * @param numberOfGoals the number of goals the agent should find in a given machine.
     * @param timeout the timeout to apply if running a multi-threaded test suite. A positive number indicates
     *                the test suite should run multi-threaded.
     */
    public TestSuiteConfiguration(int numberOfIterations, int numberOfGoals, int timeout) {
        if (numberOfIterations < 1)
            throw new IllegalArgumentException("numberOfIterations cannot be less than 1.");
        if (numberOfGoals < 1)
            throw new IllegalArgumentException("numberOfGoals cannot be less than 1.");
        this.numberOfIterations = numberOfIterations;
        this.numberOfGoals = numberOfGoals;
        this.timeout = timeout;
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

    /**
     * The timeout to use if running multi-threaded. For simplicity's sake, at the moment if this
     * comes back as a positive non-zero integer, then the suite will run multi-threaded and use
     * that value for the timeout.
     *
     * @return the number of hours to configure for a multi-threaded timeout on the test suite.
     */
    public int getTimeout() { return this.timeout; }

    //endregion
}
