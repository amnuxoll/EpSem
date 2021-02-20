package agents.phujus;

import framework.Action;
import framework.IAgent;
import framework.IIntrospector;
import framework.SensorData;
import environments.fsm.FSMEnvironment.Sensor;

import java.util.ArrayList;

public class PhuJusAgent implements IAgent {

    public static final int MAXDEPTH = 3;
    public static final int MAXNUMRULES = 8;
    public static final int NUMINTERNAL = 4;

    //a list of all the rules in the system (can't exceed some maximum)
    private ArrayList<Rule> rules = new ArrayList<Rule>(); // convert to arraylist

    //map to find a rule that activates a particular internal sensor.
    //key:  an internal sensor index
    //value:  the rule that lights it up
    //e.g., Rule #631 causes internal sensor #298 to turn on when it fires
    private Rule[] internalMap = new Rule[NUMINTERNAL];

    private int now = 0; //current timestep 't'

    private Action[] actionList;

    //current sensor values
    int[] currInternal = new int[NUMINTERNAL];
    SensorData currExternal;

    //predicted sensor values for t+1
    int[] nextInternal = new int[NUMINTERNAL];

    //2 represents the number of arrays of external sensors. NUMEXTERNAL represents the number of external sensors we're using
    double[][] predictedExternal = { {0.0, 0.0, 0.0, 0.0}, {0.0, 0.0, 0.0, 0.0} }; // possibly switch to hashmap
    private ArrayList<Double> predictedExternalOn = new ArrayList<Double>();
    private ArrayList<Double> predictedExternalOff = new ArrayList<Double>();

    //root of the prediction tree
    TreeNode root; // init this somewhere ...
    @Override
    public void initialize(Action[] actions, IIntrospector introspector) {
        actionList = actions;

    }

    @Override
    public Action getNextAction(SensorData sensorData) throws Exception {
        this.currExternal = sensorData;
        this.root = new TreeNode(this, this.rules, now, this.currInternal, this.currExternal);
        this.root.genSuccessors(1);

        return null;
    }

    public boolean ruleLimitReached() {
        if (rules.size() >= MAXNUMRULES) { return true; }
        return false;
    }

    public void addRule(Rule newRule) {
        if (this.ruleLimitReached()) {
            return;
        }
        rules.add(newRule);
    }

    public void removeRule(Rule oldRule) {
        rules.remove(oldRule);
    }

    public void setNow(int now) {
        this.now = now;
    }

    public int getNow() {
        return now;
    }

    public int getNumActions() {
        return actionList.length;
    }


//    public char someMethod() {
//        root = new TreeNode(this.ruleList, this.now, this.currInternal, this.currExternal);
//        root.genSuccessor(MAXDEPTH);
//        char action = root.findBestGoalPath(); //TODO: write this method
//        return action;
//
//    }




}
