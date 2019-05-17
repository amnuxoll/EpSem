package framework;

/**
 * An IAgentProvider is used to generate new instances of {@link IAgent} for multiple test runs.
 * @author Zachary Paul Faltersack
 * @version 0.95
 */
public interface IAgentProvider {
    //region Methods

    /**
     * (optional) add a ctor to your subclass that allows the instantiator to
     * pass in configuration information.
     */
    
    /**
     * Get a new {@link IAgent}.
     * @return a new {@link IAgent}.
     */
    IAgent getAgent();

    /** @return a unique string to identify your agent */
    String getAlias();
    //endregion
}
