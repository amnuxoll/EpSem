package agents.phujus;
import framework.Action;
import framework.SensorData;

import java.util.HashMap;
import java.util.Random;

import static java.lang.Math.abs;

/**
 * class Rule
 *
 * describes a single rule
 */

public class Rule {

    //Constants
    public static final int ACTHISTLEN = 10;

    /** probability of a new rule getting an extra condition */
    private static final double EXTRACONDFREQ = 0.33;

    //to assign a unique id to each rule this shared variable is incremented by the ctor
    public static int nextRuleId = 1;

    //to track which internal sensor values are available for use
    public static boolean[] intSensorInUse = new boolean[PhuJusAgent.NUMINTERNAL]; //TODO shouldn't this be in the agent?

    private static Random rand = PhuJusAgent.rand;

    //The agent using this rule
    private PhuJusAgent agent = null;

    //each rule has a unique integer id
    private int ruleId;

    //define the LHS of the rule.  This consists of:
    // - 0 or more internal sensors that have a particular value (0 or 1)
    // - 0 or more values for external sensors
    // (note:  you must have at least 1 of one of the above)
    // private int[][] lhsInternal; //example: {{467, 801, 21}, {0, 1, 1} }
    private HashMap<Integer, Boolean> lhsInternal = new HashMap<>();
    private SensorData  lhsExternal = null;  //example: {1,-1,0,-1}  note: -1 is "wildcard"
    private char action;

    //define the RHS of the rule.  The RHS always contains:
    // - a value for exactly one external sensor
    // - optionally, an internal sensor that turns on ('1') when this rule matched in prev epsiode
    private SensorData rhsExternal = null;
    private int rhsInternal = -1;  //index into internal sensors *or* -1 if none

    //other data  (ignore these for feb 11-18)
    private int[] lastActTimes = new int[ACTHISTLEN];  // last N times the rule was activated
    private double[] lastActAmount = new double[ACTHISTLEN]; // amount of activation last N times
    private int nextActPos = 0;
    private double activationLevel;
    private int lastActUpdate; //when was last time I updated activation
    private double decayRate = 0.95;
    private int decayAmount = 0;

    //Constructor with RHS prediction
    public Rule(PhuJusAgent agent, char act, SensorData currExt, HashMap<Integer, Boolean> currInt, String rhsSensorName, boolean rhsValue){
        this.agent = agent;
        this.ruleId = this.nextRuleId++;
        this.action = act;
        this.lhsExternal = currExt;
        this.lhsInternal = currInt;
        //if external sensor is goal, set value
        if (rhsSensorName.equals(SensorData.goalSensor)) {
            this.rhsExternal = new SensorData(rhsValue);
        } else {
            this.rhsExternal = new SensorData(false);
            this.rhsExternal.setSensor(rhsSensorName, rhsValue);
            this.rhsExternal.removeSensor(SensorData.goalSensor);
        }
    }


    //Constructor to create random rule + add time idx for all previous sensors in previous episode
    public Rule(PhuJusAgent agent){
        this.agent = agent;
        this.ruleId = this.nextRuleId++;

        Action[] actions = agent.getActionList();

        //choose random action for the character
        this.action = actions[rand.nextInt(agent.getNumActions())].getName().charAt(0);

        //50% chance that rule has a specific sensor
        //Possible problems -> Rules that have no current external sensors
        SensorData externalSensors = agent.getCurrExternal();


        String[] sNames =
                externalSensors.getSensorNames().toArray(new String[0]);

        //select one random rhs external sensor
        int sensorIndex = rand.nextInt(sNames.length);
        String rhsSensorName = sNames[sensorIndex];

        //rhs value is determined by current external sensor value
        boolean rhsValue = (boolean) agent.getCurrExternal().getSensor(rhsSensorName);

        //if external sensor is goal, set value
        if (rhsSensorName.equals(SensorData.goalSensor)) {
            this.rhsExternal = new SensorData(rhsValue);
        } else {
            this.rhsExternal = new SensorData(false);
            this.rhsExternal.setSensor(rhsSensorName, rhsValue);
            this.rhsExternal.removeSensor(SensorData.goalSensor);
        }

        pickRandomLHS();

    }

    /**
     * addExternalCondition
     *
     * selects a random external sensor condition for this rule.  This is
     * always selected from the previous external sensor values to ensure
     * that the selected condition is possible.  The method verifies that
     * the condition is unique.
     *
     * CAVEAT:  if a new condition can't be added this method does nothing
     *
     */
    public void addExternalCondition() {
        //see if lhsExternal needs to be initialized
        if(this.lhsExternal == null) {
            this.lhsExternal = SensorData.createEmpty();
        }

        //Special Case:  LHS already contains all sensor names
        SensorData prevExternal = this.agent.getPrevExternal();
        if (this.lhsExternal.size() == prevExternal.size()) {
            return; //fail silently...
        }

        //select a LHS condition from the prev external sensors
        int numExternal = prevExternal.getSensorNames().size();
        String sName = null;
        do {
            String[] sNames = prevExternal.getSensorNames().toArray(new String[0]);
            sName = sNames[rand.nextInt(sNames.length)];
        } while(this.lhsExternal.hasSensor(sName));

        //Add the condition
        Boolean val = (Boolean) prevExternal.getSensor(sName);
        this.lhsExternal.setSensor(sName, val);

    }//addExternalCondition

    /**
     * addInternalCondition
     *
     * same as {@link #addExternalCondition()} but for an internal condition
     *
     */
    public void addInternalCondition() {
        int newCondIdx = -1;
        SensorData prevExternal = this.agent.getPrevExternal();
        Integer newCond = null;
        String sName = null;
        do {
            newCond = Integer.valueOf(rand.nextInt(PhuJusAgent.NUMINTERNAL));
        } while(this.lhsInternal.containsKey(newCond));

        //Add the condition
        Boolean val = (Boolean) agent.getPrevInternalValue(newCond);
        this.lhsInternal.put(newCond, val);
    }//addInternalCondition

    /**
     * pickRandomLHS
     *
     * picks random lhs external or internal sensor and adds it to the lhs
     * of this rule. The value that it assigns to that sensor in this rule
     * will be set to match a given episode. This method takes care not to
     * add a sensor that's already present i.e. no duplicates
     */
    public void pickRandomLHS() {
        // ensure at least one external condition
        addExternalCondition();

        //Add other conditions sometimes
        while(rand.nextDouble() < EXTRACONDFREQ) {
            //flip a coin: external or internal
            if (rand.nextInt(2) == 0) {
                addExternalCondition();
            } else {
                addInternalCondition();
            }
        }
    }//pickRandomLHS





    public void printRule(){
        System.out.print("#" + this.ruleId + ":");
        System.out.print(this.action);

        //print associated internal sensor
        System.out.print("<");
        if (this.rhsInternal != -1) {
            System.out.print(this.rhsInternal);
        }
        System.out.print(">");

        for(Integer i : this.lhsInternal.keySet()){
            System.out.print('(' + String.valueOf(i.intValue()) + ',' + this.lhsInternal.get(i) + ')');
        }
        System.out.print("|");
        if(this.lhsExternal != null) {
            for (String s : this.lhsExternal.getSensorNames()) {
                System.out.print('(' + s + ',' + this.lhsExternal.getSensor(s) +
                        ')');
            }
        }
        System.out.print(" -> ");
        for(String s : this.rhsExternal.getSensorNames()) {
            System.out.print('(' + s + ',' + this.rhsExternal.getSensor(s) + ')');
        }
        System.out.print(" * " + this.getActivationLevel());
        System.out.println();
    }


    //calculates the activation of the rule atm
    public double oldGetActivation() {
        double result = 0.0;
        for(int j=0; j < lastActTimes.length; ++j) {
            if(lastActTimes[j] != 0) {
                result += Math.pow(lastActAmount[j], -0.8);
            }
        }

        if(result != 0.0) {
            result = abs(Math.log(result));
        }

        //the amount of times needed to decay (for all missed episodes)
        for (int i = 0; i<decayAmount; i++) {
            result = result*decayRate;
        }

        this.activationLevel = result;
        return this.activationLevel;
    }

    //calculates the activation of the rule atm
    public double calculateActivation(int now) {
        double result = 0.0;
        for(int j=0; j < lastActTimes.length; ++j) {
            if(lastActTimes[j] != 0) {
                double decayAmount = Math.pow(decayRate, now-lastActTimes[j]);
                result += lastActAmount[j]*decayAmount;
            }
        }

        this.activationLevel = result;
        return this.activationLevel;
    }

    public void setLastActivation(double[] lastActivation) {
        this.lastActAmount = lastActivation;
    }

    public double[] getLastActivation() {
        return this.lastActAmount;
    }

    public double getActivationLevel() {
        return this.activationLevel;
    }

    public void setActivationLevel(int now, boolean goal, boolean punish, Rule [] matchArray, int lastMatchIdx, int matchHistLength){
        if(goal) {
            double reward = 4;
            this.addActivation(now, reward);
//            if(punish) {
//                reward*=-1;
//            }
            // Reward rules that have recently matched, under the assumption they helped. Then clears match history
            int i = lastMatchIdx;
            while(matchArray[i] != null) {
                reward = reward*.8;
                matchArray[i].addActivation(now, reward);
                // Null it out so it does not get rewarded twice
                matchArray[i] = null;
                // Iterate to next rule
                i--;
                if(i < 0) {
                    i = matchHistLength - 1;
                }
            }
//        } else { // Potentially remove this portion and have rules get rewarded just from chaining back goal rewards
//            this.lastActAmount[this.nextActPos] = 2;
        }
    }

    /**
     * addActivation
     *
     * adds a new activation event to this rule.
     *
     * @param now time of activation
     * @param reward amount of activation (can be negative to punish)
     */
    public void addActivation(int now, double reward) {
        this.lastActTimes[this.nextActPos] = now;
        this.lastActAmount[this.nextActPos] = reward;
        this.nextActPos = (this.nextActPos + 1) % ACTHISTLEN;
    }


    public int getRHSInternal(){
        return this.rhsInternal;
    }

    public void setRHSInternal(int newInt) {
        this.rhsInternal = newInt;
    }

    public void turnOffIntSensorInUse(int rhsInternalIdx){
        this.intSensorInUse[rhsInternalIdx] = false;
        this.setRHSInternal(-1);
    }

    /**
     * matches
     *
     * @return true if this rule matches the given action and sensor values
     */
    public boolean matches(char action, SensorData currExternal, HashMap<Integer, Boolean> currInternal)
    {
        //action match
        if (this.action != action) { return false; }

        //internal sensors match
        if(this.lhsInternal != null) {
            for (Integer i : this.lhsInternal.keySet()) {
                if (this.lhsInternal.get(i) != currInternal.get(i)) {
                    return false;
                }
            }
        }

        //external sensors match
        if(this.lhsExternal != null) {
            for (String s : this.lhsExternal.getSensorNames()) {
                if (!this.lhsExternal.getSensor(s)
                        .equals(currExternal.getSensor(s))) {
                    return false;
                }
            }
        }

        //all tests passed
        return true;
    }//matches

    //returns true if this rule matches the given action and sensor values
    public boolean correctMatch(char action, SensorData currExternal, HashMap<Integer, Boolean> currInternal, SensorData correctRHS)
    {
        if (this.action != action) { return false; }
        if(this.lhsInternal != null) {
            for (Integer i : this.lhsInternal.keySet()) {
                if (this.lhsInternal.get(i) != currInternal.get(i)) {
                    return false;
                }
            }
        }
        if(this.lhsExternal != null) {
            for (String s : this.lhsExternal.getSensorNames()) {
                if (!this.lhsExternal.getSensor(s)
                        .equals(currExternal.getSensor(s))) {
                    return false;
                }
            }
        }
        if(this.rhsExternal != null) {
            for (String s : this.rhsExternal.getSensorNames()) {
                if(correctRHS != null) {
                    if (!rhsExternal.getSensor(s)
                            .equals(correctRHS.getSensor(s))) {
                        return false;
                    }
                } else {
                    return false;
                }
            }
        }
        return true;
    }

    public int getAssocSensor() {
        return this.rhsInternal;
    }

    public char getChar() {
        return this.action;
    }

    public SensorData getLHSExternal() { return this.lhsExternal; }

    public String getRHSSensorName(){
        for(String s : this.rhsExternal.getSensorNames()){
            return s;
        }
        return "";
    }

    public int getRHSValue(){
        for(String s : this.rhsExternal.getSensorNames()){
            if((boolean)this.rhsExternal.getSensor(s)){
                return 1;
            }
            else{
                return 0;
            }
        }
        return 0;
    }
    public int getRuleId(){
        return this.ruleId;
    }

    /**
     * equals
     *
     * checks for rule equivalence on both LHS and RHS
     * Note: rhsInternal is not compared, and activation levels are ignored
     */
    @Override
    public boolean equals(Object o) {
        // If it references itself, then return a true
        if(o == this) {
            return true;
        }

        // If a non-rule is passed in, return false
        if(!(o instanceof Rule)) {
            return false;
        }

        Rule rule = (Rule) o;

        // Make sure actions match
        if(rule.action != this.action) {
            return false;
        }

        // Make sure the LHS internal sensors match
        // If both non-null, check the specific sensors for non-matches, return false
        if((rule.lhsInternal != null) && (this.lhsInternal != null)) {
            // If they're not the same size, return false
            if(rule.lhsInternal.size() != this.lhsInternal.size()) {
                return false;
            }
            for(Integer i : this.lhsInternal.keySet()) {
                if(rule.lhsInternal.get(i) != this.lhsInternal.get(i)) {
                    return false;
                }
            }
        // If one is null and the other isn't, return false
        } else if(rule.lhsInternal != this.lhsInternal) {
            return false;
        }

        // Make sure the LHS external sensors match
        // If both non-null, check the specific sensors for non-matches, return false
        if((rule.lhsExternal != null) && (this.lhsExternal != null)) {
            if(!(this.lhsExternal.equals(rule.lhsExternal))) {
                return false;
            }
        // If one is null and the other isn't, return false
        } else if(rule.lhsExternal != this.lhsExternal) {
            return false;
        }

        // Make sure the RHS external sensors match
        // If both non-null, check the sensors for match, if they don't, return false
        if((rule.rhsExternal != null) && (this.rhsExternal != null)) {
            for(String s : this.rhsExternal.getSensorNames()) {
                if (!this.rhsExternal.getSensor(s).equals(rule.rhsExternal.getSensor(s))) {
                    return false;
                }
            }
        // If one is null and the other isn't, return false
        } else if(rule.rhsExternal != this.rhsExternal) {
            return false;
        }

        // All conditions match, return true
        return true;
    }


}//class Rule
