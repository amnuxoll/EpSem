package agents.phujus;

import framework.Action;
import framework.IAgent;
import framework.IIntrospector;
import framework.SensorData;

import java.util.*;
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
 * > review long methods and break into main + helper methods
 *
 * TODO research items
 * > Rules refine/generalize themselves based on experience.  Rules should be able to:
 *    - merge when they become very similar
 *    - split when it will improve the activation of both progeny
 * > Rule accurancy should be tracked using the same mechanism as confidence
 *   (frequency and recency of correctness)
 */
public class PhuJusAgent implements IAgent {
    public static final int MAXNUMRULES = 5000;
    public static final int MAX_SEARCH_DEPTH = 5; //TODO: get the search to self prune again
    public static final int MAX_TIME_DEPTH = 7;  //size of short term memory

//region InnerClasses

    /**
     * class RuleMatchProfile describes how well two EpRule objects match each other
     */
    public class RuleMatchProfile {
        public EpRule given = null;
        public EpRule match = null;
        public double shortScore = -1.0;  //LHS match score at depth of shorter rule
        public double longScore = -1.0;  //LHS match score at depth of shorter rule
        public double rhsScore = -1.0;  //RHS matchs score

        public RuleMatchProfile(EpRule initGiven) { this.given = initGiven; }
    }//class RuleMatchProfile

//endregion

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

        //DEBUG: break here to debug
        if(this.stepsSinceGoal >= 40) {
            debugPrintln("");
        }

        //see which rules correctly predicted these sensor values
        updateRuleConfidences();

        //New rule may be added, old rule may be removed
        EpRule newRule = updateRuleSet();

        //If a new rule was added, retroactively put it in currInternal
        //for use by future rules
        if (newRule != null) {
            this.currInternal.add(newRule.getId());
        }

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
        while (prevInternal.size() > MAX_TIME_DEPTH) {
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
     * At the moment, "sufficient" is any non-zero match score
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
     */
    public Vector<EpRule> getRHSMatches() {
        Vector<EpRule> result = new Vector<>();

        //Get a RHS match score for every rule
        //  also calc the average and best
        for (EpRule r : this.rules) {
            double score = r.rhsMatchScore(this.currExternal);
            if (score == 1.0) {
                result.add(r);
            }
        }

        return result;
    }//getRHSMatches

    /**
     * updateRuleConfidences
     *
     * examines all the rules that fired last timestep and updates
     * the confidence of those that made an effective prediction.
     * The list of effective rules is returned to the caller.
     *
     * Side Effect:  incorrect rules are removed from this.currInternal
     *
     */
    private void updateRuleConfidences() {
        //Compare all the rules that matched RHS to the ones that matched LHS last timestep
        Vector<EpRule> rhsMatches = getRHSMatches();
        for(EpRule r : this.rules) {
            if (this.currInternal.contains(r.getId())) {  //did the rule match last timestep?
                r.incrMatches();
                if (rhsMatches.contains(r)) {
                    r.incrPredicts();
                    //effective prediction means we can adjust confidences
                    r.updateConfidencesForPrediction(getAllPrevInternal(), this.prevExternal, this.currExternal);
                } else {
                    //try to extend the incorrect rule in hopes of increasing its future accuracy
                    int ret = r.extendTimeDepth();
                    if (ret == 0) r.decrMatches(); //retroactively un-match this rule

                    //Since the rule made an incorrect prediction, remove it from currInternal
                    //to prevent future rules from having incorrect LHS
                    this.currInternal.remove(r.getId());
                }
            }
        }

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
     * findBestMatchingRule
     *
     * finds the rule in the ruleset that most closely matches a given one.
     * The match is first scored at the timedepth at the shorter rule.
     * Ties are broken by RHS match and then long depth.
     *
     * @return a RuleMatchProfile object describing the best match
     */
    public RuleMatchProfile findBestMatchingRule(EpRule given) {
        //Find the existing rule that is most similar
        RuleMatchProfile result = new RuleMatchProfile(given);
        for (EpRule r : this.rules) {
            if (r.equals(given)) continue;  //Rule may not match itself

            int shorterDepth = Math.min(given.getTimeDepth(), r.getTimeDepth());
            double score = r.compareLHS(given, shorterDepth);

            //If they match at short depth, break the potential with with RHS
            double rhsScore = 0.0;
            if (score == 1.0) {
                rhsScore = given.rhsMatchScore(r.getRHSExternal());
            }

            //if they also match at RHS score, break the tie with long depth
            double longScore = 0.0;
            if (rhsScore == 1.0) {
                int longerDepth = Math.max(given.getTimeDepth(), r.getTimeDepth());
                longScore = r.compareLHS(given, longerDepth);
            }

            double totalScore = score + rhsScore + longScore;
            double bestTotal = result.shortScore + result.rhsScore + result.longScore;
            if ( totalScore  >= bestTotal) {
                result.match = r;
                result.shortScore = score;
                result.longScore = longScore;
                result.rhsScore = rhsScore;
            }
        }//for each rule

        return result;
    }//findBestMatchingRule

    /**
     * resolveRuleConflict
     *
     * is called when a rule has a short match on the LHS with an existing
     * rule.  The method attempts to resolve the conflict as best it can
     * so that both rules can be used.
     *
     * @param prof  a description of how well the rules match.
     *              prof.given is presumed to be the shorter rule
     * @return  null for success.  Otherwise the method results the rule to
     *          discard (conflict not resolved)
     */
    public EpRule resolveRuleConflict(RuleMatchProfile prof) {

        //sanity check:  nothing to resolve?
        if(prof.shortScore != 1.0) {
            return null;
        }

        // Special handling is needed if match is in a sisterhood
        if ( (prof.rhsScore == 1.0) && (prof.match.getSisters().size() > 0) ) {
            int ret = prof.given.adjustSisterhood(prof.match);
            //on success, given replaces match
            if (ret == 0) {
                return prof.match;
            }
        }//if sisterhood

        //Try to resolve the match
        int ret = prof.given.resolveMatchingLHS(prof.match);
        if(ret < 0) {
            if (prof.rhsScore < 1.0) {
                //If the RHS differ, then create a new sisterhood (resolved...ish)
                prof.match.addSister(prof.given);
            } else {
                //Since LHS and RHS both match, these rules are too redundant.
                //Ditch the rule with the least info
                if (prof.given.maxTimeDepth() > prof.match.maxTimeDepth()) {
                    //candidate has more to offer
                    while(prof.given.getTimeDepth() < prof.match.getTimeDepth()) {
                        prof.given.extendTimeDepth();
                    }
                    return prof.match;
                }
                else {
                    return prof.given;
                }
            }//else no resolution
        }//if resolve with expand

        return null;  //success
    }//resolveRuleConflict

    /**
     * updateRuleSet
     *
     * replaces current rules with low activation with new rules that might
     * be more useful.
     *
     * @return the new EpRule added to this.rules (or null if none added)
     */
    public EpRule updateRuleSet() {
        //Can't create a rule if there is not a previous timestep
        if (this.now == 1) return null;

        //Create a candidate new rule based on agent's current state
        EpRule cand = new EpRule(this);

        //TODO: This is handled elsewhere now?
//        //If the candidate is a GOAL rule give it an appropriate activation for that
//        if ( (cand.getRHSExternal().hasSensor(SensorData.goalSensor))
//            && ((Boolean)(cand.getRHSExternal().getSensor(SensorData.goalSensor))) ) {
//            cand.addActivation(this.now, EpRule.FOUND_GOAL_REWARD);
//        }

        //Find the extant rule most similar to cand
        RuleMatchProfile prof = findBestMatchingRule(cand);

        //DEBUG
        debugPrintln("cand: #" + cand.getId() + ": " + cand.toStringAllLHS() + cand.toStringShort());
        if (prof.match != null) debugPrintln("best: #" + prof.match.getId() + ": " + prof.match.toStringAllLHS() + prof.match.toStringShort());

        // If the LHS is an exact match, try to resolve it
        EpRule discardme = resolveRuleConflict(prof);
        if (discardme != null) {//null is success here
            if (discardme == prof.match) {
                removeRule(prof.match, prof.given);
            } else {
                return null;  //discard candidate rule
            }
        }

        //If we haven't reached max just add the candidate
        if (this.rules.size() < MAXNUMRULES) {
            addRule(cand);
            return cand;
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

        //out with the old, in with the new...was there a baby in that bath water?
        removeRule(worstRule, null);
        addRule(cand);
        return cand;

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
     * TODO:  try merging rule with most similar instead?
     *
     * CAVEAT: recursive
     *
     * @param removeMe  the rule to remove
     * @param replacement (can be null) the rule that will be replacing this one
     */
    public void removeRule(EpRule removeMe, EpRule replacement) {
        rules.remove(removeMe);

        //DEBUGGING
        if (replacement == null) debugPrint("removed: ");
        else debugPrint("replaced: ");
        debugPrintln(removeMe.toString());

        // If any rule has a condition that test for 'removeMe' then that
        // condition must also be removed or replaced
        Vector<EpRule> truncated = new Vector<>(); //stores rules that were truncated by this process
        for (EpRule r : this.rules) {
            if (r.testsIntSensor(removeMe.getId())) {
                int replId = (replacement == null) ? -1 : replacement.getId();
                int ret = r.removeIntSensor(removeMe.getId(), replId);

                //if the rule was truncated we have to resolve that
                if (ret < 0) {
                    truncated.add(r);
                }
            }
        }

        //Truncated rules are extra nasty to resolve well.  For now we're just going
        // to remove them if they conflict with other rules in the ruleset.
        // I'm sure this will come back and bite us later...
        Vector<EpRule> removeThese = new Vector<>();
        for(EpRule r : truncated) {
            RuleMatchProfile prof = findBestMatchingRule(r);
            if (prof.shortScore == 1.0) {
                removeThese.add(r);
            }
        }
        for(EpRule r : removeThese) {
            removeRule(r, null);  //recursion here
        }


        //the replacement may also have to-be-removed rule in its sensor set
        if (replacement != null) {
            if (replacement.testsIntSensor(removeMe.getId())) {
                replacement.removeIntSensor(removeMe.getId(), replacement.getId());
            }
        }

        //If the removed rule was in the internal sensor set, it has to be fixed as well
        if (this.currInternal.contains(removeMe.getId())) {
            this.currInternal.remove(removeMe.getId());
            if (replacement != null) {
                this.currInternal.add(replacement.getId());
            }
        }

        //TODO:  remove from all levels in this.prevInternal as well?

    }//removeRule



    /**
     * rewardRulesForGoal
     *
     * is called when the agent reaches a goal to reward all the rules
     * that predicted that would happen.  Rewards passed back with decay
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

    /** returns the most recent */
    public HashSet<Integer> getPrevInternal() {
        return this.prevInternal.lastElement();
    }

    /** return all prev internal in short term memory */
    public Vector<HashSet<Integer>> getAllPrevInternal() {
        return this.prevInternal;
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
