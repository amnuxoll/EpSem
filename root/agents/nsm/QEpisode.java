package agents.nsm;

import framework.Episode;
import framework.Move;
import framework.SensorData;

/**
 *
 * @author Zachary Paul Faltersack
 * @version 0.95
 */
public class QEpisode extends Episode {
    //region Static Variables
    public static double DISCOUNT         =  0.8;
    public static double LEARNING_RATE    =  0.85;
    //endregion

    //region Class Variables
    public double qValue = 0.0;
    private double reward = 0.0;
    private double successReward;
    private double failureReward;
    //endregion

    //region Constructors
    public QEpisode(Move move, double successReward, double failureReward) {
        super(move);
        this.successReward = successReward;
        this.failureReward = failureReward;
    }
    //endregion

    //region Episode Overrides
    @Override
    public void setSensorData(SensorData sensorData) {
        super.setSensorData(sensorData);
        if (sensorData.isGoal())
            this.reward = this.successReward;
        else
            this.reward = this.failureReward;
    }
    //endregion

    //region Public Methods
    public void updateQValue(double utility)
    {
        this.qValue = (1.0 - LEARNING_RATE) * (this.qValue) + LEARNING_RATE * (this.reward + DISCOUNT * utility);
    }
    //endregion
}
