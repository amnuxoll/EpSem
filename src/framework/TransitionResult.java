package framework;

/**
 *
 * @author Zachary Paul Faltersack
 * @version 0.95
 */
public class TransitionResult {
    //region Class Variables
    private int state;
    private SensorData sensorData;
    //endregion

    //region Constructors
    public TransitionResult(int state, SensorData sensorData) {
        if (sensorData == null)
            throw new IllegalArgumentException("sensorData cannot be null.");
        this.state = state;
        this.sensorData = sensorData;
    }
    //endregion

    //region Public Methods
    public int getState() {
        return this.state;
    }

    public SensorData getSensorData() {
        return this.sensorData;
    }
    //endregion
}
