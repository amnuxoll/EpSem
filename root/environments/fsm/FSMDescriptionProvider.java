package environments.fsm;

import framework.IEnvironmentDescription;
import framework.IEnvironmentDescriptionProvider;
import framework.Move;
import utils.FSMTransitionTableBuilder;
import utils.Randomizer;

import java.util.*;

/**
 * An FSMDescriptionProvider generates new {@link FSMDescription}s for consecutive test runs.
 * @author Zachary Paul Faltersack
 * @version 0.95
 */
public class FSMDescriptionProvider implements IEnvironmentDescriptionProvider {
    private FSMTransitionTableBuilder transitionTableBuilder;
    private EnumSet<FSMDescription.Sensor> sensorsToInclude;

    /**
     * Create an instance of a {@link FSMDescriptionProvider}.
     * @param transitionTableBuilder The transition table builder for FSMs.
     * @param sensorsToInclude The sensor data to include when navigating the FSM.
     */
    public FSMDescriptionProvider(FSMTransitionTableBuilder transitionTableBuilder, EnumSet<FSMDescription.Sensor> sensorsToInclude) {
        if (transitionTableBuilder == null)
            throw new IllegalArgumentException("transitionTableBuilder cannot be null");
        if (sensorsToInclude == null)
            throw new IllegalArgumentException("sensorsToInclude cannot be null");
        this.transitionTableBuilder = transitionTableBuilder;
        this.sensorsToInclude = sensorsToInclude;
    }

    /**
     * Get a new instance of a {@link FSMDescription}.
     * @return The new {@link FSMDescription}.
     */
    @Override
    public IEnvironmentDescription getEnvironmentDescription() {
        HashMap<Move, Integer>[] transitionTable = this.transitionTableBuilder.getTransitionTable();
        FSMDescription toReturn = new FSMDescription(transitionTable, this.sensorsToInclude);
        toReturn.setShortestSequences(this.transitionTableBuilder.getShortestSequences());
        toReturn.setShortestSequences(this.transitionTableBuilder.getShortestSequences());
        System.out.println("Universal sequence: "+toReturn.getUniversalSequence());
        return toReturn;
    }
}
