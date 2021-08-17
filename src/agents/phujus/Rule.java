package agents.phujus;

import framework.SensorData;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Random;
import java.util.Vector;

/**
 * class Rule
 *
 * parent class of all rules used by the PhuJusAgent
 */

public abstract class Rule {

    /**
     * class Confidence
     *
     * tracks a rule's confidence in something over time.  The most
     * recent call to increase/decrease confidence bears the most
     * weight.  Previous calls have diminishing influence
     */
    public static class Confidence {
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
    }//class Confidence



    //to assign a unique id to each rule this shared variable is incremented by the ctor
    private static int nextRuleId = 1;

    //The agent using this rule
    protected final PhuJusAgent agent;

    //each rule has a unique integer id
    protected final int ruleId;

    //the rule's historical accuracy is tracked with a Confidence object
    protected Confidence accuracy = new Confidence();

    //All of the children
    protected Vector<Rule> children = new Vector<>();

    public Rule(PhuJusAgent agent) {
        this.agent = agent;
        this.ruleId = this.nextRuleId++;
    }

    //region Getters and Setters

    public int getId() { return this.ruleId; }

    public double getAccuracy() { return accuracy.getConfidence(); }

    public void increaseConfidence() {this.accuracy.increaseConfidence(); }

    public void decreaseConfidence() { this.accuracy.decreaseConfidence(); }

    //endregion

    //region Abstract Methods
    /**
     * calculates how closely this rule matches a given rhs sensors
     *
     * @return  a match score from 0.0 to 1.0
     */
    public abstract double rhsMatchScore(SensorData rhsExt);

    /**
     * calculates how closely this rule matches a given action and lhs sensors
     * A total match score [0.0..1.0] is calculated as the product of the
     * match score for each level (this.timeDepth) of the rule.
     *
     * @return  a match score from 0.0 to 1.0
     */
    public abstract double lhsMatchScore(char action, HashSet<Integer> lhsInt, SensorData lhsExt);

    /** convenience function the rule is expected to use the sensors in the current agent */
    public double lhsMatchScore(char action) {
        return lhsMatchScore(action, this.agent.getCurrInternal(), this.agent.getCurrExternal());
    }

    /**
     * calculateActivation
     *
     * calculates the activation of the rule atm. Activation is a measure of
     * the frequency and recency with which a rule is used to reach a goal
     */
    public abstract double calculateActivation(int now);

    //endregion


}//class Rule
