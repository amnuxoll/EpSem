package agents.phujus;

import framework.SensorData;

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
        public int sId;  //sensor id

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

    //The LHS of an EpRule consists of:
    // - a set of internal sensor values indicating what other rules fired two timesteps ago
    // - a set of internal sensor values that should NOT be present for this rule to fire (initially none)
    // - a set of external sensor values that were present in the previous timestep
    // - the action that the agent took just before this rule was created (See BaseRule)
    //Initially, all sensor values are present but, over time, they may get culled if they
    //prove inconsistent with the agent's experiences
    //Note:  the instance variables for all but these not-sensors (below) are inherited from BaseRule
    protected Vector<HashSet<IntCond>> lhsNotInternal;

    //How many timesteps of internal sensors are required for a match with this rule
    private int timeDepth = 0;

//region Ctors and Init
    /** this ctor initializes the rule from the agent's current and previous episodes */
    public EpRule(PhuJusAgent agent, int timeDepth){
        super(agent);
        this.lhsNotInternal = new Vector<>();
        this.timeDepth = timeDepth;
    }

    /** convenience default for timeDepth = 0 */
    public EpRule(PhuJusAgent agent){
        this(agent, 0);
    }

    /** this ctor initializes the rule from the properties of an existing rule */
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
     * lhsInternalLevelMatch
     *
     * is a helper method for {@link #lhsInternalMatchScore(HashSet)} it
     * compares all the sensors in a given set with its conditions.  The
     * sensors in one level of a rule's lhsInternal are treated as a
     * disjunction:  only one needs to match to get a score.  Therefore,
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
     * that creates a score for all levels of internal sensors.
     */
    private double lhsInternalMatchScore(HashSet<Integer> lhsInt) {
        //match irrelevant at zero depth
        if (this.timeDepth == 0) return 1.0;

        //Compare LHS internal values (disjunction)
        double score = -1.0;  //any match score will beat this one
        for(int i = 1; i <= timeDepth; ++i) {
            HashSet<IntCond> level = getInternalLevel(i);
            score = Math.max(score, lhsInternalLevelMatch(lhsInt, level));
        }//for timedepth

        //Special case:  no matching positive conditions (prev loop did nothing)
        if (score == -1.0) score = 1.0;

        //Check LHS internal "not" sensors (conjunction)
        for(int i = 1; i <= timeDepth; ++i) {
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
        if (score == 0.0) return 0.0;

        score *= lhsExternalMatchScore(lhsExt);
        if (score == 0.0) return 0.0;

        score *= lhsInternalMatchScore(lhsInt);

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
                if (thisCond.equals(otherCond)) {
                    found = true;
                    break;
                }
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
    public double compareLHSIntLevel(EpRule other, int depth, boolean useConf) {
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
     * Note:  to support NDFAs this would need to be expanded.
     *
     * @return 0 on success, negative on fail
     */
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

    /** @return true if one HashSet<IntCond> is a superset of the other */
    private boolean isSuperSet(HashSet<IntCond> a, HashSet<IntCond> b) {
        //handle null params
        if (a == null) return (b == null);
        if (b == null) return false;

        return a.containsAll(b);
    }//isSuperSet

    /**
     * mergeWith
     *
     * attempts to merge two rules together that are identical at their current depth.
     *
     * @param shadow will be cannibalized to merge its parts into this EpRule
     *
     * @return true if shadow was absorbed by this
     */
    public boolean mergeWith(EpRule shadow) {
        //See if one rule is a superset of the other
        boolean thisSuper = true;
        boolean shadSuper = true;
        int maxDepth = Math.max(this.maxTimeDepth(), shadow.maxTimeDepth());
        for(int i = this.timeDepth + 1; i < maxDepth; ++i) {
            HashSet<IntCond> thisLevel = getInternalLevel(i);
            HashSet<IntCond> shadLevel = shadow.getInternalLevel(i);

            if (! isSuperSet(thisLevel, shadLevel)) {
                thisSuper = false;
                if (! shadSuper) break;
            }

            if (! isSuperSet(shadLevel, thisLevel)) {
                shadSuper = false;
                if (! thisSuper) break;
            }
        }

        //Easy case:  shadow has nothing to add
        if (thisSuper) return true;

        //Easy case:  they can't be merged
        if (!shadSuper) return false;

        //Tricky Case:  do the merge
        for(int i = this.timeDepth + 1; i < maxTimeDepth(); ++i) {
            HashSet<IntCond> thisLevel = getInternalLevel(i);
            HashSet<IntCond> shadLevel = shadow.getInternalLevel(i);
            //ok to add all since it's a HashSet there will be no duplicates
            thisLevel.addAll(shadLevel);
        }

        return true;
    }//mergeWith

    /**
     * spawn
     *
     * Creates a new rule that is one time depth greater than its parent
     *
     * @return null if the child could not be created.  Caller should
     *          handle this possibility!
     */
    @Override
    public EpRule spawn() {
        EpRule child = super.spawn();
        child.timeDepth = this.timeDepth;
        if (child.extendTimeDepth() < 0) return null;  //could not be extended
        return child;
    }

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


    //region Debug Printing Methods

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
        result.append(toStringShortRHS(this.rhsExternal));

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
        //rule #
        StringBuilder result = new StringBuilder();
        result.append("#");
        if (this.ruleId < 10) result.append(" ");
        result.append(this.ruleId);
        result.append(": ");
        result.append(toStringShort());
        result.append("    ");

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

        //Activation & Accuracy
        result.append(" ^ ");
        result.append(String.format("act=%.5f", calculateActivation(agent.getNow())));
        result.append(String.format("  acc=%.5f", getAccuracy()));

        return result.toString();
    }//toString

    //endregion

    //region Getters and Setters

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
        return (level == null) || (level.size() <= 0);
    }




    /** @return the max depth for which this rule has non-empty LHS internal data */
    public int maxTimeDepth() {
        int max = timeDepth + 1;
        while (max <= PhuJusAgent.MAX_TIME_DEPTH) {
            HashSet<IntCond> level = getInternalLevel(max);
            if ((level == null) || (level.size() == 0)) break;
            max++;
        }
        return max - 1;
    }


    //endregion


}//class EpRule

