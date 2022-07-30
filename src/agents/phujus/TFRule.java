package agents.phujus;

import framework.SensorData;

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

    /**
     * enum RuleOperator is used to determine what logic is used with the internal sensor.
     *   ANDOR - the best matching internal sensor is used to calculate a match score
     *   AND - all the internal sensors are used to calculate a match score
     *   ALL - the internal matchs score is always neutral (0.0)
     *
     * By default, if no RuleOperator is specified, the default is 'AND.'
     */
    public enum RuleOperator {
        AND(";"),ANDOR("/"),ALL("*");

        public final String character;
        RuleOperator(String c) {
            this.character = c;
        }
    }

    /**
     * class Cond
     *
     * Tracks external conditions (LHS or RHS)
     */
    public static class Cond implements Comparable<Cond> {
        public String sName;  //sensor name
        public int sId;       //sensor id #
        public double numMatches;
        public double numOn;

        public Cond(String initSName, boolean initVal) {
            this.sName = initSName;
            this.sId = -1; //only set for internal sensors
            this.numMatches = 1.0;
            this.numOn = initVal ? 1.0 : 0.0;
        }

        //internal sensors have a sensor id instead of a name
        public Cond(int initSId, boolean initVal) {
            this("" + initSId, initVal);
            this.sId = initSId;
        }

        public double getTF() {
            return this.numOn / this.numMatches;
        }

        /** Conds are equal if they have the same name.
         *   TFData is not compared. */
        @Override
        public boolean equals(Object o) {
            if (! (o instanceof Cond)) return false;
            Cond other = (Cond) o;
            return this.sName.equals(other.sName);
        }

        @Override
        public String toString() {
            return sName + "=" + String.format("%.2f", getTF());
        }

        public String toStringShort() {
            //round to the nearest value (true or false)
            return "" +  ((getTF() >= 0.5) ? 1 : 0);
        }

        @Override
        public int compareTo(Cond o) {
            try{
                int a = Integer.parseInt(this.sName);
                int b = Integer.parseInt(o.sName);

                return a-b;
            } catch(Exception e){
                return this.sName.compareTo(o.sName);
            }
        }

        @Override
        public int hashCode() { return Objects.hash(sName); }

    }//class Cond

    //endregion

    //region Instance Variables


    //How similar two match scores need to be to each other to be "near"
    public static final double MATCH_NEAR = 0.1;

    //the action of the rule
    private final char action;

    //the type of internal sensor comparison. Default value is AND
    private RuleOperator operator = RuleOperator.AND;

    //the conditions of the LHS and RHS
    private final HashSet<Cond> lhsInternal;
    private final HashSet<Cond> lhsExternal;
    private final HashSet<Cond> rhsExternal;

    //endregion

    /** ctor initializes this rule from the agents current state */
    public TFRule(PhuJusAgent agent) {
        super(agent);

        this.action = agent.getPrevAction();
        this.lhsExternal = initExternal(agent.getPrevExternal());
        this.rhsExternal = initExternal(agent.getCurrExternal());
        this.lhsInternal = initInternal();
    }//TFRule

    /**
     * ctor to initalize a tf rule given an action, LHSInt, LHSExt, RHSExt, and confidence
     */
    public TFRule(PhuJusAgent agent, char action, String[] lhsInt, SensorData lhsExt, SensorData rhsExt, double conf) {
        super(agent);

        this.action = action;

        this.lhsInternal = initInternal(lhsInt);
        this.lhsExternal = initExternal(lhsExt);
        this.rhsExternal = initExternal(rhsExt);

        this.confidence.setConfidence(conf);
    }//TFRule

    /**
     * ctor to initalize a tf rule given an action, LHSInt, LHSExt, RHSExt, confidence, and operator
     */
    public TFRule(PhuJusAgent agent, char action, String[] lhsInt, SensorData lhsExt, SensorData rhsExt, double conf, RuleOperator operator) {
        super(agent);

        this.action = action;

        this.lhsInternal = initInternal(lhsInt);
        this.lhsExternal = initExternal(lhsExt);
        this.rhsExternal = initExternal(rhsExt);

        this.confidence.setConfidence(conf);

        this.operator = operator;
    }//TFRule


    /**
     * initInternal
     *
     * Creates an instance of Cond for each sensor in the input HashSet and returns
     * them as a HashSet.
     *
     * @return HashSet of IntConds for each sensor
     */
    private HashSet<Cond> initInternal(){
        // Gets the previous internal sensors that were on
        HashSet<Integer> prevInternal = agent.getPrevInternal();

        //Add a condition to this rule for each sensor
        HashSet<Cond> set = new HashSet<>();
        for(TFRule tf : agent.getTfRules()){
            boolean on = prevInternal.contains(tf.ruleId);
            //TODO:  should this just be true for internal sensors?
            set.add(new Cond(tf.ruleId, on));
        }

        return set;
    }//initInternal

    //TODO:  this method should take an int[] not a String[]
    private HashSet<Cond> initInternal(String[] in) {
        HashSet<Cond> set = new HashSet<>();
        if(in == null)
            return set;

        for(String s: in){
            int sId = Integer.parseInt(s);
            set.add(new Cond(sId, true));
        }

        return set;
    }

    /** converts from SensorData (used by the FSM environment) to HashSet<ExtCond> */
    private HashSet<Cond> initExternal(SensorData sData) {

        HashSet<Cond> result = new HashSet<>();
        for(String sName : sData.getSensorNames()) {
            result.add(new Cond(sName, (Boolean)sData.getSensor(sName)));
        }

        return result;
    }//initExternal

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

        // Return false if the actions are not the same
        if (this.action != compAction) {
            return false;
        }

        // Returns false if the lhs external conditions don't match the previous external sensors
        if (!helperMatch(this.lhsExternal, prevExt)) {
            return false;
        }

        // Returns false if the rhs external condtions don't match the current external sensors
        return helperMatch(this.rhsExternal, currExt);

    }//isExtMatch

    /**
     * isExtMatch
     *
     * Checks if a given rule matches the agent's most recent experience
     * (external sensors and action only)
     *
     * @return whether the rule matches the agent's most recent experience
     */
    public boolean isExtMatch() {
        return isExtMatch(this.agent.getPrevAction(), this.agent.getPrevExternal(), this.agent.getCurrExternal());
    }

    /**
     * helperMatch
     *
     * Helper function for checking whether the conditions of the TFRule match the
     * current or previous SensorData of the agent. This is done by checking if the
     * SensorData (converted to HashSet<Cond>) contains each condition of the rule.
     *
     * @param conditions the external conditions of the TFRule
     * @param sensors the current or previous SensorData
     * @return whether the conditions and sensors match
     */
    private boolean helperMatch(HashSet<Cond> conditions, SensorData sensors) {

        // Converts SensorData to HashSet<Cond> for comparison
        HashSet<Cond> convertSensors = initExternal(sensors);

        // First checks that the size of the two sets are equal.
        // If so, returns false if the converted sensors don't contain the external conditions.
        if (conditions.size() == convertSensors.size()) {

            // Convert the HashSets to Lists for greater usability
            Cond[] condList = conditions.toArray(new Cond[0]);
            List<Cond> convertList = Arrays.asList(convertSensors.toArray(new Cond[0]));

            // Checks if each condtion is present
            for (Cond cond : condList) {

                // Extract the current condition from conditions
                // Return false if the converted sensor data doesn't contain the condition
                if (convertList.contains(cond)) {

                    // Get the index of the matching condition
                    int condIndex = convertList.indexOf(cond);

                    // Return false if the TF vals of the conditions don't match
                    if (cond.getTF() != convertList.get(condIndex).getTF()) {
                        return false;
                    }
                } else {
                    return false;
                }
            }
        } else {
            return false;
        }

        return true;
    }//helperMatch

    /**
     * isMatch
     *
     * Checks if the given rule matches this rule.
     *
     * @param rule the rule to compare against
     * @return whether the rule matches this rule
     */
    public boolean isMatch(TFRule rule){

        boolean sameAction = this.action == rule.getAction();
        boolean sameRhsExt = this.rhsExternal.equals(rule.rhsExternal);
        boolean sameLhsExt = this.lhsExternal.equals(rule.lhsExternal);

        return sameAction && sameRhsExt && sameLhsExt;

    }//isMatch

    /**
     * updateTFVals
     *
     * updates the TFVals for this rule by looking at the given prevInternal.
     * The presumption is that this method is called whenever this rule fires.
     *
     * Note:  this method does not update LHSExternal and RHSExternal right now
     * as they are always a 100% match. Once we get into partial matching this
     * will need to be updated
     */
    public void updateTFVals(HashSet<Integer> previnternal){

        //loop through all the tf hashsets (lhs, rhs, etc.), check if the sensor id is within prev internal
        // if it is, then update the tf accordingly
        // -> increment the numerator by one
        // no matter what, the denominator is incremented by one as well

        for(Cond cond: this.lhsInternal){
            cond.numMatches++;
            if(previnternal.contains(Integer.parseInt(cond.sName))) {
                cond.numOn++;
            }
        }

    }//updateTFVals

    /**
     * rhsMatchScore
     *
     * calculates how closely this rule matches a given rhs sensor set
     *
     * @param rhsExt the rhs external sensorData
     * @return  a match score from 0.0 to 1.0
     */
    public double rhsMatchScore(SensorData rhsExt) {
        double score = 0.0;

        // Loops through all external sensors of the rule and checks if the incoming
        // sensor data contains the external sensor
        int c = 0;
        for (Cond eCond : this.rhsExternal) {
            c++;
            // If so, then we calculate the TF/DF value to be added to the score
            if (rhsExt.hasSensor(eCond.sName)) {

                // Calculates the TF and DF values, and if the sensor values are the same
                double tfValue = eCond.getTF();
                double dfValue = agent.getExternalPercents().get(eCond.sName).getSecond();
                Boolean sVal = (Boolean) rhsExt.getSensor(eCond.sName);

                score += calculateTFIDF(tfValue, dfValue, sVal);
            }
        }

        return score/c;
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
        int c = 0;
        // Loops through all external sensors of the rule and checks if the incoming
        // sensor data contains the external sensor
        for (Cond eCond : this.lhsExternal) {
            c++;
            // If so, then we calculate the TF/DF value to be added to the score
            if (lhsExt.hasSensor(eCond.sName)) {

                // Calculates the TF and DF values, and if the sensor values are the same
                double tfValue = eCond.getTF();
                double dfValue = 0;

                // Sometimes, when loading rules from a file, this can be null. This check helps
                // prevent that
                if (agent.getExternalPercents().containsKey(eCond.sName)) {
                    dfValue = agent.getExternalPercents().get(eCond.sName).getSecond();
                }
                Boolean sVal = (Boolean) lhsExt.getSensor(eCond.sName);

                if (PhuJusAgent.TFIDF) {
                    // Adds the TF/DF to the current score
                    score += calculateTFIDF(tfValue, dfValue, sVal);
                } else {
                    score += 0.5;  //hard-coded max possible tf-idf val
                }
            }
        }
        // no sensors so can't match
        if(c == 0) return 0;
        return score/c;
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

        //ALL operator rule receive a baseline score
        if(this.operator == RuleOperator.ALL)
            return 0.0;

        double score = 0.0;
        for (Cond cond : this.lhsInternal) {
            // Calculates the TF and DF values, and if the sensor values are the same
            double tf = cond.getTF();
            double df = agent.intPctCache[cond.sId];
            boolean wasOn = lhsInt.contains(Integer.parseInt(cond.sName));
            double tfidf = calculateTFIDF(tf, df, wasOn);

            if(this.operator == RuleOperator.AND) {
                score += tfidf;
            } else if (this.operator == RuleOperator.ANDOR) {
                if(tfidf >= score) score = tfidf;
            }
        }

        // no sensors so base match of 1.0
        if(score == 0.0) return lhsInt.size() == 0 ? 1.0 : 0.0;
        if (this.operator == RuleOperator.ANDOR) return score;
        return score/this.lhsInternal.size();  //AND operator
    }//lhsIntMatchScore


    /**
     * lhsMatchScore
     *
     * calculates how closely this rule matches a given action and lhs sensors
     *
     * TODO:  I've noticed this method gets called A LOT.  Should we cache results for a speedup?
     *
     * @param action the action made by the rule to compare against
     * @param lhsInt a HashSet of integers containing the internal sensors that were on
     * @param lhsExt the lhs external sensorData
     * @return  a match score from -1.0 to 1.0
     */
    public double lhsMatchScore(char action, HashSet<Integer> lhsInt, SensorData lhsExt){
        // Immediately return 0.0 if the actions don't match
        if (action != this.action) return 0.0;

        double score = lhsExtMatchScore(lhsExt);
        score += lhsIntMatchScore(lhsInt);
        score = Math.min(1.0, score);  //TODO:  could do an avg here?

        return score;
    }//lhsMatchScore

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
     * @return the tfidf
     */
    private double calculateTFIDF(double tf, double df, boolean wasOn) {
        //Calculate a base match degree on the scale [-1.0..1.0]
        double tfidf = wasOn ? tf : (1.0 - tf);
        tfidf -= 0.5;
        tfidf *= 2.0;

        //Adjust the score based on its relevance.
        double relevance = Math.abs(tf - df);
        tfidf *= relevance;

        return tfidf;
    }//calculateTFIDF

    /** @return true if this rule has a given internal sensor on its LHS */
    public boolean testsIntSensor(int id){
        for(Cond cond: this.lhsInternal){
            if(Integer.parseInt(cond.sName)==id){
                return true;
            }
        }
        return false;
    }

    /** replaces one internal sensor with another in this rule (for merging) */
    public void replaceIntSensor(int oldId, int newId){
        Vector<Cond> toRemove = new Vector<>();
        for(Cond cond: this.lhsInternal) {
            if (Integer.parseInt(cond.sName) == oldId) {
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
            if (result.get(i).sName.equals(SensorData.goalSensor)) {
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
    protected String toStringShortINT(HashSet<Cond> lhs) {
        StringBuilder result = new StringBuilder("(");
        Vector<Cond> sortedInternal = new Vector<>(lhs);
        Collections.sort(sortedInternal);

        String operator = this.operator.character;

        int c = 0;
        for (Cond cond : sortedInternal) {

            String name = cond.sName;
            if (name.equals("0")) {
                continue;
            }
            if (name.equals("-1")) {
                result.append("*");
                break;
            }
            if (cond.getTF() < 0.001) {
                continue;
            }
            result.append(name);
            if (c < sortedInternal.size()-1) {
                result.append(operator);
            }
            c++;
        }

        result.append(")");
        // This removes an additional comma that might be added to the end (typically the case when printing
        // generated rules)
        String resultStr = result.toString();
        if (this.operator != RuleOperator.ALL && resultStr.endsWith(operator + ")")) {
            resultStr = resultStr.substring(0, resultStr.length() - 2);
            resultStr += ")";
        }

        return resultStr;
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
        result.append(toStringShortINT(this.lhsInternal));
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
        result.append(toStringShortINT(this.lhsInternal));
        result.append("|");
        result.append(toStringShortRHS(this.lhsExternal));
        result.append(this.action);
        result.append(" -> ");
        result.append(toStringShortRHS(this.rhsExternal));
        //note:  replaceAll call removes extra trailing 0's to improve readability
        result.append(String.format(" ^  conf=%.5f", getConfidence()).replaceAll("0+$", "0"));
        double matchScore = lhsMatchScore(this.action, agent.getCurrInternal() , agent.getCurrExternal());
        result.append(String.format(" Score: %.3f\t|| ", matchScore).replaceAll("0+$","0"));
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

    public HashSet<Cond> getRHSExternalRaw() {
        return this.rhsExternal;
    }

    public SensorData getRHSExternal() {
        SensorData result = SensorData.createEmpty();
        for(Cond cond : this.rhsExternal) {
            //pick true/false by rounding
            result.setSensor(cond.sName, cond.getTF() >= 0.5);
        }
        return result;
    }

    public SensorData getLHSExternal() {
        SensorData result = SensorData.createEmpty();
        for(Cond cond : this.lhsExternal) {
            //pick true/false by rounding
            result.setSensor(cond.sName, cond.getTF() >= 0.5);
        }
        return result;
    }

    public HashSet<Cond> getLhsInternal() {
        return this.lhsInternal;
    }

    public RuleOperator getOperator() { return this.operator; }

    //endregion

}//class TFRule
