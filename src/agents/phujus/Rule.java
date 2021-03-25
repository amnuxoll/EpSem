package agents.phujus;
import environments.fsm.FSMEnvironment.Sensor;
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

    //to assign a unique id to each rule this shared variable is incremented by the ctor
    private static int nextRuleId = 1;

    //to track which internal sensor values are available for use
    private static boolean[] intSensorInUse = new boolean[PhuJusAgent.NUMINTERNAL];

    private Random rand = new Random();

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
    private double activationLevel;
    private int lastActUpdate; //when was last time I updated activation
    private double decayRate;

    //Constructor with RHS prediction
    public Rule(char act, SensorData currExt, HashMap<Integer, Boolean> currInt, String rhsSensorName, boolean rhsValue){
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
        this.lastActAmount = new double[]{0.0, 0.0};
    }


    //Picks random lhs external or internal sensor and adds it to the lhs of this rule. The value
    // that it assigns to that sensor in this rule will be set to match a given episode.
    // This method takes care not to add a sensor that's already present i.e. no duplicates
    public void pickRandomLHS(PhuJusAgent agent) {
        //select a random sensor
        int numExternal = agent.getCurrExternal().getSensorNames().size();
        int numInternal = PhuJusAgent.NUMINTERNAL;
        int randIdx = rand.nextInt(numExternal+numInternal);

        //this if statements checks to see if we select an external sensor or internal sensor
        if (randIdx < numExternal) {
            //lhs sensors are determined by lhs sensors from previous episode
            String[] sNames = agent.getPrevExternal().getSensorNames().toArray(new String[0]);
            //check if this is the first external sensor
            if(this.lhsExternal == null) {
                //handle differently if it is the goal sensor
                if (sNames[randIdx].equals(SensorData.goalSensor)) {
                    this.lhsExternal = new SensorData(rand.nextInt(2) == 1);
                } else {
                    this.lhsExternal = new SensorData(false);
                    //lhs external sensor is determined by external sensor from previous episode
                    this.lhsExternal.setSensor(sNames[randIdx], agent.getPrevExternal().getSensor(sNames[randIdx]));
                    this.lhsExternal.removeSensor(SensorData.goalSensor);
                }
            } else {
                if (this.lhsExternal.getSensor(sNames[randIdx]) != null) {
                    //try again
                    pickRandomLHS(agent);
                } else {
                    //lhs external sensor is determined by external sensor from previous episode
                    this.lhsExternal.setSensor(sNames[randIdx], agent.getPrevExternal().getSensor(sNames[randIdx]));
                }
            }
        } else {
            // subtract the num of External sensors for valid index
            randIdx = randIdx - numExternal;

            if (lhsInternal.containsKey(Integer.valueOf(randIdx))) {
                //try again if accidentally selected duplicate sensor
                pickRandomLHS(agent);
            } else {
                lhsInternal.put(Integer.valueOf(randIdx), agent.getPrevInternalValue(randIdx));
            }
        }
    }

    //Constructor to create random rule + add time idx for all previous sensors in previous episode
    public Rule(PhuJusAgent agent){
        this.ruleId = this.nextRuleId++;

        //assign a sensor if one is available
        int isBase = rand.nextInt(PhuJusAgent.NUMINTERNAL);
        for(int i = 0; i < PhuJusAgent.NUMINTERNAL; ++i) {
            int isIndex = (isBase + i) % PhuJusAgent.NUMINTERNAL;
            if (! this.intSensorInUse[isIndex]) {
                this.rhsInternal = isIndex;
                this.intSensorInUse[isIndex] = true;
                break;
            }
        }

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

        do {
            pickRandomLHS(agent);
        } while (rand.nextInt(2) == 0);


        this.lastActAmount = new double[]{0.0, 0.0};
    }

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
        System.out.println();
    }

    //calculates the activation of the rule atm
    public double getActivation(int now) {
        lastActTimes[now] = 2;
        double result = 0.0;
        for(int j=0; j < lastActTimes.length; ++j) {
            if(lastActTimes[j] != 0) {
                result += Math.pow(lastActTimes[j], -0.8);
            }
        }
        result = abs(Math.log(result));
        this.activationLevel = result;

        System.out.println();
        return this.activationLevel;
    }

    public void setLastActivation(double[] lastActivation) {
        this.lastActAmount = lastActivation;
    }

    public double[] getLastActivation() {
        return this.lastActAmount;
    }


    //returns true if this rule matches the given action and sensor values
    public boolean matches(char action, SensorData currExternal, HashMap<Integer, Boolean> currInternal)
    {
        if (this.action != action) { return false; }
        if(this.lhsExternal != null) {
            for (String s : this.lhsExternal.getSensorNames()) {
                if (!this.lhsExternal.getSensor(s)
                        .equals(currExternal.getSensor(s))) {
                    return false;
                }
            }
        }
        if(this.lhsInternal != null) {
            for (Integer i : this.lhsInternal.keySet()) {
                if (this.lhsInternal.get(i) != currInternal.get(i)) {
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
}//class Rule
