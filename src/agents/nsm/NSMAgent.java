package agents.nsm;

import framework.*;

import java.util.*;

/**
 * class NSMAgent
 *
 * This agent is an implementation of Andrew McCallum's Nearest Sequence Memory
 * algorithm.
 *
 * @author: Andrew Nuxoll (with many thanks to Zach Faltersack for his original
 * implementation in C)
 *
 */
public class NSMAgent implements IAgent {
    //region Static Variables
    private static Random random = new Random();
    //endregion

    //region Class Variables
    private NHood selectedNHood;
    private double randChance;  //how frequently the agent make a random move
    private QEpisodicMemory episodicMemory;
    private int Successes = 0;
    private Move[] moves;
    private QLearningConfiguration qLearningConfiguration;
    //endregion

    //region Constructors
    /**
	 * The constructor for the agent simply initializes it's instance variables
	 */
	public NSMAgent(QLearningConfiguration qLearningConfiguration) {
	    this.qLearningConfiguration = qLearningConfiguration;
	    this.randChance = this.qLearningConfiguration.INIT_RAND_CHANCE;
	}//NSMAgent ctor
    //endregion

    //region IAgent Members
    @Override
    public void initialize(Move[] moves, IIntrospector introspector) {
        this.moves = moves;
        this.episodicMemory = new QEpisodicMemory();
    }

    @Override
    public Move getNextMove(SensorData sensorData) {
        if (this.episodicMemory.any()) {
            if (sensorData.isGoal()) {
                this.Successes++;
                if (this.randChance > this.qLearningConfiguration.MIN_RAND_CHANCE) {
                    this.randChance *= this.qLearningConfiguration.RAND_DECREASE;
                }
            }
            // Update the q-values for the previous iteration of this loop
            this.updateAllLittleQ();
        }

        Move move = this.selectNextMove();
        this.episodicMemory.add(new QEpisode(sensorData, move, this.qLearningConfiguration.REWARD_SUCCESS, this.qLearningConfiguration.REWARD_FAILURE));
        return move;
    }//exploreEnvironment
    //endregion

    //region Private Methods
    private Move selectNextMove() {
        // We can't use NSM until we've found the goal at least once
        // (if not using random action) select the action that has the neighborhood with the highest Q-value
        if(this.Successes > 0 && NSMAgent.random.nextDouble() >= this.randChance) {
            this.selectedNHood = this.getBestNeighborhood();
            return this.selectedNHood.getMove();
        }//if
        this.selectedNHood = null;
        return this.moves[NSMAgent.random.nextInt(this.moves.length)];
    }

    /**
     * populateNHoods
     *
     * creates a neighborhood of k-nearest NBors for each action.  The NHoods
     * must be regenerated each time that that a new episode is added to the
     * store.
     */
    private NHood getBestNeighborhood() {
        //Create a new neighborhood for each command
        NHood bestNHood = null;
        for (Move move : this.moves)
        {
            NHood nhood = this.episodicMemory.buildNeighborhoodForMove(move);
            if (bestNHood == null || nhood.getQValue() > bestNHood.getQValue())
                bestNHood = nhood;
        }//for
        return bestNHood;
    }//populateNHoods

    /**
     * updateAllLittleQ
     *
     * This function will update the expected future discounted rewards for the
     * action that was most recently executed. We cannot guarantee that the
     * chosen action was not selected randomly because of the exploration
     * rate. To account for this we will index into the vector of neighborhoods
     * and update the neighborhood relevant to the executed action.
     *
     * @arg ep A pointer to the episode containing the most recently executed action
     */
    private void updateAllLittleQ() {
        if (this.selectedNHood == null)
            return;
        // Recalculate the Q value of the neighborhood associated with the
        // episode's action
        double utility = this.selectedNHood.getQValue();

        // Update the q values for each of the voting episodes for the most
        // recent action
        for(int i = 0; i < this.selectedNHood.nbors.size(); ++i) {
            //Update the root episode
            NBor nbor = this.selectedNHood.nbors.get(i);
            nbor.qEpisode.updateQValue(utility); // <--- TODO Is this really necessary? I feel like this is a bug
                                                 // that got ported over
            //Update all the root's predecessors that participated in the match
            double prevUtility = utility;
            for(int j = 0; j < nbor.len; ++j)
            {
                QEpisode prevEp = this.episodicMemory.getFromOffset(j);
                prevEp.updateQValue(prevUtility);
                prevUtility = prevEp.qValue;
            }
        }//for
    }//updateAllLittleQ
    //endregion
}//class NSMAgent
