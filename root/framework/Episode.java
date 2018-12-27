package framework;

import java.util.Objects;

/**
 * An Episode describes a pairing of a {@link SensorData} and {@link Move} where the move was selected
 * as a result of the sensor data.
 * @author Zachary Paul Faltersack
 * @version 0.95
 */
public class Episode {
    //region Class Variables
    private Move move;
    private SensorData sensorData;
    //endregion

    //region Constructors
    /**
     * Create an Episode.
     * @param move The {@link Move} associated with the episode.
     */
    public Episode(Move move) {
        if (move == null)
            throw new IllegalArgumentException("move cannot be null");
        this.move = move;
    }
    //endregion

    //region Public Methods
    public void setSensorData(SensorData sensorData) {
        if (sensorData == null)
            throw new IllegalArgumentException("sensorData cannot be null");
        this.sensorData = sensorData;
    }

    /**
     * Get the move for this episode.
     * @return The {@link Move} of the episode.
     */
    public Move getMove() {
        return this.move;
    }

    /**
     * Get the sensor data for this episode.
     * @return The {@link SensorData} of the episode.
     */
    public SensorData getSensorData() {
        return this.sensorData;
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
        if (o == this) {
            return true;
        }
        if (!(o instanceof Episode)) {
            return false;
        }
        Episode episode = (Episode) o;
        if (this.sensorData == null && episode.sensorData != null)
            return false;
        if (this.sensorData != null && episode.sensorData == null)
            return false;
        if (this.sensorData != null && !this.sensorData.equals(episode.sensorData))
            return false;
        return this.move.equals(episode.move);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.move, this.sensorData);
    }

    @Override
    public String toString(){
        String str= this.move.toString() + sensorData.toString(false);

        return str;
    }
    //endregion
}
