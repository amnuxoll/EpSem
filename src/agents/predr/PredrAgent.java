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
     * mergeRule
     *
     * merges a given rule into the current ruleset (this.rules).
     *
     * @return true if the rule was merged successfully?
     */
    private boolean mergeRule(Rule newRule) {

        //Find the existing rule (if any) that matches the new one
        Rule match = null;
        for(Rule rule : this.rules) {
            if (rule.canMerge(newRule)) {
                match = rule;
                break;
            }
        }

        //Merge or add as needed
        if (match != null) {
            this.rules.remove(match);
            this.rules.add(match.mergeWith(newRule));

            //TODO: should check for a LHS with nothing in it (all wildcards) in
            //the new rule and extend/expand the old rule instead.  We may
            //possibly want to keep the old all wildcards rule as well just long
            //enough to be sure it's not correct.  (Can it ever be correct? not
            //sure.) This addition require multiple indexes and considerable
            //testing.  TBD.
        } else {
            this.rules.add(newRule);
        }

        return true;
    }//mergeRule

    
    /**
     * adjustRulesForNewSensorData
     *
     * Each time the agent has taken a step and receives new sensor data, the
     * rule set needs to be adjusted to reflect that the previous episode led to
     * this sensing.  This method does that. :)
     *
     * CAVEAT:  This method must be called before you create a new episode with
     * the new sensor data (i.e., before you've selected an action)
     *
     * @param sensorData the new sensor data that occured after the current episode
     */
    private void adjustRulesForNewSensorData(SensorData sensorData) {
        //Create a rule from this sensor and the previous episode
        if (this.epmem.length() > 0) {
            Episode prevEpisode = this.epmem.current();
            
            for(String sName : sensorData.getSensorNames()) {
                Rule newRule = new Rule(new Episode(prevEpisode),
                                        new SensorData(sensorData),
                                        sName,
                                        this.epmem.currentIndex());
                mergeRule(newRule);
            }
        }//if

    }//adjustRulesForNewEpisode

    /**
     * getMatchingRules
     *
     * returns a list of all the Rule objects in this.rules that match the
     * current sensing and episodic memory.
     *
     * For example, if the current sensor data is 01101  and the three most
     * recent epsiodes in episodic memory are:  00101b, 01111b, 00100a then
     * the following rules would both match:
     *    0.10.b -> 1....
     *    001..a, 0..0.a -> ...1.
     *
     * Specifically, the sensordata must match the LHS of the episode in the
     * rule that is closest to the 'arrow' in the rule.  Previous episodes in
     * the rules must match the most recent episodes in episodic memory.
     */
    public ArrayList<Rule> getMatchingRules(SensorData sensorData) {

        ArrayList<Rule> result = new ArrayList<Rule>();

        //Special case:  if the sensorData is empty, there should be no matching
        //rules
        if (sensorData.getSensorNames().size() == 0) return result;

        //Find all rules that match the sensorData
        for(Rule rule : this.rules) {
            SensorData ruleSD = rule.getLHS().get(0).getSensorData();
            if (ruleSD.contains(sensorData)) {
                result.add(rule);
            }
        }


        //TODO:  for rules with multiple episodes in the LHS they need to also
        //match the corresponding most recent episodes in episodic memory.  For
        //now, however, all our rules contain only one episode on the LHS.


        return result;
    }//getMatchingRules


    /**
     * findRuleBasedSequence
     *
     * finds a sequence of actions that can reach the goal in N steps or less
     *
     * @param steps      - max length of the sequence
     * @param sensorData - the sensor data that should match the LHS of the rules used
     *
     * TODO:  This should eventually support rules with multiple episodes on the LHS
     */
    private Sequence findRuleBasedSequence(int steps, SensorData sensorData) {
        Sequence result = new Sequence();

        //TODO:  START HERE NEXT TIME!

        return result;
    }//findRuleBasedSequence

    /**
     * Get the next move based on the provided sensorData and update this
     * agent.  Helper methods for this one: {@link #adjustRulesForNewEpisode}
     * and {@link #mergeRule}.
     *
     * @param sensorData The {@link SensorData} from the current move.
     * @return the next {@link Action} to attempt.
     * @throws Exception
     */
    public Action getNextAction(SensorData sensorData) throws Exception {
        //Now that we've taken another step, update rules based on what we've learned
        adjustRulesForNewSensorData(sensorData);

        //TODO: See if our rules suggest a good move
        
        //Get the next next move
        if (! nst.hasNext()) {
            this.nst = nstGen.nextPermutation(nstNum);
            nstNum++;
        }
        Action nextAction =  nst.next();

        //Create the new episode
        Episode nextEpisode = new Episode(sensorData, nextAction);
        this.epmem.add(nextEpisode);
        
        return nextAction;
    }//getNextAction


    
}//class PredrAgent
