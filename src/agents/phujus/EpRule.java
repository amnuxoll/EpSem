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
    private final Vector<HashSet<IntCond>> lhsNotInternal;
    private final HashSet<ExtCond> lhsExternal;
    private final char action;

    //The RHS of the rule consists of a set of external sensor values that were
    // present when the rule was created
    private final HashSet<ExtCond> rhsExternal;

    //In some cases, a rule becomes indeterminate:  multiple RHS have been
    //seen but the agent doesn't know how to predict which will occur.
    //When this happens, this rule acquires multiple "sister" rules that
    //each predict a different RHS
    private HashSet<EpRule> sisters = new HashSet<>();

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
        this.lhsNotInternal = new Vector<>();
        this.lhsInternal = initInternal(agent.getAllPrevInternal());
        this.rhsExternal = initExternal(agent.getCurrExternal());
    }

    /**
     * this ctor initializes the rule from a specified merging of two rules
     */
    public EpRule(PhuJusAgent agent, char action, HashSet<ExtCond> lhsExternal,
                  Vector<HashSet<IntCond>> lhsInternal, HashSet<ExtCond> rhsExternal){
        this.agent = agent;
        this.ruleId = EpRule.nextRuleId++;
        this.action = action;
        this.lhsExternal = lhsExternal;
        this.lhsNotInternal = new Vector<>();
        this.lhsInternal = lhsInternal;
        this.rhsExternal = rhsExternal;
    }

    /** converts from SensorData to HashSet<ExtCond> */
    private HashSet<ExtCond> initExternal(SensorData sData) {
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
     * toStringIntConds
     *
     * converts a given HashSet<IntCond> to a string containing comma-separated
     * list of sensor ids and, optionally, their confidence values
     *
     * @param level the HashSet to stringify
     * @param nots whether each sId should be prepended with a bang (!)
     * @param includeConf  whether the confidence values should be included in the output
     */
    private String toStringIntConds(HashSet<IntCond> level, boolean nots, boolean includeConf) {
        if (level == null) return "";
        StringBuilder result = new StringBuilder();
        int count = 0;
        for (IntCond iCond : level) {
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
     * toStringInternalLHS
     *
     * is a helper method for {@link #toString()} and {@link #toStringShort()}
     * that converts internal sensors to a String
     *
     * @param includeConf  whether the confidence values should be included in the output
     */
    private String toStringInternalLHS(boolean includeConf) {
        StringBuilder result = new StringBuilder();
        for(int i = timeDepth; i >= 1; --i) {
            result.append('(');
            //positive conditions
            HashSet<IntCond> level = getInternalLevel(i);
            String intStr = toStringIntConds(level, false, includeConf);
            result.append(intStr);

            //negative conditions ("not")
            level = getNotInternalLevel(i);
            String notStr = toStringIntConds(level, true, includeConf);
            if ((intStr.length() > 0) && (notStr.length() > 0)) {
                result.append(',');
            }
            result.append(notStr);
            result.append(')');
        }

        return result.toString();
    }//toStringInternalLHS

    /** toStringShortRHS
     *
     * is a helper method for {@link #toStringShort()} to convert a RHS to a bit string
     */
    private String toStringShortRHS(HashSet<ExtCond> rhs) {
        StringBuilder result = new StringBuilder();
        for (ExtCond eCond : sortedConds(rhs)) {
            char bit = (eCond.val) ? '1' : '0';
            result.append(bit);
        }
        return result.toString();
    }//toStringShortRHS

    /**
     * like toString() but much less verbose.  Each external condition is
     * represented by a single '1' or '0'
     * Example:   (33, 99)|010a->100
     *
     */
    public String toStringShort() {
        StringBuilder result = new StringBuilder();

        //LHS internal sensors
        result.append(toStringInternalLHS(false));
        result.append('|');

        //LHS external sensors
        for (ExtCond eCond : sortedConds(this.lhsExternal)) {
            char bit = (eCond.val) ? '1' : '0';
            result.append(bit);
        }

        //action and arrow
        result.append(this.action);
        result.append(" -> ");

        //RHS external sensors
        if (this.sisters.size() == 0) {
            result.append(toStringShortRHS(this.rhsExternal));
        } else {
            //Handle sister rules
            result.append('[');
            int count = 0;
            for(EpRule r : this.sisters) {
                if (count > 0) result.append('|');
                count++;
                result.append(toStringShortRHS(r.rhsExternal));
            }
            result.append(']');
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
        result.append(toStringInternalLHS(true));
        result.append('|');

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
        result.append(')');

        //sister rules
        if (this.sisters.size() > 0) {
            result.append(" sisters: ");
            count = 0;
            for(EpRule r : this.sisters) {
                if (count > 0) result.append("/");
                count++;
                result.append(r.getId());
            }
        }

        //Activation & Accuracy
        result.append(" ^ ");
        result.append(String.format("act=%.5f", calculateActivation(agent.getNow())));
        result.append(String.format("  acc=%.5f", getAccuracy()));

        return "#" + this.ruleId + ": " + toStringShort() + "    " + result;
    }//toString


    /**
     * lhsInternalMatchSum
     *
     * is a helper method for {@link #lhsInternalMatchScore(HashSet)} it
     * compares all the sensors in a given set with its conditions and
     * calculates a sum of confidence level for each matching cond in this rule.
     * Mismatches are penalized by the same amount.
     *
     * @param lhsInt
     * @param level
     * @return sum of matching confidences
     */
    private double lhsInternalMatchSum(HashSet<Integer> lhsInt, HashSet<IntCond> level) {
        if (level == null) return 0.0;
        double sum = 0.0;
        for (IntCond iCond : level) {
            Integer sIdVal = iCond.sId;
            if (lhsInt.contains(sIdVal)) {
                sum += iCond.getConfidence();  //reward for match
            } else {
                sum -= iCond.getConfidence();  //penalize for non-match
            }
        }
        return sum;
    }//lhsInternalMatchSum

    /**
     * lhsInternalMatchScore
     *
     * is a helper method for {@link #lhsMatchScore(char, HashSet, SensorData)}
     * that creates a score for all levels of internal sensors
     */
    private double lhsInternalMatchScore(HashSet<Integer> lhsInt) {
        double score = 1.0;
        //Compare LHS internal values
        for(int i = 1; i <= timeDepth; ++i) {
            HashSet<IntCond> level = getInternalLevel(i);
            double sum = lhsInternalMatchSum(lhsInt, level);
            HashSet<IntCond> notLevel = getNotInternalLevel(i);
            sum += (-1.0 * lhsInternalMatchSum(lhsInt, notLevel));

            //Calculate the divisor for the average
            double count = 0.0;
            if (level != null) count += level.size();
            if (notLevel != null) count += notLevel.size();

            //the score is the average confidence for this level
            if (sum <= 0.0) return 0.0;
            if (count > 0) score *= sum / count;
        }//for timedepth

        return score;
    }//lhsInternalMatchScore

    /**
     * lhsExternalMatchScore
     *
     * is a helper method for {@link #lhsMatchScore(char, HashSet, SensorData)}
     * that creates a score for the external sensors
     */
    private double lhsExternalMatchScore(SensorData lhsExt) {
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
        return sum / this.lhsExternal.size();
    }//lhsExternalMatchScore


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

        double score = lhsInternalMatchScore(lhsInt);
        if (score <= 0.0) return 0.0;

        //Compare LHS external values
        score *= lhsExternalMatchScore(lhsExt);
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

        double result = sum / this.rhsExternal.size();
        result *= getAccuracy();
        return result;

    }//rhsMatchScore

    /**
     * calculates how closely this rule matches another given rule
     *
     * @param other  the other rule to compare it to
     * @return  a match score from 0.0 to 1.0
     */
    public double compareLHS(EpRule other, int depth) {

        //actions must match
        if (this.action != other.action) return 0.0;

        //Independent score for each of the levels of internal sensors + 1 for external sensors
        double[] lhsScores = new double[depth + 1];

        for(int i = 1; i <= depth; ++i) {
            HashSet<IntCond> thisLevel = this.getInternalLevel(i);
            HashSet<IntCond> otherLevel = other.getInternalLevel(i);

            // If they are both null, they match
            if(thisLevel == null && otherLevel == null) {
                lhsScores[i] = 1.0;
                continue;
            }

            if(thisLevel == null || otherLevel == null) {
                lhsScores[i] = 0.0;
                continue;
            }

            double sum = 0.0;

            //Compare LHS internal values
            HashSet<IntCond> largerLHSInt = thisLevel;
            HashSet<IntCond> smallerLHSInt = otherLevel;
            if (largerLHSInt.size() < smallerLHSInt.size()) {
                largerLHSInt = otherLevel;
                smallerLHSInt = thisLevel;
            }
            for (IntCond iCond1 : largerLHSInt) {
                for (IntCond iCond2 : smallerLHSInt) {
                    if (iCond1.equals(iCond2)) {
                        sum += (iCond1.getConfidence() + iCond2.getConfidence()) / 2;
                        break;
                    }
                }
            }

            lhsScores[i] = sum / ((double) largerLHSInt.size());

        }

        double sum = 0.0;

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

        lhsScores[0] = sum / ((double) largerLHSExt.size());

        sum = 0.0;

        for(double score : lhsScores) {
            sum += score;
        }

        return (sum / (depth + 1)); // +1 accounts for lhsExt
    }//compareLHS

    /**
     * convenience function that compares levels at the depth of the deeper rule
     *
     * @param other the other rule to compare it to
     * @return a match score from 0.0 to 1.0
     */
    public double compareLHS(EpRule other) {
        int maxLevelDepth = Math.max(this.timeDepth, other.timeDepth);
        return compareLHS(other, maxLevelDepth);
    }//compareLHS

    public EpRule intersectRules(EpRule other) {
        EpRule ret = intersectRulesHelper(other);

        // Remove time depths if there are any empty internal levels
        int depth = ret.timeDepth;
        for(int i = depth; i >= 1; --i) {
            if(ret.getInternalLevel(i).size() == 0) {
                ret.timeDepth = i-1;
            }
        }

        return ret;
    }

    private EpRule intersectRulesHelper(EpRule other) {

        // Used in the constructor
        HashSet<ExtCond> lhsExt = new HashSet<>();
        Vector<HashSet<IntCond>> lhsInt = new Vector<>();
        HashSet<ExtCond> rhsExt = new HashSet<>();

        int depth = Math.max(this.timeDepth, other.timeDepth);

        for(int i = 1; i <= depth; ++i) {
            HashSet<IntCond> thisLevel = this.getInternalLevel(i);
            HashSet<IntCond> otherLevel = other.getInternalLevel(i);

            HashSet<IntCond> lhsIntToAdd = new HashSet<>();

            if(thisLevel == null && otherLevel == null) {
                continue;
            }

            //Compare LHS internal values
            HashSet<IntCond> largerLHSInt = thisLevel;
            HashSet<IntCond> smallerLHSInt = otherLevel;
            if (largerLHSInt.size() < smallerLHSInt.size()) {
                largerLHSInt = otherLevel;
                smallerLHSInt = thisLevel;
            }
            for (IntCond iCond1 : largerLHSInt) {
                for (IntCond iCond2 : smallerLHSInt) {
                    if (iCond1.equals(iCond2)) {
                        lhsIntToAdd.add(iCond1);
                        break;
                    }
                }
            }
            lhsInt.add(lhsIntToAdd);
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
                    lhsExt.add(thisECond);
                    break;
                }
            }
        }

        //Compare RHS external values
        HashSet<ExtCond> largerRHSExt = this.rhsExternal;
        HashSet<ExtCond> smallerRHSExt = other.rhsExternal;
        if (largerRHSExt.size() < smallerRHSExt.size()) {
            largerRHSExt = other.rhsExternal;
            smallerRHSExt = this.rhsExternal;
        }
        for (ExtCond thisECond : largerRHSExt) {
            for (ExtCond otherECond : smallerRHSExt) {
                if (thisECond.equals(otherECond)) {
                    rhsExt.add(thisECond);
                    break;
                }
            }
        }

        EpRule ret = new EpRule(this.agent, this.action, lhsExt, lhsInt, rhsExt);
        return ret;
    }

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
    public void updateConfidencesForPrediction(Vector<HashSet<Integer>> lhsInt, SensorData lhsExt, SensorData rhsExt) {
        //Compare LHS internal values
        for(int i = 1; i <= timeDepth; ++i) {
            HashSet<IntCond> level = this.getInternalLevel(i);
            if(level == null) {
                continue;
            }
            HashSet<Integer> sensors = lhsInt.get(lhsInt.size() - i);
            for (IntCond iCond : level) {
                Integer sIdVal = iCond.sId;
                if (sensors.contains(sIdVal)) {
                    iCond.increaseConfidence();
                } else {
                    iCond.decreaseConfidence();
                }
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
        //TODO: no longer relevant with sister rules in place?
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

    /**
     * extendRuleDepth
     *
     * adds another depth to a rule assuming it has not reached the max depth
     */
    //TODO: this will need to be expanded to handle NDFA's
    public int extendRuleDepth() {
        if(this.timeDepth < PhuJusAgent.SHORT_TERM_SIZE) {
            this.timeDepth++;
        } else {
            return -1; // code failed
        }

        //Add "not" conditions for this level
        while(this.lhsNotInternal.size() < this.timeDepth) {  //should only iterate once...
            this.lhsNotInternal.add(new HashSet<>());
        }

        //Check to see if this new level is empty
        HashSet<IntCond> level = getInternalLevel(this.timeDepth);
        if(level.size() == 0) {
            return -2; // code failed
        }

        return this.timeDepth;
    }//extendRuleDepth

    /**
     * matchingLHSExpansion
     *
     * makes this rule's LHS different from a given rule's by expanding one or
     * both rules
     *
     * Note:  Caller is responsible for making sure the given rules have
     *        different RHS.
     *
     * Example input:
     *   #14: (8)|00a -> 00
     *   #8: (5)(8)|00a -> 01
     * Resulting output:
     *   #14: (7)(8)|00a -> 00
     *   #8: (5)(8)|00a -> 01
     *
     * @param other the other rule
     * @return 0 for success, negative for failure
     */
    public int matchingLHSExpansion(EpRule other) {
        //We always want 'other' to be the rule with more time levels
        if (other.timeDepth < this.timeDepth) return other.matchingLHSExpansion(this);

        //while 'this' is shorter, expand it until they are different
        double matchScore = this.compareLHS(other, this.timeDepth);
        int thisRuleDepthFlag = 0;
        while((this.timeDepth < other.timeDepth) && (matchScore == 1.0) && (thisRuleDepthFlag >= 0)) {
            thisRuleDepthFlag = this.extendRuleDepth();
            matchScore = this.compareLHS(other, this.timeDepth);
        }

        // Run until the rules are different on the LHS and can still be expanded
        int otherRuleDepthFlag = 0;
        while( (matchScore == 1.0) && (thisRuleDepthFlag >= 0) && (otherRuleDepthFlag >= 0) ) {
            thisRuleDepthFlag = this.extendRuleDepth();
            otherRuleDepthFlag = other.extendRuleDepth();
            matchScore = this.compareLHS(other);
        }

        //Check for and remove empty levels
        boolean foundEmptyLevels = false;
        while ((this.timeDepth > 0) && (this.getInternalLevel(this.timeDepth).size() == 0)) {
            foundEmptyLevels = true;
            this.timeDepth--;
        }
        while ((other.timeDepth > 0) && (other.getInternalLevel(other.timeDepth).size() == 0)) {
            foundEmptyLevels = true;
            other.timeDepth--;
        }

        if(foundEmptyLevels) {
            return -2;
        }

        return (matchScore == 1.0) ? -1 : 0;
    }//matchingLHSExpansion

    /**
     * matchingLHSNotFix
     *
     * makes this rule's LHS different from a given rule's by adding a "NOT"
     * condition to the shorter rule.  The expected input should be a rule
     * that matches this one up to a particular level in which this rule
     * has an empty level and the given rule does not.  Note: a failed call
     * to {@link #matchingLHSExpansion(EpRule)} will often yield a pair of
     * rules like this.
     *
     * Note that if there are multiple internal conditions in the deeper rule
     * that only one of these is used (e.g., there is no !11 in rule #14 below).
     *
     * //TODO: should we be less arbitrary about which sensor to negate?
     * Example input:
     *   #14: (8)|00a -> 00
     *   #8: (5,11)(8)|00a -> 01
     * Resulting output:
     *   #14: (!5)(8)|00a -> 00
     *   #8: (5,11)(8)|00a -> 01
     *
     * @param other the other rule
     * @return 0 for success, negative number for failure
     */
    public int matchingLHSNotFix(EpRule other) {
        //We always want 'other' to be the rule with more time levels
        if (other.timeDepth < this.timeDepth) return other.matchingLHSNotFix(this);

        //Invalid input: one rule should have more depth than the other
        if (other.timeDepth == this.timeDepth) return -1;

        //Expand the shorter rule one level and verify that it is empty
        this.timeDepth++;
        HashSet<IntCond> thisConds = this.getInternalLevel(this.timeDepth);
        if (thisConds.size() > 0) return -2;

        //Verify that the corresponding level in the other has at least one condition
        HashSet<IntCond> otherConds = other.getInternalLevel(this.timeDepth);
        if (otherConds.size() == 0) return -3;

        //Add a "not" sensor to 'this'
        HashSet<IntCond> notConds = getNotInternalLevel(this.timeDepth);
        notConds.add(otherConds.iterator().next());  //just take one

        return 0; //success
    }//matchingLHSNotFix

    /**
     * adjustSisterhood
     *
     * when a new candidate rule is discovered that matches an existing
     * rule sisterhood, this method resolves the outcome
     * @param theHood the EpRule in the sisterhood that has the same RHS as this
     * @return
     */
    public int adjustSisterhood(EpRule theHood) {

        // See if the rule can be expanded past the size of theHood
        while(this.timeDepth < theHood.timeDepth+1) {
            if (this.extendRuleDepth() < 0) {
                return -1; // candidate rule is too general
            }
        }

        // Just copy all internal conditions from cand to theHood
        theHood.extendRuleDepth();
        HashSet<IntCond> thisLevel = this.getInternalLevel(this.timeDepth);
        HashSet<IntCond> theHoodLevel = theHood.getInternalLevel((theHood.timeDepth));
        for(IntCond iCond : thisLevel) {
            theHoodLevel.add(iCond);
        }

        // Add _not_ conditions to the other members of the sisterhood
        for(EpRule r : theHood.sisters) {
            if(r.equals(theHood)) {
                continue; // we want to skip ourselves
            }

            r.matchingLHSNotFix(theHood);
        }

        // Extract theHood from the sisterhood
        if(theHood.sisters.size() == 2) {
            for(EpRule r : theHood.sisters) {
                if (r.equals(theHood)) {
                    continue; // we want to skip ourselves
                }
                r.sisters = new HashSet<>();
            }
        } else {
            theHood.sisters.remove(theHood);
        }

        theHood.sisters = new HashSet<>();

        return 0;
    }//adjustSisterhood




//region Getters and Setters

    public int getId() { return this.ruleId; }
    public char getAction() { return this.action; }
    public HashSet<ExtCond> getRHSConds() { return this.rhsExternal; }
    public int getTimeDepth() { return this.timeDepth; }

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
        for(HashSet<IntCond> level : this.lhsInternal) {
            for (IntCond iCond : level) {
                if (iCond.sId == sId) return true;
            }
        }
        return false;
    }

    /**
     * removeIntSensor
     *
     * is called to remove an internal sensor from any levels of this rule.
     * It is generally called when the rule associated with the sensor
     * is being removed.
     *
     * @return a success/fail code
     *         0 - sensor not found (no change made)
     *         n - a positive number indicates the sensor was removed from n levels
     *        -n - a negative number indicates the sensor was removed from n levels
     *             and the rule was made invalid (one or more empty levels) as a result.
     * */
    public int removeIntSensor(int sId) {
        int result = 0;
        boolean invalid = false;
        IntCond removeMe = null;
        for(HashSet<IntCond> level : this.lhsInternal) {
            for (IntCond iCond : level) {
                if (iCond.sId == sId) {
                    removeMe = iCond;
                    break;
                }
            }
            if (removeMe != null) {
                level.remove(removeMe);
                result++;
            }

            //If this level is now empty then this rule is now invalid
            if (level.size() == 0) invalid = true;
        }

        return invalid ? -result : result;
    }

    //helper method for getInternalLevel and getNotInternalLevel
    private HashSet<IntCond> getLevelHelper(int depth, Vector<HashSet<IntCond>> vec) {
        if (depth > vec.size()) return null; //bad input
        if (depth <= 0) return null; //also bad input

        int index = vec.size() - depth;
        return vec.get(index);
    }

    //Retrieve's the nth level of internal sensors from the end of this.lhsInternal
    public HashSet<IntCond> getInternalLevel(int depth) {
        return getLevelHelper(depth, this.lhsInternal);
    }

    //Retrieve's the nth level of _not_ internal sensors from the end of this.lhsInternal
    public HashSet<IntCond> getNotInternalLevel(int depth) {
        return getLevelHelper(depth, this.lhsNotInternal);
    }

    /**
     * Add a new sister rule.
     *
     * Important: this new sister rule should not already have sisters
     */

    public void addSister(EpRule sis) {
        sis.sisters = this.sisters; //share the HashSet
        sis.sisters.add(sis);
        if (sis.sisters.size() == 1) sis.sisters.add(this);
    }

    public HashSet<EpRule> getSisters() { return this.sisters; }


//endregion

}//class EpRule
