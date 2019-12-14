package framework;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

/**
 * An {@link IResultCompiler} gets configured for a {@link TestSuite} and then manages all the handling of
 * statistical data captured during the execution of any test runs that comprise the suite.
 *
 * @author Zachary Paul Faltersack
 * @version 0.95
 */
public interface IResultCompiler {

    //region Methods

    /**
     * Notifies the {@link IResultCompiler} that an agent with the provided configurations will be used during a run.
     *
     * @param agentId The ID of the agent being registered.
     * @param agentAlias A human-friendly name for the agent.
     * @param dataToTrack The set of data expected to be captured for the agent during the test run.
     */
    void registerAgent(int agentId, String agentAlias, String[] dataToTrack);

    /**
     * Notifies the {@link IResultCompiler} that an environment with the provided configurations will be used
     * during a run.
     *
     * @param environmentId The ID of the environment being registered.
     * @param environmentAlias A human-friendly name for the environment.
     */
    void registerEnvironment(int environmentId, String environmentAlias);

    /**
     * Once all the registrations have been made, this method is invoked to allow the implementation a
     * chance to build out any internal state it will use for logging data during a test run.
     *
     * @throws IOException
     */
    void build() throws IOException;

    /**
     * Tracks results across agents, environments, and iterations.
     *
     * @param iteration Which iteration is being updated. Since the cross-product of agent/environments is itself
     *                  executed multiple times, this groups those results.
     * @param agentId The ID of the agent being updated.
     * @param environmentId The ID of the environment being updated.
     * @param goalNumber Which goal the test is at.
     * @param data The collection of {@link Datum} containing the results to log.
     * @throws IOException
     */
    void logResult(int iteration, int agentId, int environmentId, int goalNumber, ArrayList<Datum> data) throws IOException;

    /**
     * When the suite is complete, this method is executed in order to all the implementation a chance to
     * finalize the data sets it has collected.
     *
     * @throws IOException
     */
    void complete() throws IOException;

    //Endregion
}
