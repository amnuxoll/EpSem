package agents.phujus;
import environments.fsm.FSMEnvironment.Sensor;
import framework.Action;
import framework.SensorData;
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

    //each rule has a unique integer id
    private int ruleId;

    //define the LHS of the rule.  This consists of:
    // - 0 or more internal sensors that have a particular value (0 or 1)
    // - 0 or more values for external sensors
    // (note:  you must have at least 1 of one of the above)
    private int[][] lhsInternal; //example: {{467, 801, 21}, {0, 1, 1} }
    private SensorData  lhsExternal;  //example: {1,-1,0,-1}  note: -1 is "wildcard"
    private char action;

    //define the RHS of the rule.  The RHS always contains:
    // - a value for exactly one external sensor
    // - optionally, an internal sensor that turns on ('1') when this rule matched in prev epsiode
    private String rhsSensorName;   //index into the external sensor array
    private int rhsValue;  // 0 or 1
    private int rhsInternal;  //index into internal sensors *or* -1 if none

    //other data  (igore these for feb 11-18)
    private int[] lastActTimes = new int[ACTHISTLEN];  // last N times the rule was activated
    private double[] lastActAmount = new double[ACTHISTLEN]; // amount of activation last N times
    private double activationLevel;
    private int lastActUpdate; //when was last time I updated activation
    private double decayRate;

    //Constructor
    public Rule(char act, SensorData currExt, int[][] currInt){
        this.action = act;
        this.lhsExternal = currExt;
        this.lhsInternal = currInt;
        this.lastActAmount = new double[]{0.0, 0.0};
    }

    //Constructor with RHS prediction
    public Rule(char act, SensorData currExt, int[][] currInt, String rhsSensName){
        this.action = act;
        this.lhsExternal = currExt;
        this.lhsInternal = currInt;
        this.rhsSensorName = rhsSensName;
        this.lastActAmount = new double[]{0.0, 0.0};
    }

    //Constructor to create random rule
    public Rule(PhuJusAgent agent){
        Action[] actions = agent.getActionList();
        Random rand = new Random();
        this.action = actions[rand.nextInt(agent.getNumActions())].getName().charAt(0);
        this.lhsExternal = agent.getCurrExternal();
        int[][] currInternal = new int[1][1];
        currInternal[0][0] = 1;
        this.lhsInternal = currInternal;
        this.lastActAmount = new double[]{0.0, 0.0};
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
    public boolean matches(char action, SensorData currExternal, int[] currInternal)
    {
        if (this.action != action) { return false; }
        int index = 0;

        for (String s : currExternal.getSensorNames()) {
            if (!this.lhsExternal.getSensor(s).equals(currExternal.getSensor(s))) {
                return false;
            }
        }
        for (int i = 0; i < currInternal.length; i++) {
            if(this.lhsInternal[0][i] != currInternal[i]){
                return false;
            }
        }

        return true;
    }

    //returns true if this rule matches the given action and sensor values
    public boolean matches(SensorData currExternal, int[] currInternal)
    {
        int index = 0;

        for (String s : currExternal.getSensorNames()) {
            if (this.lhsExternal.getSensor(s) != currExternal.getSensor(s)) {
                return false;
            }
        }
        for (int i = 0; i < currInternal.length; i++) {
            if(this.lhsInternal[0][i] != currInternal[i]){
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

    public String getRHSSensorName() {
        return this.rhsSensorName;
    }

    public char getChar() {
        return this.action;
    }

    public SensorData getLHSExternal() { return this.lhsExternal; }

    public void setRhsValue(int rhsValue) {
        this.rhsValue = rhsValue;
    }
}//class Rule
