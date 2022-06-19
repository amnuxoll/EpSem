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
    public static final int MAXNUMRULES = 400; //10 to 100 for testing
    public static final int MAX_SEARCH_DEPTH = 5; //TODO: get the search to self prune again
    public static final int MAX_TIME_DEPTH = 7;  //size of short term memory



//region InnerClasses

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

    /**
     * class RuleMatrix describes an adjacency matrix for storing pairs of internal sensors which fire simultaneously.
     * TODO Look at other data structures for this (the current implementation could be very expensive)
     */
    public class RuleMatrix {

        private int[][] adjMatrix; // Keeps track of rules when they fire together
        private int[][] disjMatrix; // Keeps track of times when rules that usually fire together don't
                                    // Do we care when they fire, or how many times they don't fire? (For both)

        public RuleMatrix(int size) {
            this.adjMatrix = new int[size][size];
            this.disjMatrix = new int[size][size];
        }

        public void updateConnections(HashSet<Integer> sensor) {

            for (Integer i : sensor) {
                for (TFRule r : tfRules) {
                    if (r.getId() == i) continue;
                    // if r is in sensor, it fired so we increment adj Mat
                    if (sensor.contains(r.getId())) {
                        adjMatrix[i][r.getId()]++;
                    }
                    else {
                        // otherwise we increment disjMat
                        disjMatrix[i][r.getId()]++;
                        disjMatrix[r.getId()][i]++;
                    }
                }

            }
        }

        public int getConnections(int rule1, int rule2) {
            return adjMatrix[rule1][rule2];
        }

        /**
         * getRulesThatFireTogether
         *
         * Returns a list of all the rules which have fired with this rule 'connections' # of times
         * TODO This matrix could break once the rules start exceeding the maximum or when rules start to merge, since
         *      a rule's index in the matrix is based on its rule ID.
         *      A different implementation of this concept could fix this issue.
         *
         * @param ruleNum ID of the rule
         * @param connections number of times a rule has to have fired with this one
         * @return
         */
        public Vector<TFRule> getSimilarRules(int ruleNum, int connections) {

            Vector<TFRule> broRules = new Vector<>();
            TFRule rule = (TFRule) rules.get(ruleNum);

            // We go through all of the rules which have fired with this rule. If the number of times that these
            // two rules have fired is greater than 'connections', then it is a potential candidate to be merged.
            for (int i = 0; i < tfRules.get(tfRules.size()-1).ruleId + 1; i++) {

                if (rules.containsKey(i)) {
                    if (adjMatrix[ruleNum][i] >= connections) {

                        if (i == ruleNum) {
                            continue;
                        }

                        TFRule broRule = (TFRule) rules.get(i); // <- Merge candidate

                        // Checking that the candidate has the same external sensors, action, and similar confidences
                        // before confirming its selection
                        if (broRule.isExtMatch(rule.getAction(), rule.getLHSExternal(), rule.getRHSExternal())) {
                            if (Math.abs(rule.getConfidence() - broRule.getConfidence()) <= 0.02) {
                                broRules.add((TFRule) rules.get(i));
                            }
                        }
                    }
                }
            }

            return broRules;
        }

        public int[][] getAdjMatrix() {
            return adjMatrix;
        }
    }//class RuleMatrix

//endregion

    // DEBUG variable to toggle println statements (on/off = true/false)
    public static final boolean DEBUGPRINTSWITCH = true;

    // FLAG variable to toggle updating of TFIDF values
    public static final boolean TFIDF = true;

    // FLAG variabale to toggle rule generation
    // if this is false, addPredeterminedRules will be called at
    // timestep 0
    public static final boolean GENERATERULES = true;

    //The agent keeps lists of the rules it is using
    private Vector<PathRule> pathRules = new Vector<>();
    private Vector<TFRule> tfRules = new Vector<>();
    private Hashtable<Integer, Rule> rules = new Hashtable<>();
    private RuleMatrix matrix = new RuleMatrix(10000); // TODO Make this value based on num of TF Rules

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

    //Tracks agent performance for debugging  (DEBUG)
    private int stepsSinceGoal = 0;
    private int numGoals = 0;
    Vector<Integer> last10Goals = new Vector<>();

    //the current, partially-formed PathRule is stored here
    private PathRule pendingPR = null;

    //"random" numbers are useful sometimes (use a hardcoded seed for debugging)
    //Important:  Always use this generator so that output is reproducible
    // with the same seed.  Don't create your own elsewhere in the agent.
    public static Random rand = new Random(2);

    //Stores the percentage that each sensor is on and relative info to calculate it.
    //HashMap maps sensor name (String) to Tuple containing creation time step (Integer) and percentage (Double)
    private HashMap<String, Tuple<Integer, Double>> externalPercents = new HashMap<>();
    private HashMap<String, Tuple<Integer, Double>> internalPercents = new HashMap<>();

    //Track the agent's current confusion level.  The agent will not
    //trust paths in which its confidence in the path is lower
    //than its confusion level.
    private double confusion = 0.0;

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

        if (this.rules.size() > 0) {  //can't update if no rules yet
            this.currInternal = genNextInternal(this.prevAction,
                                                this.getPrevInternal(),
                                                this.prevExternal,
                                                this.currExternal);
        }

        this.stepsSinceGoal++;

        if(GENERATERULES) {
            //rules may be added, rules may be removed, rule confidences are adjusted
            updateRuleSet();
        }
        else if(this.now == 2) {
            addPredeterminedRules();
            updateInternalPercents();
            updateExternalPercents();
        }

        this.matrix.updateConnections(this.currInternal);

        //DEBUG:  Tell the human what time it is
        if (PhuJusAgent.DEBUGPRINTSWITCH) {
            debugPrintln("TIME STEP: " + this.now);
            printInternalSensors(this.currInternal);
            printExternalSensors(this.currExternal);
        }

        //DEBUG: breakpoint here to debug
        if(this.stepsSinceGoal >= 15) {
            debugPrintln("");
        }
        if (this.now == 131) {
                debugPrintln("");
        }
        if(this.rules.values().size() > 25){
            debugPrintln("");
        }



        if(TFIDF) {
            //Calculate percentage that each sensor is on
            updateInternalPercents();
            updateExternalPercents();

            //DEBUG
            //printInternalPercents();
            //printExternalPercents();
        }

        if (DEBUGPRINTSWITCH) {
            if (this.now > 2) {
                debugPrintln("TF Rules:");
                for (int i = 0; i < tfRules.size(); i++) {
                    debugPrintln("" + tfRules.get(i));
                }
                debugPrintln("NUM RULES: " + tfRules.size());
            }
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
     * addPredeterminedRules
     *
     * adds the predetermined rules to the rule vector.
     * Note: This method is only called if the GENERATERULES flag is false
     */
    private void addPredeterminedRules() {

        RuleLoader loader = new RuleLoader(this);
        loader.loadRules("./src/agents/phujus/res/flatland.csv", this.tfRules);


        for(TFRule rule: this.tfRules){
            rules.put(rule.ruleId, rule);
        }
    }

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

        //The agent will not use paths whose confidence is less than its confusion level
        if ((this.pathToDo != null) && (this.pathToDo.lastElement().getConfidence() < this.confusion)) {
            if (DEBUGPRINTSWITCH) {
                debugPrint("I'm too confused for this path: ");
                debugPrint(this.pathToDo.lastElement().getPathStr());
                debugPrint(" (conf: " + this.pathToDo.lastElement().getConfidence());
                debugPrintln(" cfsn: " + this.confusion + ")");
            }
            this.pathToDo = null;
            this.confusion = 0.0;  //reset so agent will use its rules again after the non-path action we're about to take
        }

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
            numGoals++;
            String avgStr = String.format("%.3f", (double)this.now / (double)numGoals);
            while (last10Goals.size() >= 10) { last10Goals.remove(0); }
            last10Goals.add(stepsSinceGoal);
            double movingAvg = 0.0;
            for (int steps : last10Goals) {
                movingAvg += steps;
            }
            movingAvg /= last10Goals.size();
            String movingAvgStr = String.format("%.3f", movingAvg);
            System.out.println("Found GOAL No. " + numGoals + " in " + this.stepsSinceGoal + " steps (avg: " + avgStr +  "; moving avg: " + movingAvgStr + ")");
            rewardRulesForGoal();
            this.stepsSinceGoal = 0;
            buildNewPath();
        } else if ((this.pathToDo.size() == 0)) {
            //DEBUG
            debugPrintln("Current path failed.");

            //The agent is now confused
            this.confusion = this.pathTraversedSoFar.lastElement().getConfidence();

            buildNewPath();
        } else {
            //If we reach this point we're partway through a path and no maint is needed

            //TODO:  needed?
            this.confusion = 0.0;

            //TODO: path validation?
            //If the agent is partway through a path but the predicted outcome
            //of it's most recent step has already not been met, then it may
            //make sense to abort the path now and build a new one.  We've tried
            //this before and the downside to this is that the agent misses out
            //on some nifty exploratory options and is more vulnerable to getting
            //into loops.  If we can avoid that issue, then this is  the right
            // thing to do.
        }



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

        return action;
    }//calcAction

    /**
     * calcPrunedInternal
     *
     * When the rule associated with an internal sensor failed to predict the
     * agent's subsequent sensing it should not be included in new rules.
     * This method prunes such issues from a current set of internal
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

            if(cand.isExtMatch()){
                result.add(cand.ruleId);
            }

        }//for each internal sensor

        return result;
    }//calcPrunedInternal

    /**
     * genNextInternal
     * <p>
     * calculates what the internal sensors will be on for the next timestep
     * by seeing which rules have a sufficient match score and correctly
     * predicted the current external sensors
     *
     * Caveat: At the moment, "sufficient" is any rule that exceeds the
     *         match cutoff.
     * <p>
     * @param action  selected action to match LHS
     * @param prevInt internal sensors to match LHS
     * @param prevExt external sensors to match LHS
     * @param currExt external sensors to match RHS
     */
    public HashSet<Integer> genNextInternal(char action,
                                            HashSet<Integer> prevInt,
                                            SensorData prevExt,
                                            SensorData currExt) {
        HashSet<Integer> result = new HashSet<>();

        // find the highest match score
        // TODO:  If we were clever we'd put all these scores in an array
        //        so we don't have to calc them twice (see next loop)
        double bestScore = 0.0;
        //useful for debugging but not strictly needed
        TFRule bestRule = null;
        for (TFRule r : this.tfRules) {
            // ignore rules that didn't predict external sensors correctly
            if (!r.isExtMatch(action, prevExt, currExt))
                continue;
            double score = r.lhsMatchScore(action, prevInt, prevExt);
            if (score > bestScore) {
                bestScore = score;
                bestRule = r;
            }
        }

        // Turn on internal sensors for all rules that are at or "near" the
        // highest match score.  "near" is currently hard-coded
        // TODO:  have "near" be adjusted based on agent experience

        for (TFRule r : this.tfRules) {
            // ignore rules that didn't predict external sensors correctly
            if (!r.isExtMatch(action, prevExt, currExt))
                continue;
            double score = r.lhsMatchScore(action, prevInt, prevExt);
            if (1.0 - (score / bestScore )  <= TFRule.MATCH_NEAR){
                result.add(r.getId());
            }
        }
        return result;
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
        debugPrintln("removed Internal Sensor number: "+ ruleNumber);
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
     * updateTFRuleConfidences
     *
     * updates the accuracy (called confidence) of the tfrules that
     * matched correctly in the prev timestep and decreases the rules that
     * didn't match correctly
     *
     */
    public void updateTFRuleConfidences() {
        //find the highest match score because the ration of a rule's match score
        //to the best score affects the amount of increase/decrease in confidence
        //TODO:  If we were clever we'd put all these scores in an array
        //       so we don't have to calc them twice (see next loop)
        double bestScore = 0.0;
        for (TFRule r : this.tfRules) {
            double score = r.lhsMatchScore(this.prevAction, this.getPrevInternal(), this.prevExternal);
            if (score > bestScore) {
                bestScore = score;
            }
        }

        //Update confidences of all matching rules
        for(TFRule rule: this.tfRules) {
            double score = rule.lhsMatchScore(this.prevAction, this.getPrevInternal(), this.prevExternal);

            if(score > 0.0) {

                //For base-event rules, confidence is not adjusted unless it's a perfect match
                if ( (rule.getOperator() == TFRule.RuleOperator.ALL)
                        && (rule.lhsExtMatchScore(this.prevExternal) < TFRule.MAX_MATCH) ) {
                    continue;
                }

                if (rule.isRHSMatch()) {
                    rule.increaseConfidence(score, bestScore);
                } else {
                    rule.decreaseConfidence(score, bestScore);
                }
            }
        }

    }//updateTFRuleConfidences

    /**
     * cullRules
     *
     * if MAXNUMRULES has been reached, this method culls the set down
     * by removing the worst rules.
     *
     * a rules score is calculated by multiplying its activation with its accuracy
     */
    private void cullRules() {

        if (this.rules.size() >= MAXNUMRULES || this.currExternal.isGoal()) {
            mergeRules();
        }
//        while(this.rules.size() > MAXNUMRULES) {
//
//            TFRule worstRule = (TFRule) this.rules.elements().nextElement();
//
//            double worstScore = worstRule.calculateActivation(this.now) * worstRule.getConfidence();
//            for (Rule rule : this.rules.values()) {
//                if (rule instanceof TFRule) {
//                    TFRule r = (TFRule) rule;
//                    double activation = r.calculateActivation(this.now);
//                    double score = activation * r.getConfidence();
//                    if (score < worstScore) {
//                        worstScore = score;
//                        worstRule = r;
//                    }
//                }
//            }
//
//        /*//Find the rule with lowest activation & accuracy
//        //TODO:  score of a node should be its own score or child's whichever is higher!
//        //       A good way to do this is to override calculateActivation in
//        //       BaseRule and have it recurse through children.  It's expensive but
//        //       calculating activation is already expensive anyway.  We can memoize
//        //       it later if needed.  -:AMN: Dec 2021
//        EpRule worstRule = (EpRule) this.rules.get(0);
//        double worstScore = worstRule.calculateActivation(this.now) * worstRule.getAccuracy();
//        for (Rule rule : this.rules.values()) {
//            if(rule instanceof EpRule) {
//                EpRule r = (EpRule) rule;
//                double activation = r.calculateActivation(this.now);
//                double score = activation * r.getAccuracy();
//                if (score < worstScore) {
//                    worstScore = score;
//                    worstRule = r;
//                }
//            }
//        }
//*/
//            //out with the old, in with the new...was there a baby in that bath water?
//            //TODO:  the removeRule method needs to change
//            removeRule(worstRule, null);
//        }
    }

    /**
     * mergeRules
     *
     * Merges similar rules. The process goes something like this:
     *   - Look at adjacency matrix and find rules which are often firing together (say more than X times)
     *   - If they've fired more than X times, have the same external sensors and actions, and similar confidences,
     *     then we merge them
     *   - Delete one of the rules, then go and adjust all internal sensors for all the rules which use the
     *     deleted rule
     */
    private void mergeRules() {

        // Compilate all of the rules which need to be merged:
        HashMap<TFRule, Vector<TFRule>> rulesToMerge = new HashMap<>();
        for (TFRule rule : tfRules) {
            Vector<TFRule> similarRules = this.matrix.getSimilarRules(rule.getId(), 1);
            boolean canAdd = true;

            // Making sure that the same rules aren't merged multiple times
            for (TFRule similarRule : similarRules) {
                if (rulesToMerge.containsKey(similarRule)) {
                    canAdd = false;
                    break;
                }
            }

            if (canAdd) {
                rulesToMerge.put(rule, similarRules);
            }
        }

        // Commence merging:
        for (TFRule rule : rulesToMerge.keySet()) {

            if (rulesToMerge.size() == 0) {
                continue;
            }
            for (TFRule delete : rulesToMerge.get(rule)) {
                removeRule(delete, rule);
            }
        }
    }//mergeRules


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

        //updates the accuracies of our tf rules
        updateTFRuleConfidences();

        //Update the TF values for all extant TFRules
        boolean wasMatch = updateAllTFValues();

        //Create new TF Rule(s) for this latest experience
        addRule(new TFRule(this));
        if(!wasMatch) {
            //create a base-event rule
            TFRule baseRule = new TFRule(this, this.prevAction, new String[]{"-1"},
                    this.getPrevExternal(), this.getCurrExternal(), 1.0, TFRule.RuleOperator.ALL);
            addRule(baseRule);

        }
        
        //See if we need to cull rule(s) to stay below max
        cullRules();

    }//updateRuleSet



    /**
     * updateAllTFValues
     *
     * updates the TF values of every condition of every TFRule based
     * on the agent's most recently completed experience
     *
     * @return whether a base-event rule exists for the experience
     */
    private boolean updateAllTFValues() {
        boolean wasMatch = false;
        for(TFRule rule : this.tfRules) {
            if (rule.isExtMatch()) {
                rule.updateTFVals();
                if (rule.getOperator() == TFRule.RuleOperator.ALL) {
                    wasMatch = true;
                }
            }
        }
        return wasMatch;
    }//updateAllTFValues


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
            //System.err.println("ERROR: Exceeded MAXNUMRULES!");
        }

        rules.put(newRule.ruleId,newRule);
        newRule.addActivation(this.now, 1.0);

        this.tfRules.add((TFRule)newRule);

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
    public void removeRule(TFRule removeMe, TFRule replacement) {
        boolean removed = rules.remove(removeMe.ruleId ,removeMe);
        //rules.remove
        tfRules.remove(removeMe);
        //tfRules.removeElement(removeMe);


        //DEBUGGING
        if (replacement == null) debugPrint("removed: ");
        else debugPrint("replaced: ");
        //debugPrintln(removeMe.toString());

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
        //TODO:  This needs to be revised
//        double reward = 1.0;
//        int time = this.now;
//        for(int i = this.pathTraversedSoFar.size() - 1; i >= 0; --i) {
//            TreeNode node = this.pathTraversedSoFar.get(i);
//            TFRule tr = node.getTermRule();
//            if (tr != null) {
//                tr.addActivation(time, reward);
//                System.out.println("current activation: " + tr.calculateActivation(this.now));
//            }
//            time--;
//            reward *= TFRule.DECAY_RATE;
//        }

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

    public double getConfusion() {
        return confusion;
    }
    //endregion

}//class PhuJusAgent
