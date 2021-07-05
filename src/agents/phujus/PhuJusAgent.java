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
 * <p>
 * A rule based episodic memory learner
 * <p>
 * TODO code maint items
 * 1. <IN PROGRESS> all the code used by this agent needs a thorough shakedown
 * a. unit test for predictionActivation()
 * 2. consider replacing the activation on rules with:
 * a. a count of correct and incorrect firings
 * b. timestamps of the last N times the rule fired correctly
 * 3. Rules refine/generalize themselves based on experience
 * a. Need to juggle multiple versions of themselves and have the best one be
 * "active" at any given time
 * b. Also need a way to split into two rules if appropriate
 * 4. implement a debug logging system with levels so we can turn on/off various
 * types of debug info on the console
 * 5. Overhaul string-ified format for Rule objects
 * 6. Use int[] for internal sensors instead of HashMap (simpler)
 *
 * TODO research items
 * 1. Experiment with different values for Rule.EXTRACONDFREQ
 * 2. Experiment with different number of rules replaced in PJA.updateRules()
 * 3. Punish rules that were wrong?  Seems like the absence of reward is enough.
 * 4. <PRIORITY> More sophisticated way to reward rules for helping find the goal
 * 5. Rule overhaul idea:
 *    - track tf-idf value for all sensor-value pairs
 *    - an initial rule's LHS is the entire episode weighted by tf-idf
 *    - use a partial match algorithm with vote-by-weight
 *    - raise/lower weights based on rule performance
 */
public class PhuJusAgent implements IAgent {
    public static final int MAXNUMRULES = 8;
    public static final int NUMINTERNAL = 4;
    public static final int INITIAL_ACTIVATION = 15;
    public static final int RULEMATCHHISTORYLEN = 40;

    //activation is tracked using a rolling array.  When the end of the array
    //is reached, the data rolls over to zero again.  Use nextMatchIdx()
    //and prevMatchIndex() on the matchIdx variable rather than the
    //'++' and '--' operators
    private Rule matchArray[] = new Rule[RULEMATCHHISTORYLEN]; //stores last N rules that fired and were correct
    private int matchIdx = 0;

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
    private HashMap<Integer, Boolean> currInternal = new HashMap<>();
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
    public static Random rand = new Random(2);

    /**
     * This method is called each time a new FSM is created and a new agent is
     * created.  As such, it also performs the duties of a ctor.
     *
     * @param actions      An array of {@link Action} representing the actions
     *                     available to the agent.
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
        for (int i = 0; i < NUMINTERNAL; i++) {
            this.currInternal.put(i, false);
            this.prevInternal.put(i, false);
            Rule.intSensorInUse[i] = false;
        }
    }

    /**
     * DEBUG
     * prints internal sensors.  Used for debugging.
     *
     * @param printMe sensors to print
     */
    private void printInternalSensors(HashMap<Integer, Boolean> printMe) {
        System.out.println("Internal Sensors: ");
        for (Integer i : printMe.keySet()) {
            System.out.println("\t" + i.toString() + ":" + printMe.get(i));
        }
    }

    /**
     * randomActionPath
     * <p>
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

        //Reset path on goal
        if (sensorData.isGoal()) {
            System.out.println("Found GOAL");
            this.path = "";
            this.cycle = 0;
        }

        //If we don't have a path to follow, calculate a goal path
        if (this.path.equals("")) {
            buildPathFromEmpty();
            updateRules();
        }

        //select next action
        char action = this.path.charAt(0);
        //the agent next action will be random until it finds the goal if there was only 1 action in the path
        if (this.path.length() == 1) {
            this.path = randomActionPath();
        } else {
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
        if (this.rules.size() > 0) {  //can't update if no rules yet
            this.currInternal = genNextInternal(action);
        }

        return new Action(action + "");
    }//getNextAction

    /**
     * predictionActivation
     * <p>
     * updates the activation level of any rules that correctly predicted
     * the current sensor values
     * <p>
     *
     */
    public void predictionActivation() {
        //don't do this on the first timestep as there is no "previous" step
        if (this.now == 1) return;

        for (Rule r : this.rules) {
            if (!r.predicts(prevAction, this.prevExternal, this.prevInternal, this.currExternal)) {
                continue;
            }

            //Update log of rules that have fired correctly since last goal
            boolean goal = (r.getRHSSensorName().equals(SensorData.goalSensor));
            if (!goal) {
                this.matchArray[this.matchIdx] = r;
                this.matchIdx = nextMatchIdx();
            }

            //Update the rule's activation
            r.activateForGoal();
            r.calculateActivation(this.now);
        }//for
    }//predictionActivation

    /**
     * genNextInternal
     * <p>
     * calculates what the internal sensors will be for the next timestep
     * <p>
     * @param action  selected action to generate from
     * @param currInt internal sensors to generate from
     * @param currExt external sensors to generate from
     */
    public HashMap<Integer, Boolean> genNextInternal(char action,
                                                     HashMap<Integer, Boolean> currInt,
                                                     SensorData currExt) {
        HashMap<Integer, Boolean> result = new HashMap<Integer, Boolean>();

        for (Rule r : rules) {
            int sIndex = r.getAssocSensor();
            if (sIndex == -1) continue; //this rule doesn't set an internal sensor
            boolean match = r.matches(action, currExt, currInt);
            result.put(sIndex, match);
        }//for

        return result;
    }//genNextInternal

    /**
     * convenience overload that uses the this.currInternal and this.currExternal
     */
    public HashMap<Integer, Boolean> genNextInternal(char action) {
        return genNextInternal(action, this.currInternal, this.currExternal);
    }//genNextInternal

    /**
     * printExternalSensors
     * <p>
     * verbose debugging println
     *
     * @param sensorData sensors to print
     */
    public void printExternalSensors(SensorData sensorData) {
        System.out.println("External Sensors: ");
        for (String s : sensorData.getSensorNames()) {
            System.out.println("\t" + s + ":" + sensorData.getSensor(s));
        }
    }//printExternalSensors

    /**
     * printRules
     * <p>
     * prints all the rules in a verbose ASCII format
     *
     * @param action the action the agent has selected.  This can be set to '\0' if the action is not yet known
     */
    public void printRules(char action) {
        if (action != '\0') {
            System.out.println("Selected action: " + action);
        }

        for (Rule r : this.rules) {
            if ((action != '\0') && (r.matches(action, this.currExternal, this.currInternal))) {
                System.out.print("@");
            } else {
                System.out.print(" ");
            }
            r.calculateActivation(now);
            r.printRule();
        }
    }

    /**
     * convenience version of printRules with no selected action
     */
    public void printRules() {
        printRules('\0');
    }

    /**
     * buildPathFromEmpty
     *
     * builds a tree of predicted outcomes to calculate a path to goal.
     * The resulting path is placed in this.path.
     */
    private void buildPathFromEmpty() {
        this.root = new TreeNode(this, this.rules, this.now, this.currInternal,
                this.currExternal, '\0', "");
        this.root.genSuccessors(3);

        //Find an entire sequence of characters that can reach the goal and get the current action to take
        this.path = this.root.bfFindBestGoalPath(this.root).replace("\0", "");
        //if unable to find path, produce random path for it to take
        if (this.path.equals("")) {
            this.path = randomActionPath();
            System.out.println("random path: " + this.path);
        } else {
            System.out.println("path: " + this.path);
        }
    }//buildPathFromEmpty

    /**
     * updateRules
     *
     * replaces current rules with low activation with new rules that might
     * be more useful.
     *
     * @param numReplacements  the number of rules to replace
     */
    public void updateRules(int numReplacements) {
        //No updates can be performed until the agent has taken at least one step
        if (now <= 1) return;

        //Special case:  create the initial set of rules once the agent has
        // a legitimate current and previous set of sensors
        if (this.rules.size() == 0) {
            generateRules();
            return;
        }

        for(int i = 0; i < numReplacements; ++i) {
            Rule worstRule = this.rules.get(0);
            worstRule.calculateActivation(this.now);
            // Find for lowest rule
            for (Rule r : this.rules) {
                if (r.calculateActivation(this.now) < worstRule.getActivationLevel()) {
                    worstRule = r;
                }
            }
            this.removeRule(worstRule);

            //DEBUGGING
            System.out.print("Removing rule: ");
            worstRule.printRule();
        }

        //refill rule set up to max capacity
        generateRules();

    }//updateRules

    /** convenient overload to replace one rule */
    public void updateRules() { updateRules(1); }

    /**
     * Fills up agent's rule inventory
     */
    public void generateRules() {
        //At timestep 0 there is no previous so this method can't generate rules
        if (this.prevExternal == null) return;

        //DEBUG:  verify agent keeps good rules
        if (rules.size() == 0) {
            // Add a good rule: (IS_EVEN, false) a -> (GOAL, true)
            SensorData gr1 = new SensorData(false);
            gr1.setSensor("IS_EVEN", false);
            gr1.removeSensor("GOAL");
            Rule cheater = new Rule(this, 'a', gr1, new HashMap<Integer, Boolean>(), "GOAL", true);
            addRule(cheater);
            cheater.addActivation(now, 15);

            // Add a good rule: (IS_EVEN, true) b -> (IS_EVEN, false)
            SensorData gr2 = new SensorData(false);
            gr2.setSensor("IS_EVEN", true);
            gr2.removeSensor("GOAL");
            Rule cheater2 = new Rule(this, 'b', gr2, new HashMap<Integer, Boolean>(), "IS_EVEN", false);
            addRule(cheater2);
            cheater2.addActivation(now, 15);
        }//if

        while (rules.size() < MAXNUMRULES) {
            Rule newRule = new Rule(this);

            // Make sure there are no duplicate rules
            while (this.rules.contains(newRule)) {
                newRule = new Rule(this);
            }

            addRule(newRule);
            newRule.addActivation(now, INITIAL_ACTIVATION);
        }
    }//generateRules

    /**
     * addRule
     *
     * adds a given {@link Rule} to the agent's repertoire.  This method will
     * fail silently if you try to exceed {@link #MAXNUMRULES}.  This method
     * will also assign an internal sensor to the new rule if one is
     * available.
     */
    public void addRule(Rule newRule) {
        if (rules.size() >= MAXNUMRULES) {
            return;
        }
        rules.add(newRule);

        //assign an internal sensor
        int isBase = rand.nextInt(PhuJusAgent.NUMINTERNAL);
        for (int i = 0; i < PhuJusAgent.NUMINTERNAL; ++i) {
            int isIndex = (isBase + i) % PhuJusAgent.NUMINTERNAL;
            if (!newRule.intSensorInUse[isIndex]) {
                newRule.setRHSInternal(isIndex);
                newRule.intSensorInUse[isIndex] = true;
                break;
            }
        }
    }

    /**
     * removeRule
     *
     * removes a rule from the agent's repertoire
     */
    public void removeRule(Rule r) {
        rules.remove(r);

        if (r.getRHSInternal() != -1) {
            System.out.println("Removed rule has internal sensor ID: " + r.getRHSInternal());
            r.turnOffIntSensorInUse(r.getRHSInternal());
        }
    }

    //region Getters and Setters

    public Rule[] getMatchArray() {
        return this.matchArray;
    }

    public int nextMatchIdx() {
        return (this.matchIdx + 1) % RULEMATCHHISTORYLEN;
    }

    public int prevMatchIndex() {
        int result = this.matchIdx - 1;
        if (result < 0) {
            result = RULEMATCHHISTORYLEN - 1;
        }
        return result;
    }

    public int getMatchIdx() {
        return this.matchIdx;
    }

    public int getRuleMatchHistoryLen() {
        return RULEMATCHHISTORYLEN;
    }

    public Vector<Rule> getRules() {
        return this.rules;
    }

    public void setNow(int now) {
        this.now = now;
    }

    public int getNow() {
        return now;
    }

    public boolean getPrevInternalValue(int id) {
        if (this.prevInternal == null) {
            System.err.println("Null!");
        }
        if (this.prevInternal.get(Integer.valueOf(id)) == null) {
            System.err.println("Null!");
        }
        return this.prevInternal.get(Integer.valueOf(id));
    }

    public HashMap<Integer, Boolean> getCurrInternal() {
        return this.currInternal;
    }

    public void setCurrInternal(HashMap<Integer, Boolean> curIntern) {
        this.currInternal = curIntern;
    }

    public SensorData getCurrExternal() {
        return this.currExternal;
    }

    public void setCurrExternal(SensorData curExtern) {
        this.currExternal = curExtern;
    }

    public SensorData getPrevExternal() {
        return this.prevExternal;
    }

    public void setPrevExternal(SensorData prevExtern) {
        this.prevExternal = prevExtern;
    }

    public int getNumActions() {
        return actionList.length;
    }

    public Action[] getActionList() {
        return actionList;
    }

    //endregion

}//class PhuJusAgent
