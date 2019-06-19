package agents.predr;

import java.util.ArrayList;
import java.util.HashMap;
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
    /** we need to keep track of the longest rule we've seen so far */
    private int longestRule = 1;
    /** keeps track of whether the current nst is generated from rules or from
       the sequence generator */
    private boolean inRuleSeq = false;

    /** should only be used for unit tests!
     *  creates an agent at the point it would be at if it had experienced the
     *  given episodic memory
     */
    public void initWithEpmem(ArrayList<Episode> initEps) {
        this.epmem = new EpisodicMemory<Episode>();
        this.rules.clear();
        this.nstNum = 1;
        this.nst = nstGen.nextPermutation(nstNum);

        //this should work...I think
        for(Episode ep : initEps) {
            adjustRulesForNewSensorData(ep.getSensorData());
            this.epmem.add(ep);
        }

        //DEBUG
        for(Rule r : this.rules) {
            System.err.println("INIT RULE: " + r);
        }
        
    }
    
    /** should only be used for unit tests!
     *
     *  @return the current sequence
     */
    public Sequence getNST() { return this.nst; }
    
    /** should only be used for unit tests!
     *
     *  sets the current value of nstNum
     */
    public void setNSTNum(long initVal) {
        this.nstNum = initVal;
        this.nst = nstGen.nextPermutation(nstNum);
    }
    
    /** should only be used for unit tests!
     *
     *  @return the rules
     */
    public ArrayList<Rule> getRules() { return this.rules; }
    
    
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
            Rule mergedRule = match.mergeWith(newRule);
            if (! mergedRule.isAllWildcards()) {
                this.rules.remove(match);
                this.rules.add(mergedRule);
            } else {
                return false;
            }

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
     * buildRuleFromIndex
     *
     * creates a new Rule that contains a subsequence of episodes from
     * this.epmem.  These will be copies (clones) so they can be modified. 
     *
     * @param epIndex - index of the first episode in the subsequence
     * @param length  - number of episodes to retrieve
     * @param sensorData - the sensorData template for the RHS
     * @param sName      - which sensor from sensorData to use
     *
     * @return the newly created Rule object
     */
    private Rule buildRuleFromIndex(int epIndex, int length, SensorData sensorData, String sName) {
        Episode[] newLHSArr = this.epmem.subset(epIndex, epIndex + length);
        ArrayList<Episode> newLHS = new ArrayList<Episode>();
        for(Episode ep : newLHSArr) {
            newLHS.add(new Episode(ep));
        }
        Rule newRule = new Rule(newLHS, sensorData, sName, epIndex);

        return newRule;
        
    }//buildRuleFromIndex

    /**
     * extendRule    <!-- RECURSIVE! -->
     *
     * Given a rule that is all wildcards, this method pulls it apart into into
     * its constituents, extends each one and then remerges them together.  This
     * can result in multiple rules since extended rules constituents may no
     * longer be compatible.  this.rules will be updated as a result of this
     * call. 
     *
     *  Example:
     *     original rule:  ..a->.1
     *     constituents:   00a->.1,  11a->.1, 01a->.1
     *     extended:       01b,00a->.1, 10a,11a->.1, 10a,01a->.1
     *     re-merged:      01b,00a->.1, 10a,.1a->.1
     *
     * Note:  it's possible one of the re-merged rules will still be all
     * wildcards.  In this case a recursive call is made to extend again.
     * 
     */
    private void extendRule(Rule mergedRule, SensorData sensorData, String sName) {
        //First, split apart the old rule into its consituents and extend
        //each consituent by one action.  Then sort the results by
        //what action is being prepended
        HashMap< Action, ArrayList<Rule> > cons = new HashMap<>();
        for(int epIndex : mergedRule.getEpmemIndexes()) {
            //corner case:  if the rule can't be extended it has to be
            //discarded
            if (epIndex == 0) continue;

            //Create an extended version of the rule and add to the list
            Rule consRule = buildRuleFromIndex(epIndex - 1,
                                               mergedRule.getLHS().size() + 1,
                                               sensorData,
                                               sName);

            //Extract the new extended action to use as a hashmap key
            Action key = consRule.getLHS().get(0).getAction();

            //Add the new rule to the hashmap
            ArrayList<Rule> al = null;
            if (! cons.containsKey(key)) {
                cons.put(key, new ArrayList<Rule>());
            }
            al = cons.get(key);
            al.add(consRule);
        }//for

        //Second, re-merge each subset of rules and add them to this.rules
        for(ArrayList<Rule> sublist : cons.values()) {
            mergedRule = sublist.remove(0);
            while(! sublist.isEmpty()) {
                Rule mergeMe = sublist.remove(0);
                mergedRule = mergedRule.mergeWith(mergeMe);
            }

            //If the result is still all wildcards we have to extend again
            if (mergedRule.isAllWildcards()) {
                extendRule(mergedRule, sensorData, sName); //recurse
            } else {
                this.rules.add(mergedRule);
            }

        }

    }//extendRule
    
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

        //Need at least one episode
        if (this.epmem.length() == 0) return;
        
        //We will need to create/merge a new rule for each sensor
        for(String sName : sensorData.getSensorNames()) {
        
            //Find the one matching rule whose LHS is from our current episodes and
            //RHS is the given sensorData
            Rule matchingRule = null;
            for (Rule rule : this.rules) {
                if (rule.canMerge(this.epmem, sensorData, sName)) {
                    matchingRule = rule;
                    break;
                }
            }

            //If no match was found this is a new rule and no merge is needed
            if (matchingRule == null) {
                Rule newRule = new Rule(new Episode(this.epmem.current()),
                                        new SensorData(sensorData),
                                        sName,
                                        this.epmem.currentIndex());
                this.rules.add(newRule);
                continue;
            }

            //Remove the old rule as we'll be replacing it
            this.rules.remove(matchingRule);

            
            //We need to merge, so create a new rule of the same length from the
            //current position in this.epmem and the given SensorData
            int newIndex = this.epmem.length() - matchingRule.getLHS().size();
            Rule newRule = buildRuleFromIndex(newIndex,
                                              matchingRule.getLHS().size(),
                                              sensorData,
                                              sName); 

            //Merge our new rule with the one that currently matches.
            Rule mergedRule = matchingRule.mergeWith(newRule);

            //If there are any sensor values left the merge has
            //succeeded and we are done
            if (! mergedRule.isAllWildcards()) {
                this.rules.add(mergedRule);
                continue;
            }

            //If we reach this point, the merge failed.  So the merged rule
            //needs to be extended
            extendRule(mergedRule, sensorData, sName);
            
        }//for (each sName)



    }//adjustRulesForNewEpisode


    /**
     * findRuleBasedSequence       ***RECURSIVE (w/ 3 base cases)***
     *
     * finds a sequence of actions that can reach the goal in N steps or less
     *
     * @param steps      - max length of the sequence
     * @param sensorData - the sensor data that should match the LHS of the rules used
     *
     * TODO:  The candNextSteps variable should be a HashTable<ArrayList<Rule>>
     *        where each arraylist contains all the rules whose LHS action
     *        sequences match.  This allows us to dodge the dodginess with
     *        lastRuleUsed and shortestRule below.
     * TODO:  This should eventually support rules with multiple episodes on the LHS
     *
     * @return the shortest valid seuqence to goal (according to the rules) or
     *         an empty sequence if one is not found.
     */
    private Sequence findRuleBasedSequence(int steps, SensorData sensorData, String debug) {
        if (steps == 0) return Sequence.EMPTY;  // base case

        //Search all the rules to find the ones where the LHS matches the given
        //sensor data
        ArrayList<Rule> candNextSteps = new ArrayList<Rule>();
        for (Rule rule : rules) {
            Episode ep = rule.getLHS().get(0);  //punt: this assumes only one ep in LHS
            if (! sensorData.contains(ep.getSensorData())) continue;

            //if this rule leads to a goal then we're done
            if ( (rule.getRHS().hasSensor(SensorData.goalSensor))
                  && (rule.getRHS().isGoal()) ) {
                
                return Sequence.EMPTY.buildChildSequence(ep.getAction());
            } else {
                candNextSteps.add(rule);
            }
        }//for

        //If no matching rules were found we are finished
        if (candNextSteps.isEmpty()) {
            return Sequence.EMPTY;  //base case #2
        }

        //If we reach this point, no single step paths to goal were found but
        //any matching rules are in candNextSteps.  So, we make a recursive call
        //with each and keep the the shortest sequence that we get back
        Sequence shortestSeq = null;
        Rule shortestRule = null;  // rule whose LHS goes with shortestSeq result
        for(Action act : this.actions) {
            
            SensorData sd = new SensorData(true);
            sd.removeSensor(SensorData.goalSensor);
            Rule lastRuleUsed = null;
            for(Rule nextStep : candNextSteps) {
                //we can't combined RHS of rules unless they all have the same
                //action on the LHS (punt:  in the future there will be a
                //sequence of actions on the LHS that all need to match)
                if (nextStep.getLHS().get(0).getAction().equals(act)) {
                    SensorData rhs = nextStep.getRHS();
                    for(String name : rhs.getSensorNames()) {
                        sd.setSensor(name, rhs.getSensor(name));
                    }

                    //We need to save a ref to one of the rules that was used
                    //here so we can can use the rule to build a sequence after
                    //the loop is done.
                    lastRuleUsed = nextStep;
                }
            }

            //make a recursive call if any rules were applied for this action
            if (lastRuleUsed != null) {
                Sequence result = findRuleBasedSequence(steps - 1, sd, debug + act.getName());

                //is this the shortest one we've seen so far?
                if (! result.equals(Sequence.EMPTY)) {
                    if ((shortestSeq == null)
                        || (shortestSeq.getLength() > result.getLength()) ) {
                        shortestSeq = result;
                        //save this to build a sequence with
                        shortestRule = lastRuleUsed;  
                    }
                }
            }
                
        }//for
        
        //At this point we have the shortest sequence and the rule that led to
        //it so we can prepend the rule's action to complete the sequence
        if (shortestSeq != null) {
            Sequence front = new Sequence(EpisodeUtils.selectMoves(shortestRule.getLHS()));
            Sequence result = front.concat(shortestSeq);
            return result;
        }

        return Sequence.EMPTY;  //no paths found (also a base case)
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

        //DEBUG
        System.err.println("----------------------------------------------------------------------");
        for(Rule r : this.rules) {
            System.err.println("RULE: " + r);
        }
        
        //DEBUG
        System.err.println();
        System.err.println("epmem: " + this.epmem);
        System.err.println("sensorData: " + sensorData);

        //If we are not currently following a rule-based sequence, see if our
        //rules suggest a good one
        if ( (! this.inRuleSeq) || (! nst.hasNext()) ) {

            Sequence rulesSeq = findRuleBasedSequence(nst.stepsRemaining(), sensorData, ">");

            if (! rulesSeq.equals(Sequence.EMPTY)) {
                //DEBUG
                System.err.println("Using rules-based sequence: " + rulesSeq);

                this.nst = rulesSeq;

                //If we weren't in a rule sequence before then we are
                //interrupting a permutation-based sequence and need to set the
                //system up to return to the permutation later
                if (! this.inRuleSeq) {
                    nstNum--;
                    this.inRuleSeq = true;
                }
            }
            //DEBUG
            else {
                System.err.println("No rules-based sequence found.");
            }
        }//if need to check for rules-based seq

        //If there is no next move available then it's time for the next
        //permutation-based seuqence
        if (! nst.hasNext()) {
            this.inRuleSeq = false;
            this.nst = nstGen.nextPermutation(nstNum);
            nstNum++;

            //DEBUG
            System.err.println("Using permutation-based sequence: " + nst);
        }
            
            

        Action nextAction =  nst.next();

        //DEBUG
        System.err.println("Action selected:" + nextAction);

        //Create the new episode
        Episode nextEpisode = new Episode(sensorData, nextAction);
        this.epmem.add(nextEpisode);
        
        return nextAction;
    }//getNextAction


    
}//class PredrAgent
