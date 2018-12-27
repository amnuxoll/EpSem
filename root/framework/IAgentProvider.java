package framework;

/**
 * An IAgentProvider is used to generate new instances of {@link IAgent} for multiple test runs.
 * @author Zachary Paul Faltersack
 * @version 0.95
 */
public interface IAgentProvider {
    //region Methods
    /**
     * Get a new {@link IAgent}.
     * @return a new {@link IAgent}.
     */
    IAgent getAgent();

    String getAlias();
    //endregion
}
