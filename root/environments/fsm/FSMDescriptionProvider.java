package environments.fsm;

import framework.IEnvironmentDescription;
import framework.IEnvironmentDescriptionProvider;
import framework.Move;

import java.util.EnumSet;
import java.util.HashMap;

/**
 * An FSMDescriptionProvider generates new {@link FSMDescription}s for consecutive test runs.
 * @author Zachary Paul Faltersack
 * @version 0.95
 */
public class FSMDescriptionProvider implements IEnvironmentDescriptionProvider {
    private int alphabetSize;
    private int numStates;
    private EnumSet<FSMDescription.Sensor> sensorsToInclude;

    /**
     * Create an instance of a {@link FSMDescriptionProvider}.
     * @param alphabetSize The number of moves to allow in the FSM.
     * @param numStates The number of states to allow in the FSM.
     * @param sensorsToInclude The sensor data to include when navigating the FSM.
     */
    public FSMDescriptionProvider(int alphabetSize, int numStates, EnumSet<FSMDescription.Sensor> sensorsToInclude) {
        if (alphabetSize < 1)
            throw new IllegalArgumentException("alphabetSize cannot be less than 1");
        if (numStates < 1)
            throw new IllegalArgumentException("numStates cannot be less than 1");
        if (sensorsToInclude == null)
            throw new IllegalArgumentException("sensorsToInclude cannot be null");
       this.alphabetSize = alphabetSize;
       this.numStates = numStates;
       this.sensorsToInclude = sensorsToInclude;
    }

    /**
     * Get a new instance of a {@link FSMDescription}.
     * @return The new {@link FSMDescription}.
     */
    @Override
    public IEnvironmentDescription getEnvironmentDescription() {
        FSMTransitionTableBuilder builder = new FSMTransitionTableBuilder(this.alphabetSize, this.numStates);
        HashMap<Move, Integer>[] transitionTable = builder.getTransitionTable();
        return new FSMDescription(transitionTable, this.sensorsToInclude);
    }
}
