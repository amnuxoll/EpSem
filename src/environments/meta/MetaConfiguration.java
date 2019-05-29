package environments.meta;

/**
 * a class with configurations for a MetaEnvironment
 */
public class MetaConfiguration {
    //region Static MetaConfigurations
    public static final MetaConfiguration DEFAULT = new MetaConfiguration(500);
    //endregion

    //region Class Variables
    private int resetGoalCount; //number of goals until tweak
    //endregion

    //region Constructors
    public MetaConfiguration(int resetGoalCount) {
        if (resetGoalCount < 1)
            throw new IllegalArgumentException("resetGoalCount must be positive");
        this.resetGoalCount = resetGoalCount;
    }
    //endregion

    //region Public Methods
    public int getResetGoalCount() {
        return resetGoalCount;
    }
    //endregion
}
