package agents.phujus;

import framework.Action;
import framework.IAgent;
import framework.IIntrospector;
import framework.SensorData;
import environments.fsm.FSMEnvironment.Sensor;

import java.io.FileWriter;
import java.util.ArrayList;
import java.util.*;
import java.util.HashMap;
import java.util.Random;

/**
 * class PhuJusAgent
 */
public class PhuJusAgent implements IAgent {
    //graphing activation levels
    private HashMap<Integer, String> rulesActivationCSV = new HashMap<>();

    public static final int MAXDEPTH = 3;
    public static final int MAXNUMRULES = 8;
    public static final int NUMINTERNAL = 4;

    //activation
    public static int RULEMATCHHISTORYLEN = 100;
    private Rule matchArray[] = new Rule[RULEMATCHHISTORYLEN]; //stores last 100 rules that match and correctly matched the future
    private int lastMatchIdx = 0;

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


    //random numbers are useful sometimes
    private Random rand = new Random();


    @Override
    public void initialize(Action[] actions, IIntrospector introspector) {
        //Store the actions the agents can take and introspector for data analysis later on
        this.actionList = actions;
        this.introspector = introspector;
    }


    //DEBUG:  print out internal sensors
    private void printInternalSensors(HashMap<Integer, Boolean> printMe) {
        System.out.println("Internal Sensors: ");
        for(Integer i : printMe.keySet()) {
            System.out.println("\t" + i.toString() + ":" + printMe.get(i));
        }
    }

    /**
     * randomActionPath
     *
     * generates a string single valid action letter in it
     */
    private String randomActionPath() {
        return actionList[this.rand.nextInt(getNumActions())].getName();
    }


    /**
     * Gets a subsequent move based on the provided sensorData.
     *
     * @param sensorData The {@link SensorData} from the current move.
     * @return the next Action to try.
     */
    @Override
    public Action getNextAction(SensorData sensorData) throws Exception {
        now++;
        System.out.println("TIME STEP: " + now);
        Action action = new Action("a" + "");
        char act = '\0';

        if(this.currExternal == null) { //Use case: time step t = 0 when the agent has not taken any action yet
            this.currExternal = sensorData;
            this.prevExternal = this.currExternal;
        }
        else { //get currExternal values from sensorData
            this.currExternal = sensorData;
        }

        if(this.currInternal.size() == 0) {
            this.initCurrInternal();
            //prev internal has no previous internal sensors, assign it to curr
            this.prevInternal = this.currInternal;
        }




        //Create rules based on the agent's current internal and external sensors
        if (sensorData.isGoal()) {
            System.out.println("We hit the goal!");
            //Generate random rules if none are in inventory
            if(this.rules.size() == 0) {
                this.generateRules();
            } else{
                //Getting rid of half the lowest rules to update them
                this.updateRules();
            }
            //Resetting the path after we've reached the goal
            this.path = "";
        }

        //If we don't have a path to follow, find a goal path
        if(this.path.equals("")) {
            this.buildPathFromEmpty();
            action = new Action(this.path.charAt(0) + "");

            //Shorten the path by one so that subsequent calls to this method can grab the right action to take
            this.path = this.path.substring(1);
        }
        else{
            //path has been found in previous call to this method, so get the next action
            action = new Action(this.path.charAt(0) + "");

            //the agent next action will be random until it finds the goal if there was only 1 action in the path
            if(this.path.length() == 1){
                this.path = randomActionPath();
            }
            else {
                this.path = this.path.substring(1);
            }
        }

        //if the agents' rule was predicted correctly, update the activation level
        if (now != 0) {
            for (Rule r : this.rules) {
                if (r.correctMatch(action.getName().charAt(0), this.prevExternal, this.prevInternal, this.currExternal)) {
                    r.setActivationLevel(now);
                }
            }
        }

        //DEBUG:
        printExternalSensors(sensorData);
        printInternalSensors(this.currInternal);
        printRules(action);



        //Now that we know what action to take, all curr values are now previous values
        this.prevInternal = this.currInternal;
        this.prevExternal = this.currExternal;
        //get the new currInternal sensors for next action based on char action taken this step
        this.getNewInternalSensors(action);

        return action;
    }

    public Rule[] getMatchArray() {
        return this.matchArray;
    }

    public void incrementMatchIdx() {
        this.lastMatchIdx=(this.lastMatchIdx+1)%RULEMATCHHISTORYLEN;
    }

    public int getLastMatchIdx() {
        return this.lastMatchIdx;
    }

    public void getNewInternalSensors(Action action) {
        this.currInternal = new HashMap<Integer, Boolean>();
        //This variable is needed to make the call but we will throw away the data that it is filled with (for now!)
        HashMap<String, double[]> deleteMe =
                new HashMap<String, double[]>();

        //create temporary treeNode to send in correct currInternal
        TreeNode tempNode = new TreeNode(this, this.rules, now, this.prevInternal,
                this.currExternal, '\0');

        tempNode.genNextSensors(action.getName().charAt(0), currInternal, deleteMe, false);
    }


    public void printExternalSensors(SensorData sensorData) {
        System.out.println("External Sensors: ");
        for(String s : sensorData.getSensorNames()) {
            System.out.println("\t" + s + ":" + sensorData.getSensor(s));
        }
    }

    public void printRules(Action action) {
        System.out.println("Selected action: " + action.getName().charAt(0));
        for (Rule r : this.rules) {
            if(r.matches(action.getName().charAt(0), this.currExternal, this.currInternal)){
                System.out.print("@");
            } else{
                System.out.print(" ");
            }
            r.printRule();

        }
    }

    private void initCurrInternal() {
        //Initialize current and previous internal sensors with random values
        for(int i = 0; i < NUMINTERNAL; i++) {
            this.currInternal.put(i, false);
        }
    }

    public void buildPathFromEmpty() {
        this.root = new TreeNode(this, this.rules, now, this.currInternal,
                this.currExternal, '\0');
        this.root.genSuccessors(3);

        //Find an entire sequence of characters that can reach the goal and get the current action to take
        this.path = this.root.findBestGoalPath(this.root).replace("\0", "");
        //if unable to find path, produce random path for it to take
        if (this.path.equals("")) {
            this.path = randomActionPath();
            System.out.println("random path: " + this.path);
        }else {
            System.out.println("path: " + this.path);
        }
    }

    public void updateRules() {
        for(int i = 0; i < this.rules.size()/2; i++) {
            double lowestActivation = 100.0;
            int curRuleId = 0;
            //Look for lowest rule
            for (Rule r : this.rules) {
                if(r.getActivation(this.getNow()) < lowestActivation){
                    lowestActivation = r.getActivation(this.getNow());
                    curRuleId = r.getRuleId();
                }
            }
            this.removeRule(curRuleId);
        }
        this.generateRules();
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
        if(this.prevInternal == null) {
            System.out.println("Null!");
        }
        if(this.prevInternal.get(Integer.valueOf(id)) == null) {
            System.out.println("Null!");
        }
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

}
