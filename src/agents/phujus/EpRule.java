package agents.phujus;

import framework.SensorData;

import java.util.*;

/**
 * class EpRule
 *
 * This is a replacement for Rule that is similar to a weighted episode.
 */
public class EpRule {
//region Inner Classes
    /**
     * class Condition
     *
     * tracks a LHS or RHS value and the rule's confidence in it
     */
    public abstract static class Condition {
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
    public static class ExtCond extends Condition implements Comparable<ExtCond> {
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
    }//class ExtCond

    /**
     * class IntCond
     *
     * Tracks internal conditions (LHS or RHS)
     */
    public static class IntCond extends Condition {
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

        @Override
        public String toString() { return "" + sId; }


    }//class IntCond

//endregion

    /** base reward for a rule that correctly predicts finding the goal. */
    public static final double FOUND_GOAL_REWARD = 20.0;

    //to assign a unique id to each rule this shared variable is incremented by the ctor
    private static int nextRuleId = 1;

    //The agent using this rule
    private final PhuJusAgent agent;

    //each rule has a unique integer id
    private final int ruleId;

    //define the LHS of the rule.  This consists of:
    // - a set of internal sensor values indicating what other rules fired two timesteps ago
    // - a set of external sensor values that were present in the previous timestep
    // - the action that the agent took just before this rule was created
    //Initially, all sensor values are present but, over time, they may get culled if they
    //prove inconsistent with the agent's experiences
    private final HashSet<IntCond> lhsInternal;
    private final HashSet<ExtCond> lhsExternal;
    private final char action;

    //The RHS of the rule consists of a set of external sensor values that were
    // present when the rule was created
    private final HashSet<ExtCond> rhsExternal;

    // Each rule has an activation level that tracks the frequency and
    // recency with which it fired and correctly predicted an external
    // sensor value
    public static final int ACTHISTLEN = 10;
    private final int[] lastActTimes = new int[ACTHISTLEN];  // last N times the rule was activated
    private final double[] lastActAmount = new double[ACTHISTLEN]; // amount of activation last N times
    private int nextActPos = 0;
    private double activationLevel;  //CAVEAT:  this value may not be correct!  Call calculateActivation() to update it.
    public static final double DECAY_RATE = 0.95;  //activation decays exponentially over time

    // Track the accuracy of this rule.  numMatches is how often it has matched (fired).
    // numPredicts is how many times it matched and correctly predicted the next step.
    // These values are init'd to 1 to account for the episodes the rule is created from
    // (also prevents divide-by-zero errors).
    private double numMatches = 1;
    private double numPredicts = 1;

    //A rule can briefly enter a refractory state where it can't fire
    //TODO:  to be more robust this would work like activation with repeated misuses making the rule unusable for longer and longer periods
    private int refractory = 0;  //how many timesteps until this rule recovers
    private int nextRefractPeriod = 1;  //how long next period will be

//region Ctors and Init
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
            if (map.get(key)) {  //only care about true's
                result.add(new IntCond(key));
            }
        }
        return result;
    }//initInternal

//endregion

    /**
     * sortedConds
     *
     * creates a Vector from a HashSet of ExtCond where the conditions are in sorted order
     * but with the GOAL at the end.
     * */
    public Vector<ExtCond> sortedConds(HashSet<ExtCond> conds) {
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


    /**
     * like toString() but much less verbose.  Each condition is represented by a single '1' or '0'
     * Example:   0100111|010a->100
     *
     */
    public String toShortString() {
        StringBuilder result = new StringBuilder();

        //LHS internal sensors
        result.append('(');
        int count = 0;
        for(IntCond iCond : this.lhsInternal){
            if (count > 0) result.append(", ");
            count++;
            result.append(iCond.sId);
        }
        result.append(")|");

        //LHS external sensors
        for (ExtCond eCond : sortedConds(this.lhsExternal)) {
            char bit = (eCond.val) ? '1' : '0';
            result.append(bit);
        }

        //action and arrow
        result.append(this.action);
        result.append(" -> ");

        //RHS external sensors
        for (ExtCond eCond : sortedConds(this.rhsExternal)) {
            char bit = (eCond.val) ? '1' : '0';
            result.append(bit);
        }

        return result.toString();

    }//toShortString

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

        //LHS internal sensors
        result.append('(');
        int count = 0;
        for(IntCond iCond : this.lhsInternal){
            if (count > 0) result.append(", ");
            count++;
            result.append(iCond.sId);
            result.append('$');
            result.append(String.format("%.2f", iCond.getConfidence()));
        }
        result.append(")|");

        //LHS external sensors
        result.append('(');
        count = 0;
        for (ExtCond eCond : sortedConds(this.lhsExternal)) {
            if (count > 0) result.append(", ");
            count++;
            if (! eCond.val) result.append('!');
            result.append(eCond.sName);
            result.append('$');
            result.append(String.format("%.2f", eCond.getConfidence()));
        }
        result.append(")");

        //action and arrow
        result.append(this.action);
        result.append(" -> ");

        //RHS external sensors
        result.append('(');
        count = 0;
        for (ExtCond eCond : sortedConds(this.rhsExternal)) {
            if (count > 0) result.append(',');
            count++;
            if (! eCond.val) result.append('!');
            result.append(eCond.sName);
            result.append('$');
            result.append(String.format("%.2f", eCond.getConfidence()));
        }
        result.append(") ^ ");

        //Activation
        result.append(String.format("%.5f", calculateActivation(agent.getNow())));

        return "#" + this.ruleId + ": " + toShortString() + "    " + result;
    }//toString

    /**
     * calculates how closely this rule matches a given action and lhs sensors
     *
     * TODO:  I think this method should ignore zero-confidence conditions.
     *
     * @return  a match score from 0.0 to 1.0
     */
    public double lhsMatchScore(char action, HashMap<Integer, Boolean> lhsInt, SensorData lhsExt) {
        double sum = 0.0;

        //If this rule is in a refractory state it can't match
        if (this.refractory > 0) return 0.0;

        //If the action doesn't match this rule can't match at all
        //TODO:  should we do partial matching for actions? Seems like no but leaving this thought in here for now.
        if (action != this.action) return 0.0;

        //Compare LHS internal values
        for (IntCond iCond : this.lhsInternal) {
            Integer sIdVal = iCond.sId;
            if ( lhsInt.containsKey(sIdVal) && lhsInt.get(sIdVal) ) {
                sum += iCond.getConfidence();
            }
        }

        //Compare LHS external values
        for (ExtCond eCond : this.lhsExternal) {
            if (lhsExt.hasSensor(eCond.sName)) {
                Boolean sVal = (Boolean) lhsExt.getSensor(eCond.sName);
                if (sVal == eCond.val){
                    sum += eCond.getConfidence();
                }
            }
        }

        //Note:  A special case here might be if sum == 0.0.  That can only
        //happen if the rule has no matching conditions.  I don't think that's worth
        //checking for.

        double count = this.lhsInternal.size() + this.lhsExternal.size();
        return sum / count;
    }//lhsMatchScore

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

        return sum / this.rhsExternal.size();
    }//rhsMatchScore

    /** convenience function that uses the sensors in the current agent */
    public double matchScore(char action) {
        return lhsMatchScore(action, this.agent.getCurrInternal(), this.agent.getCurrExternal());
    }

    /**
     * calculates how closely this rule matches another given rule
     *
     * @param other  the other rule to compare it to
     * @return  a match score from 0.0 to 1.0
     */
    public double compareTo(EpRule other) {
        double sum = 0.0;

        //Compare LHS internal values
        HashSet<IntCond> largerLHSInt = this.lhsInternal;
        HashSet<IntCond> smallerLHSInt = other.lhsInternal;
        if (largerLHSInt.size() < smallerLHSInt.size()) {
            largerLHSInt = other.lhsInternal;
            smallerLHSInt = this.lhsInternal;
        }
        for (IntCond iCond1 : largerLHSInt) {
            for (IntCond iCond2 : smallerLHSInt) {
                if (iCond1.equals(iCond2)) {
                    sum += (iCond1.getConfidence() + iCond2.getConfidence()) / 2;
                    break;
                }
            }
        }

        //Compare LHS external values
        HashSet<ExtCond> largerLHSExt = this.lhsExternal;
        HashSet<ExtCond> smallerLHSExt = other.lhsExternal;
        if (largerLHSExt.size() < smallerLHSExt.size()) {
            largerLHSExt = other.lhsExternal;
            smallerLHSExt = this.lhsExternal;
        }
        for (ExtCond thisECond : largerLHSExt) {
            for (ExtCond otherECond : smallerLHSExt) {
                if (thisECond.equals(otherECond)) {
                    sum += (thisECond.getConfidence() + otherECond.getConfidence()) / 2;
                    break;
                }
            }
        }

        //Compare RHS external values
        HashSet<ExtCond> largerRHSExt = this.rhsExternal;
        HashSet<ExtCond> smallerRHSExt = other.rhsExternal;
        if (largerRHSExt.size() > smallerRHSExt.size()) {
            largerRHSExt = other.rhsExternal;
            smallerRHSExt = this.rhsExternal;
        }
        for (ExtCond thisECond : largerRHSExt) {
            for (ExtCond otherECond : smallerRHSExt) {
                if (thisECond.equals(otherECond)) {
                    sum += (thisECond.getConfidence() + otherECond.getConfidence()) / 2;
                    break;
                }
            }
        }

        double count = largerLHSInt.size() + largerLHSExt.size() + largerRHSExt.size();
        return sum / count;
    }//compareTo

    /**
     * calculateActivation
     *
     * calculates the activation of the rule atm.  Activation is increased
     * by fixed amounts and each increase decays over time.
     * The sum of these values is the total activation.
     *
     * @see #addActivation(int, double)
     */
    public double calculateActivation(int now) {
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
    }

    /**
     * updateConfidencesForPrediction
     *
     * adjusts the confidence values of this rule when it effectively predicts
     * the next state
     *
     * CAVEAT:  This rule should be called when the rule correctly predicted
     *          the future.  Not when it didn't.
     * CAVEAT:  Do not call this method with sensors it didn't match with!
     */
    public void updateConfidencesForPrediction(HashMap<Integer, Boolean> lhsInt, SensorData lhsExt, SensorData rhsExt) {
        //Compare LHS internal values
        for (IntCond iCond : this.lhsInternal) {
            Integer sIdVal = iCond.sId;
            if ( lhsInt.containsKey(sIdVal) && lhsInt.get(sIdVal) ) {
                iCond.increaseConfidence();
            } else {
                iCond.decreaseConfidence();
            }
        }

        //Compare LHS external values
        for (ExtCond eCond : this.lhsExternal) {
            if (lhsExt.hasSensor(eCond.sName)) {
                Boolean sVal = (Boolean) lhsExt.getSensor(eCond.sName);
                if (sVal == eCond.val){
                    eCond.increaseConfidence();
                } else {
                    eCond.decreaseConfidence();
                }
            } else {
                eCond.decreaseConfidence();
            }
        }

        //Compare RHS external values
        for (ExtCond eCond : this.rhsExternal) {
            if (rhsExt.hasSensor(eCond.sName)) {
                Boolean sVal = (Boolean) rhsExt.getSensor(eCond.sName);
                if (sVal == eCond.val){
                    eCond.increaseConfidence();
                } else {
                    eCond.decreaseConfidence();
                }
            } else {
                eCond.decreaseConfidence();
            }
        }
    }//updateConfidencesForPrediction

    /**
     * when a rule is used to build a path that fails, I want to decrease my
     * confidence in conditions that matched and caused the rule to be
     * selected to build that path.
     */
    public void updateConfidencesForBadPath(TreeNode misstep) {
        HashMap<Integer, Boolean> lhsInt = misstep.getCurrInternal();
        SensorData lhsExt = misstep.getCurrExternal();

        //Compare LHS internal values
        for (IntCond iCond : this.lhsInternal) {
            Integer sIdVal = iCond.sId;
            if ( lhsInt.containsKey(sIdVal) && lhsInt.get(sIdVal) ) {
                iCond.decreaseConfidence();
            }
        }

        //Compare LHS external values
        for (ExtCond eCond : this.lhsExternal) {
            if (lhsExt.hasSensor(eCond.sName)) {
                Boolean sVal = (Boolean) lhsExt.getSensor(eCond.sName);
                if (sVal == eCond.val) {
                    eCond.decreaseConfidence();
                }
            }
        }
    }//updateConfidencesForBadPath

//region Getters and Setters

    public int getId() { return this.ruleId; }
    public char getAction() { return this.action; }

    /** convert this.lhsInternal back to to a HashMap */
    public HashMap<Integer, Boolean> getLHSInternal() {
        HashMap<Integer, Boolean> result = new HashMap<>();
        for(IntCond iCond : this.lhsInternal) {
            result.put(iCond.sId, true);
        }
        return result;
    }

    /** convert this.lhsExternal back to to a SensorData */
    public SensorData getLHSExternal() {
        SensorData result = SensorData.createEmpty();
        for(ExtCond eCond : this.lhsExternal) {
            result.setSensor(eCond.sName, eCond.val);
        }
        return result;
    }

    /** convert this.rhsExternal back to to a SensorData */
    public SensorData getRHSExternal() {
        SensorData result = SensorData.createEmpty();
        for(ExtCond eCond : this.rhsExternal) {
            result.setSensor(eCond.sName, eCond.val);
        }
        return result;
    }

    public void incrMatches() {
        numMatches++;
    }

    public void incrPredicts() {
        numPredicts++;
    }

    public double getAccuracy() {
        return numPredicts / numMatches;
    }//getAccuracy

    public boolean testsIntSensor(int sId) {
        for(IntCond iCond : this.lhsInternal) {
            if (iCond.sId == sId) return true;
        }
        return false;
    }

    /** called when the associated rule is deleted */
    public void removeIntSensor(int sId) {
        IntCond removeMe = null;
        for(IntCond iCond : this.lhsInternal) {
            if (iCond.sId == sId) {
                removeMe = iCond;
                break;
            }
        }
        if (removeMe != null) this.lhsInternal.remove(removeMe);
    }


//endregion

}//class EpRule
