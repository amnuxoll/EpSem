package agents.predr;

import java.util.ArrayList;
import framework.*;
import utils.*;


/**
 * Rule
 *
 * An instance of this class contains a rule that looks like this:
 *    011.0b, 10..1a -> .1...
 * where the '.' is a wildcard that can be either 0 or 1.  The RHS of any
 * particular rule always has only one non-wildcard bit.  The LHS consists
 * of one or more episodes which may contain wildcards as well.
 *
 * @see PredrAgent to see how this is used
 *
 * @author Faltersack and Nuxoll
 */
public class Rule {
    /** a set of episodes on the left-hand-side of the rule.  Sensors that have
        been "wildcarded" are simply removed from the SensorData in the
        Episode. */
    private ArrayList<Episode> LHS;

    /** the right-hand-side of the rule.  This will only contain one
        sensor-value pair (at least for now).*/
    private SensorData RHS;

    /** index into the agent's episodic memory to the episode(s) this rule was
        derived from.
    */
    private ArrayList<Integer> epmemIndexes = new ArrayList<>();

    /**
     * Rule ctor
     *
     * Overload to initialize using a single episode.
     *
     */
    public Rule(Episode initLHS, SensorData initRHS, String sensorToTarget, int initEpmemIndex) {
        this(new ArrayList<>(), initRHS, sensorToTarget, initEpmemIndex);
        this.LHS.add(initLHS);
    }//ctor

    /**
     * Rule ctor
     *
     * initializes a new rule from a set of episodes and rhs sensors array. In
     * this ctor, it's assumed no wildcards on the LHS
     *
     * @param initLHS         the episode on the LHS of the rule (initially there is only one)
     * @param initRHS         the sensors that resulted (one of these will be used for the rule
     * @param sensorToTarget  the name of the sensor that won't be wildcarded on the RHS
     * @param initEpmemIndex  the index of the of the leftmost episode in LHS in the episodic memory
     */
    public Rule(ArrayList<Episode> initLHS, SensorData initRHS, String sensorToTarget, int initEpmemIndex) {
        this.LHS = initLHS;
        this.RHS = new SensorData(initRHS.isGoal());
        if (! sensorToTarget.equals(SensorData.goalSensor))
        {
            //in this weird case, we don't want a goal sensor at all as we are
            //effectively wildcarding it
            this.RHS.removeSensor(SensorData.goalSensor);

            //add the sensor we do want here
            this.RHS.setSensor(sensorToTarget, initRHS.getSensor(sensorToTarget));
        }
        
        this.epmemIndexes.add(initEpmemIndex);
    }//ctor
    

    /**
     * Rule ctor
     *
     * this is only used internally to created merged rule results
     *
     * @param initLHS         the sensors on the LHS of the rule
     * @param initRHS         the sensors that resulted (one of these will be used for the rule
     * @param initIndexes     the indices where the sequences in epmem exist that create this rule
     */
    private Rule(ArrayList<Episode> initLHS, SensorData initRHS, ArrayList<Integer> initIndexes) {
        this.LHS = initLHS;
        this.RHS = initRHS;
        this.epmemIndexes = initIndexes;
    }//ctor

    public ArrayList<Episode> getLHS() {
        return this.LHS;
    }
    
    public SensorData getRHS() {
        return this.RHS;
    }

    public ArrayList<Integer> getEpmemIndexes() {
        return this.epmemIndexes;
    }

    /**
     * @return true if the episodes at the end of a given episodic memory match
     * this rule
     */
    public boolean matches(EpisodicMemory<Episode> epmem) {
        if (epmem.length() < this.LHS.size())
            return false;

        for (int i = 0; i < this.LHS.size(); ++i) {
            Episode ruleEp = this.LHS.get(i);
            // getFromOffset expects 0-based offsets so we need to drop size by 1
            Episode epmemEp = epmem.getFromOffset(this.LHS.size() - 1 - i);
            SensorData ruleEpSd = ruleEp.getSensorData();
            SensorData epmemEpSd = epmemEp.getSensorData();
            
            if (! (epmemEpSd.contains(ruleEpSd)) &&
                   epmemEp.getAction().equals(ruleEp.getAction())) {
                return false;
            }
        }
        return true;
    }

    /**
     * @return true iff the actions in the LHS of this rule match the given
     * episodic memory and the RHS of this rule matches the given sensor value
     * as specified by a SensorData object and the name of a particular sensor
     * in it.
     *
     * example:
     *    epmem:  01b,00a
     *       sd:  01
     *    sName:  alpha (presuming alpha is the first sensor)
     * matches:
     *     rule:  01b,..a->0.
     */
    public boolean canMerge(EpisodicMemory<Episode> epmem,
                            SensorData sd, String sName) {

        if (epmem.length() < this.LHS.size())
            return false;

        //check a RHS sensor name match
        if (! this.RHS.hasSensor(sName)) {
            return false;
        }

        //check RHS sensor values
        Object myVal = this.RHS.getSensor(sName);
        Object sdVal = sd.getSensor(sName);
        if (!myVal.equals(sdVal)) {
            return false;
        }

        //Make sure the actions match
        for (int i = 0; i < this.LHS.size(); ++i) {
            Episode ruleEp = this.LHS.get(i);
            Episode epmemEp = epmem.getFromOffset(this.LHS.size() - 1 - i);
            if (! epmemEp.getAction().equals(ruleEp.getAction())) {
                return false;
            }
        }
        
        return true;
    }//canMerge

    
    /** @return true iff:
     *      1.  each corresponding episode on the LHS of each Rule has the same
     *          action 
     *      2.  the RHS of a given Rule matches this one
     *
     * CAVEAT:  For now, this method assumes that the LHS will always have the
     *          same sensor set
     */
    public boolean canMerge(Rule other) {
        if (other.LHS.size() != this.LHS.size()) return false;
        for(int i = 0; i < this.LHS.size(); ++i) {
            Action thisAct = this.LHS.get(i).getAction();
            Action otherAct = other.LHS.get(i).getAction();
            if (! thisAct.equals(otherAct)) return false;
        }
        return this.RHS.equals(other.RHS);
    }

    /**
     * given two rules, create a new rules with appropriate wildcards so that it
     * matches both.
     *
     * CAVEAT:  This method assumes that other {@link #canMerge} with this one
     * CAVEAT:  It is possible the merged rule with be all wildcards (no sensors
     *          in the sensor data in the LHS episode(s))
     *
     * @param other to Rule to merge with this one
     *
     * @return null if the merge fails
     */
    public Rule mergeWith(Rule other) {
        ArrayList<Episode> newLHS = new ArrayList<Episode>();
        //For each position in the 'this' and 'other' LHS, create a new episode
        //that is the intersection of the two (actions should match).  
        for(int i = 0; i < other.LHS.size(); ++i) {
            SensorData thisSD = this.LHS.get(i).getSensorData();
            SensorData otherSD = other.LHS.get(i).getSensorData();
            SensorData newSD = thisSD.intersection(otherSD);
            Episode newEp = new Episode(newSD, this.LHS.get(i).getAction());
            newLHS.add(newEp);
        }

        //make an epmem indexes list for the new rule
        ArrayList<Integer> newEI = new ArrayList<Integer>();
        newEI.addAll(this.epmemIndexes);
        newEI.addAll(other.epmemIndexes);

        Rule result = new Rule(newLHS, this.RHS, newEI);

        return result;
    }//mergeWith

    /** @return true if all SensorData objects in this rule's LHS are empty */
    public boolean isAllWildcards() {
        for(Episode ep : this.LHS) {
            if (! ep.getSensorData().isEmpty()) {
                return false;
            }
        }

        return true;
    }
    
    /** possibly useful? */
    @Override
    public String toString() {
        String result = "{ ";
        for(Episode ep : this.LHS) {
            result += ep.toString() + ", ";
        }
        result = result.substring(0, result.length() - 2);
        result += " } -> ";
        result += RHS.toString(true);
        return result;
    }//toString

    
    
}//class Rule
