package agents.nsm;

import framework.Episode;
import framework.Move;

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
    public double reward = 0.0;
    //endregion

    //region Constructors
    public QEpisode(Move move) {
        super(move);
    }
    //endregion

    //region Public Methods
    public void updateQValue(double utility)
    {
        this.qValue = (1.0 - LEARNING_RATE) * (this.qValue) + LEARNING_RATE * (this.reward + DISCOUNT * utility);
    }
    //endregion
}
