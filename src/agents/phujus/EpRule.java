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

        @Override
        public int hashCode() { return Objects.hash(sName, val); }
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

        @Override
        public int hashCode() { return Objects.hash(sId); }
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
    private final Vector<HashSet<IntCond>> lhsInternal;
    private final HashSet<IntCond> removed_lhsNotInternal = null;
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
    private int lastActCalcTime = -1;  //the last timestep when activation for which activation was calculated
    public static final double DECAY_RATE = 0.95;  //activation decays exponentially over time

    // Track the accuracy of this rule.  numMatches is how often it has matched (fired).
    // numPredicts is how many times it matched and correctly predicted the next step.
    // These values are init'd to 1 to account for the episodes the rule is created from
    // (also prevents divide-by-zero errors).
    private double numMatches = 1;
    private double numPredicts = 1;

    //How many timesteps of internal sensors are required for a match with this rule
    private int timeDepth = 0;

//region Ctors and Init
    /**
     * this ctor initializes the rule from the agent's current and previous episodes
     */
    public EpRule(PhuJusAgent agent){
        this.agent = agent;
        this.ruleId = EpRule.nextRuleId++;
        this.action = agent.getPrevAction();
        this.lhsExternal = initExternal(agent.getPrevExternal());
        //REMOVE: this.lhsNotInternal = new HashSet<>();
        this.lhsInternal = initInternal(agent.getAllPrevInternal());
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
    private Vector<HashSet<IntCond>> initInternal(Vector<HashSet<Integer>> initLHS) {
        Vector<HashSet<IntCond>> result = new Vector<>();
        for(HashSet<Integer> initLevel : initLHS) {
            HashSet<IntCond> level = new HashSet<>();
            for (Integer sId : initLevel) {
                level.add(new IntCond(sId));
            }
            result.add(level);
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
     * like toString() but much less verbose.  Each external condition is
     * represented by a single '1' or '0'
     * Example:   (33, 99)|010a->100
     *
     */
    public String toShortString() {
        StringBuilder result = new StringBuilder();

        //LHS internal sensors
        for(int i = 1; i <= this.timeDepth; ++i) {
            result.append('(');
            int count = 0;
            for (IntCond iCond : getInternalLevel(i)) {
                if (count > 0) result.append(", ");
                count++;
                result.append(iCond.sId);
            }
            result.append(')');
        }
        result.append('|');

        //LHS _not_ internal sensors
        //REMOVE this code if time expansion works
//        if (this.lhsNotInternal.size() > 0) {
//            if (count > 0) result.append(", ");
//            result.append("!x");
//            result.append(lhsNotInternal.size());
//        }
//        result.append(")|");

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
        for(int i = 1; i <= this.timeDepth; ++i) {
            result.append('(');
            int count = 0;
            for (IntCond iCond : getInternalLevel(i)) {
                if (count > 0) result.append(", ");
                count++;
                result.append(iCond.sId);
                result.append('$');
                result.append(String.format("%.2f", iCond.getConfidence()));
            }
            result.append(')');
        }
        result.append('|');

        //REMOVE this code if time expansion works
//        for(IntCond iCond : this.lhsNotInternal){
//            if (count > 0) result.append(", ");
//            count++;
//            result.append('!');
//            result.append(iCond.sId);
//            result.append('$');
//            result.append(String.format("%.2f", iCond.getConfidence()));
//        }
//        result.append(")|");

        //LHS external sensors
        result.append('(');
        int count = 0;
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

        //Activation & Accuracy
        result.append(String.format("act=%.5f", calculateActivation(agent.getNow())));
        result.append(String.format("  acc=%.5f", getAccuracy()));

        return "#" + this.ruleId + ": " + toShortString() + "    " + result;
    }//toString

    /**
     * calculates how closely this rule matches a given action and lhs sensors
     * A total match score [0.0..1.0] is calculated as the product of the
     * match score for each level (this.timeDepth) of the rule.
     *
     * TODO:  I think this method should ignore zero-confidence conditions.
     *  - Already sort of ignores them, however it still increments count, so test if it shouldn't
     *
     * @return  a match score from 0.0 to 1.0
     */
    public double lhsMatchScore(char action, HashSet<Integer> lhsInt, SensorData lhsExt) {
        //If the action doesn't match this rule can't match at all
        if (action != this.action) return 0.0;

        double score = 1.0;  //this will be the final match score

        //Compare LHS internal values
        for(int i = 1; i <= timeDepth; ++i) {
            HashSet<IntCond> level = getInternalLevel(i);
            double sum = 0.0;
            for (IntCond iCond : level) {
                Integer sIdVal = iCond.sId;
                if (lhsInt.contains(sIdVal)) {
                    sum += iCond.getConfidence();  //reward for match
                } else {
                    sum -= iCond.getConfidence();  //penalize for non-match
                }
            }
            if (level.size() > 0) {
                score *= sum / level.size();
            }
            if (score <= 0.0) return 0.0;
        }//for timedepth

        //REMOVE this code if time expansion works
//        //Compare LHS *not* internal values (subtract confidence from sum if present)
//        for (IntCond iCond : this.lhsNotInternal) {
//            Integer sIdVal = iCond.sId;
//            if (lhsInt.contains(sIdVal)) {
//                sum -= iCond.getConfidence();  //penalize for match
//            } // Don't reward for non-match! This can cause a partial match based solely
//            // on the absence of these conditions which leads to loops
//        }



        //Compare LHS external values
        double sum = 0.0;
        for (ExtCond eCond : this.lhsExternal) {
            if (lhsExt.hasSensor(eCond.sName)) {
                Boolean sVal = (Boolean) lhsExt.getSensor(eCond.sName);
                if (sVal == eCond.val){
                    sum += eCond.getConfidence();  //reward for match
                } else {
                    sum -= eCond.getConfidence();  //penalize for non-match
                }
            }
        }
        if (this.lhsExternal.size() > 0) {
            score *= sum / this.lhsExternal.size();
        }
        if (score <= 0.0) return 0.0;

        return score;

    }//lhsMatchScore

    /** convenience function that uses the sensors in the current agent */
    public double lhsMatchScore(char action) {
        return lhsMatchScore(action, this.agent.getCurrInternal(), this.agent.getCurrExternal());
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

        return sum / this.rhsExternal.size();
    }//rhsMatchScore

    /**
     * calculates how closely this rule matches another given rule
     *
     * @param other  the other rule to compare it to
     * @return  a match score from 0.0 to 1.0
     */
    public double compareTo(EpRule other) {
        double sum = 0.0;

        //actions must match
        if (this.action != other.action) return 0.0;

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
     *
     * @param lhsInt  the internal sensors that the rule matched
     * @param lhsExt  the external sensors that the rule matched
     * @param rhsExt  the external sensors that appeared on the next timestep
     */
    public void updateConfidencesForPrediction(HashSet<Integer> lhsInt, SensorData lhsExt, SensorData rhsExt) {
        //Compare LHS internal values
        for (IntCond iCond : this.lhsInternal) {
            Integer sIdVal = iCond.sId;
            if (lhsInt.contains(sIdVal)) {
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
     * reevaluateInternalSensors
     *
     * adds *not* internal sensors that will also be used in conditions when checking
     * for a match score in lhsMatchScore
     * @param prevInternal Internal sensors from previous time step
     */
    public void reevaluateInternalSensors(HashSet<Integer> prevInternal) {
        for(Integer i : prevInternal) {
            //check to see if it's already present as a positive condition
            boolean found = false;
            for(IntCond iCond : this.lhsInternal) {
                if (iCond.sId == i) {
                    found = true;
                    break;
                }
            }
            if (!found) this.addNotInternal(i);

        }//for
    }//reevaluateInternalSensors

    public void resetMetaInfo() {
        // reset trackers
        this.numMatches = 1.0;
        this.numPredicts = 1.0;

        //reset activation
        for(int i = 0; i < ACTHISTLEN; ++i) {
            lastActTimes[i] = 0;
            lastActAmount[i] = 0.0;
        }
        nextActPos = 0;
        activationLevel = 0.0;
        lastActCalcTime = agent.getNow();

        //TODO: tentatively keeping this, but could be worth testing again later
        if(this.getRHSExternal().isGoal()) {
            addActivation(this.agent.getNow(), EpRule.FOUND_GOAL_REWARD);
            calculateActivation(this.agent.getNow());
        }
    }//resetMetaInfo


//region Getters and Setters

    public int getId() { return this.ruleId; }
    public char getAction() { return this.action; }
    public HashSet<ExtCond> getRHSConds() { return this.rhsExternal; }

    /** convert this.lhsInternal back to to a HashMap */
    public HashMap<Integer, Boolean> getLHSInternal() {
        HashMap<Integer, Boolean> result = new HashMap<>();
        for(IntCond iCond : this.lhsInternal) {
            result.put(iCond.sId, true);
        }
        return result;
    }

    /** convert this.lhsNotInternal back to to a HashMap */
    public HashMap<Integer, Boolean> getLHSNotInternal() {
        HashMap<Integer, Boolean> result = new HashMap<>();
        for(IntCond iCond : this.lhsNotInternal) {
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

    public void addNotInternal(Integer i) {
        this.lhsNotInternal.add(new IntCond(i));
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

    //Retrieve's the nth level of internal sensors from the end of this.lhsInternal
    private HashSet<IntCond> getInternalLevel(int depth) {
        if (depth > this.lhsInternal.size()) return null; //bad input
        if (depth <= 0) return null; //also bad input

        int index = this.lhsInternal.size() - depth;
        return this.lhsInternal.get(index);
    }



//endregion

}//class EpRule
