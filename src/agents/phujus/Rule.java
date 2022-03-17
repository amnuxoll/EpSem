package agents.phujus;

import framework.SensorData;
import tests.agents.phujus.PhuJusAgentTest;

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

    //region Inner Classes
    /**
     * class Confidence
     *
     * tracks a rule's confidence in something over time.  The most
     * recent call to increase/decrease confidence bears the most
     * weight.  Previous calls have diminishing influence
     *
     * This is a parent class for {@link BaseRule.ExtCond} and
     * {@link EpRule.IntCond}.
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

        //In some cases a Confidence object needs to be set to min instead of max (default)
        public void minConfidence() {
            this.conf = 0;
            this.increaseConfidence();
        }

    }//class Confidence

    //endregion

    //region Instance Variables

    //to assign a unique id to each rule this shared variable is incremented by the ctor
    private static int nextRuleId = 1;

    //The agent using this rule
    protected final PhuJusAgent agent;

    //each rule has a unique integer id
    protected final int ruleId;

    //the rule's historical accuracy is tracked with a Confidence object
    //TODO:  rename this variable to confidence
    protected Confidence accuracy = new Confidence();

    // Each rule has an activation level that tracks the frequency and
    // recency with which it has helped the agent reach a goal.  The
    // following instance variables track this activation level.
    public static final int ACTHISTLEN = 10;
    private final int[] lastActTimes = new int[ACTHISTLEN];  // last N times the rule was activated
    protected final double[] lastActAmount = new double[ACTHISTLEN]; // amount of activation last N times
    protected int nextActPos = 0;
    protected double activationLevel;  //CAVEAT:  this value may not be correct!  Call calculateActivation() to update it.
    protected int lastActCalcTime = -1;  //the last timestep when activation for which activation was calculated
    public static final double DECAY_RATE = 0.95;  //activation decays exponentially over time

    //endregion

    public Rule(PhuJusAgent agent) {
        this.agent = agent;
        this.ruleId = this.nextRuleId++;
    }

    //region Rule Activation
    /**
     * addActivation
     *
     * adds a new activation event to this rule.
     *
     * @param now time of activation
     * @param reward amount of activation (can be negative to punish)
     *
     * @return true if the reward was applied
     */
    public boolean addActivation(int now, double reward) {
        //Check: rule can't be activated twice in the same timestep
        int prevIdx = this.nextActPos - 1;
        if (prevIdx < 0) prevIdx = this.lastActTimes.length - 1;
        if (lastActTimes[prevIdx] == now) {
            if (lastActAmount[prevIdx] < reward) {
                this.lastActAmount[prevIdx] = reward;
                return true;
            }
            return false;
        }

        this.lastActTimes[this.nextActPos] = now;
        this.lastActAmount[this.nextActPos] = reward;
        this.nextActPos = (this.nextActPos + 1) % ACTHISTLEN;
        return true;
    }//addActivation

    /**
     * calculateActivation
     *
     * calculates the activation of the rule atm.  Activation is increased
     * by fixed amounts and each increase decays over time.
     * The sum of these values is the total activation.
     */
    public double calculateActivation(int now) {
        //If we've already updated the activation level used that value
        if (lastActCalcTime == now) return this.activationLevel;

        double result = 0.0;
        for(int j=0; j < lastActTimes.length; ++j) {
            if(lastActTimes[j] != 0) {
                double decayAmount = Math.pow(DECAY_RATE, now-lastActTimes[j]);
                result += lastActAmount[j]*decayAmount;
            }
        }

        this.activationLevel = result;
        return this.activationLevel;
    }//calculateActivation

    //endregion

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



    //endregion


}//class Rule
