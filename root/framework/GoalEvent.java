package framework;

import java.util.EventObject;

/**
 * A GoalEvent is used to indicate that a goal was located while traversing an {@link Environment}.
 * @author Zachary Paul Faltersack
 * @version 0.95
 */
class GoalEvent extends EventObject {
    private int stepCountToGoal;
    private int decisionCountToGoal;

    /**
     * Constructs a prototypical Event.
     *
     * @param source The object on which the Event initially occurred.
     * @throws IllegalArgumentException if source is null.
     */
    public GoalEvent(Object source, int stepCountToGoal, int decisionsToGoal) {
        super(source);
        this.stepCountToGoal = stepCountToGoal;
        this.decisionCountToGoal = decisionsToGoal;
    }

    /**
     * Get the number of steps taken before the goal was reached.
     * @return the number of steps.
     */
    public int getStepCountToGoal() {
        return this.stepCountToGoal;
    }

    public int getDecisionCountToGoal() { return this.decisionCountToGoal; }
}
