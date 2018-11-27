package framework;

import java.util.Objects;

/**
 * An Episode describes a pairing of a {@link SensorData} and {@link Move} where the move was selected
 * as a result of the sensor data.
 * @author Zachary Paul Faltersack
 * @version 0.95
 */
public class Episode {
    private Move move;
    private SensorData sensorData;

    /**
     * Create an Episode.
     * @param move The {@link Move} associated with the episode.
     */
    public Episode(Move move) {
        if (move == null)
            throw new IllegalArgumentException("move cannot be null");
        this.move = move;
    }

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

    /**
     * return a weighted match to the given episode
     * based on the provided weights
     *
     * @param ep the episode to compare this episode to
     * @param weights the weights to use in the comparison
     * @return a nomalized weighted match score between this episode and ep in range [0,1]
     */
    public double matchScore(Episode ep, EpisodeWeights weights){
        double score= 0;
        if(ep.move.equals(this.move)){
            score+= weights.getActionWeight();
        }

        //find match of each sensor
        for(String sensorName : sensorData.getSensorNames()){
            //if sensor values match
            if(this.sensorData.getSensor(sensorName).equals(ep.sensorData.getSensor(sensorName))){
                score+= weights.getSensorWeight(sensorName);
            }
        }

        double sum= weights.sumEntries();
        return sum == 0 ? 0 : score/sum;
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
}
