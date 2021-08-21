package agents.phujus;

import framework.SensorData;

import java.security.InvalidParameterException;
import java.util.*;

/**
 * class EpRule
 *
 * This is a rule that is based upon an episode in the agent's experience.
 */
public class EpRule extends BaseRule {
//region Inner Classes


    /**
     * class IntCond
     *
     * Tracks internal conditions (LHS or RHS)
     */
    public static class IntCond extends Confidence {
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

    //define the LHS of the rule.  This consists of:
    // - a set of internal sensor values indicating what other rules fired two timesteps ago
    // - a set of internal sensor values that should NOT be present for this rule to fire (initially none)
    // - a set of external sensor values that were present in the previous timestep
    // - the action that the agent took just before this rule was created (See BaseRule)
    //Initially, all sensor values are present but, over time, they may get culled if they
    //prove inconsistent with the agent's experiences
    //Note:  the instance variables for all but these not-sensors are inherited from BaseRule
    protected Vector<HashSet<IntCond>> lhsNotInternal;

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

    //How many timesteps of internal sensors are required for a match with this rule
    private int timeDepth = 0;

//region Ctors and Init
    /**
     * this ctor initializes the rule from the agent's current and previous episodes
     */
    public EpRule(PhuJusAgent agent, int timeDepth){
        super(agent);
        this.lhsNotInternal = new Vector<>();
        this.timeDepth = timeDepth;
    }

    /** convenience default for timeDepth = 0 */
    public EpRule(PhuJusAgent agent){
        this(agent, 0);
    }

    /**
     * this ctor initializes the rule from a specified merging of two rules
     */
    public EpRule(PhuJusAgent agent, char action, HashSet<ExtCond> lhsExternal,
                  Vector<HashSet<IntCond>> lhsInternal, HashSet<ExtCond> rhsExternal) {
        super(agent);

        //override some of the base behavior of BaseRule's ctor
        this.action = action;
        this.lhsExternal = lhsExternal;
        this.lhsInternal = lhsInternal;
        this.rhsExternal = rhsExternal;

        this.lhsNotInternal = new Vector<>();
    }



//endregion

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
                if (r.equals(this)) result.append('*');
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
     * lhsInternalLevelMatch
     *
     * is a helper method for {@link #lhsInternalMatchScore(HashSet)} it
     * compares all the sensors in a given set with its conditions.  The
     * sensors in one level of a rule's lhsInternal are treated as a
     * disjunction:  only one needs to match to get a score.  Therefore
     * this rule returns the highest confidence match it has for
     * any one condition with a sensor.
     *
     * @return highest matching confidence
     */
    private double lhsInternalLevelMatch(HashSet<Integer> lhsInt, HashSet<IntCond> level) {
        if (level == null) return 0.0;
        double best = 0.0;
        for (IntCond iCond : level) {
            Integer sIdVal = iCond.sId;
            if (lhsInt.contains(sIdVal)) {
                if (best < iCond.getConfidence()) {
                    best = iCond.getConfidence();
                    if (best == 1.0) return 1.0; //can't do better than 1.0
                }
            }
        }
        return best;
    }//lhsInternalLevelMatch

    /**
     * lhsNotInternalLevelMatch
     *
     * is a helper method for {@link #lhsInternalMatchScore(HashSet)} it
     * compares all the sensors in a given set with its not-conditions.  The
     * not-sensors in one level of a rule's lhsInternal are treated as a
     * conjunction:  all the not-sensors must be satisfied to get a score.
     *
     * @return highest confidence condition (or 0.0 if any condition is violated)
     */
    private double lhsNotInternalLevelMatch(HashSet<Integer> lhsInt, HashSet<IntCond> level) {
        if (level == null) return 1.0;  //no not-condition to violate
        double best = 0.0;
        for (IntCond iCond : level) {
            Integer sIdVal = iCond.sId;
            if (lhsInt.contains(sIdVal) && (iCond.getConfidence() > 0.0)) {
                return 0.0; //fail
            }
            if (best < iCond.getConfidence()) {
                best = iCond.getConfidence();
            }
        }
        return best;  //Note: this can still be 0.0 if getConfidence() returns 0.0
                      //      on all satisfied conditions
    }//lhsNotInternalLevelMatch

    /**
     * lhsInternalMatchScore
     *
     * is a helper method for {@link #lhsMatchScore(char, HashSet, SensorData)}
     * that creates a score for all levels of internal sensors.  Each
     */
    private double lhsInternalMatchScore(HashSet<Integer> lhsInt) {
        double score = 1.0;
        //Compare LHS internal values
        for(int i = 1; i <= timeDepth; ++i) {
            HashSet<IntCond> level = getInternalLevel(i);
            score = lhsInternalLevelMatch(lhsInt, level);
            HashSet<IntCond> notLevel = getNotInternalLevel(i);
            score *= lhsNotInternalLevelMatch(lhsInt, notLevel);
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
    @Override
    public double lhsMatchScore(char action, HashSet<Integer> lhsInt, SensorData lhsExt) {
        //If the action doesn't match this rule can't match at all
        double score = super.lhsMatchScore(action, lhsInt, lhsExt);

        score *= lhsInternalMatchScore(lhsInt);
        if (score <= 0.0) return 0.0;

        //Compare LHS external values
        score *= lhsExternalMatchScore(lhsExt);
        if (score <= 0.0) return 0.0;

        return score;

    }//lhsMatchScore

    /**
     * compareLHSCondSets
     *
     * is a helper for {@link #compareLHSIntLevel} that compares two HashSet<IntCond>
     *
     * @param level1  level to compare
     * @param level2  level to compare
     * @param useConf factor confidences into match score?
     * @return a two-cell array containing:  the size of the union and
     *         intersection of these sets.  If useConf is set, the union
     *         size is counted by confidence rather than 1.0 for each.
     */
    private double[] compareLHSCondSets(HashSet<IntCond> level1, HashSet<IntCond> level2, boolean useConf) {
        double[] result = new double[2];
        result[0] = 0.0;
        result[1] = 0.0;

        // Special Case: both null
        if(level1 == null && level2 == null) {
            return result;
        }

        //Special Case:  one is null
        if(level1 == null) {
            result[1] = level2.size();
            return result;
        }
        if(level2 == null) {
            result[1] = level1.size();
            return result;
        }

        //See how many conditions in 'this' are in 'other'
        for (IntCond thisCond : level1) {
            result[1]++;
            for (IntCond otherCond : level2) {
                if (thisCond.equals(otherCond)) {
                    if (useConf) {
                        result[0] += (thisCond.getConfidence() + otherCond.getConfidence()) / 2;
                    } else {
                        result[0]++;
                    }
                    break;
                }
            }
        }

        //See if there are any additional conditions in 'other' that are not in 'this'
        //We only need to count these since intersecting values were found above.
        for(IntCond otherCond : level2) {
            boolean found = false;
            for(IntCond thisCond : level1) {
                if (thisCond.equals(otherCond)) found = true;
            }
            if (!found) result[1]++;
        }

        return result;
    }//compareLHSCondSets


    /**
     * compareLHSIntLevel
     *
     * compares the LHSInternal sensors at a particular depth in this rule
     * those of another rule at the same depth
     *
     * @param other  other rule to compare to
     * @param depth  depth to compare at
     * @param useConf  should the confidences of the conditions be factored
     *                 into the match score?
     * @return a match score from 0.0 to 1.0
     */
    private double compareLHSIntLevel(EpRule other, int depth, boolean useConf) {
        //start with the positive conditions
        HashSet<IntCond> thisLevel = this.getInternalLevel(depth);
        HashSet<IntCond> otherLevel = other.getInternalLevel(depth);
        double[] ret = compareLHSCondSets(thisLevel, otherLevel, useConf);
        double sum = ret[0];
        double count = ret[1];

        //now do the "not" conditions
        thisLevel = this.getNotInternalLevel(depth);
        otherLevel = other.getNotInternalLevel(depth);
        ret = compareLHSCondSets(thisLevel, otherLevel, useConf);
        sum += ret[0];
        count += ret[1];

        if (count == 0.0) return 1.0; //both empty, count as match
        return sum / count;
    }//compareLHSIntLevel

    /** @return a match score for the LHS external of this rule and a given rule */
    private double compareLHSExternal(EpRule other) {
        //Compare LHS external values
        double sum = 0.0;
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
        return sum / ((double) largerLHSExt.size());
    }//compareLHSExternal

    /**
     * calculates how closely this rule matches another given rule
     *
     * @param other  the other rule to compare it to
     * @param depth compare up to this depth
     * @return  a match score from 0.0 to 1.0
     */
    @Override
    public double compareLHS(EpRule other, int depth) {

        //actions must match
        if (super.compareLHS(other, depth) == 0.0) return 0.0;

        //Independent score for each of the levels of internal sensors + 1 for external sensors
        double[] lhsScores = new double[depth + 1];
        for(int i = 1; i <= depth; ++i) {
            lhsScores[i] = compareLHSIntLevel(other, i, true);
        }

        lhsScores[0] = compareLHSExternal(other);

        //Final score is average of the score for each level
        //TODO:  why not product?
        double sum = 0.0;
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
    @Override
    public void updateConfidencesForPrediction(Vector<HashSet<Integer>> lhsInt, SensorData lhsExt, SensorData rhsExt) {
        super.updateConfidencesForPrediction(lhsInt, lhsExt, rhsExt);

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

    /**
     * extendTimeDepth
     *
     * adds another depth to a rule assuming it has not reached the max depth
     *
     * @return 0 on success, negative on fail
     */
    //TODO: this will need to be expanded to handle NDFA's
    public int extendTimeDepth() {
        if(this.timeDepth < PhuJusAgent.MAX_TIME_DEPTH) {
            this.timeDepth++;
        } else {
            return -1; // code failed
        }

        //Check to see if this new level is empty
        if(isEmptyLevel(this.timeDepth)) {
            timeDepth--;
            return -2; // code failed
        }

        //Add "not" conditions for this level
        while(this.lhsNotInternal.size() < this.timeDepth) {  //should only iterate once...
            this.lhsNotInternal.add(new HashSet<>());
        }

        return this.timeDepth;
    }//extendTimeDepth


    /**
     * resolveMatchingLHS
     *
     * makes this rule's LHS different from a given rule's by increasing
     * the time depth of one or both rules
     *
     * Example input:
     *   #14: (8)|00a -> 00
     *   #8: (5)(8)|00a -> 01
     * Resulting output:
     *   #14: (7)(8)|00a -> 00
     *   #8: (5)(8)|00a -> 01
     *
     * @param other the other rule
     * @return 0 for success, negative for failure on error, 1 for failure due to exact match
     */
    public int resolveMatchingLHS(EpRule other) {
        //We always want 'other' to be the rule with more time levels
        if (other.timeDepth < this.timeDepth) return other.resolveMatchingLHS(this);

        //save their original depths to restore later if things go wrong
        int thisOrigDepth = this.timeDepth;
        int otherOrigDepth = other.timeDepth;

        //get the initial match score
        double matchScore = compareLHSIntLevel(other, this.timeDepth, false);
        if (this.timeDepth == 0) {
            matchScore = compareLHSExternal(other);
        }

        //Sanity check:  should match else why did you call this method?!
        if (matchScore != 1.0) return -3;

        //while 'this' is shorter, expand it until they are different
        while((this.timeDepth < other.timeDepth) && (matchScore == 1.0)) {
            // (Note: can't use extendTimeDepth() for this because we need less err checking)
            this.timeDepth++;
            while(this.lhsNotInternal.size() < this.timeDepth) {
                this.lhsNotInternal.add(new HashSet<>());
            }
            matchScore = compareLHSIntLevel(other, this.timeDepth, false);
        }//while

        //If rules are equal depth and still no diff, we need to keep expanding both
        while ((this.timeDepth == other.timeDepth) && (matchScore == 1.0)) {
            //check for max depth
            if (this.timeDepth == PhuJusAgent.MAX_TIME_DEPTH) break;

            //extend both rules
            // (Note: can't use extendTimeDepth() for this because we need less err checking)
            this.timeDepth++;
            other.timeDepth++;
            this.lhsNotInternal.add(new HashSet<>());
            other.lhsNotInternal.add(new HashSet<>());


            //check for both empty (unresolveable match)
            if ( (this.getInternalLevel(this.timeDepth).size() == 0)
                    && (this.getInternalLevel(this.timeDepth).size() == 0) ) {
                this.timeDepth = thisOrigDepth;
                other.timeDepth = otherOrigDepth;
                return -2;
            }

            //Now compare again
            matchScore = compareLHSIntLevel(other, this.timeDepth, false);
        }//while

        //Still a complete match?
        if (matchScore == 1.0) return 1;

        //If we reach this point the rules now mismatch in some way at this depth
        //Extract the mis-matching levels
        HashSet<IntCond> thisLevel = this.getInternalLevel(this.timeDepth);
        HashSet<IntCond> otherLevel = other.getInternalLevel(this.timeDepth);
        HashSet<IntCond> thisNotLevel = this.getNotInternalLevel(this.timeDepth);
        HashSet<IntCond> otherNotLevel = other.getNotInternalLevel(this.timeDepth);

        //DEBUG:  this if statement should never be true
        if ((thisLevel == null)  || (otherLevel == null)
                || (thisNotLevel == null) || (otherNotLevel == null)) {
            agent.debugPrintln("HELP!!");
            return -3;
        }

        //Special Case:  if the RHS match then we can't resolve by negating a condition
        //               because we get silly rules like this:  (-3)|00a -> 10 and (3)|00a -> 10
        if ((thisLevel.size() == 0) || (otherLevel.size() == 0)) {
            if (this.rhsMatchScore(other.getRHSExternal()) == 1.0) {
                return 1;
            }
        }


        //Handle all cases where one or both rules lack positive sensors
        if ((thisLevel.size() > 0) && (otherLevel.size() == 0)){
            //Example:  (3)(4)00a->01  --  ()(4)00a->01 ==> (!3)(4)00a->01
            //Example:  (3)(4)00a->01  --  (!6)(4)00a->01 ==> (!3,!6)(4)00a->01
            otherNotLevel.add(thisLevel.iterator().next());
        } else if ((thisLevel.size() == 0) && (otherLevel.size() > 0)){
            //Example: as above but reversed
            thisNotLevel.add(otherLevel.iterator().next());
        } else if ((thisLevel.size() == 0) && (otherLevel.size() == 0)){
            //Example:  (!3)(4)00a->01  --  ()(4)00a->01 ==> (3)(4)00a->01
            //Example:  (!3)(4)00a->01  --  (!5)(4)00a->01 ==> (3,!5)(4)00a->01
            if (thisNotLevel.size() > 0) {
                otherLevel.add(thisNotLevel.iterator().next());
            }
            //This can't be an else-if because both could be true (see 2nd Example)
            if (otherNotLevel.size() > 0) {
                thisLevel.add(otherNotLevel.iterator().next());
            }
        }

        return 0;
    }//resolveMatchingLHS

    /**
     * TODO
     * @param epRule
     */
    private void mergeWith(EpRule epRule) {
    }

    /**
     * spawn
     *
     * creates a child node from this one that has the same content but +1 depth
     * @return the new rule or null if the next level was empty
     */
    private EpRule spawn() {
        if (isEmptyLevel(this.timeDepth + 1)) return null;
        EpRule child = new EpRule(this.agent, this.action, this.lhsExternal,
                this.lhsInternal, this.rhsExternal);
        child.timeDepth = this.timeDepth + 1;
        this.children.add(child);
        return child;
    }


    /**
     * resolveRuleConflict
     *
     * is called when a new EpRule has the same LHS but different RHS from an
     * existing rule and this conflict needs to be resolved.  This is done with
     * two steps:
     *  a) the extant rule's confidence level is decreased
     *  b) if possible, the extant rule is expanded to create a child (or more
     *     distant descendant) that differentiates it from 'this'
     *
     * Note:  The caller is responsible for guaranteeing the following:
     *  - that 'this' is a new rule not yet added to the agent's ruleset
     *  - that 'this' and 'extant' have the same time depth
     *  - that 'this' and 'extant' having matching LHS
     *  - that 'this' also matches the agent's current RHS but 'extant' does not
     *
     *  Warning:  This method is recursive.
     *
     * @param extant the existing rule
     * @return 0 for success, negative for failure (duplicate rule)
     */
    public int resolveRuleConflict(EpRule extant) {
        //due to conflict we can't be confident in either rule
        this.decreaseConfidence();
        extant.decreaseConfidence();

        //If they also have a RHS match, merge the rules
        double rhsMatchScore = this.rhsMatchScore(extant.getRHSExternal());
        if (rhsMatchScore == 1.0) {
            extant.mergeWith(this);
            return -1;
        }

        //Expand the rules
        EpRule thisChild = this.spawn();
        EpRule extantChild = extant.spawn();

        //If the new children also match we need to recurse to resolve them
        if ((thisChild != null) && (extantChild != null)) {
            if (thisChild.compareLHS(extantChild, thisChild.timeDepth) == 1.0) {
                thisChild.resolveRuleConflict(extantChild);  //Note:  this should always return 0
            }
        }

        return 0;
    }//resolveRuleConflict


    /** remove myself from my sisterhood (which may cause the sisterhood to be dissolved) */
    private void removeFromSisterhood() {
        // Extract ourself from the sisterhood
        if (this.sisters.size() == 0) {
            return;  //no sisterhood to remove from
        }
        else if (this.sisters.size() == 1) {
            this.sisters = new HashSet<>(); //this should never happen...
        }
        else if(this.sisters.size() == 2) {
            for(EpRule r : this.sisters) {
                r.sisters = new HashSet<>();
            }
        } else {  //size 3+
            //since all sisters share the sisters list just remove myself from it
            this.sisters.remove(this);
            this.sisters = new HashSet<>();
        }
    }

    /**
     * adjustSisterhood
     *
     * when a new candidate rule is discovered that matches an existing
     * rule sisterhood, this method resolves the outcome.  If this method
     * is successful, the new rule should replace the old one in the
     * agent's rule set
     *
     * @param equiv the EpRule in the sisterhood that has the same RHS as 'this'
     * @return 0 if success, negative otherwise
     */
    public int adjustSisterhood(EpRule equiv) {

        //We can't adjust anything if there is no resolution
        int ret = resolveMatchingLHS(equiv);
        if (ret < 0) return ret;

        //Since that worked, replace equiv with 'this' in the sisterhood
        equiv.addSister(this);
        equiv.removeFromSisterhood();

        //Remove any rules that can now be differentiated from their sisters
        //Future:  if there is a speed bottleneck here you can adjust this nested
        //         loop to iterate through a vector using indexes and, thus, the
        //         inner loop can start with "for(int j = i+1" to speed this up.
        Vector<EpRule> toRemove = new Vector<>();
        for(EpRule sis1 : this.sisters) {
            boolean remove = true;
            for(EpRule sis2 : this.sisters) {
                if (sis1.equals(sis2)) continue; //don't compare to self
                //Are they already different?
                double diff = sis1.compareLHS(sis2, Math.min(sis1.timeDepth, sis2.timeDepth));
                if (diff < 1.0) continue;

                //Can they be made different
                ret = sis1.resolveMatchingLHS(sis2);
                if (ret != 0) remove = false;
            }

            if (remove) toRemove.add(sis1);
        }

        //remove everything else
        for(EpRule sis : toRemove) {
            sis.removeFromSisterhood();
        }

        return 0;
    }//adjustSisterhood



    /**
     * removeIntSensor
     *
     * is called to remove an internal sensor from any levels of this rule.
     * It is generally called when the rule associated with the sensor
     * is being removed.
     *
     * Warning:  if removing a sensor creates an empty level, the rule
     *           is truncated to remove that level!
     *
     * @param oldId   the sensor id being removed
     * @param newId   the sensor to replace it with (or -1 if none)
     *
     * @return 0 = no changes made
     *         n = this many replacements were performed
     *        -n = this many replacements were performed and the rule was truncated
     */
    public int removeIntSensor(int oldId, int newId) {
        int removeCount = 0;
        int truncateFlag = 1;
        for(int depth=1; depth <= PhuJusAgent.MAX_TIME_DEPTH; ++depth) {
            HashSet<IntCond> level = getInternalLevel(depth);
            if (level == null) continue;
            IntCond removeMe = null;
            for (IntCond iCond : level) {
                if (iCond.sId == oldId) {
                    removeCount++;
                    if (newId >= 1) {
                        iCond.sId = newId;  //replace
                    } else {
                        removeMe = iCond;  //mark for removal
                    }
                    break;
                }
            }
            //remove the offending condition from this level
            if (removeMe != null) {
                level.remove(removeMe);

                //If this level is now empty then this rule may need to be truncated
                if (isEmptyLevel(depth)) {
                    //only need to truncate if this level is part of the rule right now
                    if (this.timeDepth >= depth) {
                        this.timeDepth = depth - 1;
                        truncateFlag = -1;
                    }
                }//if empty
            }//if remove

        }//for each depth

        return removeCount * truncateFlag;

    }//removeIntSensor

//region Getters and Setters

    public char getAction() { return this.action; }
    public HashSet<ExtCond> getRHSConds() { return this.rhsExternal; }
    public int getTimeDepth() { return this.timeDepth; }




    /** @return true if the given sensor is one of the conditions of this rule */
    public boolean testsIntSensor(int sId) {
        for(HashSet<IntCond> level : this.lhsInternal) {
            for (IntCond iCond : level) {
                if (iCond.sId == sId) return true;
            }
        }
        return false;
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

    /** @return true if both the sensors and not-sensors at this level are null or zero-length */
    public boolean isEmptyLevel(int depth) {
        HashSet<IntCond> level = getInternalLevel(depth);
        if ((level != null) && (level.size() > 0)) return false;
        level = getNotInternalLevel(depth);
        if ((level != null) && (level.size() > 0)) return false;
        return true;
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

    /** @return the max depth for which this rule has non-empty LHS internal data */
    public int maxTimeDepth() {
        int max = timeDepth + 1;
        while (max <= PhuJusAgent.MAX_TIME_DEPTH) {
            if (getInternalLevel(max).size() == 0) break;
            max++;
        }
        return max - 1;
    }


//endregion


    //DEBUG
    @Override
    public String toStringAllLHS() {
        StringBuilder result = new StringBuilder();
        for (int i = this.lhsInternal.size(); i >= 1; --i) {
            result.append('(');
            HashSet<EpRule.IntCond> level = this.lhsInternal.get(this.lhsInternal.size() - i);
            String intStr = toStringIntConds(level, false, false);
            result.append(intStr);
            result.append(')');
            if (i == this.timeDepth) result.append(" // "); //time depth divider
        }

        if (this.timeDepth == 0) result.append(" // ");

        return result.toString();
    }


}//class EpRule

