package agents.phujus;

import framework.SensorData;

import java.util.*;

public class TFRule extends Rule{

//region Inner Classes
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


    private char action;
    private HashSet<IntCond> lhsInternal;
    private HashSet<ExtCond> lhsExternal;
    private HashSet<ExtCond> rhsExternal;
    private HashMap<String, TFData> lhsExtTF;
    private HashMap<String, TFData> rhsExtTF;
    private HashMap<String, TFData> lhsIntTF;

    public TFRule(PhuJusAgent agent) {

        super(agent);
        this.action = agent.getPrevAction();
        this.lhsExternal = initExternal(agent.getPrevExternal());
        this.rhsExternal = initExternal(agent.getCurrExternal());
        lhsInternal = initInternal(agent.getPrevInternal());
        lhsExtTF = initExtTF(lhsExternal);
        lhsIntTF = initIntTF(lhsInternal);
        rhsExtTF = initExtTF(rhsExternal);

    }

    private HashMap<String, TFData> initExtTF(HashSet<ExtCond> external){
        HashMap<String, TFData> map = new HashMap<String, TFData>();

        for(ExtCond sensor:external){
            TFData data = new TFData();

            if(sensor.val)
                data.numOn = 1.0;
            else
                data.numOn = 0.0;

            map.put(sensor.sName ,data);
        }

        return map;
    }

    private HashMap<String, TFData> initIntTF(HashSet<IntCond> internal){
        HashMap<String, TFData> map = new HashMap<String, TFData>();

        //we will go through all of the previous internal sensors we have sensed
        //and add them to a single super set. This will hopefuly not be necessary
        //in the future as we wil have an instance variable in phujus agent that
        //will keep track of this data for us.
        Vector<HashSet<Integer>> allInternal = agent.getAllPrevInternal();
        HashSet<Integer> prevInternal = allInternal.lastElement();

        HashSet<Integer> combinedSet = new HashSet<Integer>();

        for(HashSet<Integer> set: allInternal){
            combinedSet.addAll(set);
        }

        for(Integer id: combinedSet){
            TFData data = new TFData();

            if(prevInternal.contains(id))
                data.numOn = 1.0;
            else
                data.numOn = 0.0;

            map.put(id.toString() ,data);
        }

        return map;
    }

    private HashSet<IntCond> initInternal(HashSet<Integer> input){
        HashSet<IntCond> set = new HashSet<IntCond>();
        for(Integer sensor:input){
            set.add(new IntCond(sensor));
        }
        return set;
    }

    /** converts from SensorData (used by the FSM environment) to HashSet<ExtCond> */
    protected HashSet<ExtCond> initExternal(SensorData sData) {
        HashSet<ExtCond> result = new HashSet<>();
        for(String sName : sData.getSensorNames()) {
            result.add(new ExtCond(sName, (Boolean)sData.getSensor(sName)));
        }

        return result;
    }//initExternal

//other methods:  isMatch,  updateTFVals

    //checks if a rule has the same lhs external and rhs external and action as this rule
    // 00a -> 10 would match if this rule was 00a->10
    // 10b -> 10 would noot match if this rule was 00a->10
    // we do not care about internal sensors for this comparison
    public boolean isMatch(TFRule rule){

        boolean sameAction = this.action == rule.getAction();
        boolean sameRhsExt = this.rhsExternal == rule.getRHSExternalRaw();
        boolean sameLhsExt = this.lhsExternal == rule.getLHSExternalRaw();

        return sameAction && sameRhsExt && sameLhsExt;

    }

    //for all of the rules, we need to check to see if the rule matches this one
    //if the rule matches then we are going to update its TFVals by
    //incrementing the denominators by 1 and the numerators by whether or not the
    //Rule is on?
    //This action above should take place in the phujusAgent class

    // this method does not update LHSExternal and RHSExternal right now as they are
    // always a 100% match. Once we get into partial matching this will need to be updated
    public void updateTFVals(){


        //get all the keys
        Enumeration<String> keys = agent.getRules().keys();
        //get all of the current internal sesnor data to see which ones are on
        HashSet<Integer> previnternal = agent.getPrevInternal();

        //loop through all the tf hashsets (lhs, rhs, etc), check if the sensor id is within curr internal
        // if it is, then update the tf accordingly
        // -> increment the numerator by one
        // no matter what, the denominator is incremented by one as well

        for(IntCond cond: this.lhsInternal){
            String ruleSTR = Integer.toString(cond.sId);
            TFData data = lhsIntTF.remove(ruleSTR);

            if(previnternal.contains(cond.sId)){
                data.numOn++;
            }

            data.numMatches++;

            lhsIntTF.put(ruleSTR,data);

        }

    }

    public double rhsMatchScore(SensorData rhsExt){
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
    }

    public double lhsMatchScore(char action, HashSet<Integer> lhsInt, SensorData lhsExt){
        if (action != this.action) return 0.0;

        return 1.0;
    }

    public char getAction() { return this.action; }

    public HashSet<ExtCond> getRHSExternalRaw() {
        return this.rhsExternal;
    }

    public HashSet<ExtCond> getLHSExternalRaw() {
        return this.lhsExternal;
    }

}//class TFRule
