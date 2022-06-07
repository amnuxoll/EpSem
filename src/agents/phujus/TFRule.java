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
public class TFRule extends Rule{

    //region Inner Classes

    /**
     * enum RuleOperator is used to determine what logic is used with the internal sensor.
     * For example: 4/2/3 (ANDOR), 5;2 (AND), or * (ALL)
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
     * class TFData packages the data and method required to calculate Term Frequency
     */
    public static class TFData {
        public double numMatches = 1.0;
        public double numOn = 0.0;

        public TFData(double numMatches, double numOn){
            this.numMatches = numMatches;
            this.numOn = numOn;
        }

        public double getTF() {
            return this.numOn / this.numMatches;
        }
    }//TFData

    /**
     * class Cond
     *
     * Tracks external conditions (LHS or RHS)
     */
    public static class Cond implements Comparable<Cond> {
        public String sName;  //sensor name
        public TFData data;   //term frequency data

        public Cond(String initSName, boolean initVal) {
            this.sName = initSName;
            if(initVal){
                this.data = new TFData(1.0,1.0);
            } else {
                this.data = new TFData(1.0,0.0);
            }

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
            return sName + "=" + String.format("%.2f", data.getTF());
        }

        public String toStringShort() {
            //round to nearest value (true or false)
            return "" +  ((data.getTF() >= 0.5) ? 1 : 0);
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
    public static final double MATCH_NEAR = 0.05;

    //the action of the rule
    private char action;

    //the type of internal sensor comparison. Default value is AND
    private RuleOperator operator = RuleOperator.AND;

    //the conditions of the LHS and RHS
    private HashSet<Cond> lhsInternal;
    private HashSet<Cond> lhsExternal;
    private HashSet<Cond> rhsExternal;

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

        // For now, use the ids from BaseRule and EpRule objects
        // which atm (Feb 2022) is all that is in the PJA.rules list
        // Later this will just be the TFRules
        Collection<Rule> combinedSet = agent.getRules().values();


        //Add a condition to this rule for each sensor
        HashSet<Cond> set = new HashSet<>();
        for(Rule rule : combinedSet){
            boolean on = prevInternal.contains(rule.ruleId);
            //TODO:                should this just be true for internal sensors?
            set.add(new Cond(Integer.toString(rule.ruleId), on));
        }

        return set;
    }//initInternal

    private HashSet<Cond> initInternal(String[] in) {
        HashSet<Cond> set = new HashSet<>();
        if(in == null)
            return set;

        for(String s: in){
            set.add(new Cond(s, true));
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

    public void addConditions(){
        //make the tf rule
        //go through each sensor we have in new TFRule and make sure it is in this TFRule
        //if not then check to make sure it is not for this TFRule
        //if it isn't then add condition to this
        Integer[] currInt =  agent.getRules().keySet().toArray(new Integer[0]);

        boolean doesHave =false;
        for(int i:currInt){
            if(i == this.ruleId)
                continue;

            for(Cond cond: this.lhsInternal){
                int name = Integer.parseInt(cond.sName);

                if(name == i){
                    doesHave = true;
                }
            }
            if(!doesHave){//we don't have this condition! time to add it
                boolean initVal = agent.getPrevInternal().contains(i);
                this.lhsInternal.add(new Cond(Integer.toString(i),initVal));
            }

        }



    }

    /**
     * isExtMatch
     *
     * Checks if this rule matches the agent's most recent experience.
     * This method only checks external sensors for comparison.
     *
     * @return whether the rule matches this rule
     */
    public boolean isExtMatch() {

        // Return false if the actions are not the same
        if (this.action != this.agent.getPrevAction()) {
            return false;
        }

        // Returns false if the rhs external condtions don't match the current external sensors
        if (!helperMatch(this.rhsExternal, this.agent.getCurrExternal())) {
            return false;
        }

        // Returns false if the lhs external conditions don't match the previous external sensors
        if (!helperMatch(this.lhsExternal, this.agent.getPrevExternal())) {
            return false;
        }

        return true;

    }//isExtMatch

    /**
     * isRHSMatch
     *
     * Checks if this rule's right-hand side matches the agent's most recent experience
     *
     * @return whether the right-hand side of this rule matches the agent
     */
    public boolean isRHSMatch(){

        boolean match = helperMatch(this.rhsExternal, this.agent.getCurrExternal());
        return match;

    }//isRHSMatch


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
            List<Cond> condList = Arrays.asList(conditions.toArray(new Cond[0]));
            List<Cond> convertList = Arrays.asList(convertSensors.toArray(new Cond[0]));

            // Checks if each condtion is present
            for (int i = 0; i < condList.size(); i++) {

                // Extract the current condition from conditions
                Cond cond = condList.get(i);

                // Return false if the converted sensor data doesn't contain the condition
                if (convertList.contains(cond)) {

                    // Get the index of the matching condition
                    int condIndex = convertList.indexOf(cond);

                    // Return false if the TF vals of the conditions don't match
                    if (cond.data.getTF() != convertList.get(condIndex).data.getTF()) {
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
     * updates the TFVals for this rule by looking at the prevInternal and checking if
     * our internal sensors appear in prevInternal
     *
     * Note:
     * this method does not update LHSExternal and RHSExternal right now as they are
     * always a 100% match. Once we get into partial matching this will need to be updated
     */
    public void updateTFVals(){

        //get all the current internal sesnor data to see which ones are on
        HashSet<Integer> previnternal = agent.getPrevInternal();


        //loop through all the tf hashsets (lhs, rhs, etc), check if the sensor id is within prev internal
        // if it is, then update the tf accordingly
        // -> increment the numerator by one
        // no matter what, the denominator is incremented by one as well

        for(Cond cond: this.lhsInternal){
            TFData data = cond.data;
            if(previnternal.contains(Integer.parseInt(cond.sName))) {
                data.numOn++;
            }
            data.numMatches++;
        }

    }//updateTFVals

    /**
     * totalMatchScore
     *
     * Adds the match scores of the lhs and rhs to compute the total match score.
     *
     * @param action the action made by the rule to compare against
     * @param lhsInt a HashSet of integers containing the internal sensors that were on
     * @param lhsExt the lhs external sensorData
     * @param rhsExt the rhs external sensorData
     * @return the total match score
     */
    public double totalMatchScore(char action, HashSet<Integer> lhsInt, SensorData lhsExt, SensorData rhsExt) {

        // Adds the lhs and rhs match scores
        return lhsMatchScore(action, lhsInt, lhsExt) + rhsMatchScore(rhsExt);
    }//totalMatchScore

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
                double tfValue = eCond.data.getTF();
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
     * @return  a match score from 0.0 to 1.0
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
                double tfValue = eCond.data.getTF();
                double dfValue = agent.getExternalPercents().get(eCond.sName).getSecond();
                Boolean sVal = (Boolean) lhsExt.getSensor(eCond.sName);

                // Adds the TF/DF to the current score
                score += calculateTFIDF(tfValue, dfValue, sVal);
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
     * @return  a match score from 0.0 to 1.0
     */
    public double lhsIntMatchScore(HashSet<Integer> lhsInt){

        if(this.operator == RuleOperator.ALL)
            return 1.0;

        double score = 0.0;
        int c = 0;
        // Loops through all internal sensors of the rule and checks if the incoming
        // sensor data contains the internal sensor
        if(this.operator == RuleOperator.AND) {
            for (Cond cond : this.lhsInternal) {
                c++;

                // Calculates the TF and DF values, and if the sensor values are the same
                double tf = cond.data.getTF();
                double df = agent.getInternalPercents().get(cond.sName).getSecond();
                boolean wasOn = lhsInt.contains(Integer.parseInt(cond.sName));

                score += calculateTFIDF(tf, df, wasOn);
            }
        }
        else if (this.operator == RuleOperator.ANDOR){
            c = 1;
            for (Cond cond : this.lhsInternal) {
                // Calculates the TF and DF values, and if the sensor values are the same
                double tf = cond.data.getTF();
                double df = agent.getInternalPercents().get(cond.sName).getSecond();
                boolean wasOn = lhsInt.contains(Integer.parseInt(cond.sName));

                double val = calculateTFIDF(tf, df, wasOn);
                if(val >= score)
                    score = val;
            }
        }
        // no sensors so base match of 1.0
        if(c == 0)
            return lhsInt.size() == 0 ? 1.0 : 0.0;
        return score/c;
    }//lhsIntMatchScore


    /**
     * lhsMatchScore
     *
     * calculates how closely this rule matches a given action and lhs sensors
     *
     * @param action the action made by the rule to compare against
     * @param lhsInt a HashSet of integers containing the internal sensors that were on
     * @param lhsExt the lhs external sensorData
     * @return  a match score from 0.0 to 1.0
     */
    public double lhsMatchScore(char action, HashSet<Integer> lhsInt, SensorData lhsExt){

        // Immediately return 0.0 if the actions don't match
        if (action != this.action) return 0.0;

        double score = lhsExtMatchScore(lhsExt);
        score *= lhsIntMatchScore(lhsInt);
        return score;
    }//lhsMatchScore

    /**
     * calculateTFIDF
     *
     * helper method that calculates the TFIDF given the tf, df and whether
     * the sensor was on
     *
     * @param tf the term frequency of the sensor
     * @param df the document frequency of the sensor
     * @param wasOn whether the sensor was on
     * @return the tfidf
     */
    private double calculateTFIDF(double tf, double df, boolean wasOn) {
        double tfidf = 0.0;

        // Added 1 to the denominator to avoid Nan, Infinity and division by 0 errors
        if (wasOn){
            tfidf = tf / (1 + df);
        } else {
            tfidf = (1 - tf) / (2 - df);
        }

        return tfidf;
    }//calculateTFIDF

    public boolean testsIntSensor(int id){
        for(Cond cond: this.lhsInternal){
            if(Integer.parseInt(cond.sName)==id){
                return true;
            }
        }
        return false;
    }

    public int removeIntSensor(int oldId, int newId){
        int removeCount = 0;

        for(Cond cond: this.lhsInternal){
            if(Integer.parseInt(cond.sName) == oldId){
                this.lhsInternal.remove(cond);
                removeCount++;
                return removeCount;
            }
        }
        return 0;
    }

    /**
     * sortedConds
     *
     * creates a Vector from a HashSet of ExtCond where the conditions are in sorted order
     * but with the GOAL at the end.  This is used by the toString methods to present
     * a consistent ordering to frazzled human eyes.
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
     * toStringShortRHS
     *
     * is a helper method for {@link #toString()} to
     * convert a RHS to a bit string
     */
    protected String toStringShortRHS(HashSet<Cond> rhs) {
        StringBuilder result = new StringBuilder();
        for (Cond eCond : sortedConds(rhs)) {
            result.append(eCond.toStringShort());
        }
        return result.toString();
    }//toStringShortRHS

    /**
     * toStringShortINT
     *
     * is a helper method for {@link #toString()} to
     * convert a RHS to a bit string
     */
    protected String toStringShortINT(HashSet<Cond> lhs) {
        StringBuilder result = new StringBuilder();

        Vector<Cond> sortedInternal = new Vector<>(lhs);
        Collections.sort(sortedInternal);
        for (Cond cond : sortedInternal) {
            result.append(cond.toStringShort());
        }
        return result.toString();
    }//toStringShortRHS

    /**
     * toStringLongLHS
     *
     * is a helper method for {@link #toString()} to
     * convert a RHS to a bit string
     */
    protected String toStringLongLHS(HashSet<Cond> lhs) {
        StringBuilder result = new StringBuilder();
        StringBuilder zeros = new StringBuilder();
        Vector<Cond> sortedInternal = new Vector<>(lhs);
        Collections.sort(sortedInternal);
        for (Cond cond : sortedInternal) {
            if(cond.data.getTF() > 0.001) {
                result.append(cond.toString());
                result.append(", ");
            } else {
                zeros.append(cond.sName);
                zeros.append(", ");
            }
        }
        // Currently doesn't print conditions with a zero TF. This can be changed by adding
        // zeros.toString to the return statement.
        return result.toString();
    }//toStringShortRHS

    @Override
    public String toString(){
        String str = "";

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
        double leftScore = lhsMatchScore(this.action, agent.getPrevInternal() , agent.getCurrExternal());
        double rightScore = 0;//rhsMatchScore(agent.getCurrExternal());
        result.append(String.format(" Score: %.3f\t|| ", leftScore+rightScore).replaceAll("0+$","0"));
        result.append(toStringLongLHS(this.lhsInternal));
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
            result.setSensor(cond.sName, cond.data.getTF() >= 0.5);
        }
        return result;
    }

    public SensorData getLHSExternal() {
        SensorData result = SensorData.createEmpty();
        for(Cond cond : this.lhsExternal) {
            //pick true/false by rounding
            result.setSensor(cond.sName, cond.data.getTF() >= 0.5);
        }
        return result;
    }

    public HashSet<Cond> getLHSExternalRaw() {
        return this.lhsExternal;
    }

    public HashSet<Cond> getLhsInternal() {
        return this.lhsInternal;
    }

    public RuleOperator getOperator() { return this.operator; }

    //endregion

}//class TFRule
