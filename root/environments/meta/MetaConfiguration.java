package environments.meta;

/**
 * a class with configurations for a MetaEnvironmentDescription
 */
public class MetaConfiguration {
    //region Static MetaConfigurations
    public static final MetaConfiguration DEFAULT = new MetaConfiguration(500);
    //endregion

    //region Class Variables
    private int successQueueMaxSize;
    private int stepThreshold;
    private int tweakPoint; //number of goals until tweak
    //endregion

    //region Constructors
    public MetaConfiguration(int tweakPoint) {
        if(tweakPoint < 1){
            throw new IllegalArgumentException("tweakPoint must be positive");
        }

        this.successQueueMaxSize = successQueueMaxSize;
        this.stepThreshold = stepThreshold;
        this.tweakPoint= tweakPoint;
    }
    //endregion

    //region Public Methods
    public int getSuccessQueueMaxSize() {
        return successQueueMaxSize;
    }

    public int getStepThreshold() {
        return stepThreshold;
    }

    public int getTweakPoint() {
        return tweakPoint;
    }
    //endregion
}
