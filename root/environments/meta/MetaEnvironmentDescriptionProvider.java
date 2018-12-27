package environments.meta;

import framework.IEnvironmentDescription;
import framework.IEnvironmentDescriptionProvider;

public class MetaEnvironmentDescriptionProvider implements IEnvironmentDescriptionProvider {
    //region Class Variables
    private IEnvironmentDescriptionProvider environmentDescriptionProvider;
    private MetaConfiguration config;
    //endregion

    //region Constructors
    public MetaEnvironmentDescriptionProvider
            (IEnvironmentDescriptionProvider environmentDescriptionProvider, MetaConfiguration config) {
        if(environmentDescriptionProvider == null || config == null)
            throw new IllegalArgumentException("Arguments cannot be null");
        this.environmentDescriptionProvider= environmentDescriptionProvider;
        this.config= config;
    }
    //endregion

    //region IEnvironmentDescriptionProvider Members
    @Override
    public IEnvironmentDescription getEnvironmentDescription() {
        return new MetaEnvironmentDescription(environmentDescriptionProvider, config);
    }
    //endregion
}
