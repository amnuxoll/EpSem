package environments.meta;

/**
 * a class with configurations for a MetaEnvironmentDescription
 */
public class MetaConfiguration {
    //region Static MetaConfigurations
    public static final MetaConfiguration DEFAULT = new MetaConfiguration(500, 0);
    //endregion

    //region Class Variables
    private int successQueueMaxSize;
    private int tweakPoint; //number of goals until tweak
    //endregion

    //region Constructors
    public MetaConfiguration(int tweakPoint, int successQueueMaxSize) {
        if(tweakPoint < 1){
            throw new IllegalArgumentException("tweakPoint must be positive");
        }

        this.successQueueMaxSize = successQueueMaxSize;
        this.tweakPoint= tweakPoint;
    }
    //endregion

    //region Public Methods
    public int getSuccessQueueMaxSize() {
        return successQueueMaxSize;
    }

    public int getTweakPoint() {
        return tweakPoint;
    }
    //endregion
}
