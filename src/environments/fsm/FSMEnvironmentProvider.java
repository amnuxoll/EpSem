package environments.fsm;

import framework.*;

import java.util.*;

/**
 * An FSMEnvironmentProvider generates new {@link FSMEnvironment}s for consecutive test runs.
 *
 * @author Zachary Paul Faltersack
 * @version 0.95
 */
public class FSMEnvironmentProvider implements IEnvironmentProvider {
    //region Class Variables
    private FSMTransitionTableBuilder transitionTableBuilder;
    private EnumSet<FSMEnvironment.Sensor> sensorsToInclude;
    private boolean serialize = false;
    //endregion

    //region Constructors

    /**
     * Create an instance of a {@link FSMEnvironmentProvider}.
     *
     * @param transitionTableBuilder The transition table builder for FSMs.
     * @param sensorsToInclude The sensor data to include when navigating the FSM.
     */
    public FSMEnvironmentProvider(FSMTransitionTableBuilder transitionTableBuilder, EnumSet<FSMEnvironment.Sensor> sensorsToInclude) {
        if (transitionTableBuilder == null)
            throw new IllegalArgumentException("transitionTableBuilder cannot be null");
        if (sensorsToInclude == null)
            throw new IllegalArgumentException("sensorsToInclude cannot be null");
        if (transitionTableBuilder.getNumStates() > 10 && sensorsToInclude.contains(FSMEnvironment.Sensor.CACTUS1))
            System.out.println("WARNING: Cactus sensors may not function properly for machines with less than 10 states.");
        this.transitionTableBuilder = transitionTableBuilder;
        this.sensorsToInclude = sensorsToInclude;
    }

    /**
     * Create an instance of a {@link FSMEnvironmentProvider}.
     *
     * @param transitionTableBuilder The transition table builder for FSMs.
     * @param sensorsToInclude The sensor data to include when navigating the FSM.
     * @param serialize Indicates generated environments should get printed to stdout when created.
     */
    public FSMEnvironmentProvider(FSMTransitionTableBuilder transitionTableBuilder, EnumSet<FSMEnvironment.Sensor> sensorsToInclude, boolean serialize){
        this(transitionTableBuilder, sensorsToInclude);
        this.serialize = serialize;
    }
    //endregion

    //region IEnvironmentDescriptionProvider Members
    /**
     * Get a new instance of a {@link FSMEnvironment}.
     * @return The new {@link FSMEnvironment}.
     */
    @Override
    public IEnvironment getEnvironment() {
        FSMTransitionTable transitionTable = this.transitionTableBuilder.getTransitionTable();
        if(serialize) System.out.println(transitionTable.toString());
        return new FSMEnvironment(transitionTable, this.sensorsToInclude);
    }

    @Override
    public String getAlias() {
        return "FSMEnvironment[" + this.transitionTableBuilder.getDetails() + "]";
    }
    //endregion
}
