package agents.wfc;

import framework.Action;
import framework.SensorData;

/**
 * class PathNode
 * <p>
 * Represents a node along an agent's path to the goal. Contains the external sensor and the action taken at that step.
 */
public class PathNode {

    private SensorData externalSensor;
    private Action     action;

    public PathNode(SensorData externalSensor, char action) {
        this.externalSensor = externalSensor;
        this.action = new Action("" + action);
    }

    public SensorData getExternalSensor() {
        return externalSensor;
    }

    public Action getAction() {
        return action;
    }

    public void setAction(char action) {
        this.action = new Action("" + action);
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof PathNode)) return false;

        PathNode other = (PathNode) obj;

        // '*' actions means that the action hasn't been assigned yet. Any action will match with it.
        if (!other.getAction().toString().equals("*") && !this.getAction().toString().equals("*")) {
            if (!other.getAction().equals(this.action)) {
                return false;
            }
        }

        if (other.getExternalSensor().equals(this.externalSensor)) {
            return true;
        }

        return false;
    }

    /**
     * partialEquals
     * <p>
     * Returns a value from 0 to 1 based on how much this PathNode matches with another. If they have different
     * actions, the value returned is zero (with the exception of * actions, which are ignored).
     * @param other
     * @param ignoreGoal whether or not the goal sensor should be ignored in the matching calculation
     * @return a value from 0 to 1 of how much they match.
     */
    public double partialEquals(PathNode other, boolean ignoreGoal) {

        // '*' actions means that the action hasn't been assigned yet. Any action will match with it.
        // TODO refactor this? (duplicate code)
        if (!other.getAction().toString().equals("*") && !this.getAction().toString().equals("*")) {
            if (!other.getAction().equals(this.action)) {
                return 0.0d;
            }
        }

        int numMatches = 0;

        for (String sensorName : this.externalSensor.getSensorNames()) {

            if (ignoreGoal && sensorName.equals("GOAL")) {
                continue;
            }

            if (this.externalSensor.getSensor(sensorName).equals(other.getExternalSensor().getSensor(sensorName))) {
                numMatches++;
            }
        }

        return ((double) numMatches  / ((double) this.externalSensor.size() - (ignoreGoal ? 1      : 0)));
    }//partialEquals

    @Override
    public String toString() {
        return "(" + this.externalSensor.toStringShort() + ")" +
                (this.action.toString().equals("*") ? "" : this.action.toString());
    }
}
