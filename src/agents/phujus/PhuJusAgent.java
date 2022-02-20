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
 * > "not" sensors don't seem to be in use anymore.  Turn them back on and use them
 *   when a rule can't be time depth extended (i.e., what internal sensors were on when
 *   I misfired that aren't on my LHS as non-"not" sensors?)
 * > "sister" rules aren't being used anymore. I don't think they are needed. Investigate and,
 *   if confirmed, remove the relevant code.
 * > PathRules need to be reviewed.  I think they are still being created but not
 *   being used.  I'm not sure they are correct anymore either.
 * > Rules refine/generalize themselves based on experience.  Rules should be able to:
 *    - merge when they become very similar
 *    - split when it will improve the activation of both progeny
 * > Rule accurancy should be tracked using the same mechanism as confidence
 *   (frequency and recency of correctness)
 */
public class PhuJusAgent implements IAgent {
    public static final int MAXNUMRULES = 20;
    public static final int MAX_SEARCH_DEPTH = 5; //TODO: get the search to self prune again
    public static final int MAX_TIME_DEPTH = 7;  //size of short term memory



//region InnerClasses

    /**
     * class EpRuleMatchProfile describes how well two EpRule objects match each other
     */
    public class EpRuleMatchProfile {
        public EpRule given = null;
        public EpRule match = null;
        public double shortScore = -1.0;  //LHS match score at depth of shorter rule
        public double longScore = -1.0;  //LHS match score at depth of shorter rule
        public double rhsScore = -1.0;  //RHS matchs score

        public EpRuleMatchProfile(EpRule initGiven) { this.given = initGiven; }
    }//class EpRuleMatchProfile

    /**
     * class Tuple describes a Tuple object for storing three values of different type
     * modified from the following sources:
     * https://commons.apache.org/proper/commons-lang/apidocs/org/apache/commons/lang3/tuple/Triple.html
     */
    public class Tuple<F, S> {
        private F first;
        private S second;

        public Tuple(F first, S second) {
            this.first = first;
            this.second = second;
        }

        public F getFirst() { return first; }
        public S getSecond() { return second; }
        public void setFirst(F newFirst) { this.first = newFirst; }
        public void setSecond(S newSecond) { this.second = newSecond; }
    }//class Triple

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
    private Vector<TFRule> tfRules = new Vector<>();
    private Hashtable<Integer, Rule> rules = new Hashtable<>();
    //private Vector<Rule> rules = new Vector<>();  //all rules (size may not exceed MAXNUMRULES)

    private int now = 0; //current timestep 't'

    private Action[] actionList;  //list of available actions in current FSM

    //internal sensors are rule ids of rules that fired in the prev time step
    private HashSet<Integer> currInternal = new HashSet<>();

    //external sensors come from the environment
    private SensorData currExternal;

    //sensor IDs of all of the used internal sensors
    private HashSet<Integer> totalInternal;

    //sensor values from the previous timesteps
    private Vector<HashSet<Integer>> prevInternal = new Vector<>();
    private SensorData prevExternal = null;
    private char prevAction = '\0';


    //The agent's current selected path to goal (a sequence of nodes in the search tree)
    private Vector<TreeNode> pathToDo = null;
    private final Vector<TreeNode> pathTraversedSoFar = new Vector<>();
    private Vector<TreeNode> prevPath = null;

    //Counter that tracks time steps since hitting a goal (DEBUG)
    private int stepsSinceGoal = 0;

    //the current, partially-formed PathRule is stored here
    private PathRule pendingPR = null;

    //"random" numbers are useful sometimes (use a hardcoded seed for debugging)
    public static Random rand = new Random(2);

    //Stores the percentage that each sensor is on and relative info to calculate it.
    //HashMap maps sensor name (String) to Tuple containing creation time step (Integer) and percentage (Double)
    private HashMap<String, Tuple<Integer, Double>> externalPercents = new HashMap<>();
    private HashMap<String, Tuple<Integer, Double>> internalPercents = new HashMap<>();

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
        this.rules = new Hashtable<>();
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

        //rules may be added, rules may be removed, rule confidences are adjusted
        updateRuleSet();

        //DEBUG:  Tell the human what time it is
        if (PhuJusAgent.DEBUGPRINTSWITCH) {
            debugPrintln("TIME STEP: " + this.now);
            printInternalSensors(this.currInternal);
            printExternalSensors(this.currExternal);

        }

        //DEBUG: breakpoint here to debug
        if(this.stepsSinceGoal >= 40) {
            debugPrintln("");
        }
        if (this.now == 8) {
            debugPrintln("");
        }

        if(this.rules.values().size() > 25){
            debugPrintln("");
        }



        //Calculate percentage that each sensor is on
        updateInternalPercents();
        updateExternalPercents();

        printInternalPercents();
        printExternalPercents();


        System.out.println("TF Rules:");
        for(int i = 0; i < tfRules.size(); i++){
            System.out.println(tfRules.get(i));
        }

        //if(this.now > 1)
        //    System.out.println("Best Match TF Rule: " + gethighestTFRule().ruleId);
//
        //Update the agent's current path
        pathMaintenance(sensorData.isGoal());


        //extract next action
        char action = calcAction();



        //Now that we know what action the agent will take, setup the sensor
        // values for the next iteration
        updateSensors(action);




        //DEBUG
        debugPrintln("----------------------------------------------------------------------");

        return new Action(action + "");
    }//getNextAction

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

        ////// *********************************************
        // THIS IF STATEMENT NEEDS TO GO BUT IT DOESN'T WORK WITHOUT IT
        ////// *********************************************
        if(this.now > 2)
            ///// ABOVE MAKES PROGRAM WORK. DON'T REMOVE UNLESS NECESARRY
            this.pathToDo = root.findBestGoalPath();

        //DEBUG
        if (PhuJusAgent.DEBUGPRINTSWITCH) {
            root.printTree();
        }

        if (this.pathToDo == null) {
            debugPrintln("no path found");

            this.pathToDo = root.findMostUncertainPath();

            //DEBUG
            if (this.pathToDo != null) {
                debugPrintln("using non-goal path with greatest uncertainty: " + this.pathToDo.lastElement().getPathStr());
            }
        }

        //DEBUG
        else {
            debugPrintln("found path: " + this.pathToDo.lastElement().getPathStr());
        }


        //Update the PathRule set as well
        addPathRule();

    }//buildNewPath

    /**
     * pathMaintenance
     *
     * updates the agent's current path information, particularly the pathToDo
     * and prevPath instance variables
     *
     * @param isGoal  did the agent just reach a goal?
     */
    private void pathMaintenance(boolean isGoal) {
        if (isGoal) {
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

        //TODO: path validation?
        //If the agent is partway through a path but the predicted outcome
        //of it's most recent step has already not been met, then it may
        //make sense to abort the path now and build a new one.  We've tried
        //this before and the downside to this is that the agent misses out
        //on some nifty exploratory options and is more vulnerable to getting
        //into loops.  If we can avoid that issue, then this is  the right
        // thing to do.

    }//pathMaintenance

    /**
     * calcAction
     *
     * determines what action the agent will take next based upon the current path
     */
    private char calcAction() {
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
        return action;
    }//calcAction

    /**
     * calcPrunedInternal
     *
     * There are cases where the agent's current internal sensors should not
     * be included in new rules:
     * 1.  When two sensors correspond to a rule and its ancestor only the
     *     most specific one should be included.  Example:
     *        # 1:    b -> 00  <-- omit this
     *        #24: |00b -> 00
     * 2. When the rule associated with the sensor failed to predict the
     *    agent's subsequent sensing
     *
     *  This method prunes such issues from a current set of internal
     *  sensors. This is called on agent.currInternal before it is added
     *  to the this.prevInternal set
     *  
     *  This is a helper method for {@link #updateSensors(char)}.
     */
    private HashSet<Integer> calcPrunedInternal() {
        HashSet<Integer> result = new HashSet<>();

        for(int i : this.currInternal) {
            //ok to type cast since all internal sensors are TFRules
            TFRule cand = (TFRule) getRuleById(i);
            if (cand == null) continue;

            /*//Check for ancestors
            for(int j : this.currInternal) {
                if (i == j) continue;
                BaseRule r = (BaseRule)getRuleById(j);
                if (cand.isAncestor(r)) {
                    cand = null;
                    break;
                }
            }*/
            if (cand == null) continue;  //ancestor found

            //Check for mis-predicts
            double score = cand.rhsMatchScore(this.currExternal);
            if (score == 1.0) {
                result.add(i);
            }

        }//for each internal sensor

        return result;
    }//calcPrunedInternal

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
        for (Rule r : this.rules.values()) {
            double score = r.lhsMatchScore(action, currInt, currExt);
            if (score > 0.0) {
                result.add(r.getId());
            }
        }
        return result;
    }//genNextInternal

    public HashSet<Integer> genNextInternalTF(char action,
                                              HashSet<Integer> currInt,
                                              SensorData currExt) {
        HashSet<Integer> result = new HashSet<>();

        for (TFRule r : this.tfRules) {
            double score = r.lhsMatchScore(action, currInt, currExt);
            if (score > 0.0){
                result.add(r.getId());
            }
            //if (r.isMatch()) {
            //    result.add(r.getId());
            //}
        }
        return result;
    }

    /** convenience overload that uses the this.currInternal and this.currExternal */
    public HashSet<Integer> genNextInternal(char action) {
        return genNextInternal(action, this.currInternal, this.currExternal);
    }//genNextInternal

    /**
     * updateSensors
     *
     * calculates the correct values for the agent's next internal sensors and
     * logs the current sensor values for use in rule creation
     *
     * @param action  the action that the agent selected for this timestep
     */
    private void updateSensors(char action) {
        this.prevInternal.add(calcPrunedInternal());
        while (prevInternal.size() > MAX_TIME_DEPTH) {
            prevInternal.remove(0);
        }
        this.prevExternal = this.currExternal;
        this.prevAction = action;
        if (this.rules.size() > 0) {  //can't update if no rules yet
            this.currInternal = genNextInternal(action);
        }
    }//updateSensors

    /**
     * updateExternalPercents
     *
     * Updates our external sensor percentage data with the latest activations of the external rules
     */
    private void updateExternalPercents() {
        //handle the external sensors
        if (this.currExternal != null) { //we have external sensor data to track

            //Go through each sensor name and check if the sensor is in the vector
            //if it is, then we calculate the new sensor percent - update the tuple
            //if it isn't then we want to calculate and add the tuple to the vector

            String[] nameArr = this.currExternal.getSensorNames().toArray(new String[0]);

            for(int i = 0; i < nameArr.length; i++){
                boolean sensorVal = (Boolean) this.currExternal.getSensor(nameArr[i]);
                Tuple<Integer, Double> sensorTuple = this.externalPercents.get(nameArr[i]);

                if(sensorTuple != null){ //We need to adjust the old percent value
                    double newPercent = calculateSensorPercent(sensorTuple.first, sensorTuple.second, sensorVal);
                    sensorTuple.setSecond(newPercent);
                }
                else { //We just set the percent to either 1.0 or 0 and set the current time
                    double percentage = sensorVal ? 1.0 : 0.0;
                    externalPercents.put(nameArr[i], new Tuple<Integer, Double>(this.now, percentage));
                }
            }
        }
    }//updateExternalPercents

    /**
     * updateInternalPercents
     *
     * Updates our internal sensor percentage data with the latest activations of the internal rules
     */
    private void updateInternalPercents() {
        if(this.currInternal != null){ //we have internal sensor data to track

            for(Rule rule: rules.values()){
                boolean wasActive = this.currInternal.contains(rule.ruleId);

                Tuple<Integer, Double> sensorTuple = this.internalPercents.get(Integer.toString(rule.ruleId));

                if (sensorTuple != null) { //We need to adjust the old percent value
                    double newPercent = calculateSensorPercent(sensorTuple.first, sensorTuple.second, wasActive);
                    sensorTuple.setSecond(newPercent);
                } else { //We just set the percent to either 1.0 or 0 and set the current time
                    double percentage = wasActive ? 1.0 : 0.0;
                    internalPercents.put(Integer.toString(rule.ruleId), new Tuple<Integer, Double>(this.now, percentage));
                }
            }

        }
    }//updateInternalPercents

    /**
     * removeInternalSensorPercent
     *
     * Removes the given internal sensor from our hashmap of internal sensor Percentages
     *
     * @param ruleNumber the rule number to be removed
     */
    private void removeInternalSensorPercent(int ruleNumber){

        this.internalPercents.remove(Integer.toString(ruleNumber));
        System.out.println("removed Internal Sensor number: "+ ruleNumber);
    }//removeInternalSensorPercent

    /**
     * calculateSensorPercent
     *
     * Calculates the percentage a given sensor is on based on the lifetime of the sensor
     * and the current percentage.
     *
     * @param creationTimeStep the time step the sensor was created
     * @param currPercent the percentage the sensor has been on
     * @param on whether the sensor is on
     * @return the newly calculated percentage
     */
    private double calculateSensorPercent(int creationTimeStep, double currPercent, boolean on) {
        double newPercentage = 0;

        //Number of instances the sensor is on
        double onInstances = currPercent * (this.now - creationTimeStep);

        int timeDiff = this.now - creationTimeStep + 1;

        //Calculates the new percentage, accounting for the next time step
        if (on) {
            newPercentage = (onInstances + 1) / timeDiff;
        } else {
            newPercentage = onInstances / timeDiff;
        }

        return newPercentage;
    }//calculateSensorPercent

    /** @return a rule with a given id (or null if not found) */
    public Rule getRuleById(int id) {

        return this.rules.get(id);
        /*int max = this.rules.size() - 1;
        int min = 1;

        //check for unfindable id
        if ( (id < 1) || (max < 0) ) return null;

        //Adjust index if needed
        int index = id - 1;  //This is often a good first guess
        if ((index < min) || (index > max)) {
            index = (min + max) / 2;
        }


        //binary search
        Rule result = null;
        while(min <= max) {
            result = this.rules.get(index);
            int guessId = result.getId();
            if (guessId == id) {
                break;  //rule found
            }
            else if (guessId < id) {
                min = index + 1;
            } else {
                max = index - 1;
            }

            //next check at the midpoint between min:max
            index = (min + max) / 2;
        }//while

        return result;*/
    }//getRuleById

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
     * getPredictingBaseRule
     *
     * retrieves the BaseRule that matches the agent's previous action and
     * current RHS.  If no such rule exists, one is created.
     *
     * SIDE EFFECT:  if a new base rule is created it is added to currInternal
     *
     * @return the matching rule or null if not found
     */
    private BaseRule getPredictingBaseRule() {
        BaseRule brMatch = null;
        for(BaseRule br : this.baseRules) {
            if(br.lhsMatchScore(this.prevAction, getPrevInternal(), getPrevExternal()) == 1.0) {
                if(br.rhsMatchScore(this.currExternal) == 1.0) {
                    brMatch = br;
                    break;
                }
            }
        }

        //If no brMatch was found, create a new BaseRule that matches
        if(brMatch == null) {
            brMatch = new BaseRule(this);
            addRule(brMatch);

            //Retroactively put this in currInternal for use by future rules
            //TODO:  I hate this I'm doing this here as as side effect but I don't see an alternative
            this.currInternal.add(brMatch.getId());
        }

        return brMatch;
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
     *
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
            if (newbChild == null) {
                return false;
            }
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

            //Is this a valid child?
            if (extantChild != null) {
                addRule(extantChild);
            }
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
     * findBestMatchingDescendent
     *
     * given a BaseRule whose LHS matched in the previous timestep, this method
     * searches for the EpRule descendent whose conditions matched best.
     * In particular, it will find the matching descendent with the most
     * time depth.
     *
     * Warning:  This method is recursive
     *
     * @return the matching child (or null)
     */
    private EpRule findBestMatchingDescendent(BaseRule br) {
        if(br == null) return null;
        EpRule matchingChild = null;
        for(EpRule er : br.children) {
            double score = er.lhsMatchScore(this.prevAction, getPrevInternal(), getPrevExternal());
            if (score == 1.0) {
                matchingChild = er;
                break;
            }
        }

        //See if there is an even better match
        if (matchingChild != null) {
            EpRule descendent = findBestMatchingDescendent(matchingChild);
            if (descendent != null) {
                matchingChild = descendent;
            }
        }

        return matchingChild;
    }//findBestMatchingDescendent

    /**
     * adjustBaseRuleForConflict
     *
     * adjusts a BaseRule's confidence for a conflict and updates
     * the BaseRule's children for same.  This may require:
     *   a.  creating a new EpRule that has 100% confidence
     *   b.  discovering existing EpRules that also were wrong
     *       and need to be adjusted
     *
     * Warning:  This method is recursive
     *
     * @param br
     */
    private void adjustBaseRuleForConflict(BaseRule br) {
        br.decreaseConfidence();

        //If this rule has never been wrong before, a new, child EpRule
        // needs to be created and integreated into the rule set
        if (! br.hasChildren()) {
            //Create a new EpRule based on this BaseRule (if it doesn't already exist).
            EpRule newb = br.spawn();
            if ( (newb != null) && (integrateNewEpRule(newb)) ) {

                addRule(newb);
            }

            //Since there are no children, no more mispredicting EpRules can be found
            return;
        }//if no children

        //see if any child rules are mispredictors
        for(EpRule child : br.children) {
            double score = child.lhsMatchScore(this.prevAction, getPrevInternal(), getPrevExternal());
            if (score == 1.0) {
                adjustBaseRuleForConflict(child);  //RECURSE
            }
        }

    }//adjustBaseRuleForConflict


    /**
     * updateEpAndBaseRuleSet
     *
     * updates the accuracy of all BaseRules and EpRules whose LHS matches
     * the most recent action.
     *
     * Side Effect:  new rules may be created
     */
    public void updateEpAndBaseRuleSet() {
        //reward the matching BaseRule
        BaseRule brMatch = getPredictingBaseRule();
        brMatch.increaseConfidence();

        //reward the matching EpRule(s)
        EpRule erMatch = findBestMatchingDescendent(brMatch);
        if (erMatch != null) {
            while (true) {
                erMatch.updateConfidencesForPrediction(this.prevInternal, this.prevExternal, this.currExternal);
                if (erMatch.parent instanceof EpRule) {
                    erMatch = (EpRule)erMatch.parent;
                } else {
                    break;
                }
            }
        }

        //TODO:  Each time a rule matches, there is likely a different
        //       sequence of internal sensors in this current experience
        //       compared to the one that created the rule initially.
        //       That experience should be recorded here in some way.
        //       For example, the internal sensor values could be merged in
        //       some way.


        //Adjust the mismatching BaseRules (this also adjusts their children)
        Vector<BaseRule> mispredicts = getMisPredictingBaseRules();
        for(BaseRule br : mispredicts) {
            adjustBaseRuleForConflict(br);
        }

    }//updateEpAndBaseRuleSet

    /**
     * cullRules
     *
     * if MAXNUMRULES has been reached, this method culls the set down
     * by removing the worst rules.
     *
     * Note: this method only removes EpRules.  MAXNUMRULES should never be
     * set so small that BaseRules need to be removed.
     */
    private void cullRules() {
        if (this.rules.size() <= MAXNUMRULES) return;

        TFRule worstRule = (TFRule) this.rules.elements().nextElement();
        double worstScore = worstRule.calculateActivation(this.now) * worstRule.getAccuracy();
        for (Rule rule : this.rules.values()) {
            if(rule instanceof TFRule) {
                TFRule r = (TFRule) rule;
                double activation = r.calculateActivation(this.now);
                double score = activation * r.getAccuracy();
                if (score < worstScore) {
                    worstScore = score;
                    worstRule = r;
                }
            }
        }

        /*//Find the rule with lowest activation & accuracy
        //TODO:  score of a node should be its own score or child's whichever is higher!
        //       A good way to do this is to override calculateActivation in
        //       BaseRule and have it recurse through children.  It's expensive but
        //       calculating activation is already expensive anyway.  We can memoize
        //       it later if needed.  -:AMN: Dec 2021
        EpRule worstRule = (EpRule) this.rules.get(0);
        double worstScore = worstRule.calculateActivation(this.now) * worstRule.getAccuracy();
        for (Rule rule : this.rules.values()) {
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
*/
        //out with the old, in with the new...was there a baby in that bath water?
        //TODO:  the removeRule method needs to change
        removeRule(worstRule, null);
    }


    /**
     * updateRuleSet
     *
     * replaces current rules with low activation with new rules that might
     * be more useful.
     *
     * @return the new EpRule added to this.rules (or null if none added)
     */
    public void updateRuleSet() {
        //Can't do anything in the first timestep because there is no previous timestep
        if (this.now == 1) return;

        //Find (or create) the rule that matches the current situation
        //Note:  in some cases multiple rules get created by this call
        /*updateEpAndBaseRuleSet();
*/
        //Update the TF values for all extant TFRules
        for(TFRule tf : this.tfRules) {
            if (tf.isMatch()) {
                tf.updateTFVals();
            }
        }

        //Create a new TF Rule for this latest experience
        addRule(new TFRule(this));

        //See if we need to cull rule(s) to stay below max
        cullRules();

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


        rules.put(newRule.ruleId,newRule);
        //rules.add(nInsert, newRule);


        //TODO add PathRule support here
//        if (newRule instanceof PathRule) {
//            this.pathRules.add((PathRule)newRule);
//        } else
        if (newRule instanceof EpRule) {
            this.epRules.add((EpRule)newRule);
        } else if (newRule instanceof BaseRule) {
            this.baseRules.add((BaseRule)newRule);
        } else if(newRule instanceof TFRule) {
            this.tfRules.add((TFRule)newRule);
        }


        //DEBUG
        debugPrintln("added: " + newRule);
    }

    /**
     * findMostSimilarRule
     *
     * finds the EpRule in the ruleset that most closely matches a given one.
     * The match is first scored at the timedepth of the shorter rule.
     * Ties are broken by RHS match and then long depth.
     *
     * @return a RuleMatchProfile object describing the best match
     */
    private EpRuleMatchProfile findMostSimilarRule(EpRule given) {
        //Find the existing rule that is most similar
        EpRuleMatchProfile result = new EpRuleMatchProfile(given);
        for (Rule rule : this.rules.values()) {
            if(rule instanceof EpRule) {
                EpRule r = (EpRule) rule;
                if (r.getId() == given.getId()) continue;  //Rule may not match itself

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
    }//findMostSimilarRule

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
    public void removeRule(TFRule removeMe, TFRule replacement) {
        boolean removed = rules.remove(removeMe.ruleId ,removeMe);
        //rules.remove
        tfRules.remove(removeMe);
        //tfRules.removeElement(removeMe);


        //DEBUGGING
        if (replacement == null) debugPrint("removed: ");
        else debugPrint("replaced: ");
        debugPrintln(removeMe.toString());

        //Removes the data from the sensor percentage HashMap
        removeInternalSensorPercent(removeMe.getId());

        // If any rule has a condition that test for 'removeMe' then that
        // condition must also be removed or replaced
        Vector<TFRule> truncated = new Vector<>(); //stores rules that were truncated by this process
        for (Rule rule : this.rules.values()) {
            if(rule instanceof TFRule) {
                TFRule r = (TFRule) rule;
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
       /* // I'm sure this will come back and bite us later...
        Vector<TFRule> removeThese = new Vector<>();
        for(TFRule r : truncated) {
            EpRuleMatchProfile prof = findMostSimilarRule(r);
            if (prof.shortScore == 1.0) {
                removeThese.add(r);
            }
        }
        for(TFRule r : removeThese) {
            removeRule(r, null);  //recursion here
        }
*/

        /*//the replacement may also have to-be-removed rule in its sensor set
        if (replacement != null) {
            if(replacement instanceof TFRule) {
                if (replacement.testsIntSensor(removeMe.getId())) {
                    replacement.removeIntSensor(removeMe.getId(), replacement.getId());
                }
            }
        }
*/
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

    //region Debug Printing Methods

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

    /** DEBUG: prints internal sensor percentages */
    public void printInternalPercents(){
        String[] names = internalPercents.keySet().toArray(new String[0]);

        System.out.print("Internal sensor activation percentages:");
        if (names.length != 0) {
            System.out.print("\n");
            for (String name : names) {
                System.out.println("\t" + name + ":\t\t" + this.internalPercents.get(name).getSecond());
            }
        } else {
            System.out.println(" none");
        }
    }//printExternalPercents

    /** DEBUG: prints external sensor percentages */
    public void printExternalPercents() {
        String[] names = externalPercents.keySet().toArray(new String[0]);

        System.out.print("External sensor activation percentages:");
        if (names.length != 0) {
            System.out.print("\n");
            for (String name : names) {
                System.out.println("\t" + name + ":\t\t" + this.externalPercents.get(name).getSecond());
            }
        } else {
            System.out.println(" none");
        }
    }//printExternalPercents

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

    //endregion

    //region Getters and Setters

    public Hashtable<Integer, Rule> getRules() { return this.rules; }
    public Vector<BaseRule> getBaseRules() { return this.baseRules; }
    public Vector<EpRule> getEpRules() { return this.epRules; }
    public Vector<TFRule> getTfRules() { return tfRules; }
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
    public HashMap<String, Tuple<Integer, Double>> getInternalPercents() {return this.internalPercents;}
    public HashMap<String, Tuple<Integer, Double>> getExternalPercents() {return this.externalPercents;}

    //endregion

}//class PhuJusAgent
