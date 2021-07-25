package agents.phujus;

import framework.SensorData;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Random;

/**
 * class EpRule
 *
 * This is a replacement for Rule that is similar to a weighted episode.
 */
public class EpRule {

    /**
     * class Condition
     *
     * tracks a LHS or RHS value and the rule's confidence in it
     */
    public abstract class Condition {
        public static final int TRUEMASK  = 0b00010000;
        public static final int MAXVAL = 0b00011111;
        //TODO:  generate the value of MAXVAL and TRUEMASK from a size parameter

        private int conf = MAXVAL; //start will full confidence (optimistic)

        public void increaseConfidence() {
            this.conf = this.conf >>> 1;
            this.conf |= TRUEMASK;
        }

        public void decreaseConfidence() {
            this.conf = this.conf >>> 1;
        }

        public double getConfidence() {
            return ((double)conf) / ((double)MAXVAL);
        }
    }//class Condition

    /**
     * class ExtCond
     *
     * Tracks external conditions (LHS or RHS)
     */
    public class ExtCond extends Condition {
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
    }//class ExtCond

    /**
     * class IntCond
     *
     * Tracks internal conditions (LHS or RHS)
     */
    public class IntCond extends Condition {
        public int sId;

        public IntCond(int initSId) {
            this.sId = initSId;
        }

        /** IntConds are equal if they have the same sId */
        @Override
        public boolean equals(Object o) {
            if (! (o instanceof IntCond)) return false;
            IntCond other = (IntCond) o;
            return (this.sId == other.sId);
        }
    }//class IntCond


    /** base reward for a rule that correctly predicts finding the goal. */
    public static final double FOUND_GOAL_REWARD = 20.0;

    //to assign a unique id to each rule this shared variable is incremented by the ctor
    private static int nextRuleId = 1;

    //same as the agent so it can be seeded for consistent behavior
    private static final Random rand = PhuJusAgent.rand;

    //The agent using this rule
    private final PhuJusAgent agent;

    //each rule has a unique integer id
    private final int ruleId;

    //define the LHS of the rule.  This consists of:
    // - a set of internal sensor values indicating what other rules fired in the previous timestep
    // - a set of external sensor values that were present in the previous timestep
    // - the action that the agent took just before this rule was created
    //Initially, all sensor values are present but, over time, they may get culled if they
    //prove inconsistent with the agent's experiences
    private HashSet<IntCond> lhsInternal;
    private HashSet<ExtCond> lhsExternal;
    private final char action;

    //The RHS of the rule consists of a set of external sensor values that were
    // present when the rule was created
    private HashSet<ExtCond> rhsExternal;

    /**
     * this ctor initializes the rule from the agent's current and previous episodes
     */
    public EpRule(PhuJusAgent agent){
        this.agent = agent;
        this.ruleId = EpRule.nextRuleId++;
        this.action = agent.getPrevAction();
        this.lhsExternal = initExternal(agent.getPrevExternal());
        this.lhsInternal = initInternal(agent.getPrevInternal());
        this.rhsExternal = initExternal(agent.getCurrExternal());
    }

    private HashSet<ExtCond> initExternal(SensorData lhs) {
        HashSet<ExtCond> result = new HashSet<>();
        for(String sName : lhs.getSensorNames()) {
            result.add(new ExtCond(sName, (Boolean)lhs.getSensor(sName)));
        }

        return result;
    }//initExternal

    /**
     * initInternal
     *
     * initializes this.lhsInternal from a given set of values
     *
     */
    private HashSet<IntCond> initInternal(HashMap<Integer, Boolean> map) {
        HashSet<IntCond> result = new HashSet<>();
        for(Integer key : map.keySet()) {
            if ((Boolean)map.get(key)) {  //only care about true's
                result.add(new IntCond(key));
            }
        }
        return result;
    }//initInternal

    /**
     * General format: [rule id]: ([internal lhs])|[external lhs] -> [ext rhs] ^ [activation]
     * Each condition consists of a value$confidence.  An external condition with
     * a false value is preceded by a '!'
     *
     */
    @Override
    public String toString() {
        //rule number
        StringBuilder result = new StringBuilder();
        result.append("#" + this.ruleId + ": ");

        //LHS internal sensors
        result.append('(');
        int count = 0;
        for(IntCond iCond : this.lhsInternal){
            if (count > 0) result.append(',');
            count++;
            result.append(iCond.sId + "$" + iCond.getConfidence());
        }
        result.append(")|");

        //LHS external sensors
        result.append('(');
        count = 0;
        for (ExtCond eCond : this.lhsExternal) {
            if (count > 0) result.append(',');
            count++;
            if (! eCond.val) result.append('!');
            result.append(eCond.sName + '$' + eCond.getConfidence());
        }
        result.append(")");

        //action and arrow
        result.append(this.action);
        result.append(" -> ");

        //RHS external sensors
        result.append('(');
        count = 0;
        for (ExtCond eCond : this.rhsExternal) {
            if (count > 0) result.append(',');
            count++;
            if (! eCond.val) result.append('!');
            result.append(eCond.sName + '$' + eCond.getConfidence());
        }
        result.append(")");

        return result.toString();
    }//toString

    /**
     * calculates how closely this rule matches the agent's current sensors
     *
     * @return  a match score from 0.0 to 1.0
     */
    public double matchScore() {
        double sum = 0.0;

        //Compare LHS internal values
        for (IntCond iCond : this.lhsInternal) {
            Integer sIdVal = Integer.valueOf(iCond.sId);
            if ( agent.getCurrInternal().containsKey(sIdVal)
                 && agent.getCurrInternal().get(sIdVal) ) {
                sum += iCond.getConfidence();
            }
        }

        //Compare RHS external values
        for (ExtCond eCond : this.lhsExternal) {
            if (agent.getCurrExternal().hasSensor(eCond.sName)) {
                Boolean sVal = (Boolean) agent.getCurrExternal().getSensor(eCond.sName);
                if (sVal == eCond.val){
                    sum += eCond.getConfidence();
                }
            }
        }

        double count = this.lhsInternal.size() + this.lhsExternal.size();
        return sum / count;
    }//matchScore

    /**
     * calculates how closely this rule matches another given rule
     *
     * @param other  the other rule to compare it to
     * @return  a match score from 0.0 to 1.0
     */
    public double compareTo(EpRule other) {
        double sum = 0.0;

        //Compare LHS internal values
        for (IntCond thisICond : this.lhsInternal) {
            for (IntCond otherICond : other.lhsInternal) {
                if (thisICond.equals(otherICond)) {
                    sum += (thisICond.getConfidence() + otherICond.getConfidence()) / 2;
                    break;
                }
            }
        }

        //Compare LHS external values
        for (ExtCond thisECond : this.lhsExternal) {
            for (ExtCond otherECond : other.lhsExternal) {
                if (thisECond.equals(otherECond)) {
                    sum += (thisECond.getConfidence() + otherECond.getConfidence()) / 2;
                    break;
                }
            }
        }

        //Compare RHS external values
        for (ExtCond thisECond : this.rhsExternal) {
            for (ExtCond otherECond : other.rhsExternal) {
                if (thisECond.equals(otherECond)) {
                    sum += (thisECond.getConfidence() + otherECond.getConfidence()) / 2;
                    break;
                }
            }
        }

        double count = this.lhsInternal.size() + this.lhsExternal.size() + this.rhsExternal.size();
        return sum / count;
    }//matchScore



}//class EpRule
