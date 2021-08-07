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
 * > Figure out why the results are non-repeatable with a seeded random
 * > Profiling (timing the code)
 * > Implement .dot format print-to-file for the current FSM
 * > add a toString() to PJA
 * > increase code coverage and thoroughness of unit tests
 * > implement a debug logging system with levels so we can turn on/off various
 *   types of debug info on the console
 * > fix all warnings
 *
 * TODO research items
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
    public static final int MAXDEPTH = 5; //TODO: get the search to self prune again
    public static final int SHORT_TERM_SIZE = 7;  //size of short term memory

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

    //a list of all the rules in the system (can't exceed some maximum)
    private Vector<EpRule> rules = new Vector<>();

    private int now = 0; //current timestep 't'

    private Action[] actionList;  //list of available actions in current FSM

    //internal sensors are rule ids of rules that fired in the prev time step
    private HashSet<Integer> currInternal = new HashSet<>();

    //external sensors come from the environment
    private SensorData currExternal;

    //sensor values from the previous timesteps
    private Vector<HashSet<Integer>> prevInternal = new Vector<>();
    private SensorData prevExternal = null;
    private char prevAction = '\0';

    //The agent's current selected path to goal (a sequence of nodes in the search tree)
    private Vector<TreeNode> pathToDo = null;
    private final Vector<TreeNode> pathTraversedSoFar = new Vector<>();
    private Vector<TreeNode> prevPath = null;

    // Counter that tracks time steps since hitting a goal
    private int stepsSinceGoal = 0;

    //PathRules are stored here
    private PathRule pendingPR = null;
    private Vector<PathRule> pathRules = new Vector<>();

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
        this.currExternal = sensorData;
        this.stepsSinceGoal++;

        //DEBUG
        if (PhuJusAgent.DEBUGPRINTSWITCH) {
            debugPrintln("TIME STEP: " + this.now);
            printInternalSensors(this.currInternal);
            printExternalSensors(this.currExternal);
        }

        //DEBUG
        if(this.stepsSinceGoal >= 20) {
            debugPrintln("");
        }

        //see which rules correctly predicted these sensor values
        Vector<EpRule> effectiveRules = updateRuleConfidences();

        //New rule may be added, old rule may be removed
        updateRuleSet();

        //Reset path when goal is reached
        if (sensorData.isGoal()) {
            System.out.println("Found GOAL");
            rewardRulesForGoal();
            this.stepsSinceGoal = 0;
            buildNewPath();
        //reset path once expended with no goal
        } else if ((this.pathToDo == null)) {
            buildNewPath();
        //reached end of path without finding goal
        } else if ((this.pathToDo.size() == 0)) {
            //DEBUG
            debugPrintln("Current path failed.");

            buildNewPath();
        } //TODO: reimplement path validation (be careful since partial matching now)

        //extract next action
        char action;
        if (pathToDo != null) {
            action = this.pathToDo.get(0).getAction();

            //DEBUG
            debugPrint("Selecting next action: " + action + " from path: ");
            if (this.pathTraversedSoFar.size() > 0) {
                debugPrint(this.pathTraversedSoFar.lastElement().getPathStr());
                debugPrint(".");
            }
            for(TreeNode node : this.pathToDo) {
                debugPrint("" + node.getAction());
            }
            debugPrintln("");

            this.pathTraversedSoFar.add(this.pathToDo.remove(0));
        } else {
            //random action
            action = actionList[rand.nextInt(actionList.length)].getName().charAt(0);
            debugPrintln("random action: " + action);
        }

        //DEBUG:
        if (PhuJusAgent.DEBUGPRINTSWITCH) {
            printRules(action);
        }

        //Now that we know what action the agent will take, setup the sensor
        // values for the next iteration
        this.prevInternal.add(this.currInternal);
        while (prevInternal.size() > SHORT_TERM_SIZE) {
            prevInternal.remove(0);
        }
        this.prevExternal = this.currExternal;
        this.prevAction = action;
        if (this.rules.size() > 0) {  //can't update if no rules yet
            this.currInternal = genNextInternal(action);
        }

        //DEBUG
        debugPrintln("----------------------------------------------------------------------");

        return new Action(action + "");
    }//getNextAction

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
    public HashSet<Integer> genNextInternal(char action,
                                            HashSet<Integer> currInt,
                                            SensorData currExt) {
        HashSet<Integer> result = new HashSet<>();
        for (EpRule r : this.rules) {
            double score = r.lhsMatchScore(action, currInt, currExt);
            if (score > 0.0) {
                result.add(r.getId());
            }
        }
        return result;
    }//genNextInternal

    /**
     * convenience overload that uses the this.currInternal and this.currExternal
     */
    public HashSet<Integer> genNextInternal(char action) {
        return genNextInternal(action, this.currInternal, this.currExternal);
    }//genNextInternal

    /**
     * DEBUG
     * prints internal sensors.  Used for debugging.
     *
     * @param printMe sensors to print
     */
    private void printInternalSensors(HashSet<Integer> printMe) {
        debugPrint("Internal Sensors: ");
        int count = 0;
        for (Integer i : printMe) {
            if (count > 0) debugPrint(", ");
            debugPrint("" + i);
            count++;
        }

        if (count == 0) {
            debugPrint("none");
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
        //Sort sensor names alphabetical order with GOAL last
        Vector<String> sNames = new Vector<>(sensorData.getSensorNames());
        Collections.sort(sNames);
        int i = sNames.indexOf(SensorData.goalSensor);
        if (i > -1) {
            sNames.remove(i);
            sNames.add(SensorData.goalSensor);
        }

        //print sensor values as bit string
        System.out.print("External Sensors: ");
        for (String sName : sNames) {
            int val = ((Boolean)sensorData.getSensor(sName)) ? 1 : 0;
            System.out.print(val);
        }

        //print sensor names after
        System.out.print("  (");
        boolean comma = false;
        for (String sName : sNames) {
            if (comma) System.out.print(", ");
            comma = true;
            System.out.print(sName);
        }
        System.out.println(")");
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
            double score = r.lhsMatchScore(action);
            if ((action != '\0') && (score > 0.0)){
                debugPrint(String.format("%.3f", score));
                debugPrint(" ");
            } else {
                debugPrint("      ");
            }

            r.calculateActivation(now);  //update this so it's accurate
            debugPrintln(r.toString());
        }
    }//printRules

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
        //If the last path was completed, save it
        if ((this.pathTraversedSoFar.size() > 0)
                && (this.pathToDo != null)
                && (this.pathToDo.size() == 0)) {
            this.prevPath = new Vector<>(this.pathTraversedSoFar);
        } else {
            //if path was aborted early, also abort any pending PathRule data
            this.prevPath = null;
            this.pendingPR = null;
        }

        this.pathTraversedSoFar.clear();

        //Find a new path to the goal
        TreeNode root = new TreeNode(this);
        this.pathToDo = root.findBestGoalPath();

        //DEBUG
        if (PhuJusAgent.DEBUGPRINTSWITCH) {
            root.printTree();
            if (this.pathToDo != null) {
                debugPrintln("found path: " + this.pathToDo.lastElement().getPathStr());
            } else {
                this.pathToDo = root.findMostUncertainPath();
                if (this.pathToDo != null) {
                    debugPrintln("found uncertain path: " + this.pathToDo.lastElement().getPathStr());
                } else {
                    debugPrintln("no path found");
                }
            }
        }

        //Update the PathRule set as well
        addPathRule();

    }//buildNewPath

    /**
     * getRHSMatches
     *
     * calculates which rules have a RHS that best match the agent's sensors.
     * This, in effect, tells you which rules *should* have matched last timestep.
     *
     * TODO:  this method uses the some iffy statistics.  Allow any non-zero match instead?
     */
    public Vector<EpRule> getRHSMatches() {
        Vector<EpRule> result = new Vector<>();
        double bestScore = 0.0;
        double scoreSum = 0.0;
        double matchCount = 0.0;
        int index = 0;
        double[] scores = new double[this.rules.size()];

        //Get a RHS match score for every rule
        //  also calc the average and best
        for (EpRule r : this.rules) {
            scores[index] = r.rhsMatchScore(this.currExternal);
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

        //Decide which rules match using an iffy threshold
        double threshold = avgScore + ((bestScore - avgScore) / 2);
        index = 0;
        for(EpRule r : this.rules) {
            if (scores[index] > threshold) {
                result.add(r);
            }
            index++;
        }//for

        return result;
    }//getRHSMatches

    /**
     * updateRuleConfidences
     *
     * examines all the rules that fired last timestep and updates
     * the confidence of those that made an effective prediction.
     * The list of effective rules is returned to the caller.
     *
     * TODO:  Do we need this method?  If trees built on all matches
     *        rather than just best matches then we could validate using
     *        the tree.  OTOH, the deeper you go in the tree the less you
     *        should be penalizing rules, yes?
     */
    private Vector<EpRule> updateRuleConfidences() {
        Vector<EpRule> effectiveRules = new Vector<EpRule>();

        //Compare all the rules that matched RHS to the ones that matched LHS last timestep
        Vector<EpRule> rhsMatches = getRHSMatches();
        for(EpRule r : this.rules) {
            if (this.currInternal.contains(r.getId())) {  //did the rule match last timestep?
                r.incrMatches();
                if (rhsMatches.contains(r)) {
                    r.incrPredicts();
                    effectiveRules.add(r);
                    //effective prediction means we can adjust confidences
                    r.updateConfidencesForPrediction(getPrevInternal(), this.prevExternal, this.currExternal);
                } else {
                    r.reevaluateInternalSensors(getPrevInternal());
                }
            }
        }

        return effectiveRules;
    }//updateRuleConfidences

    /**
     * metaScorePath
     *
     * provides an opinion about a given path based on the PathRule set
     *
     * @return a double 0.0..1.0 scoring the agent's opinion
     */
    public double metaScorePath(Vector<TreeNode> path) {
        //If there is no previous path then nothing can match
        if (this.prevPath == null) return 0.5;  //neutral response

        double score = 0.0;
        int count = 0;
        for(PathRule pr : this.pathRules) {
            if (pr.matches(this.prevPath, path)) {
                if (path.lastElement().getCurrExternal().isGoal()) {
                    score++;
                }
                count++;
            }
        }

        //If no PathRules match, then be neutral
        if (count == 0) return 0.5;

        return score / (double)count;

    }//metaScorePath

    /**
     * addPathRule
     *
     * creates a new PathRule if the stars are right.
     *
     * Call this method immediately after building a new path
     */
    public void addPathRule() {
        //TODO:  limit number of path rules ala MAXNUMRULES

        //Need two completed paths to build a PathRule
        if (this.prevPath == null) return;
        if (this.prevPath.size() == 0) return;
        if (this.pathTraversedSoFar.size() != 0) return;
        if (this.pathToDo == null) return;
        if (this.pathToDo.size() == 0) return;

        //If there is a pending PathRule, complete it
        if (this.pendingPR != null) {
            this.pendingPR.setRHS(this.currExternal);
            if (! this.pathRules.contains(this.pendingPR)) {
                this.pathRules.add(this.pendingPR);

                //DEBUG
                debugPrintln("Completed PathRule: " + this.pendingPR);
            }
        }
        this.pendingPR = null;

        //If last path was completed, complete pending PathRule and save data for building a new PathRule
        SensorData firstExt = this.prevPath.firstElement().getCurrExternal();
        TreeNode firstPath = this.prevPath.lastElement();
        SensorData secondExt = this.pathToDo.firstElement().getCurrExternal();
        TreeNode secondPath = this.pathToDo.lastElement();
        this.pendingPR = new PathRule(firstExt, firstPath, secondExt, secondPath);

        //DEBUG
        debugPrintln("Pending PathRule: " + this.pendingPR);


    }//addPathRule

    /**
     * updateRuleSet
     *
     * replaces current rules with low activation with new rules that might
     * be more useful.
     *
     */
    public void updateRuleSet() {
        //Can't create a rule if there is not a previous timestep
        if (this.now == 1) return;

        //Create a candidate new rule based on agent's current state
        EpRule cand = new EpRule(this);

        //If the candidate is a GOAL rule give it an appropriate activation for that
        if ( (cand.getRHSExternal().hasSensor(SensorData.goalSensor))
            && ((Boolean)(cand.getRHSExternal().getSensor(SensorData.goalSensor))) ) {
            cand.addActivation(this.now, EpRule.FOUND_GOAL_REWARD);
        }

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
        double mergeScore = cand.compareTo(bestMatch);
        if (mergeScore > 0.75) {
            //TODO: merge would happen here if mergeScore is high enough.  Need something less arbitrary than 0.75
        }

        //Find the rule with lowest activation & accuracy
        EpRule worstRule = this.rules.get(0);
        double worstScore = worstRule.calculateActivation(this.now) * worstRule.getAccuracy();
        for (EpRule r : this.rules) {
            double activation = r.calculateActivation(this.now);
            double score = activation * r.getAccuracy();
            if (score < worstScore) {
                worstScore = score;
                worstRule = r;
            }
        }

        //remove and replace
        removeRule(worstRule);
        addRule(cand);

    }//updateRuleSet


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

        //DEBUG
        debugPrintln("added: " + newRule);
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
                r.resetMetaInfo();
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

    public Vector<EpRule> getRules() {
        return this.rules;
    }

    public int getNow() {
        return now;
    }

    public HashSet<Integer> getCurrInternal() {
        return this.currInternal;
    }

    public HashSet<Integer> getPrevInternal() {
        return this.prevInternal.lastElement();
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
