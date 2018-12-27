package framework;

import java.util.EventObject;
import java.util.HashMap;

/**
 * A GoalEvent is used to indicate that a goal was located while traversing an {@link Environment}.
 * @author Zachary Paul Faltersack
 * @version 0.95
 */
class GoalEvent extends EventObject {
    //region Class Variables
    private String stepCountToGoal;
    private HashMap<String, String> agentResults;
    //endregion

    //region Constructors
    /**
     * Constructs a prototypical Event.
     *
     * @param source The object on which the Event initially occurred.
     * @throws IllegalArgumentException if source is null.
     */
    public GoalEvent(Object source, int stepCountToGoal, HashMap<String, String> agentResults) {
        super(source);
        this.stepCountToGoal = Integer.toString(stepCountToGoal);
        this.agentResults = agentResults;
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

    public HashMap<String, String> getAgentResults() {
        return this.agentResults;
    }
    //endregion
}
