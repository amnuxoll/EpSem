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

    /**
     * class RuleRelationship
     *
     * Used inside of the HashMap(Integer, RuleRelationship) variable called 'relationships'. Contains information
     * about how often this relationship has been on, and often it has been off. Doesn't actually contain references
     * to the rules since that's not really necessary.
     */
    protected class RuleRelationship {

        private int numOn;
        private int numOff;

        // When we make a new RuleRelationship, it's because two rules have fired at the same time.
        public RuleRelationship() {
            this.numOn = 1;
        }

        public double getPercentage() {
            if (numOff == 0) return 1.0d;
            return (numOn + 0.0d) / numOff;
        }

        /**
         * append
         *
         * Appends the information from another RuleRelationship to this one.
         * @param other
         */
        public void append(RuleRelationship other) {
            this.numOn += other.numOn;
            this.numOff += other.numOff;
        }

        public void turnOn() {
            this.numOn++;
        }

        public void turnOff() {
            this.numOff++;
        }

        public int getNumOn() {
            return numOn;
        }

        public int getNumOff() {
            return numOff;
        }
    }

    //endregion

    //region Instance Variables

    //to assign a unique id to each rule this shared variable is incremented by the ctor
    private static int nextRuleId = 1;

    //The agent using this rule
    protected final PhuJusAgent agent;

    //each rule has a unique integer id
    protected final int ruleId;

    //the rule's historical accuracy is tracked with a Confidence object
    protected Confidence confidence = new Confidence();

    // The rule's relationships to other rules (how often they fire together/not together) is tracked here.
    // The Integer value represents the ID of the rule it's related to, and the RuleRelationship tracks the data
    // about the relationship between this rule and the rules it's related to.
    protected HashMap<Integer, RuleRelationship> relationships = new HashMap<>();

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

    /**
     * updateRelationships
     *
     * Updates the relationships HashMap inside of Rule. Adds an activation
     *
     * @param rules A HashSet containing the ruleIDs of all rules that have fired with this one. Should be
     *              PhuJusAgent's currInternal.
     */
    public void updateRelationships(HashSet<Integer> rules) {

        // Add a relationship for this rule if it doesn't exist already, or increment it if it does.
        for (Integer ruleID : rules) {

            if (ruleID == this.ruleId) continue;

            if (this.relationships.containsKey(ruleID)) {
                this.relationships.get(ruleID).turnOn();
            } else {
                this.relationships.put(ruleID, new RuleRelationship());
            }
        }

        // For every one of the existing relationships, add +1 to the turnOff counter if it wasn't activated
        // during this timestep.
        for (Integer relationshipID : this.relationships.keySet()) {
            if (!rules.contains(relationshipID)) {
                relationships.get(relationshipID).turnOff();
            }
        }
    }

    //endregion

    //region Getters and Setters

    public static int getNextRuleId() { return nextRuleId; }

    //use with caution!!
    public static void resetRuleIds() { Rule.nextRuleId = 1; }

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
