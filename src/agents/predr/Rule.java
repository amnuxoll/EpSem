package agents.predr;

import java.util.ArrayList;
import framework.*;


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
    private ArrayList<Episode> LHS = new ArrayList<Episode>();

    /** the right-hand-side of the rule.  This will only contain one
        sensor-value pair (at least for now).*/
    private SensorData RHS = null;

    /** index into the agent's episodic memory where this rule was derived
        from.
        TODO:  This should be an arraylist since a rule can be derived from
               multiple epsiodes.  For now, a single index is enough and we
               are using -1 to indicate that a rule is the result of merging two
               other rules.
    */
    private int epmemIndex;

    /**
     * Rule ctor
     *
     * initializes a new rule from a single episode and rhs sensors array. In
     * this ctor, it's assumed no wildcards on the LHS
     *
     * @param initLHS         the episode on the LHS of the rule (initially there is only one)
     * @param initRHS         the sensors that resulted (one of these will be used for the rule
     * @param sensorToTarget  the name of the sensor that won't be wildcarded on the RHS
     * @param initEpmemIndex  the index of the of the leftmost episode in LHS in the episodic memory
     */
    public Rule(Episode initLHS, SensorData initRHS, String sensorToTarget, int initEpmemIndex) {
        this.LHS.add(initLHS);
        this.RHS = new SensorData(initRHS.isGoal());
        if (! sensorToTarget.equals(SensorData.goalSensor))
        {
            //in this weird case, we don't want a goal sensor at all as we are
            //effectively wildcarding it
            this.RHS.removeSensor(SensorData.goalSensor);

            //add the sensor we do want here
            this.RHS.setSensor(sensorToTarget, initRHS.getSensor(sensorToTarget));
        }
        this.epmemIndex = initEpmemIndex;
    }//ctor

    /**
     * Rule ctor
     *
     * this is only used internally to created merged rule results
     *
     * @param initLHS         the sensors on the LHS of the rule
     * @param initMove        the move taken on the LHS
     * @param initRHS         the sensors that resulted (one of these will be used for the rule
     */
    private Rule(ArrayList<Episode> initLHS, SensorData initRHS) {
        this.LHS = initLHS;
        this.RHS = initRHS;
        this.epmemIndex = -1; //TODO:  fix this someday
    }//ctor

    public ArrayList<Episode> getLHS() {
        return this.LHS;
    }
    
    public SensorData getRHS() {
        return this.RHS;
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
     * CAVEAT:  For now, this method assumes that the LHS will always have the
     *          same sensor set
     *
     * @param other to Rule to merge with this one
     *
     * @return null if the merge fails
     */
    public Rule mergeWith(Rule other) {
        ArrayList<Episode> newLHS = new ArrayList<Episode>();
        //For each position in the 'this' and 'other' LHS, create a new episode
        //that is the intersection of the two (actions should match)
        for(int i = 0; i < other.LHS.size(); ++i) {
            SensorData thisSD = this.LHS.get(i).getSensorData();
            SensorData otherSD = other.LHS.get(i).getSensorData();
            SensorData newSD = thisSD.intersection(otherSD);
            Episode newEp = new Episode(newSD, this.LHS.get(i).getAction());
            newLHS.add(newEp);
        }

        Rule result = new Rule(newLHS, this.RHS);

        return result;
    }//mergeWith

    
}//class Rule
