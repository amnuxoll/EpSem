package environments.meta;

import framework.*;

public class MetaEnvironmentProvider implements IEnvironmentProvider {
    //region Class Variables
    private IEnvironmentProvider environmentProvider;
    private MetaConfiguration config;
    //endregion

    //region Constructors
    public MetaEnvironmentProvider(IEnvironmentProvider environmentProvider, MetaConfiguration config) {
        if(environmentProvider == null || config == null)
            throw new IllegalArgumentException("Arguments cannot be null");
        this.environmentProvider = environmentProvider;
        this.config= config;
    }
    //endregion

    //region IEnvironmentDescriptionProvider Members
    @Override
    public IEnvironment getEnvironment() {
        return new MetaEnvironment(this.environmentProvider, this.config);
    }

    @Override
    public String getAlias() {
        return "MetaEnvironment{" + this.environmentProvider.getAlias() + "}";
    }
    //endregion
}
