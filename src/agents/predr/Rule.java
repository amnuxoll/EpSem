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
    private ArrayList<SensorData> LHSsensors = new ArrayList<SensorData>();
    private ArrayList<Move> LHSmoves = new ArrayList<Move>();
    private SensorData RHS = null;
    private int epmemIndex;

    /**
     * Rule ctor
     *
     * initializes a new rule from a single episode and rhs sensors array. In
     * this ctor, it's assumed no wildcards on the LHS
     *
     * @param initLHS         the sensors on the LHS of the rule
     * @param initMove        the move taken on the LHS
     * @param initRHS         the sensors that resulted (one of these will be used for the rule
     * @param sensorToTarget  the name of the sensor that won't be wildcarded on the RHS
     * @param initEpmemIndex  the index of the of the leftmost episode in LHS in the episodic memory
     */
    public Rule(SensorData initLHS, Move initMove, SensorData initRHS, String sensorToTarget, int initEpmemIndex) {
        this.LHSsensors.add(initLHS);
        this.LHSmoves.add(initMove);
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
    private Rule(ArrayList<SensorData> initLHSsensors, ArrayList<Move> initLHSmoves, SensorData initRHS) {
        this.LHSsensors = initLHSsensors;
        this.LHSmoves = initLHSmoves;
        this.RHS = initRHS;
        this.epmemIndex = -1; //TODO:  this won't work!!  
    }//ctor

    public ArrayList<SensorData> getLHS() {
        return this.LHSsensors;
    }
    
    public ArrayList<Move> getMoves() {
        return this.LHSmoves;
    }
    
    public SensorData getRHS() {
        return this.RHS;
    }
    
    /** possibly useful? */
    @Override
    public String toString() {
        String result = "{ ";
        for(int i = 0; i < LHSsensors.size(); ++i) {
            result += LHSsensors.get(i).toString(true);
            result += LHSmoves.get(i).getName();
            if (i < LHSsensors.size() - 1) {
                result += ", ";
            }
        }
        result += " } -> ";
        result += RHS.toString(true);
        return result;
    }//toString

    /** @return true if the RHS of a given Rule matches this one
     * CAVEAT:  For now, this method assumes that the LHS will always have the
     *          same sensor set
     */
    public boolean canMerge(Rule other) {
        if (other.LHSsensors.size() != this.LHSsensors.size()) return false;
        if (other.LHSmoves.size() != this.LHSmoves.size()) return false;
        if (! other.LHSmoves.get(0).equals(this.LHSmoves.get(0))) return false;
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
        ArrayList<SensorData> newLHSsensors = new ArrayList<SensorData>();
        for(int i = 0; i < other.LHSsensors.size(); ++i) {
            newLHSsensors.add(this.LHSsensors.get(i).intersection(other.LHSsensors.get(i)));
        }

        Rule result = new Rule(newLHSsensors, this.LHSmoves, this.RHS);

        return result;
    }//mergeWith
    
}//class Rule
