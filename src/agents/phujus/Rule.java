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
     */
    public static class Confidence {

        private double conf = 1.0; //start will full confidence (optimistic)

        public void increaseConfidence(double matchScore, double bestScore) {
            this.conf += ((1.0 - this.conf) / 2.0) * (matchScore / bestScore);
        }

        public void decreaseConfidence(double matchScore, double bestScore) {
           this.conf -= (this.conf / 2.0) * (matchScore / bestScore);
        }

        public double getConfidence() {
            return conf;
        }

        public void setConfidence(double conf) { this.conf = conf; }

        //In some cases a Confidence object needs to be set to min instead of max (default)
        public void minConfidence() {
            this.conf = 0.0;
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
    protected Confidence confidence = new Confidence();

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

    /** a shorter string format designed to be used inline */
    public abstract String toStringShort();

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

    public static int getNextRuleId() { return nextRuleId; }

    public int getId() { return this.ruleId; }

    public double getConfidence() { return this.confidence.getConfidence(); }

    public void increaseConfidence(double matchScore, double bestScore) {
        this.confidence.increaseConfidence(matchScore, bestScore);
    }

    public void decreaseConfidence(double matchScore, double bestScore) {
        this.confidence.decreaseConfidence(matchScore, bestScore);
    }

    //endregion



}//class Rule
