package environments.meta;

/**
 * a class with configurations for a MetaEnvironmentDescription
 */

public class MetaConfiguration {
    private int successQueueMaxSize;
    private int stepThreshold;
    private int tweakPoint; //number of goals until tweak

    public static final MetaConfiguration DEFAULT = new MetaConfiguration(50);

    public MetaConfiguration(int tweakPoint) {
        /*if (successQueueMaxSize < 1) {
            throw new IllegalArgumentException("successQueueMaxSize must be positive");
        }
        if (stepThreshold < 1) {
            throw new IllegalArgumentException("stepThreshold must be positive");
        }*/
        if(tweakPoint < 1){
            throw new IllegalArgumentException("tweakPoint must be positive");
        }

        this.successQueueMaxSize = successQueueMaxSize;
        this.stepThreshold = stepThreshold;
        this.tweakPoint= tweakPoint;
    }

    public int getSuccessQueueMaxSize() {
        return successQueueMaxSize;
    }

    public int getStepThreshold() {
        return stepThreshold;
    }

    public int getTweakPoint() {
        return tweakPoint;
    }
}
