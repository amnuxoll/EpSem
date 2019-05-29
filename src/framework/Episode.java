package framework;

import java.util.Objects;

/**
 * An Episode describes a pairing of a {@link SensorData} and {@link Action} where the action was selected
 * as a result of the sensor data.
 * @author Zachary Paul Faltersack
 * @version 0.95
 */
public class Episode {
    //region Class Variables
    private Action action;
    private SensorData sensorData;
    //endregion

    //region Constructors
    /**
     * Create an Episode.
     * @param action The {@link Action} associated with the episode.
     */
    public Episode(SensorData sensorData, Action action) {
        // TODO -- validate sensor data is not null
        if (action == null)
            throw new IllegalArgumentException("action cannot be null");
        this.sensorData = sensorData;
        this.action = action;
    }
    //endregion

    //region Public Methods
    /**
     * Get the action for this episode.
     * @return The {@link Action} of the episode.
     */
    public Action getAction() {
        return this.action;
    }

    /**
     * Get the sensor data for this episode.
     * @return The {@link SensorData} of the episode.
     */
    public SensorData getSensorData() {
        return this.sensorData;
    }

    public boolean hitGoal() {
        return this.sensorData.isGoal();
    }
    //endregion

    //region Object Overrides
    /**
     * Determine if another object equals this {@link Episode}.
     * @param o The other object to compare with.
     * @return true if the objects are equal; otherwise false.
     */
    @Override
    public boolean equals(Object o) {
        if (o == this) return true;
        if (!(o instanceof Episode)) return false;

        Episode episode = (Episode) o;
        if (this.sensorData == null && episode.sensorData != null)
            return false;
        if (this.sensorData != null && episode.sensorData == null)
            return false;
        if (this.sensorData != null && !this.sensorData.equals(episode.sensorData))
            return false;
        return this.action.equals(episode.action);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.action, this.sensorData);
    }

    @Override
    public String toString() {
        String value = this.action.toString();
        if (this.sensorData != null)
            value = this.sensorData.toString(false) + value;
        return value;
    }
    //endregion
}
