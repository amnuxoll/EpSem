package agents.phujus;

import framework.Action;
import framework.IAgent;
import framework.IIntrospector;
import framework.SensorData;

import java.util.*;
import java.util.HashMap;
import java.util.Random;

/**
 * class PhuJusAgent
 * 
 * A rule based episodic memory learner
 * 
 * TODO list
 * 1. <IN PROGRESS> all the code used by this agent needs a thorough shakedown
 *    a. unit test for predictionActivation()
 * 2. consider replacing the activation on rules with:
 *    a. a count of correct and incorrect firings
 *    b. timestamps of the last N times the rule fired correctly
 * 3. Rules refine/generalize themselves based on experience
 *    a. Need to juggle multiple versions of themselves and have the best one be
 *       "active" at any given time
 *    b. Also need a way to split into two rules if appropriate
 * 4. implement a debug logging system with levels so we can turn on/off various
 *    types of debug info on the console
 * 5. Overhaul string-ified format for Rule objects
 */
public class PhuJusAgent implements IAgent {
    public static final int MAXNUMRULES = 8;
    public static final int NUMINTERNAL = 4;
    private static final int MINSEEDSTEPS = 5;

    //activation
    public static int RULEMATCHHISTORYLEN = 40;
    private Rule matchArray[] = new Rule[RULEMATCHHISTORYLEN]; //stores last 100 rules that match and correctly matched the future
    private int lastMatchIdx = 0;

    //a list of all the rules in the system (can't exceed some maximum)
    private Vector<Rule> rules = new Vector<Rule>(); // convert to arraylist

    //map to find a rule that activates a particular internal sensor.
    //key:  an internal sensor index
    //value:  the rule that lights it up
    //e.g., Rule #631 causes internal sensor #298 to turn on when it fires
    private Rule[] internalMap = new Rule[NUMINTERNAL];

    private int now = 0; //current timestep 't'

    private Action[] actionList;  //list of available actions in current FSM
    private IIntrospector introspector;  //required by framework, not used by this agent

    //current sensor values
    private HashMap<Integer, Boolean> currInternal = new HashMap<>();  //TODO: why isn't this an array or String?
    private SensorData currExternal;

    //sensor values from the previous timestep
    private SensorData prevExternal = null;
    private HashMap<Integer, Boolean> prevInternal = new HashMap<>();
    private char prevAction = '\0';

    //The agent's current selected path to goal (a sequence of actions)
    private String path = "";
    private int cycle = 0;  //steps taken since last goal

    //This tree predicts outcomes of future actions
    TreeNode root;

    //random numbers are useful sometimes (hardcoded seed for debugging)
    private static Random rand = new Random(2);

    /**
     * This method is called each time a new FSM is created and a new agent is
     * created.  As such, it also performs the duties of a ctor.
     *
     * @param actions An array of {@link Action} representing the actions
     *                available to the agent.
     * @param introspector an {@link IIntrospector} that can be used to request
     *                     metadata for tracking {@link IAgent} data.
     */
    @Override
    public void initialize(Action[] actions, IIntrospector introspector) {
        this.rules = new Vector<Rule>();
        this.actionList = actions;
        this.introspector = introspector;
        initInternalSensors();
    }

    /**
     * Resets the internal sensor-related variables.
     */
    private void initInternalSensors() {
        for(int i = 0; i < NUMINTERNAL; i++) {
            this.currInternal.put(i, false);
            this.prevInternal.put(i, false);
            Rule.intSensorInUse[i] = false;
        }
    }

    /**  DEBUG
     * prints internal sensors.  Used for debugging.
     *
     *
     *
     * @param printMe  sensors to print
     */
    private void printInternalSensors(HashMap<Integer, Boolean> printMe) {
        System.out.println("Internal Sensors: ");
        for(Integer i : printMe.keySet()) {
            System.out.println("\t" + i.toString() + ":" + printMe.get(i));
        }
    }

    /**
     * randomActionPath
     *
     * generates a path string with a single valid action letter in it
     */
    private String randomActionPath() {
        return actionList[this.rand.nextInt(actionList.length)].getName();
    }


    /**
     * Gets a subsequent move based on the provided sensorData.
     *
     * @param sensorData The {@link SensorData} from the current move.
     * @return the next Action to try.
     */
    @Override
    public Action getNextAction(SensorData sensorData) throws Exception {
        this.now++;
        this.cycle++;

        //DEBUG
        System.out.println("----------------------------------------------------------------------");
        System.out.println("TIME STEP: " + this.now);

        //reward rules that correctly predicted the present
        this.currExternal = sensorData;
        predictionActivation();

        updateRules();

        //Reset path on goal
        if (sensorData.isGoal()) {
            System.out.println("Found GOAL");
            this.path = "";
            cycle = 0;
        }

        //If we don't have a path to follow, calculate a goal path
        if(this.path.equals("")) {
            buildPathFromEmpty();
        }

        //select next action
        char action = this.path.charAt(0);
        //the agent next action will be random until it finds the goal if there was only 1 action in the path
        if(this.path.length() == 1){
            this.path = randomActionPath();
        }
        else {
            this.path = this.path.substring(1);
        }

        //DEBUG:
        printExternalSensors(this.currExternal);
        printInternalSensors(this.currInternal);
        printRules(action);

        //Now that we know what action to take, setup the sensor values for the next iteration
        this.prevInternal = this.currInternal;
        this.prevExternal = this.currExternal;
        this.prevAction = action;
        this.currInternal = genNextInternal(action);

        return new Action(action + "");
    }//getNextAction

    /**
     * predictionActivation
     *
     * updates the activation level of any rules that correctly predicted
     * the current sensor values
     *
     * TODO:  punish rules that were wrong?
     */
    public void predictionActivation() {
        //don't do this on the first timestep as there is no "previous" step
        if (this.now == 1) return;

        for (Rule r : this.rules) {
            if (! r.correctMatch(prevAction, this.prevExternal, this.prevInternal, this.currExternal)) {
                continue;
            }

            //Update log of rules that have fired correctly since last goal
            boolean goal = (r.getRHSSensorName().equals(SensorData.goalSensor));
            if  (!goal){
                this.matchArray[this.lastMatchIdx] = r;
                this.lastMatchIdx = (this.lastMatchIdx + 1) % RULEMATCHHISTORYLEN;
            }

            //Update the rule's activation
            int prevMatch = this.lastMatchIdx - 1;
            if(prevMatch < 0) {
                prevMatch = RULEMATCHHISTORYLEN - 1;
            }
            r.setActivationLevel(this.now, goal, false, this.matchArray, prevMatch, this.getRuleMatchHistoryLen());
            r.calculateActivation(this.now);
        }//for
    }//predictionActivation

    /**
     * genNextInternal
     *
     * calculates what the internal sensors will be for the next timestep
     *
     * TODO: stop pull a helper method out of genNextSensors() for this to avoid the deleteMe variable
     *
     * @param action  selected action to generate from
     * @param currInt internal sensors to generate from
     * @param currExt external sensors to generate from
     */
    public HashMap<Integer, Boolean> genNextInternal(char action,
                                                     HashMap<Integer, Boolean> currInt,
                                                     SensorData currExt) {

        HashMap<Integer, Boolean> result = new HashMap<Integer, Boolean>();

        for(Rule r : rules) {
            int sIndex = r.getAssocSensor();
            if (sIndex == -1) continue; //this rule doesn't set an internal sensor
            boolean match = r.matches(action, currExt, currInt);
            result.put(sIndex, match);
        }//for

        return result;
    }//genNextInternal

    /** convenience overload that uses the this.currInternal and this.currExternal */
    public HashMap<Integer, Boolean> genNextInternal(char action) {
        return genNextInternal(action, this.currInternal, this.currExternal);
    }//genNextInternal

    //TODO:  this is where :AMN: left off on the overhaul

    public void printExternalSensors(SensorData sensorData) {
        System.out.println("External Sensors: ");
        for(String s : sensorData.getSensorNames()) {
            System.out.println("\t" + s + ":" + sensorData.getSensor(s));
        }
    }

    /**
     * printRules
     *
     * prints all the rules in an abbreviated format
     *
     * @param action  the action the agent has selected.  This can be set to '\0' if the action is not yet known
     */
    public void printRules(char action) {
        if (action != '\0') {
            System.out.println("Selected action: " + action);
        }

        for (Rule r : this.rules) {
            if( (action != '\0') && (r.matches(action, this.currExternal, this.currInternal)) ){
                System.out.print("@");
            } else {
                System.out.print(" ");
            }
            r.calculateActivation(now);
            r.printRule();
        }
    }

    /** convenience version of printRules with no selected action */
    public void printRules() { printRules('\0'); }




    public void buildPathFromEmpty() {
        this.root = new TreeNode(this, this.rules, this.now, this.currInternal,
                this.currExternal, '\0', "");
        this.root.genSuccessors(3);

        //Find an entire sequence of characters that can reach the goal and get the current action to take
        this.path = this.root.bfFindBestGoalPath(this.root).replace("\0", "");
        //if unable to find path, produce random path for it to take
        if (this.path.equals("")) {
            this.path = randomActionPath();
            System.out.println("random path: " + this.path);
        }else {
            System.out.println("path: " + this.path);
        }
    }

    /**
     * updateRules
     *
     * replaces current rules with low activation with new rules that might be more useful
     */
    public void updateRules() {
        //Special case:  create the initial set of rules once the agent has
        // a legitimate current and previous set of sensors
        if (now == 2) {
            generateRules();
            return;
        }

        //no updates for the first N timesteps
        //TODO: is this needed?
        if (now < MINSEEDSTEPS) return;

        //no updates until a path has failed
        //TODO:  This should not be hard-coded.  Instead the agent should track whether a selected path was successful.  Do we need the cycle variable at all?
        if ((cycle > 1) && (cycle < 5)) return;

        if (this.rules.size() > 0) {
//        for(int i = 0; i < this.rules.size()/5; i++) {
//            double lowestActivation = 1000.0;
            Rule worstRule = this.rules.get(0);
            worstRule.calculateActivation(this.now);
            // Find for lowest rule
            for (Rule r : this.rules) {
                if (r.calculateActivation(this.now) < worstRule.getActivationLevel()) {
                    worstRule = r;
                }
            }
            this.removeRule(worstRule);
            System.out.print("Removing rule: ");
            worstRule.printRule();
//        }
        }
        this.generateRules();
    }

    /**
     * Fills up agent's rule inventory
     *
     */
    public void generateRules(){
        //At timestep 0 there is no previous so this method can't generate rules
        if (this.prevExternal == null) return;

//        //DEBUG: add a good rule to start: (IS_EVEN, false) a -> (GOAL, true)
//        SensorData s = new SensorData(false);
//        s.setSensor("IS_EVEN", false);
//        s.removeSensor("GOAL");
//        addRule(new Rule('a', s, new HashMap<Integer, Boolean>(), "GOAL", true));
//
//        //DEBUG: add another good rule: (IS_EVEN, true) b -> (IS_EVEN, false)
//        SensorData secondSensor = new SensorData(false);
//        secondSensor.setSensor("IS_EVEN", true);
//        secondSensor.removeSensor("GOAL");
//        addRule(new Rule('b', secondSensor, new HashMap<Integer, Boolean>(), "IS_EVEN", false));

        if(rules.size() == 0) {
            // Add a good rule: (IS_EVEN, false) a -> (GOAL, true)
            SensorData gr1 = new SensorData(false);
            gr1.setSensor("IS_EVEN", false);
            gr1.removeSensor("GOAL");
            Rule cheater = new Rule('a', gr1, new HashMap<Integer, Boolean>(), "GOAL", true);
//        newbie.setRHSInternal(0);
//        Rule.intSensorInUse[0] = true;
            addRule(cheater);
            cheater.addActivation(now, 15);

            // Add a good rule: (IS_EVEN, true) b -> (IS_EVEN, false)
            SensorData gr2 = new SensorData(false);
            gr2.setSensor("IS_EVEN", true);
            gr2.removeSensor("GOAL");
            Rule cheater2 = new Rule('b', gr2, new HashMap<Integer, Boolean>(), "IS_EVEN", false);
            addRule(cheater2);
            cheater2.addActivation(now, 15);

        }

        while(!ruleLimitReached()){
            Rule newRule = new Rule(this);
            // Make sure there are no duplicate rules
            while(this.rules.contains(newRule)) {
                newRule = new Rule(this);
            }
            addRule(newRule);
            newRule.addActivation(now, 15); // TODO this should not be a hard coded value
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
        //assign a sensor if one is available
        int isBase = rand.nextInt(PhuJusAgent.NUMINTERNAL);
        for(int i = 0; i < PhuJusAgent.NUMINTERNAL; ++i) {
            int isIndex = (isBase + i) % PhuJusAgent.NUMINTERNAL;
            if (! newRule.intSensorInUse[isIndex]) {
                newRule.setRHSInternal(isIndex);
                newRule.intSensorInUse[isIndex] = true;
                break;
            }
        }
    }

    public void removeRule(Rule r) {
        while(rules.contains(r)) {
            rules.remove(r);
            if(r.getRHSInternal() != -1) {
                System.out.println("Removed rule has internal sensor ID: " + r.getRHSInternal());
                r.turnOffIntSensorInUse(r.getRHSInternal());
            }
        }
    }

    public Rule[] getMatchArray() { return this.matchArray; }
    public void incrementMatchIdx() { this.lastMatchIdx=(this.lastMatchIdx+1)%RULEMATCHHISTORYLEN; }
    public int getLastMatchIdx() { return this.lastMatchIdx; }
    public int getRuleMatchHistoryLen() { return RULEMATCHHISTORYLEN; }

    public Vector<Rule> getRules() {return this.rules;}

    public void setNow(int now) { this.now = now; }
    public int getNow() { return now; }

    public boolean getPrevInternalValue(int id){
        if(this.prevInternal == null) {
            System.out.println("Null!");
        }
        if(this.prevInternal.get(Integer.valueOf(id)) == null) {
            System.out.println("Null!");
        }
        return this.prevInternal.get(Integer.valueOf(id));
    }

    public void setPrevInternal(HashMap<Integer, Boolean> prevInternal) { this.prevInternal = prevInternal; }

    public HashMap<Integer, Boolean> getCurrInternal() {return this.currInternal;}
    public void setCurrInternal(HashMap<Integer, Boolean> curIntern) {this.currInternal = curIntern;}

    public SensorData getCurrExternal() {return this.currExternal;}
    public void setCurrExternal(SensorData curExtern) {this.currExternal = curExtern;}

    public SensorData getPrevExternal() {return this.prevExternal;}
    public void setPrevExternal(SensorData prevExtern) {this.prevExternal = prevExtern;}

    public int getNumActions() { return actionList.length; }
    public Action[] getActionList() {return actionList;}

}
