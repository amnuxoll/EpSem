package agents.phujus;

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

        /**
         * increases or decreases this confidence value
         *
         * @param degree  how much to change the value on a scale: -1.0..1.0
         */
        public void adjustConfidence(double degree) {
            if (degree == 0.0) return;
            if (degree > 0.0) {
                this.conf += ((1.0 - this.conf) / 2.0) * degree;
            } else {
                this.conf += (this.conf / 2.0) * degree;
            }
        }

        public double getConfidence() {
            return conf;
        }

        public void setConfidence(double conf) { this.conf = conf; }

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
    public static final double DECAY_RATE = 0.99999;  //activation decays exponentially over time

    //endregion

    public Rule(PhuJusAgent agent) {
        this.agent = agent;
        this.ruleId = Rule.nextRuleId++;
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
     * calcActivation
     *
     * calculates the activation of the rule atm.  Activation is increased
     * by fixed amounts and each increase decays over time.
     * The sum of these values is the total activation.
     */
    public double calcActivation() {
        //If we've already updated the activation level used that value
        if (lastActCalcTime == agent.getNow()) return this.activationLevel;

        double result = 0.0;
        for(int j=0; j < lastActTimes.length; ++j) {
            if(lastActTimes[j] != 0) {
                double decayAmount = Math.pow(DECAY_RATE, agent.getNow()-lastActTimes[j]);
                result += lastActAmount[j]*decayAmount;
            }
        }

        this.activationLevel = result;
        return this.activationLevel;
    }//calcActivation

    //endregion

    //region Getters and Setters

    public static int getNextRuleId() { return nextRuleId; }

    //use with caution!!
    public static void resetRuleIds() { Rule.nextRuleId = 1; }

    public int getId() { return this.ruleId; }

    public double getConfidence() { return this.confidence.getConfidence(); }

    public void adjustConfidence(double degree) {
        this.confidence.adjustConfidence(degree);
    }

    //endregion



}//class Rule
