package framework;

/**
 * An IEnvironmentDescriptionProvider generates new {@link IEnvironmentDescription} for consecutive test runs.
 * @author Zachary Paul Faltersack
 * @version 0.95
 */
public interface IEnvironmentDescriptionProvider {
    //region Methods
    /**
     * Get a new {@link IEnvironmentDescription}.
     * @return The new {@link IEnvironmentDescription}.
     */
    IEnvironmentDescription getEnvironmentDescription();
    //endregion
}
