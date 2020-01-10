package framework;

import java.util.ArrayList;
import java.util.EventObject;

/**
 * A GoalEvent is used to indicate that a goal was located while traversing an {@link IEnvironment}.
 *
 * @author Zachary Paul Faltersack
 * @version 0.95
 */
public class GoalEvent extends EventObject {

    //region Class Variables

    /** The collection of {@link Datum} representing additional {@link IAgent} statistical data. */
    private ArrayList<Datum> agentData;

    /** Indicates which goal number triggered this event. */
    private int goalCount;

    //endregion

    //region Constructors

    /**
     * Constructs a prototypical Event.
     *
     * @param source The object on which the Event initially occurred.
     * @param goalCount Which goal this event was triggered by.
     * @param  stepCountToGoal The number of steps taken to reach this goal
     * @param agentData Additional statistical data captured by the agent to preserve.
     * @throws IllegalArgumentException if source is null.
     */
    public GoalEvent(Object source, int goalCount, int stepCountToGoal, ArrayList<Datum> agentData) {
        super(source);
        if (stepCountToGoal < 1)
            throw new IllegalArgumentException("step count must be one or greater.");
        if (agentData == null)
            this.agentData = new ArrayList<>();
        else
            this.agentData = agentData;
        this.agentData.add(new Datum("steps", stepCountToGoal));
        this.goalCount = goalCount;
    }

    //endregion

    //region Public Methods

    /**
     * Get the additional statistical data provided by the {@link IAgent}.
     *
     * @return the list of additional statistical data.
     */
    public ArrayList<Datum> getAgentData() {
        return this.agentData;
    }

    /**
     * Gets the goal number for this event.
     *
     * @return The number indicating which goal triggered this event.
     */
    public int getGoalNumber() { return this.goalCount; }

    //endregion
}
