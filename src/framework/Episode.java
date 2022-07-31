package framework;

import java.util.Objects;

/**
 * An Episode describes a pairing of a {@link SensorData} and {@link Action} where the action was selected
 * as a result of the sensor data.
 *
 * Frequent shorthand may look similar to 0010A where:
 * 0010 represents some binary sensor data; and
 * A represents the selection action.
 *
 * @author Zachary Paul Faltersack
 * @version 0.95
 */
public class Episode {
    //region Class Variables

    /** the {@link SensorData} for this {@link Episode}. */
    private SensorData sensorData;

    /** the {@link Action} for this {@link Episode}. */
    private Action action;

    //endregion

    //region Constructors

    /**
     * Create an Episode.
     *
     * @param action The {@link Action} associated with the episode.
     */
    public Episode(SensorData sensorData, Action action) {
        // TODO -- validate sensor data is not null
        if (sensorData == null)
            throw new IllegalArgumentException("sensorData cannot be null");
        if (action == null)
            throw new IllegalArgumentException("action cannot be null");
        this.sensorData = sensorData;
        this.action = action;
    }

    /**
     * Copy constructor to create a "deep copy" of an {@link Episode}.
     *
     * CAVEAT: is that the actual sensor values are not themselves deep copied (yet) but for current
     * purposes this is fine. If ever we should want to treat a given sensor value as mutable then this
     * should be revisited.
     *
     * @param orig the {@link Episode} to copy.
     */
    public Episode(Episode orig) {
        if (orig == null)
            throw new IllegalArgumentException("cannot copy a null episode");
        this.sensorData = new SensorData(orig.getSensorData());
        this.action = new Action(orig.getAction());
    }

    //endregion

    //region Public Methods

    /**
     * Get the action for this episode.
     *
     * @return The {@link Action} of the episode.
     */
    public Action getAction() {
        return this.action;
    }

    /**
     * Get the sensor data for this episode.
     *
     * @return The {@link SensorData} of the episode.
     */
    public SensorData getSensorData() {
        return this.sensorData;
    }

    /**
     * Determines whether or not the goal was hit in this episode.
     *
     * @return true if the {@link Episode#sensorData} contains the goal sensor; otherwise false.
     */
    public boolean hitGoal() {
        return this.sensorData.isGoal();
    }

    /**
     * partialEquals
     * <p>
     * Returns a value from 0 to 1.0 of how much this episode matches the given episode. If the actions don't match,
     * it's a zero.
     * @param e the other episode being compared
     * @return
     */
    public double partialEquals(Episode e) {

        double score = 0.0d;

        if (this.sensorData == null || e.sensorData == null) {
            return score;
        }

        if (!this.action.equals(e.action)) {
            return score;
        }

        int numMatches = 0;

        for (String sensorName : this.sensorData.getSensorNames()) {

            if (this.sensorData.getSensor(sensorName).equals(e.sensorData.getSensor(sensorName))) {
                numMatches++;
            }
        }

        return ((double) numMatches  / ((double) this.sensorData.size()));
    }//partialEquals
    //endregion

    //region Object Overrides
    /**
     * Determine if another object equals this {@link Episode}.
     *
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

    /**
     * @return the hashcode of this {@link Episode}.
     */
    @Override
    public int hashCode() {
        return Objects.hash(this.action, this.sensorData);
    }

    /**
     * @return the string representation of this {@link Episode]}.
     */
    @Override
    public String toString() {
        return this.sensorData.toString(true) + this.action.toString();
    }
    //endregion
}
