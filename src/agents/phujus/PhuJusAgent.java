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
    public class EpRuleMatchProfile {
        public EpRule given = null;
        public EpRule match = null;
        public double shortScore = -1.0;  //LHS match score at depth of shorter rule
        public double longScore = -1.0;  //LHS match score at depth of shorter rule
        public double rhsScore = -1.0;  //RHS matchs score

        public EpRuleMatchProfile(EpRule initGiven) { this.given = initGiven; }
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

    //The agent keeps lists of the rules it is using
    private Vector<BaseRule> baseRules = new Vector<>();
    private Vector<EpRule> epRules = new Vector<>();
    private Vector<PathRule> pathRules = new Vector<>();
    private Vector<Rule> rules = new Vector<>();  //all rules (size may not exceed MAXNUMRULES)

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

    //the current, partially-formed PathRule is stored here
    private PathRule pendingPR = null;

    //"random" numbers are useful sometimes (use a hardcoded seed for debugging)
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

        //DEBUG: breakpoint here to debug
        if(this.stepsSinceGoal >= 40) {
            debugPrintln("");
        }

        //New rule may be added, old rule may be removed
        Rule newRule = updateRuleSet();

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
        //If agent was unable to build a path last time step.  Try now to build a new path.
        } else if ((this.pathToDo == null)) {
            buildNewPath();
        //reached end of current path without finding goal
        } else if ((this.pathToDo.size() == 0)) {
            //DEBUG
            debugPrintln("Current path failed.");

            buildNewPath();
        }

        //TODO: reimplement path validation (be careful since partial matching now)

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
     * calculates what the internal sensors will be on for the next timestep
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
        for (Rule r : this.rules) {
            double score = r.lhsMatchScore(action, currInt, currExt);
            if (score > 0.0) {
                result.add(r.getId());
            }
        }
        return result;
    }//genNextInternal

    /** convenience overload that uses the this.currInternal and this.currExternal */
    public HashSet<Integer> genNextInternal(char action) {
        return genNextInternal(action, this.currInternal, this.currExternal);
    }//genNextInternal

    /** DEBUG: prints internal sensors */
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

    /** DEBUG: prints external sensors */
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
     * printBaseRule
     *
     * prints a given BaseRule to the console (DEBUG)
     *
     * @param action the action the agent was selected.  This can be set to '\0' if the action is not yet known
     */
    private void printBaseRule(BaseRule br, char action) {
        //Print a match score first
        double score = br.lhsMatchScore(action);
        if ((action != '\0') && (score > 0.0)) {
            debugPrint(String.format("%.3f", score));
            debugPrint(" ");
        } else {
            debugPrint("      ");
        }

        //Print an '@' in front of leaf rules
        if (!br.hasChildren()) debugPrint("@");
        else debugPrint(" ");

        br.calculateActivation(now);  //update this so it's accurate
        debugPrintln(br.toString());
    }//printBaseRule


    /**
     * printRules
     *
     * prints all the rules in a verbose ASCII format  (DEBUG)
     *
     * @param action the action the agent was selected.  This can be set to '\0' if the action is not yet known
     */
    public void printRules(char action) {
        if (this.rules.size() == 0) System.out.println("(There are no rules yet.)");

        //print all BaseRules first
        for (BaseRule br : this.baseRules) {
            printBaseRule(br, action);
        }

        //then print EpRules in order of increasing depth
        for(int depth = 0; depth <= MAX_TIME_DEPTH; ++depth) {
            boolean found = false;
            for (EpRule er : this.epRules) {
                if (er.getTimeDepth() == depth) {
                    printBaseRule(er, action);
                    found = true;
                }
            }
            if (!found) break;
        }//for

        //TODO:  add PathRules here

    }//printRules



    /** DEBUG: prints the sequence of actions discovered by a path */
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
    public Vector<Rule> getRHSMatches() {
        Vector<Rule> result = new Vector<>();

        //Get a RHS match score for every rule
        //  also calc the average and best
        for (Rule r : this.rules) {
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
        Vector<Rule> rhsMatches = getRHSMatches();
        for(Rule rule : this.rules) {
            if (this.currInternal.contains(rule.getId())) {  //did the rule match last timestep?
                //TODO:  What about parent rules that also matched?
                if (rhsMatches.contains(rule)) {
                    if(rule instanceof EpRule) {
                        //effective prediction means we can adjust confidences
                        EpRule r = (EpRule) rule;
                        r.updateConfidencesForPrediction(getAllPrevInternal(), this.prevExternal, this.currExternal);
                    } else {  //assume it's a BaseRule
                        rule.increaseConfidence();
                    }
                } else {
                    if(rule instanceof EpRule) {
                        EpRule r = (EpRule) rule;
                        //try to extend the incorrect rule in hopes of increasing its future accuracy
                        int ret = r.extendTimeDepth();

                        //extension failed, decrease accuracy measure
                        if (ret != 0)
                            r.decreaseConfidence();

                        //Since the rule made an incorrect prediction, remove it from currInternal
                        //to prevent future rules from having incorrect LHS
                        this.currInternal.remove(r.getId());
                    } else {  //BaseRule
                        //Create a new EpRule based on this BaseRule (if it doesn't already exist).
                        // TODO: This creates problems if we are already at max rules
                        BaseRule br = (BaseRule) rule;
                        EpRule newb = br.spawn();
                        addRule(newb);
                    }
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
                this.pathRules.add(pendingPR);

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
     * finds the EpRule in the ruleset that most closely matches a given one.
     * The match is first scored at the timedepth at the shorter rule.
     * Ties are broken by RHS match and then long depth.
     *
     * @return a RuleMatchProfile object describing the best match
     */
    private EpRuleMatchProfile findBestMatchingRule(EpRule given) {
        //Find the existing rule that is most similar
        EpRuleMatchProfile result = new EpRuleMatchProfile(given);
        for (Rule rule : this.rules) {
            if(rule instanceof EpRule) {
                EpRule r = (EpRule) rule;
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
                if (totalScore >= bestTotal) {
                    result.match = r;
                    result.shortScore = score;
                    result.longScore = longScore;
                    result.rhsScore = rhsScore;
                }
            }
        }//for each rule

        return result;
    }//findBestMatchingRule

    /**
     * getPredictingBaseRule
     *
     * retrieves the BaseRule that matches the agent's previous action and
     * current RHS
     *
     * @return the matching rule or null if not found
     */
    private BaseRule getPredictingBaseRule() {
        for(BaseRule br : this.baseRules) {
            if(br.lhsMatchScore(this.prevAction, getPrevInternal(), getPrevExternal()) == 1.0) {
                if(br.rhsMatchScore(this.currExternal) == 1.0) {
                    return br;
                }
            }
        }

        return null;
    }//getPredictingBaseRule

    /**
     * getMisPredictingBaseRules
     *
     * retrieves the BaseRules that matches the agent's previous action
     * but mispredicted the current RHS
     *
     * @return a Vector of mispredicting BaseRules
     */
    private Vector<BaseRule> getMisPredictingBaseRules() {
        Vector<BaseRule> result = new Vector<>();
        for(BaseRule br : this.baseRules) {
            if(br.lhsMatchScore(this.prevAction) == 1.0) {
                if(br.rhsMatchScore(this.currExternal) < 1.0) {
                    result.add(br);
                }
            }
        }

        return result;
    }//getMisPredictingBaseRules


    /**
     * resolveRuleConflict
     *
     * is called when a new EpRule has the same LHS but different RHS from an
     * existing rule and this conflict needs to be resolved.  This is done with
     * two steps:
     *  a) both rules' confidence levels are decreased
     *  b) if possible, the rules are expanded to create a child (or more
     *     distant descendant) that differentiates them
     *
     * Note:  The caller is responsible for guaranteeing the following:
     *  - that 'newb' is a new rule not yet added to the agent's ruleset
     *  - that 'newb' and 'extant' have the same time depth
     *  - that 'newb' and 'extant' having matching LHS
     *
     *  Side Effect:  new rules will be added to the agent
     *
     *  Warning:  This method is recursive.
     *
     * @param newb the new rule
     * @param extant the existing rule
     * @return 'true' if newb has been adjusted and should be added to the
     *          agent's rule set (false otherwise)
     */
    public boolean resolveRuleConflict(EpRule newb, EpRule extant) {
        //due to conflict we can't be confident in either rule
        newb.decreaseConfidence();
        extant.decreaseConfidence();

        //If they also have a RHS match, they are siblings
        double rhsMatchScore = newb.rhsMatchScore(extant.getRHSExternal());
        if (rhsMatchScore == 1.0) {
            //first, try to merge newb and extant together
            if (extant.mergeWith(newb)) return false;

            //DEBUG
            if (extant.parent == null) {
                System.out.println();
            }

            extant.parent.children.add(newb);
            return true;
        }

        //Get/create the child rule for newb.
        //Note: since newb is new it can only have one child atm
        EpRule newbChild = null;
        if (newb.hasChildren()) {
            newbChild = newb.children.get(0);
        } else {
            newbChild = newb.spawn();
            addRule(newbChild);
        }

        //Find the best matching extant child
        EpRule extantChild = null;
        if (extant.hasChildren()) {
            double bestScore = 0.0;
            for(EpRule child : extant.children) {
                double score = child.compareLHSIntLevel(newbChild, newbChild.getTimeDepth(), false);
                if (score > bestScore) {
                    bestScore = score;
                    extantChild = child;
                }
            }
        } else {
            extantChild = extant.spawn();
            addRule(extantChild);
        }


        //If the new children also match we need to recurse to resolve them
        if ((newbChild != null) && (extantChild != null)) {
            if (newbChild.compareLHS(extantChild, newbChild.getTimeDepth()) == 1.0) {
                resolveRuleConflict(newbChild, extantChild);  //Note:  this should always return 0
            }
        }

        return true;
    }//resolveRuleConflict


    /**
     * integrateNewEpRule
     *
     * given a new EpRule finds contradictory EpRules and resolves the conflict
     * in some manner if it can.
     *
     * Important:  This method assumes that newb's RHS matches the agent's
     * current RHS.
     *
     * @param newb a new rule that may be creating a conflict
     * @return true if the rule should be added to the agent's ruleset
     */
    private boolean integrateNewEpRule(EpRule newb) {

        //Search for a rule that has a perfect LHS match with newb
        Vector<EpRule> conflicts = new Vector<>();
        for(EpRule er : this.epRules) {
            if (er.equals(newb)) continue; //can't conflict with self
            if (er.getTimeDepth() != newb.getTimeDepth()) continue;
            if (er.compareLHS(newb, er.getTimeDepth()) == 1.0) {
                conflicts.add(er);
            }
        }//for
        if (conflicts.isEmpty()) return true;  //no conflict to resolve

        //Resolve the conflicts (all conflicts must be resolved to add newb)
        boolean ret = true;
        for(EpRule er : conflicts) {
            ret = resolveRuleConflict(newb, er);
            if (!ret) break;
        }

        return ret;

    }//integrateNewEpRule

    /**
     * adjustBaseRuleForConflict
     *
     * adjusts a BaseRule's confidence for a conflict and (if appropriate)
     * spawns an EpRule child that still has full confidence.
     *
     * @param br
     */
    private void adjustBaseRuleForConflict(BaseRule br) {
        br.decreaseConfidence();

        //If this rule has never been wrong before, a new, child EpRule
        // needs to be created and integreated into the rule set
        if (!br.hasChildren()) {
            //Create a new EpRule based on this BaseRule (if it doesn't already exist).
            // TODO: This creates problems if we are already at max rules
            EpRule newb = br.spawn();
            if(integrateNewEpRule(newb)) addRule(newb);
        }
    }//adjustBaseRuleForConflict


    /**
     * updateBaseRuleSet
     *
     * updates the accuracy of all BaseRules whose LHS matches the most recent action.
     * If no BaseRule exists that matches the agent's current experience,
     * a new BaseRule is created and added to the baseRules list
     *
     * @return the (possibly new) BaseRule that matches
     */
    public Rule updateBaseRuleSet() {
        BaseRule match = getPredictingBaseRule();
        Vector<BaseRule> mispredicts = getMisPredictingBaseRules();  //TODO what about mis-predicting EpRules?  We should get those too.
        for(BaseRule br : mispredicts) {
            adjustBaseRuleForConflict(br);
        }

        //If no match was found, create a new BaseRule that matches
        if(match == null) {
            match = new BaseRule(this);
            addRule(match);

            //If there were mispredicts then this new rule effectively conflicts
            // with those BaseRules so it needs an initial adjustment
            if (!mispredicts.isEmpty()) {
                adjustBaseRuleForConflict(match);
            }
        }//if

        //TODO:  If match is not null then we have an instance of a BaseRule
        //       being correct more than once.  There is likely a different
        //       sequence of internal sensors in this current experience
        //       compared to the one that created the BaseRule initially.
        //       That experience could (should?) be recorded here in some way.
        //       For example, the internal sensor values could be merged in.
        //       But what about external sensor values?  Those don't fit.
        //       A better solution would be for a BaseRule to keep a set of
        //       proto-children EpRules ready to add to the ruleset if a
        //       conflict arises.  How many of these to keep?  Hrm, hyper-parameter?
        //       For now I'm ignoring the issue and letting the knowledge be lost.

        //See if we can find a better matching EpRule
        while(match.getAccuracy() < 1.0) {
            //give credit for the correct prediction
            match.increaseConfidence();

            //no children means no chance of a better match
            if (!match.hasChildren()) break;

            EpRule matchingChild = null;
            for(EpRule er : match.children) {
                double score = er.lhsMatchScore(this.prevAction, getPrevInternal(), getPrevExternal());
                if (score == 1.0) {
                    matchingChild = er;
                    break;
                }
            }

            //In some cases none of the rule's children will match
            if (matchingChild == null) {
                //Make a new child to reflect the current experience
                EpRule newChild = null;
                if (match instanceof EpRule) {
                    int depth = ((EpRule)match).getTimeDepth() + 1;
                    newChild = new EpRule(this, depth);
                } else {  //BaseRule
                    newChild = new EpRule(this);
                }
                newChild.parent = match;  //who's your daddy?
                if (integrateNewEpRule(newChild)) {
                    addRule(newChild);
                    match.children.add(newChild);
                }
                break;
            } else {
                match = matchingChild;  //set match for next iteration
            }

        }//while

        return match;
    }//updateBaseRuleSet




    /**
     * updateRuleSet
     *
     * replaces current rules with low activation with new rules that might
     * be more useful.
     *
     * @return the new EpRule added to this.rules (or null if none added)
     */
    public Rule updateRuleSet() {
        //Can't create a rule if there is not a previous timestep
        if (this.now == 1) return null;

        //Find (or create) the rule that matches the current situation
        //Note:  in some cases multiple rules get created by this call
        Rule match = updateBaseRuleSet();

        //See if we need to cull rule(s) to stay below max
        if (this.rules.size() <= MAXNUMRULES) return match;

        //Find the rule with lowest activation & accuracy
        //TODO:  this needs to look at BaseRules too
        //TODO:  score of a node should be its own score or child's whichever is higher!
        EpRule worstRule = (EpRule) this.rules.get(0);
        double worstScore = worstRule.calculateActivation(this.now) * worstRule.getAccuracy();
        for (Rule rule : this.rules) {
            if(rule instanceof EpRule) {
                EpRule r = (EpRule) rule;
                double activation = r.calculateActivation(this.now);
                double score = activation * r.getAccuracy();
                if (score < worstScore) {
                    worstScore = score;
                    worstRule = r;
                }
            }
        }

        //out with the old, in with the new...was there a baby in that bath water?
        //TODO:  the removeRule method needs to change
        removeRule(worstRule, null);
        return match;

    }//updateRuleSet


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
            System.err.println("ERROR: Exceeded MAXNUMRULES!");
        }
        rules.add(newRule);
        //TODO add PathRule support here
//        if (newRule instanceof PathRule) {
//            this.pathRules.add((PathRule)newRule);
//        } else
        if (newRule instanceof EpRule) {
            this.epRules.add((EpRule)newRule);
        } else if (newRule instanceof BaseRule) {
            this.baseRules.add((BaseRule)newRule);
        }


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
        for (Rule rule : this.rules) {
            if(rule instanceof EpRule) {
                EpRule r = (EpRule) rule;
                if (r.testsIntSensor(removeMe.getId())) {
                    int replId = (replacement == null) ? -1 : replacement.getId();
                    int ret = r.removeIntSensor(removeMe.getId(), replId);

                    //if the rule was truncated we have to resolve that
                    if (ret < 0) {
                        truncated.add(r);
                    }
                }
            }
        }

        //Truncated rules are extra nasty to resolve well.  For now we're just going
        // to remove them if they conflict with other rules in the ruleset.
        // I'm sure this will come back and bite us later...
        Vector<EpRule> removeThese = new Vector<>();
        for(EpRule r : truncated) {
            EpRuleMatchProfile prof = findBestMatchingRule(r);
            if (prof.shortScore == 1.0) {
                removeThese.add(r);
            }
        }
        for(EpRule r : removeThese) {
            removeRule(r, null);  //recursion here
        }


        //the replacement may also have to-be-removed rule in its sensor set
        if (replacement != null) {
            if(replacement instanceof EpRule) {
                if (replacement.testsIntSensor(removeMe.getId())) {
                    replacement.removeIntSensor(removeMe.getId(), replacement.getId());
                }
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
            BaseRule br = node.getRule();
            if (br != null) br.addActivation(time, reward);
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

    public Vector<Rule> getRules() { return this.rules; }
    public Vector<BaseRule> getBaseRules() { return this.baseRules; }
    public Vector<EpRule> getEpRules() { return this.epRules; }
    public int getNow() { return now; }
    public HashSet<Integer> getCurrInternal() { return this.currInternal; }
    /** returns the most recent */
    public HashSet<Integer> getPrevInternal() { return this.prevInternal.lastElement(); }
    /** return all prev internal in short term memory */
    public Vector<HashSet<Integer>> getAllPrevInternal() { return this.prevInternal; }
    public SensorData getCurrExternal() { return this.currExternal; }
    public void setCurrExternal(SensorData curExtern) { this.currExternal = curExtern; }
    public SensorData getPrevExternal() { return this.prevExternal; }
    public Action[] getActionList() { return actionList; }
    public char getPrevAction() { return prevAction; }

    //endregion

}//class PhuJusAgent
