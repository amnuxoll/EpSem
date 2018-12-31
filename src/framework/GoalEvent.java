package framework;

import java.util.ArrayList;
import java.util.EventObject;

/**
 * A GoalEvent is used to indicate that a goal was located while traversing an {@link Environment}.
 * @author Zachary Paul Faltersack
 * @version 0.95
 */
public class GoalEvent extends EventObject {
    //region Class Variables
    private String stepCountToGoal;
    private ArrayList<Datum> agentData;
    //endregion

    //region Constructors
    /**
     * Constructs a prototypical Event.
     *
     * @param source The object on which the Event initially occurred.
     * @throws IllegalArgumentException if source is null.
     */
    public GoalEvent(Object source, int stepCountToGoal, ArrayList<Datum> agentData) {
        super(source);
        if (stepCountToGoal < 1)
            throw new IllegalArgumentException("step count must be one or greater.");
        this.stepCountToGoal = Integer.toString(stepCountToGoal);
        if (agentData == null)
            this.agentData = new ArrayList<>();
        else
            this.agentData = agentData;
    }
    //endregion

    //region Public Methods
    /**
     * Get the number of steps taken before the goal was reached.
     * @return the number of steps.
     */
    public String getStepCountToGoal() {
        return this.stepCountToGoal;
    }

    public ArrayList<Datum> getAgentData() {
        return this.agentData;
    }
    //endregion
}
