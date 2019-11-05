package agents.dart;

import framework.Action;
import framework.SensorData;

public class ActionSense {
    public Action action;
    public SensorData sensorData;

    public ActionSense(Action action, SensorData sensorData) {
        this.action = action;
        this.sensorData = sensorData;
    }
}
