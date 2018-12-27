package environments.fsm;

import framework.IEnvironmentDescription;
import utils.FSMTransitionTableBuilder;
import utils.Randomizer;

import java.util.EnumSet;

public class FSMDescriptionTweakingProvider extends FSMDescriptionProvider {
    private FSMDescription lastDescription;

    /**
     * Create an instance of a {@link FSMDescriptionProvider}.
     *
     * @param transitionTableBuilder The transition table builder for FSMs.
     * @param sensorsToInclude       The sensor data to include when navigating the FSM.
     */
    public FSMDescriptionTweakingProvider(FSMTransitionTableBuilder transitionTableBuilder, EnumSet<FSMDescription.Sensor> sensorsToInclude) {
        super(transitionTableBuilder, sensorsToInclude);
    }

    @Override
    public IEnvironmentDescription getEnvironmentDescription()
    {
        if (this.lastDescription == null)
            this.lastDescription = (FSMDescription) super.getEnvironmentDescription();
        else
            this.lastDescription.tweakTable(2, new Randomizer());
        return this.lastDescription;
    }
}
