package agents.phujus;
import environments.fsm.FSMEnvironment.Sensor;
import framework.SensorData;

/**
 * class Rule
 *
 * describes a single rule
 */

public class Rule {

    //Constants
    public static final int ACTHISTLEN = 10;

    //each rule has a unique integer id
    private int ruleId;

    //define the LHS of the rule.  This consists of:
    // - 0 or more internal sensors that have a particular value (0 or 1)
    // - 0 or more values for external sensors
    // (note:  you must have at least 1 of one of the above)
    private int[][] lhsInternal; //example: {{467, 801, 21}, {0, 1, 1} }
    private int[]  lhsExternal;  //example: {1,-1,0,-1}  note: -1 is "wildcard"
    private char action;

    //define the RHS of the rule.  The RHS always contains:
    // - a value for exactly one external sensor
    // - optionally, an internal sensor that turns on ('1') when this rule matched in prev epsiode
    private int rhsIndex;   //index into the external sensor array
    private byte rhsValue;  // 0 or 1
    private int rhsInternal;  //index into internal sensors *or* -1 if none

    //other data  (igonre these for feb 11-18)
    private int[] lastActTimes = new int[ACTHISTLEN];  // last N times the rule was activated
    private double[] lastActAmount = new double[ACTHISTLEN]; // amount of activation last N times
    private double activationLevel;
    private int lastActUpdate; //when was last time I updated activation
    private double decayRate;

    //Constructor
    public Rule(char act, int[] currExt, int[][] currInt){
        this.action = act;
        this.lhsExternal = currExt;
        this.lhsInternal = currInt;
    }

    //calculates the activation of the rule atm
    public double getActivation(int now) {
        if (now != lastActUpdate) {
            for(int i = 0; i < ACTHISTLEN; ++i) {
                //TODO: do some math here to calculate curr act level
            }
        }

        return this.activationLevel;
    }


    //returns true if this rule matches the given action and sensor values
    public boolean matches(char action, SensorData currExternal, int[] currInternal)
    {
        if (this.action != action) { return false; }
        int index = 0;
        for (String s : currExternal.getSensorNames()) {
            int val = (int) currExternal.getSensor(s);
            if(val != this.lhsExternal[index]){
                return false;
            }
            index++;
        }

        for (int i = 0; i < currInternal.length; i++) {
            if(this.lhsInternal[1][i] != currInternal[i]){
                return false;
            }
        }
        return true;
    }


    public int getAssocSensor() {
        return this.ruleId;
    }

    public int getRHSValue() {
        return this.rhsValue;
    }

    public int getRHSIndex() {
        return this.rhsIndex;
    }
}//class Rule
