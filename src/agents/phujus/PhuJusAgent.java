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
 * > Overhaul string-ified format for Rule objects
 * > add a toString() to PJA
 * > remove "new state" message spam and put new state number with TIME STEP message
 * > increase code coverage and thoroughness of unit tests
 * > implement a debug logging system with levels so we can turn on/off various
 *   types of debug info on the console
 * > Use int[] for internal sensors instead of HashMap (simpler)
 * > Figure out why the results are non-repeatable with a seeded random
 * > Profiling (timing the code)
 *
 * TODO research items
 * > Consider replacing the activation on rules with:
 *    - timestamps of the last N times the rule fired and whether it was correct
 * > Rule overhaul idea:
 *    - track tf-idf value for all sensor-value pairs
 *    - an initial rule's LHS is the entire episode weighted by tf-idf
 *    - use a partial match algorithm with vote-by-weight
 *    - raise/lower weights based on rule performance
 * > Rules refine/generalize themselves based on experience.  Rules should be able to:
 *    - merge when they become very similar
 *    - split when it will improve the activation of both progeny
 * > Experiment with different values for Rule.EXTRACONDFREQ
 * > Experiment with different number of rules replaced in PJA.updateRules()
 * > Should the TreeNode not expand nodes that have no predicted external sensor values?
 * > Punish rules that were wrong?  Seems like the absence of reward is enough.
 */
public class PhuJusAgent implements IAgent {
    public static final int MAXNUMRULES = 50;
    public static final int NUMINTERNAL = MAXNUMRULES/2;
    public static final int INITIAL_ACTIVATION = 15;  //initial activation for first set of rules
    public static final int RULEMATCHHISTORYLEN = MAXNUMRULES * 5;
    public static final int FIRINGS_TIL_REPLACE = MAXNUMRULES * 6;

    // Arrays that track internal sensors' longevity and trues and falses
    private final int[] internalLongevity = new int[NUMINTERNAL];
    private final int[] internalTrues = new int[NUMINTERNAL];
    private final int[] internalFalses = new int[NUMINTERNAL];

    // Arrays that track external sensors' trues and falses
    private HashMap<String, Integer> externalTrues = new HashMap<>();
    private HashMap<String, Integer> externalFalses = new HashMap<>();

    // DEBUG variable to toggle println statements (on/off = true/false)
    public static final boolean DEBUGPRINTSWITCH = true;

    //activation is tracked using a rolling array.  When the end of the array
    //is reached, the data rolls over to zero again.  Use nextMatchIdx()
    //and prevMatchIndex() on the matchIdx variable rather than the
    //'++' and '--' operators
    private final Rule[] matchArray = new Rule[RULEMATCHHISTORYLEN]; //stores last N rules that fired and were correct
    private final int[] matchTimes = new int[RULEMATCHHISTORYLEN]; //mirrors matchArray, but tracks each rule's timestep when fired
    private int matchIdx = 0;

    //a list of all the rules in the system (can't exceed some maximum)
    private Vector<Rule> rules = new Vector<>(); // convert to arraylist

    private int now = 0; //current timestep 't'

    private Action[] actionList;  //list of available actions in current FSM

    //current sensor values
    private HashMap<Integer, Boolean> currInternal = new HashMap<>();
    private SensorData currExternal;

    //sensor values from the previous timestep
    private SensorData prevExternal = null;
    private HashMap<Integer, Boolean> prevInternal = new HashMap<>();
    private char prevAction = '\0';

    //The agent's current selected path to goal (a sequence of actions)
    private String path = "";

    //to track which rules have a given internal sensor on their RHS
    private final Rule[] intSensorInUse = new Rule[PhuJusAgent.NUMINTERNAL];

    //Track how many rule firings have occurred since a rule was removed
    private int ruleFirings = 0;

    //random numbers are useful sometimes (hardcoded seed for debugging)
    public static Random rand = new Random();

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
        this.rules = new Vector<>();
        this.actionList = actions;
        initInternalSensors();
    }

    /**
     * Resets the internal sensor-related variables.
     */
    private void initInternalSensors() {
        for (int i = 0; i < NUMINTERNAL; i++) {
            this.currInternal.put(i, false);
            this.prevInternal.put(i, false);
            this.intSensorInUse[i] = null;
        }
    }

    /**
     * randomActionPath
     * <p>
     * generates a path string with a single valid action letter in it
     */
    private String randomActionPath() {
        return actionList[rand.nextInt(actionList.length)].getName();
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

        //DEBUG
        debugPrintln("----------------------------------------------------------------------");
        debugPrintln("TIME STEP: " + this.now);

        //reward rules that correctly predicted the present
        this.currExternal = sensorData;
        ruleAccounting();

        //Reset path on goal
        if (sensorData.isGoal()) {
            System.out.println("Found GOAL");
            rewardRulesForGoal();
            this.path = "";
        }

        // Sanity check the path each time to make sure it's valid
        TreeNode root = buildTree();
        if(this.path.equals("") || !root.isValidPath(this.path)) {
            buildPathFromEmpty(root);
        }
        updateRules();

        //select next action
        char action = this.path.charAt(0);
        this.path = this.path.substring(1);

        //DEBUG:
        printExternalSensors(this.currExternal);
        trackExternals(this.currExternal);
        printExternalData();
        printInternalSensors(this.currInternal);
        trackInternals(this.currInternal);
        printInternalData();
        debugPrintln("Selected action: " + action + " from path: " + action + "" + this.path);
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
     * keeps up accounting information about rules that is used by the algorithm
     */
    public void ruleAccounting() {
        //don't do this on the first timestep as there is no "previous" step
        if (this.now == 1) return;

        for (Rule r : rules) {
            //record rules that fired in the prev timestep
            if(! r.matches(this.prevAction, this.prevExternal, this.prevInternal)) {
                continue;
            }
            ruleFirings++;
            r.incrMatches();

            //record rules that correctly predicted the current external sensors
            if (!r.predicts(prevAction, this.prevExternal, this.prevInternal, this.currExternal)) {
                continue;
            }
            r.incrPredicts();
            this.matchArray[this.matchIdx] = r;
            this.matchTimes[this.matchIdx] = now-1;
            this.matchIdx = nextMatchIdx();
        }//for

    }//ruleAccounting


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
        HashMap<Integer, Boolean> result = new HashMap<>();

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
     * DEBUG
     * prints internal sensors.  Used for debugging.
     *
     * @param printMe sensors to print
     */
    private void printInternalSensors(HashMap<Integer, Boolean> printMe) {
        debugPrintln("Internal Sensors: ");
        for (Integer i : printMe.keySet()) {
            debugPrintln("\t" + i.toString() + ":" + printMe.get(i));
        }
    }

    /**
     * DEBUG
     * prints the tracker data of the internal sensors. Used for debugging.
     */
    private void printInternalData() {
        System.out.println("Internal Sensor Data: ");
        for(int i = 0; i < NUMINTERNAL; i++) {
            System.out.println("\t" + i + ": age-" + internalLongevity[i] +
                                        "  trues-" + internalTrues[i] +
                                        "   falses-" + internalFalses[i]);
        }
    }

    /**
     * DEBUG
     * prints the tracker data of the external sensors. Used for debugging.
     */
    private void printExternalData() {
        System.out.println("External Sensor Data: ");
        for(String sname : this.externalTrues.keySet()) {
            System.out.println("\t" + sname + ":   trues-" + externalTrues.get(sname) + "   falses-" + externalFalses.get(sname));
        }
    }

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
        if (this.rules.size() == 0) System.out.println("(There are no rules yet.)");

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
    private void buildPathFromEmpty(TreeNode root) {
        //Find an entire sequence of characters that can reach the goal and get the current action to take
        this.path = root.findBestGoalPath();
        //if unable to find path, produce random path for it to take
        root.printTree();
        if (this.path.equals("")) {
            this.path = randomActionPath();
            System.out.println("random path: " + this.path);
        } else {
            System.out.println("path: " + this.path);
        }
    }//buildPathFromEmpty

    /**
     * buildTree
     *
     * @return a prediction tree based on the current rules and sensing
     */
    private TreeNode buildTree() {
        TreeNode root = new TreeNode(this, this.rules, this.now, this.currInternal,
                this.currExternal, "");
        root.genSuccessors(3);
        return root;
    }

    /**
     * updateRules
     *
     * replaces current rules with low activation with new rules that might
     * be more useful.
     *
     */
    public void updateRules() {
        //If we haven't reached max just add a rule
        if (this.rules.size() < MAXNUMRULES) {
            generateRule(INITIAL_ACTIVATION);
            return;
        }

        //Shouldn't remove a rule until a sufficient number of firings have occurred
        if (this.ruleFirings < FIRINGS_TIL_REPLACE) return;

        //Find the rule with lowest activation
        double activationSum = 0.0;
        Rule worstRule = this.rules.get(0);
        double worstScore = worstRule.calculateActivation(this.now) * worstRule.getAccuracy();
        for (Rule r : this.rules) {
            double activation = r.calculateActivation(this.now);
            double score = activation * r.getAccuracy();
            if (score < worstScore) {
                worstScore = score;
                worstRule = r;
            }

            //we'll need this info later
            activationSum += activation;
        }

        //remove and replace
        removeRule(worstRule);
        generateRule(activationSum / this.rules.size());
        this.ruleFirings = 0;

    }//updateRules

    /**
     * Adds a new rule to the agent's inventory
     *
     * @param initAct initial activation level for this rule
     */
    public void generateRule(double initAct) {
        //At timestep 0 there is no previous so this method can't generate rules
        if (this.prevExternal == null) return;

        Rule newRule = new Rule(this);

        // Make sure there are no duplicate rules
        while (this.rules.contains(newRule)) {
            newRule = new Rule(this);
        }

        addRule(newRule);
        newRule.addActivation(now, initAct);

    }//generateRule

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
            if (this.intSensorInUse[isIndex] == null) {
                newRule.setRHSInternal(isIndex);
                this.intSensorInUse[isIndex] = newRule;
                break;
            }
        }
    }

    /**
     * removeRule
     *
     * removes a rule from the agent's repertoire.  If the rule has an internal
     * sensor on its RHS then any rules that test it must also be removed.
     *
     * CAVEAT: recursive
     */
    public void removeRule(Rule removeMe) {
        rules.remove(removeMe);

        //DEBUGGING
        debugPrint("Removed rule: ");
        if(DEBUGPRINTSWITCH) {
            removeMe.printRule();
        }


        //if the removed rule had an internal sensor on its RHS, then all rules test
        // that sensor must also be removed
        int rhsInternal = removeMe.getRHSInternal();
        if (rhsInternal != -1) {
            resetInternalTrackers(rhsInternal); // reset the data tracker for that sensor
            this.intSensorInUse[rhsInternal] = null;

            //have to gather all "to remove" vectors first to avoid ConcurrentModification
            Vector<Rule> toRemove = new Vector<>();
            for (Rule r : this.rules) {
                if (r.getLHSInternal().containsKey(rhsInternal)) {
                    toRemove.add(r);
                }
            }

            for (Rule r : toRemove) {
                removeRule(r);  //recursive call
            }
        }
    }//removeRule

    /**
     * findRules
     *
     * finds all the rules that had matched at a given timestamp and predicted a given sensor value
     * @return the matching rules
     */
    public Vector<Rule> findRules(int timestamp, String sName, boolean value) {
        Vector<Rule> foundRules = new Vector<>();

        //special case:  timestamp too far in the past
        if (timestamp == 0) return foundRules;

        for(int i = 0; i < matchTimes.length; i++) {
            if(timestamp == matchTimes[i]) {
                if((matchArray[i].getRHSSensorName().equals(sName) && (matchArray[i].getRHSSensorValue() == value))) {
                    // Make sure this rule has not been removed
                    if(this.rules.contains(matchArray[i])) {
                        foundRules.add(matchArray[i]);
                    }
                }
            }
        }

        return foundRules;
    }//findRules

    /**
     * rewardRule
     *
     * gives a reward to a given rule and a discounted reward to any rules
     * that, in a given timestep, predicted the values that let this
     * reward match.
     *
     * CAVEAT:  This method is recursive
     */
    public void rewardRule(Rule rule, int timestamp, double reward) {
        //base case #1:  reward too small to matter
        if (reward < 0.01) return;  //TODO: use a constant

        //Issue the reward
        boolean success = rule.addActivation(this.now, reward);
        if (!success) return; //if this rule has already been rewarded don't recurse

        //Also reward rules that predicted the external LHS conditions int the prev timestep
        SensorData lhsExternal = rule.getLHSExternal();
        for(String sName : lhsExternal.getSensorNames()) {
            Boolean val = (Boolean)lhsExternal.getSensor(sName);

            //Note: another implicit base case comes from the finitely-sized matchArray
            //which findRules uses to search
            Vector<Rule> foundRules = findRules(timestamp-1, sName, val);
            for(Rule r : foundRules) {
                //DEBUG
                debugPrint("   for predicting " + sName + "=" + val + " in timestep " + (timestamp) + " ");

                rewardRule(r, timestamp-1, reward * Rule.DECAY_RATE);
            }
        }

        //reward rules that fired to activate the internal LHS conditions
        HashMap<Integer, Boolean> lhsInternal = rule.getLHSInternal();
        for(int key : lhsInternal.keySet()) {
            if (lhsInternal.get(key)) {
                //DEBUG
                debugPrint("   for predicting <" + key + ">=true in timestep " + (timestamp) + " ");

                Rule rewardMe = this.intSensorInUse[key];
                rewardRule(rewardMe, timestamp-1, reward * Rule.DECAY_RATE);
            }
        }

        //Note: Should we also reward rules that correctly did NOT fire so that their associated internal sensor was false?
        //THis doesn't seem to make sense since a rule that never matches can get rewarded.

    }//rewardRule

    /**
     * rewardRulesForGoal
     *
     * is called when the agent reaches a goal to reward all the rules
     * that predicted that would happen
     */
    private void rewardRulesForGoal() {
        Vector<Rule> goalRules = findRules(this.now - 1, SensorData.goalSensor, true);
        for(Rule r : goalRules) {
            //DEBUG
            debugPrint("   for predicting _GOAL_ in timestep " + (now) + " ");

            rewardRule(r, this.now - 1, Rule.FOUND_GOAL_REWARD);
        }
    }//rewardRulesForGoal

    /**
     * debugPrintln
     *
     * is utilized as a helper method to print useful debug information to the console on a line
     * It can be toggled on and off using the DEBUGPRINT variable
     */
    public void debugPrintln(String db) {
        if(DEBUGPRINTSWITCH) {
            System.out.println(db);
        }
    }//debugPrintln

    /**
     * debugPrintln
     *
     * is utilized as a helper method to print useful debug information to the console
     * It can be toggled on/off (true/false) using the DEBUGPRINT variable
     */
    public void debugPrint(String db) {
        if(DEBUGPRINTSWITCH) {
            System.out.print(db);
        }
    }//debugPrint

    /**
     * trackExternals
     *
     * updates the values of the arrays tracking external sensor data including true occurrences
     * and false occurrences
     *
     */
    public void trackExternals(SensorData sensorData) {
        for(String sname : sensorData.getSensorNames()) {
            boolean val = (Boolean)sensorData.getSensor(sname);
            HashMap<String, Integer> toUpdate = this.externalTrues;
            if (!val) toUpdate = this.externalFalses;
            if (toUpdate.containsKey(sname)) {
                toUpdate.put(sname, toUpdate.get(sname) + 1);
            } else {
                toUpdate.put(sname, 1);
            }
        }
    }//trackExternals

    /**
     * trackInternals
     *
     * updates the values of the arrays tracking internal sensor data including
     * longevity, true occurrences, and false occurrences
     *
     * @param internals the Hashmap including all internal sensors
     */
    public void trackInternals(HashMap<Integer, Boolean> internals) {
        for(Integer i : internals.keySet()) {
            internalLongevity[i]++;
            if(internals.get(i)) {
                internalTrues[i]++;
            } else if(!(internals.get(i))) {
                internalFalses[i]++;
            }
        }
    }//trackInternals

    /**
     * resetInternalTracker
     *
     * resets the counts of the internalLongevity, internalTrues, and internalFalses trackers
     * for a specific internal sensor number
     *
     * @param i the index of the internal sensor to reset the values of
     */
    public void resetInternalTrackers(int i) {
        internalLongevity[i] = 0;
        internalTrues[i] = 0;
        internalFalses[i] = 0;
    }//resetInternalTrackers

    /**
     * getExternalRarity
     *
     * @param sName the sensor we are checking
     * @param value the value that we are checking
     * @return the likelihood that a particular external sensor has a particular value
     */

    public double getExternalRarity(String sName, boolean value) {
        int numTrues = 0;
        int numFalses = 0;
        if(this.externalTrues.containsKey(sName)) {
            numTrues = this.externalTrues.get(sName);
        }
        if(this.externalFalses.containsKey(sName)) {
            numFalses = this.externalFalses.get(sName);
        }
        int sum = numTrues + numFalses;
        if(sum == 0) {
            return -1.0; // Passing the buck! Divide by zero should be handled where getExternalRarity is called
        }
        if(value) {
            return ((double) numTrues) / ((double) sum);
        } else {
            return ((double) numFalses) / ((double) sum);
        }
    }//getExternalRarity

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

    public Vector<Rule> getRules() {
        return this.rules;
    }

    public int getNow() {
        return now;
    }

    public Integer[] getPrevInternalKeys() {
        return this.prevInternal.keySet().toArray(new Integer[0]);
    }

    public boolean getPrevInternalValue(int id) {
        if (this.prevInternal == null) {
            System.err.println("Null!");
        }
        if (this.prevInternal.get(id) == null) {
            System.err.println("Null!");
        }
        return this.prevInternal.get(id);
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

    public char getPrevAction() {
        return prevAction;
    }

    //endregion

}//class PhuJusAgent
