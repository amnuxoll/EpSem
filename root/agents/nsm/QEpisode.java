package agents.nsm;

import framework.Episode;
import framework.Move;
import framework.SensorData;

public class QEpisode extends Episode {
    public static double DISCOUNT         =  0.8;
    public static double LEARNING_RATE    =  0.85;

    public double qValue = 0.0;
    public double reward = 0.0;

    public QEpisode(Move move) {
        super(move);
    }

    public void updateQValue(double utility)
    {
        this.qValue = (1.0 - LEARNING_RATE) * (this.qValue) + LEARNING_RATE * (this.reward + DISCOUNT * utility);
    }
}
