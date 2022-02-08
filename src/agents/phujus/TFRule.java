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
     * class TFData packages the data and method required to calculate Term Frequency
     */
    public class TFData {
        public double numMatches = 1.0;
        public double numOn = 0;

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
    public class Cond implements Comparable<Cond> {
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

        /** Conds are equal if they have the same sName and val */
        @Override
        public boolean equals(Object o) {
            if (! (o instanceof Cond)) return false;
            Cond other = (Cond) o;
            //TODO:  should we compare the TFData??
            return (this.sName.equals(other.sName));
        }

        @Override
        public String toString() {
            return sName + "=" + String.format("%.2f", data.getTF());
        }

        public String toStringShort() {
            //return a 0 or 1 by rounding the TFData to the nearest int
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
    //the action of the rule
    private char action;

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
        Collection<Integer> combinedSet = agent.getRules().keySet();


        //Add a condition to this rule for each sensor
        HashSet<Cond> set = new HashSet<>();
        for(Integer sensor : combinedSet){
            boolean on = prevInternal.contains(sensor);
            set.add(new Cond(Integer.toString(sensor), on));
        }

        return set;
    }//i

    /** converts from SensorData (used by the FSM environment) to HashSet<ExtCond> */
    private HashSet<Cond> initExternal(SensorData sData) {

        HashSet<Cond> result = new HashSet<>();
        for(String sName : sData.getSensorNames()) {
            result.add(new Cond(sName, (Boolean)sData.getSensor(sName)));
        }

        return result;
    }//initExternal

    /**
     * isMatch
     *
     * Checks if this rule matches the agent's most recent experience
     *
     * @return whether the rule matches this rule
     */
    public boolean isMatch(){

        boolean sameAction = this.action == this.agent.getPrevAction();
        boolean sameRhsExt = this.rhsExternal.equals(initExternal(this.agent.getCurrExternal()));
        boolean sameLhsExt = this.lhsExternal.equals(initExternal(this.agent.getPrevExternal()));


        return sameAction && sameRhsExt && sameLhsExt;

    }//isMatch

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
        for (Cond eCond : this.rhsExternal) {

            // If so, then we calculate the TF/DF value to be added to the score
            if (rhsExt.hasSensor(eCond.sName)) {

                // Calculates the TF and DF values, and if the sensor values are the same
                double tfValue = eCond.data.getTF();
                double dfValue = agent.getExternalPercents().get(eCond.sName).getSecond();
                Boolean sVal = (Boolean) rhsExt.getSensor(eCond.sName);

                score += calculateTFIDF(tfValue, dfValue, sVal);
            }
        }

        return score;
    }//rhsMatchScore

    /**
     * lhsMatchScore
     *
     * calculates how closely this rule matches a given action and lhs sensors
     * For a BaseRule this is trivial since the LHS is just an action.
     * This method is overridden in child classes
     *
     * @param action the action made by the rule to compare against
     * @param lhsInt a HashSet of integers containing the internal sensors that were on
     * @param lhsExt the lhs external sensorData
     * @return  a match score from 0.0 to 1.0
     */
    public double lhsMatchScore(char action, HashSet<Integer> lhsInt, SensorData lhsExt){

        // Immediately return 0.0 if the actions don't match
        if (action != this.action) return 0.0;

        double score = 0.0;

        // Loops through all external sensors of the rule and checks if the incoming
        // sensor data contains the external sensor
        for (Cond eCond : this.lhsExternal) {

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

        // Gets the internal sensors that were previously on
        HashSet<Integer> prevInternal = agent.getPrevInternal();

        // Loops through all internal sensors of the rule and checks if the incoming
        // sensor data contains the internal sensor
        for (Cond cond : this.lhsInternal) {

            // If so, then we calculate the TF/DF value to be added to the score
            if (lhsInt.contains(Integer.parseInt(cond.sName))) {

                // Calculates the TF and DF values, and if the sensor values are the same
                double tf = cond.data.getTF();
                double df = agent.getInternalPercents().get(cond.sName).getSecond();
                boolean wasOn = prevInternal.contains(cond.sName);

                score += calculateTFIDF(tf,df,wasOn);
            }
        }

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

        if (wasOn){
            tfidf = tf / df;
        } else {
            tfidf = (1 - tf) / (1 - df);
        }

        return tfidf;
    }//calculateTFIDF

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
     * is a helper method for {@link #toString()} & {@link EpRule#toStringShort()} to
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
     * is a helper method for {@link #toString()} & {@link EpRule#toStringShort()} to
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
     * is a helper method for {@link #toString()} & {@link EpRule#toStringShort()} to
     * convert a RHS to a bit string
     */
    protected String toStringLongLHS(HashSet<Cond> lhs) {
        StringBuilder result = new StringBuilder();
        Vector<Cond> sortedInternal = new Vector<>(lhs);
        Collections.sort(sortedInternal);
        for (Cond cond : sortedInternal) {
            result.append(cond.toString());
            result.append(", ");
        }
        return result.toString();
    }//toStringShortRHS

    @Override
    public String toString(){
        String str = "";
        //str += ;

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
        result.append(String.format(" ^  acc=%.5f", getAccuracy()).replaceAll("0+$", "0"));
        double leftScore = lhsMatchScore(agent.getPrevAction(), agent.getPrevInternal() , agent.getCurrExternal());
        result.append(String.format("\tMatch Score: lhs=%.3f",leftScore).replaceAll("0+$","0"));
        double rightScore = rhsMatchScore(agent.getCurrExternal());
        result.append(String.format(" rhs=%.3f tot=%.3f\t",rightScore, leftScore+rightScore).replaceAll("0+$","0"));
        result.append(toStringLongLHS(this.lhsInternal));
        return result.toString();
    }

    //region getters

    public char getAction() { return this.action; }

    public HashSet<Cond> getRHSExternalRaw() {
        return this.rhsExternal;
    }

    public HashSet<Cond> getLHSExternalRaw() {
        return this.lhsExternal;
    }

    //endregion

}//class TFRule
