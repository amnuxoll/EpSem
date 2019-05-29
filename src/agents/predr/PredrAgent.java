package agents.predr;

import java.util.ArrayList;
import framework.*;
import utils.*;

/**
 * Predictor Agent
 *
 * This agent uses episodes like this:
 *    01100a -> 00010
 *    00011b -> 01011
 *    10101a -> 00000
 *
 * To generate rules that look like this:
 *    10..1a -> .1...
 * where the '.' is a wildcard that can be either 0 or 1.  The RHS of any
 * particular rule always has only one non-wildcard bit.  Furthermore, there
 * only one rule per bit position/value.  Thus, for example, if there are five
 * binary sensors and three actions there can only be 5 x 2 x 3 = 30 rules at
 * maximum.
 *
 * The rules are used to predict the outcome of an action.  For example if my
 * current sensors are 00010 and I have a rule like 0..1.a -> .1... Then I
 * predict that the second sensor will turn on if I take action 'a'.
 *
 * @author Faltersack and Nuxoll
 */
public class PredrAgent implements framework.IAgent {

    private Action[] actions = null;
    /** used to generate the "next sequence to try" */
    private SequenceGenerator nstGen = null;
    /** index of the "next sequence to try" in nstGen */
    private long nstNum = 1;  
    /** current "next sequence to try" */
    private Sequence nst = null;
    /** the agent has an episodic memory of course */
    private EpisodicMemory<Episode> epmem = new EpisodicMemory<Episode>();
    /** all the rules that this agent has created so far */
    private ArrayList<Rule> rules = new ArrayList<Rule>();
    
    /**
     * Set the available {@link Action}s for the agent in the current environment.
     * @param actions An array of {@link Action} representing the actions available to the agent.
     */
    public void initialize(Action[] actions, IIntrospector introspector) {
        this.actions = actions;
        this.nstGen = new SequenceGenerator(actions);
        this.nst = nstGen.nextPermutation(nstNum);
        nstNum++;
    }

    /**
     * Get the next move based on the provided sensorData.
     * @param sensorData The {@link SensorData} from the current move.
     * @return the next {@link Action} to attempt.
     * @throws Exception
     */
    public Action getNextMove(SensorData sensorData) throws Exception {
        //first call will give me null for initialization but I don't care
        if (sensorData == null) return null;

        //Get the next next move
        if (! nst.hasNext()) {
            this.nst = nstGen.nextPermutation(nstNum);
            nstNum++;
        }
        Action nextAction =  nst.next();

        //Add a new episode
        Episode nextEpisode = new Episode(sensorData, nextAction);

        //Create a rule from this sensor and the previous episode
        if (this.epmem.length() > 0) {
            Episode prevEpisode = this.epmem.current();
            
            for(String sName : sensorData.getSensorNames()) {
                Rule newRule = new Rule(prevEpisode.getSensorData(),
                                        prevEpisode.getAction(),
                                        sensorData,
                                        sName,
                                        this.epmem.currentIndex());
                this.rules.add(newRule);
            }
        }//if

        this.epmem.add(nextEpisode);
        return nextAction;
    }//getNextMove


    
}//class PredrAgent
