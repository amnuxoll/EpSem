package agents.phujus;

import framework.SensorData;

import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Vector;

/**
 * BaseRule
 *
 * describes the prior probability that a particular action will take the
 * agent to a particular outcome
 */
public class BaseRule extends Rule {

    /**
     * class ExtCond
     *
     * Tracks external conditions (LHS or RHS)
     */
    public static class ExtCond extends Confidence implements Comparable<ExtCond> {
        public String sName;  //sensor name
        public boolean val;   //sensor value

        public ExtCond(String initSName, boolean initVal) {
            this.sName = initSName;
            this.val = initVal;
        }

        /** ExtConds are equal if they have the same sName and val */
        @Override
        public boolean equals(Object o) {
            if (! (o instanceof ExtCond)) return false;
            ExtCond other = (ExtCond) o;
            return (this.sName.equals(other.sName)) && (this.val == other.val);
        }

        @Override
        public String toString() { return sName + "=" + val; }

        @Override
        public int compareTo(ExtCond o) { return this.sName.compareTo(o.sName); }

        @Override
        public int hashCode() { return Objects.hash(sName, val); }
    }//class ExtCond

    protected char action;

    //While a base rule has only an action on the LHS, it may later spawn
    // a child EpRule that needs to use LHS sensors based upon
    // the sequence of episodes that led up to this base rule
    protected Vector<HashSet<EpRule.IntCond>> lhsInternal;
    protected HashSet<ExtCond> lhsExternal;

    //The RHS of the rule consists of a set of external sensor values that were
    // present when the rule was created
    protected HashSet<ExtCond> rhsExternal;

    //Parent rule this was spawned from (this is null for BaseRule objects)
    protected BaseRule parent = null;

    //All of the children
    protected Vector<EpRule> children = new Vector<>();

    // Each rule has an activation level that tracks the frequency and
    // recency with which it fired and correctly predicted an external
    // sensor value
    public static final int ACTHISTLEN = 10;
    private final int[] lastActTimes = new int[ACTHISTLEN];  // last N times the rule was activated
    private final double[] lastActAmount = new double[ACTHISTLEN]; // amount of activation last N times
    private int nextActPos = 0;
    private double activationLevel;  //CAVEAT:  this value may not be correct!  Call calculateActivation() to update it.
    private int lastActCalcTime = -1;  //the last timestep when activation for which activation was calculated
    public static final double DECAY_RATE = 0.95;  //activation decays exponentially over time

    public BaseRule(PhuJusAgent agent) {
        super(agent);
        this.action = agent.getPrevAction();
        this.lhsExternal = initExternal(agent.getPrevExternal());
        this.lhsInternal = initInternal(agent.getAllPrevInternal());
        this.rhsExternal = initExternal(agent.getCurrExternal());
    }//BaseRule ctor

    /** converts from SensorData to HashSet<ExtCond> */
    protected HashSet<ExtCond> initExternal(SensorData sData) {
        HashSet<ExtCond> result = new HashSet<>();
        for(String sName : sData.getSensorNames()) {
            result.add(new ExtCond(sName, (Boolean)sData.getSensor(sName)));
        }

        return result;
    }//initExternal

    /**
     * initInternal
     *
     * initializes this.lhsInternal from a given set of values
     *
     */
    private Vector<HashSet<EpRule.IntCond>> initInternal(Vector<HashSet<Integer>> initLHS) {
        Vector<HashSet<EpRule.IntCond>> result = new Vector<>();
        for(HashSet<Integer> initLevel : initLHS) {
            HashSet<EpRule.IntCond> level = new HashSet<>();
            for (Integer sId : initLevel) {
                level.add(new EpRule.IntCond(sId));
            }
            result.add(level);
        }
        return result;
    }//initInternal

    /**
     * toStringIntConds
     *
     * converts a given HashSet<IntCond> to a string containing comma-separated
     * list of sensor ids and, optionally, their confidence values
     *
     * @param level the HashSet to stringify
     * @param nots whether each sId should be prepended with a bang (!)
     * @param includeConf  whether the confidence values should be included in the output
     */
    protected String toStringIntConds(HashSet<EpRule.IntCond> level, boolean nots, boolean includeConf) {
        if (level == null) return "";
        StringBuilder result = new StringBuilder();
        int count = 0;
        for (EpRule.IntCond iCond : level) {
            if (count > 0) result.append(", ");
            count++;
            if(nots) result.append('!');
            result.append(iCond.sId);
            if (includeConf) {
                result.append('$');
                result.append(String.format("%.2f", iCond.getConfidence()));
            }
        }
        return result.toString();
    }

    /**
     * sortedConds
     *
     * creates a Vector from a HashSet of ExtCond where the conditions are in sorted order
     * but with the GOAL at the end.
     * */
    protected Vector<ExtCond> sortedConds(HashSet<ExtCond> conds) {
        //Sort the vector
        Vector<ExtCond> result = new Vector<>(conds);
        Collections.sort(result);

        //Move the GOAL to the end
        int goalIndex = 0;
        for(int i = 0; i < result.size(); ++i) {
            if (result.get(i).sName.equals(SensorData.goalSensor)) {
                goalIndex = i;
                break;
            }
        }
        ExtCond goalCond = result.remove(goalIndex);
        result.add(goalCond);

        return result;
    }//sortedConds

    /** toStringShortRHS
     *
     * is a helper method for {@link #toString()} & {@link EpRule#toStringShort()} to
     * convert a RHS to a bit string
     */
    protected String toStringShortRHS(HashSet<ExtCond> rhs) {
        StringBuilder result = new StringBuilder();
        for (ExtCond eCond : sortedConds(rhs)) {
            char bit = (eCond.val) ? '1' : '0';
            result.append(bit);
        }
        return result.toString();
    }//toStringShortRHS

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder();
        result.append("#");
        if (this.ruleId < 10) result.append(" "); //to line up with two-digit rule ids
        result.append(this.ruleId);
        result.append(":    ");  //extra space so it will line up with 0-depth EpRule
        result.append(this.action);
        result.append(" -> ");
        result.append(toStringShortRHS(this.rhsExternal));
        result.append(String.format(" ^  acc=%.5f", getAccuracy()));
        return result.toString();
    }

    /**
     * calculates how closely this rule matches a given rhs sensors
     *
     * @return  a match score from 0.0 to 1.0
     */
    public double rhsMatchScore(SensorData rhsExt) {
        double sum = 0.0;

        //Compare RHS external values
        for (ExtCond eCond : this.rhsExternal) {
            if (rhsExt.hasSensor(eCond.sName)) {
                Boolean sVal = (Boolean) rhsExt.getSensor(eCond.sName);
                if (sVal == eCond.val){
                    sum += eCond.getConfidence();
                }
            }
        }

        return (sum / this.rhsExternal.size());

    }//rhsMatchScore


    /**
     * calculates how closely this rule matches a given action and lhs sensors
     * A total match score [0.0..1.0] is calculated as the product of the
     * match score for each level (this.timeDepth) of the rule.
     *
     * TODO:  I think this method (and helpers) should ignore zero-confidence conditions.
     *  - Already sort of ignores them, however it still increments count, so test if it shouldn't
     *
     * @return  a match score from 0.0 to 1.0
     */
    public double lhsMatchScore(char action, HashSet<Integer> lhsInt, SensorData lhsExt) {
        //If the action doesn't match this rule can't match at all
        if (action != this.action) return 0.0;

        return 1.0;
    }//lhsMatchScore

    /**
     * calculates how closely this rule matches another given rule
     *
     * @param other  the other rule to compare it to
     * @param depth compare up to this depth
     * @return  a match score from 0.0 to 1.0
     */
    public double compareLHS(EpRule other, int depth) {
        //actions must match
        if (this.action != other.action) return 0.0;

        return 1.0;
    }//compareLHS

    /**
     * updateConfidencesForPrediction
     *
     * adjusts the confidence values of this rule when it effectively predicts
     * the next state
     *
     * CAVEAT:  This rule should be called when the rule correctly predicted
     *          the future.  Not when it didn't.
     * CAVEAT:  Do not call this method with sensors it didn't match with!
     *
     * @param lhsInt  the internal sensors that the rule matched
     * @param lhsExt  the external sensors that the rule matched
     * @param rhsExt  the external sensors that appeared on the next timestep
     */
    public void updateConfidencesForPrediction(Vector<HashSet<Integer>> lhsInt, SensorData lhsExt, SensorData rhsExt) {
        //Update overall confidence
        this.increaseConfidence();
    }//updateConfidencesForPrediction

    /**
     * spawn
     *
     * create an EpRule from this BaseRule based on the sensors that were present when
     * the BaseRule was originally created
     * @return the resulting EpRule
     */
    public EpRule spawn() {
        EpRule child = new EpRule(this.agent, this.action, this.lhsExternal,
                this.lhsInternal, this.rhsExternal);
        this.children.add(child);
        child.parent = this;
        return child;
    }//spawn



//region Getters and Setters

    public char getAction() { return this.action; }
    public boolean hasChildren() { return this.children.size() > 0; }


    /** convert this.rhsExternal back to to a SensorData */
    public SensorData getRHSExternal() {
        SensorData result = SensorData.createEmpty();
        for(ExtCond eCond : this.rhsExternal) {
            result.setSensor(eCond.sName, eCond.val);
        }
        return result;
    }
//endregion

    //DEBUG
    public String toStringAllLHS() {
        StringBuilder result = new StringBuilder();
        for (int i = this.lhsInternal.size(); i >= 1; --i) {
            result.append('(');
            HashSet<EpRule.IntCond> level = this.lhsInternal.get(this.lhsInternal.size() - i);
            String intStr = toStringIntConds(level, false, false);
            result.append(intStr);
            result.append(')');
        }
        result.append(" // "); //time depth divider

        return result.toString();
    }

}//class BaseRule
