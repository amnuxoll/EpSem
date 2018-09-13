package agents.nsm;

import framework.Episode;
import framework.Move;
import framework.SensorData;

public class QEpisode extends Episode{
    public double qValue = 0.0;
    public double reward = 0.0;

    public QEpisode(SensorData sensorData, Move move) {
        super(move);
    }
}
