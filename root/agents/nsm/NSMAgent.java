package agents.nsm;

import framework.*;

import java.io.FileWriter;
import java.io.IOException;
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



    /**
     * ************************************************************************************
     * VARIABLES
     * ************************************************************************************
     */
    // Defines for Q-Learning algorithm
    public static double REWARD_SUCCESS   =  1.0;
    public static double REWARD_FAILURE   = -0.1;
    public static double INIT_RAND_CHANCE =  0.8;
    public static double RAND_DECREASE    =  0.95;  //mult randChance by this
                                                    //value at each goal
    public static double MIN_RAND_CHANCE  =  0.0;

    public static final int MAX_EPISODES = 2000000;
    public static final int NUM_GOALS = 1000;

    protected HashMap<Move, NHood> nhoods;
    protected double randChance = INIT_RAND_CHANCE;  //how frequently the agent
                                                     //make a random move
    public static int informationColumns = 0; //for now before consolidation of recording data must be declared in each agent
    protected ArrayList<QEpisode> episodicMemory;

    protected int Successes = 0;

    /**
	 * The constructor for the agent simply initializes it's instance variables
	 */
	public NSMAgent() {
	}//NSMAgent ctor

    private Move[] moves;

    @Override
    public void initialize(Move[] moves)
    {
        this.moves = moves;
        informationColumns = 2;

        nhoods = new HashMap<>();
        episodicMemory = new ArrayList<>();
        episodicMemory.clear();
    }

    /**
     * populateNHoods
     *
     * creates a neighborhood of k-nearest NBors for each action.  The NHoods
     * must be regenerated each time that that a new episode is added to the
     * store.
     */
    public void populateNHoods() {
        //Create a new neighborhood for each command
        for (Move move : this.moves)
        {
            NHood nHood = new NHood(move);
            nhoods.put(move, nHood);

            //temporarily set the to-be-issued command to this value
            episodicMemory.add(new QEpisode(move));

            //find the kNN
            for(int i = 0; i <= episodicMemory.size() - 2; ++i) {
                int matchLen = matchedMemoryStringLength(i);
                if ((matchLen > 0) && ((nHood.shortest <= matchLen) || (nHood.nbors.size() < nHood.K_NEAREST))) {
                    nHood.addNBor(new NBor(i, matchLen, this.episodicMemory.get(i)));
                }
            }//for

            episodicMemory.remove(episodicMemory.size() - 1);
        }//for
    }//populateNHoods

    /**
     * matchedMemoryStringLength
     *
     * Starts from a given index and the end of the Agent's episodic memory and
     * moves backwards, comparing each episode to the present episode and it
     * prededessors until the corresponding episdoes no longer match.
     *
     * @param endOfStringIndex The index from which to start the backwards search
     * @return the number of consecutive matching characters
     */
    protected int matchedMemoryStringLength(int endOfStringIndex) {
        int length = 0;
        int indexOfMatchingAction = episodicMemory.size() - 1;
        if (!episodicMemory.get(indexOfMatchingAction).getMove().equals(episodicMemory.get(endOfStringIndex).getMove()))
            return 0;
        endOfStringIndex--;
        indexOfMatchingAction--;
        for (int i = endOfStringIndex; i >= 0; i--) {
            //We want to compare the command from the prev episode and the
            //sensors from the "right now" episode to the sequence at the
            //index indicated by 'i'
            if (episodicMemory.get(indexOfMatchingAction).equals(episodicMemory.get(i))) {
                length++;
                indexOfMatchingAction--;
            }
            else {
                return length;
            }
        }//for

        return length;
    }//matchedMemoryStringLength

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
    public void updateAllLittleQ(QEpisode ep)
    {
        // Recalculate the Q value of the neighborhood associated with the
        // episode's action
        NHood nhood = nhoods.get(ep.getMove());
        double utility = nhood.getQValue();

        // Update the q values for each of the voting episodes for the most
        // recent action
        for(int i = 0; i < nhood.nbors.size(); ++i) {
            //Update the root episode
            NBor nbor = nhood.nbors.get(i);
            int index = nbor.end - i;
            if (index < 0)
                continue;  //don't fall off the end
            episodicMemory.get(index).updateQValue(utility);
            double prevUtility = utility;

            //Update all the root's predecessors that participated in the match
            for(int j = 1; j < nbor.len; ++j)
            {
                QEpisode prevEp = episodicMemory.get(episodicMemory.size() - j);
                prevEp.updateQValue(prevUtility);
                prevUtility = prevEp.qValue;
            }
        }//for

        // Update the given (most recent) episode's Q value
        ep.updateQValue(utility);

    }//updateAllLittleQ

    //Used to print steps to success for each goal at the console for teh HOO-maans
    int lastSuccess = 0;

    public static Random random = new Random();

    @Override
    public Move getNextMove(SensorData sensorData) throws Exception
    {
        if (episodicMemory.size() > 0) {
            QEpisode episode = episodicMemory.get(episodicMemory.size() - 1);
            episode.setSensorData(sensorData);

            if (sensorData.isGoal()) {
                episode.reward = REWARD_SUCCESS;
                Successes++;
                if (randChance > MIN_RAND_CHANCE) {
                    randChance *= RAND_DECREASE;
                }

                //Inform the user of steps that were required
                //System.out.print(episodicMemory.size() - lastSuccess);
                //System.out.print(",");
                lastSuccess = episodicMemory.size();
            } else {
                episode.reward = REWARD_FAILURE;
            }
            //Update the q-values for the previous iteration of this loop
            if (nhoods.size() > 0)
                updateAllLittleQ(episode);
        }

        Move move = this.moves[random.nextInt(this.moves.length)];  //default is random for now

        // We can't use NSM until we've found the goal at least once
        if(Successes > 0) {
            // Ensure there isn't anything to incorrectly update should we choose a random move.
            nhoods.clear();
            // (if not using random action) select the action that has the
            // neighborhood with the highest Q-value
            if (random.nextDouble() >= this.randChance) {
                populateNHoods();
                double bestQ = 0;
                Move bestMove = null;
                for (Move moveToConsider : nhoods.keySet())
                {
                    NHood nHood = nhoods.get(moveToConsider);
                    double qVal = nHood.getQValue();
                    if (bestMove == null || qVal > bestQ)
                    {
                        bestQ = qVal;
                        bestMove = nHood.getMove();
                    }
                }
                move = bestMove;
            }//if
        }//if

        episodicMemory.add(new QEpisode(move));
        return move;
    }//exploreEnvironment

    @Override
    public void addAgentListener(IAgentListener listener) {

    }

    @Override
    public String getMetaData() {
        return "";
    }
}//class NSMAgent
