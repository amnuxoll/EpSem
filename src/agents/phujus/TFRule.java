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
        public double numOn;

        public double getTF() {
            return this.numOn / this.numMatches;
        }
    }//TFData

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

    /**
     * class ExtCond
     *
     * Tracks external conditions (LHS or RHS)
     */
    public static class ExtCond extends Confidence implements Comparable<ExtCond> {
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

    //endregion

    //region Instance Variables
    //the action of the rule
    private char action;

    //the conditions of the LHS and RHS
    private HashSet<IntCond> lhsInternal;
    private HashSet<ExtCond> lhsExternal;
    private HashSet<ExtCond> rhsExternal;

    //HashMaps containing the name of the sensor and the TFData associated with the sensor
    private HashMap<String, TFData> lhsExtTF;
    private HashMap<String, TFData> rhsExtTF;
    private HashMap<String, TFData> lhsIntTF;

    //endregion

    /** ctor initializes this rule from the agents current state */
    public TFRule(PhuJusAgent agent) {
        super(agent);

        this.action = agent.getPrevAction();
        this.lhsExternal = initExternal(agent.getPrevExternal());
        this.rhsExternal = initExternal(agent.getCurrExternal());
        lhsInternal = initInternal(agent.getAllPrevInternal());  //TODO:  need to base on all agent's curr sensors
        lhsExtTF = initExtTF(lhsExternal);
        rhsExtTF = initExtTF(rhsExternal);
        lhsIntTF = initIntTF();

    }//TFRule

    /**
     * initExtTF
     *
     * Initializes the TF values for a given HashSet of external sensors (both lhs and rhs).
     * Goes through each external sensor and creates a TFData instance for each, with numOn
     * initialized to 0 or 1.
     *
     * @param external the HashSet containing external sensors (either lhs or rhs)
     * @return HashMap with key/value pairs corresponding to the sensor indentifier and TF data
     */
    private HashMap<String, TFData> initExtTF(HashSet<ExtCond> external){

        // The data HashMap to be returned
        HashMap<String, TFData> map = new HashMap<>();

        // Creates TFData for each sensor that was previosuly on.
        // Since there is only a single instance at the time of rule creation,
        // numOn is set to 1.0 or 0.0 -- either a 100% on or off.
        for(ExtCond sensor:external){
            TFData data = new TFData();

            if(sensor.val)
                data.numOn = 1.0;
            else
                data.numOn = 0.0;

            // Adds the pair to the map
            map.put(sensor.sName ,data);
        }

        return map;
    }//initExtTF

    /**
     * initIntTF
     *
     * Initializes the TF values for the internal sensors. Gets all the sensors from the previous
     * time steps and creates a TFData instance for each, with numOn initialized to 0 or 1.
     *
     * @return HashMap with key/value pairs corresponding to the sensor indentifier and TF data
     */
    private HashMap<String, TFData> initIntTF(){

        // The data HashMap to be returned
        HashMap<String, TFData> map = new HashMap<>();

        // Vector containing the internal sensors that were on at each time step
        Vector<HashSet<Integer>> allInternal = agent.getAllPrevInternal();

        // Gets the previous internal sensors that were on
        HashSet<Integer> prevInternal = allInternal.lastElement();

        // Stores all the internal sensors across all tiem steps
        HashSet<Integer> combinedSet = new HashSet<>();

        // Adds all the on sensors from the previous time steps to a single HashSet
        for(HashSet<Integer> set: allInternal){
            combinedSet.addAll(set);
        }

        // Creates TFData for each sensor that was previosuly on.
        // Since there is only a single instance at the time of rule creation,
        // numOn is set to 1.0 or 0.0 -- either a 100% on or off.
        for(Integer id: combinedSet){
            TFData data = new TFData();

            if(prevInternal.contains(id))
                data.numOn = 1.0;
            else
                data.numOn = 0.0;

            // Adds the pair to the map
            map.put(id.toString() ,data);
        }

        return map;
    }//initIntTF

    /**
     * initInternal
     *
     * Creates an instance of IntCond for each sensor in the input HashSet and returns
     * them as a HashSet.
     *
     * @param input HashSet containing internal sensors that fired in the previous time step
     * @return HashSet of IntConds for each sensor
     */
    private HashSet<IntCond> initInternal(Vector<HashSet<Integer>> input){
        //TODO:Make sure this works? we only have 5 internal sensors when we have 21 rules?
        System.out.println("Keys : " + input.size());
        HashSet<IntCond> set = new HashSet<>();
        for(HashSet<Integer> layer:input){
            for(Integer sensor:layer){
                set.add(new IntCond(sensor));
            }
        }

        return set;
    }//initInternal

    /** converts from SensorData (used by the FSM environment) to HashSet<ExtCond> */
    private HashSet<ExtCond> initExternal(SensorData sData) {

        HashSet<ExtCond> result = new HashSet<>();
        for(String sName : sData.getSensorNames()) {
            result.add(new ExtCond(sName, (Boolean)sData.getSensor(sName)));
        }

        return result;
    }//initExternal

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

        //loop through all the tf hashsets (lhs, rhs, etc), check if the sensor id is within curr internal
        // if it is, then update the tf accordingly
        // -> increment the numerator by one
        // no matter what, the denominator is incremented by one as well

        for(IntCond cond: this.lhsInternal){
            String ruleSTR = Integer.toString(cond.sId);
            TFData data = lhsIntTF.get(ruleSTR);

            if(previnternal.contains(cond.sId)){
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

        return (sum / this.rhsExternal.size());
    }//rhsMatchScore

    /**
     * lhsMatchScore
     *
     * calculates how closely this rule matches a given action and lhs sensors
     * For a BaseRule this is trivial since the LHS is just an action.
     * This method is overridden in child classes
     *
     * @return  a match score from 0.0 to 1.0
     */
    public double lhsMatchScore(char action, HashSet<Integer> lhsInt, SensorData lhsExt){
        if (action != this.action) return 0.0;
        return 1.0;
    }//lhsMatchScore

    /**
     * sortedConds
     *
     * creates a Vector from a HashSet of ExtCond where the conditions are in sorted order
     * but with the GOAL at the end.  This is used by the toString methods to present
     * a consistent ordering to frazzled human eyes.
     */
    protected Vector<ExtCond> sortedConds(HashSet<ExtCond> conds) {
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
     * toStringShortRHS
     *
     * is a helper method for {@link #toString()} & {@link EpRule#toStringShort()} to
     * convert a RHS to a bit string
     */
    protected String toStringShortRHS(HashSet<ExtCond> rhs) {
        StringBuilder result = new StringBuilder();
        for (ExtCond eCond : sortedConds(rhs)) {
            char bit = (eCond.val) ? '1' : '0';
            result.append(bit);
        }
        return result.toString();
    }//toStringShortRHS

    /**
     * toStringShortRHS
     *
     * is a helper method for {@link #toString()} & {@link EpRule#toStringShort()} to
     * convert a RHS to a bit string
     */
    protected String toStringShortINT(HashSet<IntCond> lhs) {
        StringBuilder result = new StringBuilder();
        HashSet<Integer> prevInternal = agent.getPrevInternal();
        int i = 0;
        System.out.println(lhs.size());
        for (IntCond iCond : lhs) {
            char bit = (prevInternal.contains(iCond.sId)) ? '1' : '0';
            result.append(bit);
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
        return result.toString();


        //LHSinternal|lhsexternalAction->RHSExternal #tfterms acc=accuracy

    }

    //region getters

    public char getAction() { return this.action; }

    public HashSet<ExtCond> getRHSExternalRaw() {
        return this.rhsExternal;
    }

    public HashSet<ExtCond> getLHSExternalRaw() {
        return this.lhsExternal;
    }

    //endregion

}//class TFRule
