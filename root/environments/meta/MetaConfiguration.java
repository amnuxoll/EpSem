package environments.meta;

/**
 * a class with configurations for a MetaEnvironmentDescription
 */

public class MetaConfiguration {
    private int successQueueMaxSize;
    private int stepThreshold;

    public static final MetaConfiguration DEFAULT = new MetaConfiguration(10,10);

    public MetaConfiguration(int successQueueMaxSize, int stepThreshold) {
        if(successQueueMaxSize < 1) {
            throw new IllegalArgumentException("successQueueMaxSize must be positive");
        }
        if(stepThreshold < 1) {
            throw new IllegalArgumentException("stepThreshold must be positive");
        }

        this.successQueueMaxSize= successQueueMaxSize;
        this.stepThreshold= stepThreshold;
    }

    public int getSuccessQueueMaxSize() {
        return successQueueMaxSize;
    }

    public int getStepThreshold() {
        return stepThreshold;
    }
}
