package framework;

/**
 * An IEnvironmentProvider generates new {@link IEnvironment} for consecutive test runs.
 *
 * @author Zachary Paul Faltersack
 * @version 0.95
 */
public interface IEnvironmentProvider {

    //region Methods

    /**
     * Get a new {@link IEnvironment}.
     * @return The new {@link IEnvironment}.
     */
    IEnvironment getEnvironment();

    /**
     * @return a user-friendly alias for the {@link IEnvironment} (primarily used to generate file names for data)
     */
    String getAlias();

    //endregion
}
