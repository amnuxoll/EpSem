package agents.phujus;

import framework.Action;
import framework.IAgent;
import framework.IIntrospector;
import framework.SensorData;
import environments.fsm.FSMEnvironment.Sensor;

import java.util.ArrayList;
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

    //root of the prediction tree
    TreeNode root; // init this somewhere ...
    @Override
    public void initialize(Action[] actions, IIntrospector introspector) {
        this.actionList = actions;
        this.introspector = introspector;
    }

    //DEBUG:  print out internal sensors
    private void printInternalSensors(HashMap<Integer, Boolean> printMe) {
        for(Integer i : printMe.keySet()) {
            System.out.println("\t" + i.toString() + ":" + printMe.get(i));
        }
    }        

    @Override
    public Action getNextAction(SensorData sensorData) throws Exception {
        if(this.currExternal == null) {
            this.currExternal = sensorData;
            this.prevExternal = this.currExternal;
        }
        else {
            this.prevExternal = this.currExternal;
            this.currExternal = sensorData;
        }
        this.currInternal.put(0, false);
        this.currInternal.put(1, false);
        this.currInternal.put(2, false);
        this.currInternal.put(3, false);
        this.prevInternal = this.currInternal;

        //print the external sensor data out
        System.out.println("External Sensors: ");
        for(String s : sensorData.getSensorNames()) {
            System.out.println("\t" + s + ":" + sensorData.getSensor(s));
        }

        System.out.println("Internal Sensors: ");
        printInternalSensors(this.currInternal);





        this.generateRules();


        
        this.root = new TreeNode(this, this.rules, now, this.currInternal, this.currExternal, '\0');
        this.root.genSuccessors(1);
        String s = this.root.findBestGoalPath(this.root);
        System.out.println("s: " + s.charAt(1));
        Action action = new Action(s.charAt(1) + "");
        System.out.println(action.getName());
        this.root.printTree();

        //Now that we know what action to take, update the internal sensors so they ar ready for the 
        //next call to this method (next time step)
        this.prevInternal = this.currInternal;
        this.currInternal = new HashMap<Integer, Boolean>();
        //This variable is needed to make the call but we will throw away the data that it is filled with (for now!)
        HashMap<String, double[]> deleteMe =  new HashMap<String, double[]>(); 
        this.root.genNextSensors(s.charAt(1), currInternal, deleteMe);


        System.out.println("Next internal sensors: ");
        printInternalSensors(this.currInternal);


        //DEBUG: For now, bail out after first call
        System.exit(0);
        
        if(!s.equals(null)) {
            return action;
        }
        else{
            Random rand = new Random();
            return this.actionList[rand.nextInt(this.getNumActions())];
        }
    }

    //TODO checkforUniqueID
    public void generateRules(){
        while(!ruleLimitReached()){
            Rule newRule = new Rule(this);
            addRule(newRule);
            newRule.printRule();
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

    public void removeRule(Rule oldRule) {
        rules.remove(oldRule);
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
