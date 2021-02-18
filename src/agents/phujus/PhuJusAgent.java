package agents.phujus;

import framework.Action;
import framework.IAgent;
import framework.IIntrospector;
import framework.SensorData;
import environments.fsm.FSMEnvironment.Sensor;

public class PhuJusAgent implements IAgent {

    public static final int ALPHASIZE = 3;  //example:  a, b, c
    public static final int MAXDEPTH = 3;
    public static final int MAXNUMRULES = 8;
    public static final int NUMINTERNAL = 4;
    public static final int NUMEXTERNAL = 2;

    //a list of all the rules in the system (can't exceed some maximum)
    private Rule[] maxRules = new Rule[MAXNUMRULES];

    //map to find a rule that activates a particular internal sensor.
    //key:  an internal sensor index
    //value:  the rule thaat lights it up
    //e.g., Rule #631 causes internal sensor #298 to turn on when it fires
    private Rule[] maxInternal = new Rule[NUMINTERNAL];

    private int now; //current timestep 't'

    //current sensor values
    byte[] currInternal = new byte[NUMINTERNAL];
    Sensor[] currExternal = new Sensor[NUMEXTERNAL];

    //predicted sensor values for t+1
    byte[] nextInternal = new byte[NUMINTERNAL];

    //2 represents the number of arrays of external sensors. NUMEXTERNAL represents the number of external sensors we're using
    double[][] predictedExternal = { {0.0, 0.0, 0.0, 0.0}, {0.0, 0.0, 0.0, 0.0} };

    //root of the prediction tree
    TreeNode root; // init this somwhere ...
    @Override
    public void initialize(Action[] actions, IIntrospector introspector) {

    }

    @Override
    public Action getNextAction(SensorData sensorData) throws Exception {
        return null;
    }

    public void setNow(int now) {
        this.now = now;
    }

    public int getNow() {
        return now;
    }

    public void setMaxInternal(Rule[] maxInternal) { this.maxInternal = maxInternal; }

    public Rule[] getMaxInternal() {
        return maxInternal;
    }




//    public char someMethod() {
//        root = new TreeNode(this.ruleList, this.now, this.currInternal, this.currExternal);
//        root.genSuccessor(MAXDEPTH);
//        char action = root.findBestGoalPath(); //TODO: write this method
//        return action;
//
//    }




}
