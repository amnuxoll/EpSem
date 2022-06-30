package agents.wfc;

import framework.Action;
import framework.SensorData;

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

    @Override
    public String toString() {
        return "(" + this.externalSensor.toStringShort() + ")" +
                (this.action.toString().equals("*") ? "" : this.action.toString());
    }
}
