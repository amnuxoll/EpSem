package environments.fsm;

import framework.IEnvironmentDescription;
import utils.Randomizer;
import java.util.EnumSet;

/**
 * An FSMDescriptionTweakingProvider generates mutated {@link FSMDescription}s for consecutive test runs.
 *
 * @author Zachary Paul Faltersack
 * @version 0.95
 */
public class FSMDescriptionTweakingProvider extends FSMDescriptionProvider {
    //region Class Variables
    private FSMDescription lastDescription;
    //endregion

    //region Constructors
    /**
     * Create an instance of a {@link FSMDescriptionProvider}.
     *
     * @param transitionTableBuilder The transition table builder for FSMs.
     * @param sensorsToInclude       The sensor data to include when navigating the FSM.
     */
    public FSMDescriptionTweakingProvider(FSMTransitionTableBuilder transitionTableBuilder, EnumSet<FSMDescription.Sensor> sensorsToInclude) {
        super(transitionTableBuilder, sensorsToInclude);
    }
    //endregion

    //region FSMDescriptionProvider Overrides
    @Override
    public IEnvironmentDescription getEnvironmentDescription() {
        if (this.lastDescription == null)
            this.lastDescription = (FSMDescription) super.getEnvironmentDescription();
        else
            this.lastDescription.tweakTable(2, new Randomizer());
        return this.lastDescription;
    }
    //endregion
}
