package agents.nsm;

/**
 *
 * @author Zachary Paul Faltersack
 * @version 0.95
 */
public class QLearningConfiguration {
    //region Class Variables
    public double REWARD_SUCCESS   =  1.0;
    public double REWARD_FAILURE   = -0.1;
    public double INIT_RAND_CHANCE =  0.8;
    public double RAND_DECREASE    =  0.95;  //mult randChance by this value at each goal
    public double MIN_RAND_CHANCE  =  0.0;
    //endregion
}
