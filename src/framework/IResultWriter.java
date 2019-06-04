package framework;

import java.io.IOException;

/**
 * An {@link IResultWriter} is used by the framework to output the result of a test run for an agent.
 *
 * @author Zachary Paul Faltersack
 * @version 0.95
 */
public interface IResultWriter extends AutoCloseable {

    //region Methods

    /**
     * Starts a new series of N runs to a goal.
     *
     * @throws IOException
     */
    void beginNewRun() throws IOException;

    /**
     * Logs a specific statistic tracked between consecutive runs to a goal.
     *
     * @param result the statistic to log.
     * @throws IOException
     */
    void logResult(String result) throws IOException;

    /**
     * Ends a set of N runs to a goal.
     *
     * @throws IOException
     */
    void complete() throws IOException;

    //endregion

}
