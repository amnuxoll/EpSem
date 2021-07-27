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
 * > Implement .dot format print-to-file for the current FSM
 * > add a toString() to PJA
 * > remove "new state" message spam and put new state number with TIME STEP message
 * > increase code coverage and thoroughness of unit tests
 * > implement a debug logging system with levels so we can turn on/off various
 *   types of debug info on the console
 * > Figure out why the results are non-repeatable with a seeded random
 * > Profiling (timing the code)
 *
 * TODO research items
 * > Rule overhaul idea:
 *    - track tf-idf value for all sensor-value pairs
 *    - an initial rule's LHS is the entire episode weighted by tf-idf
 *    - use a partial match algorithm with vote-by-weight
 *    - raise/lower weights based on rule performance
 * > Rules refine/generalize themselves based on experience.  Rules should be able to:
 *    - merge when they become very similar
 *    - split when it will improve the activation of both progeny
 * > Change currInternal to be a HashSet of rules that fired?  I'm not sure if
 *    logging that a rule didn't fire is useful?
 */
public class PhuJusAgent implements IAgent {
    public static final int MAXNUMRULES = 50;
    public static final int INITIAL_ACTIVATION = 15;  //initial activation for first set of rules
    public static final int RULEMATCHHISTORYLEN = MAXNUMRULES * 5;
    public static final int MAXDEPTH = 100;

    // These variables are used to track sensor longevity and rarity of sensor values
    //TODO: implement this
//    private final int[] internalLongevity = new int[MAXNUMRULES];
//    private final int[] internalTrues = new int[MAXNUMRULES];
//    private final int[] internalFalses = new int[MAXNUMRULES];
//
//    // Arrays that track external sensors' trues and falses
//    private HashMap<String, Integer> externalTrues = new HashMap<>();
//    private HashMap<String, Integer> externalFalses = new HashMap<>();

    // DEBUG variable to toggle println statements (on/off = true/false)
    public static final boolean DEBUGPRINTSWITCH = true;

    //recent rule matches are tracked using a rolling array.  When the end of
    // the array is reached, the data rolls over to zero again.  Use
    // nextMatchIdx() and prevMatchIndex() on the matchIdx variable rather
    // than the '++' and '--' operators
    private final EpRule[] matchArray = new EpRule[RULEMATCHHISTORYLEN]; //stores last N rules that fired TODO: and were correct?
    private final int[] matchTimes = new int[RULEMATCHHISTORYLEN]; //mirrors matchArray, but tracks each rule's timestep when fired
    private int matchIdx = 0;

    //a list of all the rules in the system (can't exceed some maximum)
    private Vector<EpRule> rules = new Vector<>();

    private int now = 0; //current timestep 't'

    private Action[] actionList;  //list of available actions in current FSM

    //current sensor values (TODO: change to HashSet of trues only)
    private HashMap<Integer, Boolean> currInternal = new HashMap<>();
    private SensorData currExternal;

    //sensor values from the previous timestep
    private HashMap<Integer, Boolean> prevInternal = new HashMap<>();  //TODO: also change to HashSet
    private SensorData prevExternal = null;
    private char prevAction = '\0';

    //The agent's current selected path to goal (a sequence of nodes in the search tree)
    private Vector<TreeNode> pathToDo = null;
    private Vector<TreeNode> pathTraversedSoFar = new Vector<>();

    //random numbers are useful sometimes (use a hardcoded seed for debugging)
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
        this.rules = new Vector<>();
        this.actionList = actions;
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

        //Reset path when goal is reached
        if (sensorData.isGoal()) {
            System.out.println("Found GOAL");
            rewardRulesForGoal();
            buildNewPath();

        //reset path once expended with no goal
        } else if ((this.pathToDo == null) || (this.pathToDo.size() == 0)) {
            buildNewPath();
        }

        //TODO: reset path if it's no longer valid

        updateRules();

        //extract next action
        char action;
        if (pathToDo != null) {
            action = this.pathToDo.get(0).getAction();
            debugPrintln("Selected action: " + action + " from path: " + pathToString(this.pathToDo));
            this.pathTraversedSoFar.add(this.pathToDo.remove(0));
        } else {
            //random action
            action = actionList[rand.nextInt(actionList.length)].getName().charAt(0);
            debugPrintln("random action: " + action);
        }

        //DEBUG:
        if (PhuJusAgent.DEBUGPRINTSWITCH) {
            printExternalSensors(this.currExternal);
            printInternalSensors(this.currInternal);
            printRules(action);
        }

        //Now that we know what action to take, setup the sensor values for the next iteration
        this.prevInternal = this.currInternal;
        this.prevExternal = this.currExternal;
        this.prevAction = action;
        if (this.rules.size() > 0) {  //can't update if no rules yet
            this.currInternal = genNextInternal(action);
        }

        return new Action(action + "");
    }//getNextAction

    private void resetPath() {
        this.pathToDo = null;
        this.pathTraversedSoFar.clear();
    }

    /**
     * genNextInternal
     * <p>
     * calculates what the internal sensors will be for the next timestep
     * by seeing which rules have a sufficient match score.
     *
     * At the moment, "sufficient" is more than halfway between the average and
     * best score.  TODO:  something more statistically sound?
     * <p>
     * @param action  selected action to generate from
     * @param currInt internal sensors to generate from
     * @param currExt external sensors to generate from
     */
    public HashMap<Integer, Boolean> genNextInternal(char action,
                                                     HashMap<Integer, Boolean> currInt,
                                                     SensorData currExt) {
        HashMap<Integer, Boolean> result = new HashMap<>();
        double bestScore = 0.0;
        double scoreSum = 0.0;
        double matchCount = 0.0;
        int index = 0;
        double[] scores = new double[this.rules.size()];

        //Get a match score for every rule
        //  also calc the average and best
        for (EpRule r : this.rules) {
            scores[index] = r.matchScore(action, currInt, currExt);
            if (scores[index] > 0.0) {
                scoreSum += scores[index];
                matchCount++;
            }
            if (scores[index] > bestScore) {
                bestScore = scores[index];
            }
            index++;
        }
        double avgScore = scoreSum / matchCount;

        //set the sensor values for matching rules
        double threshold = avgScore + ((bestScore - avgScore) / 2);
        index = 0;
        for(EpRule r : this.rules) {
            boolean val = scores[index] > threshold;
            result.put(r.getId(), val);
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
        debugPrint("Internal Sensors: ");
        for (Integer i : printMe.keySet()) {
            boolean val = printMe.get(i);
            if (val) debugPrint("" + i.toString() + ", ");
        }
        debugPrintln("");
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

        for (EpRule r : this.rules) {
            //Print a match score first
            if (action != '\0') {
                double score = r.matchScore(action);
                System.out.print(String.format("0.3f", score));
            } else {
                System.out.print("     ");
            }

            //TODO: update activation here?: r.calculateActivation(now);

            debugPrintln(r.toString());
        }
    }//printRules

    /** convenience version of printRules with no selected action */
    public void printRules() { printRules('\0'); }

    /** prints the sequence of actions discovered by a path */
    public String pathToString(Vector<TreeNode> path) {
        if (path == null) return "<null path>";
        StringBuilder sbResult = new StringBuilder();
        for(TreeNode node : path) {
            sbResult.append(node.getAction());
        }
        return sbResult.toString();
    }//pathToString

    /**
     * buildNewPath
     *
     * uses a search tree to find a path to goal.  The resulting path is
     * placed in this.pathToDo if found.  Otherwise, this.pathToDO is
     * set to null.
     *
     */
    private void buildNewPath() {
        this.pathTraversedSoFar.clear();

        //Find a new path to the goal
        TreeNode root = new TreeNode(this);
        this.pathToDo = root.findBestGoalPath();

        //DEBUG
        if ( (PhuJusAgent.DEBUGPRINTSWITCH) && (this.pathToDo != null) ) {
            root.printTree();
            debugPrintln("found path: " + pathToString(this.pathToDo));
        }

    }//buildNewPath

    /**
     * updateRules
     *
     * replaces current rules with low activation with new rules that might
     * be more useful.
     *
     */
    public void updateRules() {
        //Can't create a rule if there is not previous timestep
        if (this.now == 1) return;

        //Create a candidate new rule based on agent's current state
        EpRule cand = new EpRule(this);

        //Find the existing rule that is most similar
        EpRule bestMatch = null;
        double bestScore = -1.0;
        for(EpRule r : this.rules) {
            double score = r.compareTo(cand);
            if (score > bestScore) {
                bestMatch = r;
                bestScore = score;
            }
        }

        //If the two rules are equal there is nothing to gain by adding this
        if (bestScore == 1.0) return;

        //If we haven't reached max just add it
        if (this.rules.size() < MAXNUMRULES) {
            addRule(cand);
            return;
        }

        //TODO:  consider merging cand with the bestRule?
        double revScore = cand.compareTo(bestMatch);
        if (revScore == 1.0) {
            //DEBUG
            debugPrintln("Note:  revScore is 1.0");
        }

        //Find the rule with lowest activation & accuracy
        double activationSum = 0.0;
        EpRule worstRule = this.rules.get(0);
        double worstScore = worstRule.calculateActivation(this.now) * worstRule.getAccuracy();
        for (EpRule r : this.rules) {
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
        addRule(cand);

    }//updateRules


    /**
     * addRule
     *
     * adds a given {@link Rule} to the agent's repertoire.  This method will
     * fail silently if you try to exceed {@link #MAXNUMRULES}.  This method
     * will also assign an internal sensor to the new rule if one is
     * available.
     */
    public void addRule(EpRule newRule) {
        if (rules.size() >= MAXNUMRULES) {
            return;
        }
        rules.add(newRule);
    }

    /**
     * removeRule
     *
     * removes a rule from the agent's repertoire.  If the rule has an internal
     * sensor on its RHS then any rules that test it must also be removed.
     *
     * CAVEAT: recursive
     */
    public void removeRule(EpRule removeMe) {
        rules.remove(removeMe);

        //DEBUGGING
        debugPrint("Removed rule: ");
        debugPrintln(removeMe.toString());

        // If any rule has a condition that test for 'removeMe' then that
        // condition must be removed
        for (EpRule r : this.rules) {
            if (r.testsIntSensor(removeMe.getId())) {
                r.removeIntSensor(removeMe.getId());
                //TODO: reset rule activation, matches and predicts?
                //TODO: reset internal trackers?
                break;
            }
        }
    }//removeRule

    /**
     * rewardRulesForGoal
     *
     * is called when the agent reaches a goal to reward all the rules
     * that predicted that would happen.  Rewards passed back decay
     * ala reinforcement learning.
     */
    private void rewardRulesForGoal() {
        //reward the rules in reverse order
        double reward = EpRule.FOUND_GOAL_REWARD;
        int time = this.now;
        for(int i = this.pathTraversedSoFar.size() - 1; i >= 0; --i) {
            TreeNode node = this.pathTraversedSoFar.get(i);
            EpRule rule = node.getRule();
            if (rule != null) rule.addActivation(time, reward);
            time--;
            reward *= EpRule.DECAY_RATE;
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


    //region Getters and Setters

    public EpRule[] getMatchArray() {
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

    public Vector<EpRule> getRules() {
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

    public HashMap<Integer, Boolean> getPrevInternal() {
        return this.prevInternal;
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
