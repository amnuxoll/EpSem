package agents.phujus;

import framework.SensorData;

import java.net.IDN;
import java.util.*;

/**
 * class TFRUle
 *
 * A "term frequency" rule that is generated from a pair of sequential episodes.
 * The rule tracks a "term frequency" for each of the agent's sensors that existed
 * at the time it was created.  Each time the rule matches, the TF values are
 * updated to reflect how frequently each corresponding sensors value was
 * on/off.  For example, if a sensor was on 2/3 of the time, the TF value
 * in the rule for that sensor would be ~0.67.
 */
public class TFRule extends Rule {

    //region Inner Classes

    //This is a mapping of external sensor names to ids.  Add more sensor names as needed.
    //The actual id used is the negative index in this array.
    public static final String[] IDNAMEMAP = {"GOAL", "IS_ODD", "NOISE1", "NOISE2"};

    /**
     * class Cond
     *
     * Tracks the tern frequency of one condition of a rule (LHS or RHS)
     *
     */
    public static class Cond implements Comparable<Cond> {
        public int sId;  //unique id for this condition (used by internal sensors)
        //Note: use float so we can do division.  Don't use double to save RAM
        public float numMatches;
        public float numOn;

        /** maps ane external sensor name to a unique integer.
         * Negative numbers are used to avoid conflict with rule ids. */
        public static int sensorNameToId(String name) {
            for(int i = 0; i < IDNAMEMAP.length; ++i) {
                if (IDNAMEMAP[i].equals(name)) return -i;
            }
            throw new IllegalArgumentException();
        }

        /** extracts a sensor name given an external sensor id */
        public static String sensorIdToName(int id) {
            if (id > 0) return "" + id;  //internal sensor
            id = -id;  //change external id to a non-negative index
            if (id >= IDNAMEMAP.length) throw new IllegalArgumentException();
            return IDNAMEMAP[id];
        }



        public Cond(int initId, boolean initVal) {
            this.sId = initId;
            this.numMatches = 1.0f;
            this.numOn = initVal ? 1.0f : 0.0f;
        }

        /** convenience ctor for external sensors */
        public Cond(String sensorName, boolean initVal) {
            this(sensorNameToId(sensorName), initVal);
        }

        public double getTF() {
            return this.numOn / this.numMatches;
        }
        public String getName() { return sensorIdToName(this.sId); }

        @Override
        public String toString() {
            return  getName() + "=" + String.format("%.2f", getTF());
        }

        public String toStringShort() {
            //round to the nearest value (true or false)
            return "" +  ((getTF() >= 0.5) ? 1 : 0);
        }

        @Override
        public int compareTo(Cond o) {
            return this.sId - o.sId;
        }

    }//class Cond

    //endregion

    //region Instance Variables


    //the action of the rule
    private final char action;

    //the conditions of the LHS and RHS
    private final HashSet<Cond> lhsInternal;
    private final HashSet<Cond> lhsExternal;
    private final HashSet<Cond> rhsExternal;

    // This is the entry in lhsInternal which MUST match in order for this rule
    // to fire.  If this is set to 'null' then this is a "base rule" or
    // "depth 0" rule.  Base rules have an empty lhsInternal.
    private Cond primaryInternal = null;

    // The time depth of the primaryInternal sensor (max number of steps to
    // get to base rules working backward via primary internal sensors)
    private final int timeDepth;

    //endregion

    /** ctor initializes this rule from the agents current state
     *  @param primary is the rule that will be the primary condition.  If a base
     *                   rule is desired, specify null for this parameter.
     *  */
    public TFRule(PhuJusAgent agent, TFRule primary) {
        super(agent);

        this.action = agent.getPrevAction();
        this.lhsExternal = initExternal(agent.getPrevExternal());
        this.rhsExternal = initExternal(agent.getCurrExternal());
        if (primary == null) {  //base rule
            this.lhsInternal = new HashSet<>(); //set to empty
            this.primaryInternal = null;
            this.timeDepth = 0;
        } else {                //non-base rule
            this.lhsInternal = initInternal(primary);
            this.timeDepth = primary.timeDepth + 1;
        }
    }//TFRule

    /**
     * ctor to initalize a tf rule given an action, LHSInt, LHSExt, RHSExt,
     * and confidence.  This is used by unit tests.
     *
     * Note:  the first entry in lhsInt will be made primary
     */
    public TFRule(PhuJusAgent agent, char action, int[] lhsInt,
                  SensorData lhsExt, SensorData rhsExt, double conf) {
        super(agent);

        this.action = action;

        this.lhsInternal = initInternal(lhsInt);
        this.lhsExternal = initExternal(lhsExt);
        this.rhsExternal = initExternal(rhsExt);

        this.primaryInternal = null;  //base rule
        this.timeDepth = 0;

        this.confidence.setConfidence(conf);
    }//TFRule

    /**
     * initInternal
     *
     * Creates an instance of Cond for each sensor in the input HashSet and returns
     * them as a HashSet.
     *
     * @param primary is the rule that will be the primary condition.  If a base
     *      *         rule is desired, specify null for this parameter.
     *
     * @return HashSet of IntConds for each sensor
     */
    private HashSet<Cond> initInternal(TFRule primary){
        // Gets the previous internal sensors that were on
        HashSet<Integer> prevInternal = agent.getFlatPrevInternal();

        //Add a condition to this rule for each sensor
        HashSet<Cond> conds = new HashSet<>();
        for(int depth = 0; depth < agent.getTfRules().size(); ++depth) {
            Vector<TFRule> subList = agent.getTfRules().get(depth);
            for (TFRule r : subList) {
                //to avoid a chicken-egg problem, don't add conditions for
                // rules that were just created this time step
                if (agent.intPctCache.length <= r.ruleId) continue;

                boolean on = prevInternal.contains(r.ruleId);
                Cond cond = new Cond(r.ruleId, on);
                conds.add(cond);

                if (r.equals(primary)) {
                    this.primaryInternal = cond;
                }
            }
        }

        return conds;
    }//initInternal

    /** This version takes an array of rule Ids.  The first entry in 'in' is
     * made primary */
    private HashSet<Cond> initInternal(int[] in) {
        HashSet<Cond> set = new HashSet<>();
        if(in == null)
            return set;

        for(int id : in){
            Cond cond = new Cond(id, true);
            set.add(cond);
            if (this.primaryInternal == null) {
                this.primaryInternal = cond;
            }
        }

        return set;
    }//initInternal

    /** converts from SensorData (used by the FSM environment) to HashSet<ExtCond> */
    private HashSet<Cond> initExternal(SensorData sData) {

        HashSet<Cond> result = new HashSet<>();
        for(String sName : sData.getSensorNames()) {
            result.add(new Cond(sName, (Boolean)sData.getSensor(sName)));
        }

        return result;
    }//initExternal

    /**
     * helperMatch
     *
     * Helper function for comparing two sets of Cond objects.
     * This is a helper for {@link #isExtMatch}.
     *
     * @return whether the sets are equal
     */
    private boolean helperMatch(HashSet<Cond> condSet1, HashSet<Cond> condSet2) {

        // First checks that the size of the two sets are equal.
        // If so, returns false if the converted sensors don't contain the external conditions.
        if (condSet1.size() != condSet2.size()) return false;

        for(Cond cond1 : condSet1) {
            boolean found = false;
            for(Cond cond2 : condSet2) {
                if (cond1.sId == cond2.sId) {
                    // Use TF values to see if they have the same value
                    // (It's not perfect but it's the best we have)
                    boolean on1 = (cond1.getTF() > 0.0);
                    boolean on2 = (cond2.getTF() > 0.0);
                    if (on1 == on2) {
                        found = true;
                        break;
                    }
                }
            }
            if (!found) return false;
        }

        return true;
    }//helperMatch

    /**
     * helperMatch
     *
     * Helper function for checking whether the conditions of the TFRule match the
     * current or previous SensorData of the agent.
     * This is a helper for {@link #isExtMatch}.
     *
     * @param conditions the external conditions of the TFRule
     * @param sensors the current or previous SensorData
     * @return whether the conditions and sensors match
     */
    private boolean helperMatch(HashSet<Cond> conditions, SensorData sensors) {
        // Converts SensorData to HashSet<Cond> for comparison
        HashSet<Cond> condSet2 = initExternal(sensors);

        return helperMatch(conditions, condSet2);
    }


    /**
     * isExtMatch
     *
     * Checks if this rule matches the agent's most recent experience.
     * This method only checks external sensors for comparison.
     *
     * @return whether the rule matches this rule
     */
    public boolean isExtMatch(char compAction,
                              SensorData prevExt,
                              SensorData currExt) {

        if (this.action != compAction) return false;
        if (!helperMatch(this.lhsExternal, prevExt)) return false;
        return helperMatch(this.rhsExternal, currExt);

    }//isExtMatch

    /**
     * isExtMatch
     *
     * Checks if a given rule matches some other rule
     * (external sensors and action only)
     */
    public boolean isExtMatch(TFRule other) {
        if (this.action != other.action) return false;
        if (! helperMatch(this.lhsExternal, other.lhsExternal)) return false;
        return helperMatch(this.rhsExternal, other.rhsExternal);
    }

    /**
     * isLHSExtMatch
     *
     * Checks if the LHS of a given rule matches gives action and LHS ext.
     *
     */
    public boolean isLHSExtMatch(char action, SensorData lhsExt) {
        if (this.action != action) return false;
        return helperMatch(this.lhsExternal, lhsExt);
    }

    /**
     * isLHSExtMatch
     *
     * Checks if the LHS of a given rule matches some other rule
     * (external sensors and action only)
     *
     * @return whether the rule matches the agent's most recent experience
     */
    public boolean isLHSExtMatch(TFRule other) {
        if (this.action != other.action) return false;
        return helperMatch(this.lhsExternal, other.lhsExternal);
    }



    /**
     * updateTFVals
     *
     * updates the TFVals for this rule by looking at the agent's current
     * sensor values (given by the caller).  The presumption is that this
     * method is called whenever this rule fires.
     */
    public void updateTFVals(HashSet<Integer> internalLHSIds,
                             SensorData externalLHS,
                             SensorData externalRHS){

        for(Cond cond: this.lhsInternal){
            cond.numMatches++;
            if(internalLHSIds.contains(cond.sId)) {
                cond.numOn++;
            }
        }

        //TODO: In the medium term I want to explore more flexible matching
        // with the ext sensors.  However, it's a non-trivial thing to get
        // working well so it seems wise to require exact match for now
        // which means no tfidf matching for external sensors yet.
//        for(Cond cond : this.lhsExternal) {
//            cond.numMatches++;
//            if(externalLHS.hasSensor(cond.sName)) {
//                boolean sVal = (boolean)externalLHS.getSensor(cond.sName);
//                if (sVal) cond.numOn++;
//            }
//        }

//        for(Cond cond : this.rhsExternal) {
//            cond.numMatches++;
//            if(externalRHS.hasSensor(cond.sName)) {
//                boolean sVal = (boolean)externalRHS.getSensor(cond.sName);
//                if (sVal) cond.numOn++;
//            }
//
//        }


    }//updateTFVals

    /**
     * rhsMatchScore
     *
     * calculates how closely this rule matches a given rhs sensor set.
     *
     * Note that if the GOAL sensor is on, then the other sensors don't matter.
     * This is only true on the RHS match!
     *
     * @param rhsExt the rhs external sensorData
     * @return  a match score from -1.0 to 1.0
     */
    public double rhsMatchScore(SensorData rhsExt) {
        double score = 0.0;

        // Loops through all external sensors of the rule and checks if the incoming
        // sensor data contains the external sensor
        double overallRelevance = 0.0;
        for (Cond eCond : this.rhsExternal) {
            // If so, then we calculate the TF/DF value to be added to the score
            if (rhsExt.hasSensor(eCond.getName())) {

                // Calculates the TF and DF values, and if the sensor values are the same
                double tfValue = eCond.getTF();
                double dfValue = agent.getExternalPercents().get(eCond.getName()).getSecond();
                Boolean sVal = (Boolean) rhsExt.getSensor(eCond.getName());

                //Special case:  GOAL=true for both sensors and rule is always a full match
                //               and the other sensors are ignored
                if ( (eCond.getName().equals(SensorData.goalSensor))
                        && (sVal) && (tfValue > 0.5)) {
                    score = tfValue;
                    overallRelevance = tfValue;
                    break;
                }

                double[] result = calculateTFIDF(tfValue, dfValue, sVal);
                score += result[0] * result[1];
                overallRelevance += result[1];      //score relevance
            }
        }

        return score/overallRelevance;
    }//rhsMatchScore

    /**
     * lhsExtMatchScore
     *
     * calculates how closely this rule matches a given action and lhs sensors
     *
     * @param lhsExt the lhs external sensorData
     *
     * @return  a match score from -1.0 to 1.0
     */
    public double lhsExtMatchScore(SensorData lhsExt){

        double score = 0.0;
        double overallRelevance = 0.0;
        if (! PhuJusAgent.TFIDF) overallRelevance = lhsExt.size();
        // Loops through all external sensors of the rule and checks if the incoming
        // sensor data contains the external sensor
        for (Cond eCond : this.lhsExternal) {
            // If so, then we calculate the TF/DF value to be added to the score
            if (lhsExt.hasSensor(eCond.getName())) {

                // Calculates the TF and DF values, and if the sensor values are the same
                double tfValue = eCond.getTF();
                double dfValue = 0.0;

                // Sometimes, when loading rules from a file, this can be null. This check helps
                // prevent that
                if (agent.getExternalPercents().containsKey(eCond.getName())) {
                    dfValue = agent.getExternalPercents().get(eCond.getName()).getSecond();
                }
                Boolean wasOn = (Boolean) lhsExt.getSensor(eCond.getName());

                if (PhuJusAgent.TFIDF) {
                    // Adds the TF/DF to the current score
                    double[] result = calculateTFIDF(tfValue, dfValue, wasOn);
                    score += result[0] * result[1];  //tfidf score
                    overallRelevance += result[1];  //relevance of score
                } else {
                    score += 1.0;
                }
            }
        }
        // no sensors so can't match
        if(overallRelevance == 0.0) return 0.0;
        return score/overallRelevance;
    }//lhsExtMatchScore


    /**
     * lhsIntMatchScore
     *
     * calculates how closely this rule matches a given action and lhs sensors
     *
     * @param lhsInt a HashSet of integers containing the internal sensors that were on
     *
     * @return  a match score from -1.0 to 1.0
     */
    public double lhsIntMatchScore(HashSet<Integer> lhsInt){

        //If this is a base rule then it always matches
        if (this.timeDepth == 0) return 1.0;

        //If the primary internal sensor is off this rule may not match
        if (! lhsInt.contains(this.primaryInternal.sId)) return -1.0;

        double score = 0.0;
        double overallRelevance = 0.0;

        //Increase the score for each condition it has that matches the given LHS
        for(int depth = 0; depth < lhsInt.size(); depth++) {

            for (Cond cond : this.lhsInternal) {
                // Calculates the TF and DF values, and if the sensor values are the same
                double tf = cond.getTF();
                double df = agent.intPctCache[cond.sId];
                boolean wasOn = lhsInt.contains(cond.sId);
                double[] result = calculateTFIDF(tf, df, wasOn);
                double tfidf = result[0];

                score += tfidf * result[1];
                overallRelevance += result[1];
            }
        }

        //Penalize the score for each given LHS entry that it does not expect
        for(int sId : lhsInt) {
            if (! testsIntSensor(sId)) {
                overallRelevance += agent.intPctCache[sId];
            }
        }

        //When relevance is zero we have to avoid a div-by-zero.
        //To do this, we detect possible reasons for the situation
        if (overallRelevance == 0.0) {
            //Possible cause #1:  tf == df for all matches
            if (lhsInt.contains(this.primaryInternal.sId)) return 1.0;
            //We shouldn't ever reach this point...
            return -1.0;
        }

        return score/overallRelevance;  //AND operator
    }//lhsIntMatchScore


    /**
     * lhsMatchScore
     *
     * calculates how closely this rule matches a given action and lhs sensors
     *
     * TODO:  I've noticed this method gets called A LOT.  Should we cache
     *        results for a speedup?  It's not clear how to do this efficiently
     *        or even if it would help.  Investigation and consideration needed.
     *
     * @param action the action made by the rule to compare against
     * @param lhsInt a HashSet of integers containing the internal sensors that were on
     * @param lhsExt the lhs external sensorData
     * @return  a match score from -1.0 to 1.0
     */
    public double lhsMatchScore(char action, HashSet<Integer> lhsInt, SensorData lhsExt){
        // Immediately return 0.0 if the actions don't match
        if (action != this.action) return -1.0;

        //For now I'm requiring a perfect match for LHS ext
        if (! isLHSExtMatch(action, lhsExt)) return -1.0;
        double extScore = 1.0;

        //TODO: someday use soft matches again
        //double extScore = lhsExtMatchScore(lhsExt);
        double intScore = lhsIntMatchScore(lhsInt);
        double score = extScore * intScore;
        //double negative should stay negative
        if ( (extScore < 0.0) && (intScore < 0.0) ) {
                score *= -1.0;
        }

        return score;
    }//lhsMatchScore

    //pre-allocating this array speeds up calculateTFIDF() but at the cost
    //of being thread-unsafe.  Ok for now.
    private static final double[] ctfidfResult = new double[2];

    /**
     * calculateTFIDF
     *
     * helper method that calculates the TF-IDF given the tf, df and whether
     * the sensor was on.  This is used to score partial matches.
     *
     * For our purposes:
     *   Term-Frequency (TF) is how often the sensor is on when the rule matches
     *   Document-Frequency (DF) is how often the sensor is on in general
     *
     * Note: We've strayed quite a bit from the canonical TF-IDF
     *       formula:  tf * -log(df).  Nonetheless, the name has stuck.
     *
     * @param tf the term frequency of the sensor
     * @param df the document frequency of the sensor
     * @param wasOn whether the sensor was on
     * @return two values in an array:
     *         0 - the tfidf match score [-1.0..1.0]
     *         1 - the relevance of this score [0.0..1.0]
     */
    private double[] calculateTFIDF(double tf, double df, boolean wasOn) {
        //Calculate a base match degree on the scale [-1.0..1.0]
        double tfidf = wasOn ? tf : (1.0 - tf);
        tfidf -= 0.5;
        tfidf *= 2.0;
        ctfidfResult[0] = tfidf;

        //Calculate its relevance.
        ctfidfResult[1] = Math.abs(tf - df);

        return ctfidfResult;
    }//calculateTFIDF

    /** @return true if this rule has a given internal sensor on its LHS */
    public boolean testsIntSensor(int id){
        for(Cond cond: this.lhsInternal){
            if(cond.sId==id){
                return true;
            }
        }
        return false;
    }

    /** replaces one internal sensor with another in this rule (for merging) */
    //TODO:  may not replace primary?
    public void replaceIntSensor(int oldId, int newId){
        Vector<Cond> toRemove = new Vector<>();
        for(Cond cond: this.lhsInternal) {
            if (cond.sId == oldId) {
                toRemove.add(cond);
            }
        }
        for(Cond cond : toRemove) {
            this.lhsInternal.remove(cond);

            //The tfdata of the old condition is relevant but has no
            // guarantee of being precise.  So we start over with a new
            // condition whose initial value is set based upon the old data.
            // Is this the best approach?  I can't think of better atm.
            boolean initVal = (cond.getTF() >= 0.5);
            this.lhsInternal.add(new Cond(newId, initVal));
        }
    }//replaceIntSensor

    /**
     * sortedConds
     *
     * creates a Vector from a HashSet of ExtCond where the conditions are in sorted order
     * but with the GOAL at the end.  This is used by the toString methods to present
     * a consistent ordering for frazzled human eyes.
     */
    protected Vector<Cond> sortedConds(HashSet<Cond> conds) {
        //Sort the vector
        Vector<Cond> result = new Vector<>(conds);
        Collections.sort(result);

        //Move the GOAL to the end
        int goalIndex = 0;
        for(int i = 0; i < result.size(); ++i) {
            if (result.get(i).getName().equals(SensorData.goalSensor)) {
                goalIndex = i;
                break;
            }
        }
        Cond goalCond = result.remove(goalIndex);
        result.add(goalCond);

        return result;
    }//sortedConds

    /**
     * toStringShortINT
     *
     * is a helper method for {@link #toString()} to convert the LHS internals to
     * a string.
     */
    protected String toStringShortIntLHS(HashSet<Cond> lhs) {
        StringBuilder result = new StringBuilder("(");
        Vector<Cond> sortedInternal = new Vector<>(lhs);
        Collections.sort(sortedInternal);

        boolean first = true;
        for (Cond cond : sortedInternal) {
            //skip the primary internal to print at the end
            if (cond.equals(this.primaryInternal)) continue;
            //skip invalid id for internal sensor
            if (cond.sId < 1) { continue; }

            //only print positive conditions
            if (cond.getTF() > 0.5) {
                if (first) {
                    first = false;
                } else {
                    result.append(";");
                }
                result.append(cond.getName());
            }
        }//for

        result.append(") ");
        if (this.primaryInternal != null) {
            result.append(primaryInternal.getName());
        }

        return result.toString();
    }//toStringShortINT

    /**
     * toStringShortLHS
     *
     * is a helper method for {@link #toStringShort()} to
     * append the LHS of a TFRule in a short format
     *
     * @param result  StringBuilder to append to
     */
    protected void toStringShortLHS(StringBuilder result) {
        result.append("#");
        result.append(this.ruleId);
        result.append(":");
        result.append(toStringShortIntLHS(this.lhsInternal));
        result.append("|");
        result.append(toStringShortRHS(this.lhsExternal));
        result.append(this.action);
    }//toStringShortLHS



    /**
     * toStringShortRHS
     *
     * is a helper method for {@link #toString()} to
     * convert the RHS to a bit string
     */
    protected String toStringShortRHS(HashSet<Cond> rhs) {
        StringBuilder result = new StringBuilder();
        for (Cond eCond : sortedConds(rhs)) {
            result.append(eCond.toStringShort());
        }
        return result.toString();
    }//toStringShortRHS

    /**
     * toStringLongLHS
     *
     * is a helper method for {@link #toString()} to
     * convert the RHS to a bit string.
     *
     * Note: doesn't print conditions with a zero TF
     */
    protected String toStringLongLHS(HashSet<Cond> lhs) {
        StringBuilder result = new StringBuilder();
        Vector<Cond> sortedInternal = new Vector<>(lhs);
        Collections.sort(sortedInternal);
        for (Cond cond : sortedInternal) {
            if(cond.getTF() > 0.001) {
                result.append(cond);
                result.append(", ");
            }
        }
        return result.toString();
    }//toStringShortRHS

    @Override
    public String toString(){
        StringBuilder result = new StringBuilder();
        result.append("#");
        if (this.ruleId < 10) result.append(" "); //to line up with two-digit rule ids

        result.append(this.ruleId);
        result.append(":    ");  //extra space so it will line up with 0-depth EpRule
        result.append(toStringShortIntLHS(this.lhsInternal));
        result.append("|");
        result.append(toStringShortRHS(this.lhsExternal));
        result.append(this.action);
        result.append(" -> ");
        result.append(toStringShortRHS(this.rhsExternal));
        //note:  replaceAll call removes extra trailing 0's to improve readability
        result.append(String.format(" ^  conf=%.5f", getConfidence()).replaceAll("0+$", "0"));
        result.append(String.format(" act=%.5f", calcActivation()).replaceAll("0+$", "0"));
        double matchScore = lhsMatchScore(this.action, agent.getFlatCurrInternal() , agent.getCurrExternal());
        result.append(String.format(" Score: %6.3f\t|| ", matchScore).replaceAll("0+$","0"));
        result.append(toStringLongLHS(this.lhsInternal));
        return result.toString();
    }

    /** a shorter string format designed to be used inline */
    @Override
    public String toStringShort(){
        StringBuilder result = new StringBuilder();
        toStringShortLHS(result);
        result.append("->");
        result.append(toStringShortRHS(this.rhsExternal));
        return result.toString();
    }

    //region getters

    public char getAction() { return this.action; }
    public HashSet<Cond> getRHSExternalRaw() { return this.rhsExternal; }

    public SensorData getRHSExternal() {
        SensorData result = SensorData.createEmpty();
        for(Cond cond : this.rhsExternal) {
            //pick true/false by rounding
            result.setSensor(cond.getName(), cond.getTF() >= 0.5);
        }
        return result;
    }

    public SensorData getLHSExternal() {
        SensorData result = SensorData.createEmpty();
        for(Cond cond : this.lhsExternal) {
            //pick true/false by rounding
            result.setSensor(cond.getName(), cond.getTF() >= 0.5);
        }
        return result;
    }

    public HashSet<Cond> getLhsInternal() { return this.lhsInternal; }
    public int getTimeDepth() { return timeDepth; }
    public Cond getPrimaryInternal() { return primaryInternal; }

    //endregion

}//class TFRule
