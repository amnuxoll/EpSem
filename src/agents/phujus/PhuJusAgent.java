package agents.phujus;

import framework.Action;
import framework.IAgent;
import framework.IIntrospector;
import framework.SensorData;
import environments.fsm.FSMEnvironment.Sensor;

import java.util.ArrayList;
import java.util.*;
import java.util.HashMap;
import java.util.Random;

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
    private IIntrospector introspector;

    //current sensor values
    private HashMap<Integer, Boolean> currInternal = new HashMap<>();
    SensorData currExternal;

    //values from previous instance to build rules
    SensorData prevExternal;
    private HashMap<Integer, Boolean> prevInternal = new HashMap<>();

    //predicted sensor values for t+1
    int[] nextInternal = new int[NUMINTERNAL];

    //2 represents the number of arrays of external sensors. NUMEXTERNAL represents the number of external sensors we're using
    private HashMap<String, double[]> predictedExternal = new HashMap<>();

    //Path to take
    private String path = "";

    //root of the prediction tree
    TreeNode root; // init this somewhere ...
    @Override
    public void initialize(Action[] actions, IIntrospector introspector) {
        //Store the actions the agents can take and introspector for data analysis later on
        this.actionList = actions;
        this.introspector = introspector;
    }

    //DEBUG:  print out internal sensors
    private void printInternalSensors(HashMap<Integer, Boolean> printMe) {
        for(Integer i : printMe.keySet()) {
            System.out.println("\t" + i.toString() + ":" + printMe.get(i));
        }
    }

    /**
     * Gets a subsequent move based on the provided sensorData.
     *
     * @param sensorData The {@link SensorData} from the current move.
     * @return the next Action to try.
     */
    @Override
    public Action getNextAction(SensorData sensorData) throws Exception {
        Action action = new Action("a" + "");
        //Use case: time step t = 0 when the agent has not taken any action yet
        if(this.currExternal == null) {
            this.currExternal = sensorData;
            this.prevExternal = this.currExternal;
        }
        else {// update external sensor values for t-1 and t
            this.prevExternal = this.currExternal;
            this.currExternal = sensorData;
        }
        this.initCurrInternal();
        this.prevInternal = this.currInternal;

//        print the external sensor data out
//        System.out.println("External Sensors: ");
//        for(String s : sensorData.getSensorNames()) {
//            System.out.println("\t" + s + ":" + sensorData.getSensor(s));
//        }
//
//        System.out.println("Internal Sensors: ");
//        printInternalSensors(this.currInternal);

        //Create rules based on the agent's current internal and external sensors
        if (sensorData.isGoal()) {
            System.out.println("We hit the goal!");
            //Generate random rules if none are in inventory
            if(this.rules == null) {
                this.generateRules();
            } else {
                //Getting rid of half the lowest rules
                for(int i = 0; i < this.rules.size()/2; i++) {
                    double lowestActivation = 100.0;
                    int curRuleId = 0;
                    //Look for lowest rule
                    for (Rule r : this.rules) {
                        if(r.getActivation(now) < lowestActivation){
                            lowestActivation = r.getActivation(now);
                            curRuleId = r.getRuleId();
                        }
                    }
                    this.removeRule(curRuleId);
                }
                //replace the rules that were removed
                this.generateRules();
            }
            //Resetting the path after we've reached the goal
            this.path = "";
        }

        //If we don't have a path to follow, find a goal path
        if(this.path.equals("")) {
            //Build a tree from the generated rules
            this.root = new TreeNode(this, this.rules, now, this.currInternal,
                    this.currExternal, '\0');
            this.root.genSuccessors(3);

            //find an entire sequence of characters that can reach the goal
            this.path = this.root.findBestGoalPath(this.root).replace("\0", "");
            System.out.println("path: " + this.path);

            //get the first character of the findbestgoalpath sequence
            action = new Action(this.path.charAt(0) + "");

            //remove first character so that agent will take the characters of the rest of the path in next move
            this.path = this.path.substring(1);

            //print statements
                //System.out.println(action.getName());
                //this.root.printTree();

                //System.out.println("Next internal sensors: ");
                //printInternalSensors(this.currInternal);
        }
        else {
            //Continue grabbing characters of the path to take as actions
            action = new Action(this.path.charAt(0) + "");

            //if the last action has been taken, substitute random characters in for the next action
            if(this.path.length() == 1){
                Random rand = new Random();
                this.path = actionList[rand.nextInt(getNumActions())].getName();
            }
            else {
                this.path = this.path.substring(1);
            }
        }

        //Now that we know what action to take, update the internal sensors so they are ready for the
        //next call to this method (next time step)
        this.prevInternal = this.currInternal;
        this.currInternal = new HashMap<Integer, Boolean>();

        //This variable is needed to make the call but we will throw away the data that it is filled with (for now!)
        HashMap<String, double[]> deleteMe = new HashMap<String, double[]>();
        this.root.genNextSensors(this.path.charAt(0), currInternal, deleteMe, true);

        return action;
        //DEBUG: For now, bail out after first call
        //System.exit(0);

    }

    private void initCurrInternal() {
        //Initialize current and previous internal sensors with random values
        for(int i = 0; i < NUMINTERNAL; i++) {
            this.currInternal.put(i, false);
        }
    }

    /**
     * Fills up agent's rule inventory
     *
     */
    public void generateRules(){
        while(!ruleLimitReached()){
            Rule newRule = new Rule(this);
            addRule(newRule);
            //newRule.printRule();
        }
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

    public void removeRule(int id) {
        for(Rule r : this.rules) {
            if(r.getRuleId() == id) {
                rules.remove(r);
                if(r.getRHSInternal() != -1) {
                    r.turnOffIntSensorInUse(r.getRHSInternal());
                }
                break;
            }
        }
    }

    public ArrayList<Rule> getRules() {return this.rules;}

    public void setCurrInternal(HashMap<Integer, Boolean> curIntern) {this.currInternal = curIntern;}

    public HashMap<Integer, Boolean> getCurrInternal() {return this.currInternal;}

    public void setCurrExternal(SensorData curExtern) {this.currExternal = curExtern;}

    public SensorData getCurrExternal() {return this.currExternal;}

    public SensorData getPrevExternal() {return this.prevExternal;}

    public void setPrevExternal(SensorData prevExtern) {this.prevExternal = prevExtern;}

    public boolean getPrevInternalValue(int id){
        return this.prevInternal.get(Integer.valueOf(id));
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

    public Action[] getActionList() {return actionList;}

    public void setPrevInternal(HashMap<Integer, Boolean> prevInternal) {
        this.prevInternal = prevInternal;
    }


//    public char someMethod() {
//        root = new TreeNode(this.ruleList, this.now, this.currInternal, this.currExternal);
//        root.genSuccessor(MAXDEPTH);
//        char action = root.findBestGoalPath(); //TODO: write this method
//        return action;
//
//    }




}
