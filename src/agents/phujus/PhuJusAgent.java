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
 * > add a toString() to PJA
 * > increase code coverage and thoroughness of unit tests
 * > add levels to the debug logging system so we can turn on/off various
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
     * class MergeQueue
     *
     * Rules that fire simultaneously are added to the MergeQueue. Rules that fire simultaneously tend to be very
     * similar/identical rules, so when mergeRules() is called, every set of rules in the queue is checked to see
     * if they should be merged.
     * <p>
     * If two rules should NOT be merged, then they are added to a blacklist so that they don't get added again.
     * Duplicate sets of rules cannot be added to the queue.
     */
    public static class MergeQueue {

        //Each array is length==2 with the number of two rules that are candidates for merging
        private Vector<Integer[]> queue;

        //Pairs of rules that have demonstated they are distinct and should not be merged
        private Vector<Integer[]> blacklist;

        public MergeQueue() {
            this.queue = new Vector<>();
            this.blacklist = new Vector<>();
        }

        public boolean hasNext() {
            return this.queue.size() > 0;
        }

        /**
         * getMinimumIdx
         *
         * Helper method for getting the index of the minimum value in the array.
         * @param arr
         * @return Index of min value
         */
        private int getMinimumIdx(Integer[] arr) {

            if (arr.length <= 0) {
                return 0;
            }

            int min = arr[0];
            int minIdx = 0;

            for (int i = 0; i < arr.length; i++) {
                if (arr[i] < min) {
                    min = arr[i];
                    minIdx = i;
                }
            }

            return minIdx;
        }

        /**
         * Adds a set of ruleIDs to the queue.
         * <p>
         * If the set of rules provided is greater than 2, then it's split up into pairs, where the first rule
         * in the pair is the one with the lowest number of internal sensors
         * @param set a set of 2+ rule IDs
         */
        public void add(HashSet<Integer> set) {

            if (set.size() <= 1) {
                return;
            }

            Integer[] convertedSet = set.toArray(new Integer[0]);

            if (this.queue.contains(convertedSet) || this.blacklist.contains(convertedSet)) {
                return;
            }

            // if the set contains > 2 rule IDs, split it up into pairs.
            // e.g. [3,5,7,8] -> [3,5] [3,7] [3,8]
            // This makes rule merging simpler.
            if (convertedSet.length > 2) {

                // This part of the code dissects an incoming array into pairs.
                // Right now, it uses the first number (which is the smallest one) as the pivot.
                // TODO set first element in pairs to rule with lowest # of internal sensors? Not sure if that's
                //      necessary since the first element most likely is
                for (int i = 1; i < convertedSet.length; i++) {

                    if (convertedSet[i] == convertedSet[0]) continue;

                    Integer[] newPair = new Integer[] {convertedSet[0], convertedSet[i]};

                    if (this.queue.contains(newPair)) {
                        continue;
                    }

                    this.queue.add(newPair);
                }
            } else {
                this.queue.add(convertedSet);
            }
        }

        /**
         * Removes the entry from the start of the queue and returns it (FIFO)
         * @return
         */
        public Integer[] pop() {
            if (this.queue.size() == 0) return null;
            return this.queue.remove(0);
        }

        /**
         * Reveals the entry at the beginning of the queue without removing it.
         * @return
         */
        public Integer[] peek() {
            if (this.queue.size() == 0) return null;
            return this.queue.get(0);
        }

        /**
         * Adds a set of ruleIDs to be blocked from being added to the queue.
         * @param block
         */
        public void blacklist(Integer[] block) {
            this.blacklist.add(block);
        }
    }//class MergeQueue

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

    // DEBUG variable to toggle printing the tree (on/off = true/false)
    public static final boolean DEBUGTREESWITCH = true;

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
    private MergeQueue mergeQueue = new MergeQueue();

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
    private boolean currPathRandom = false;
    private int confusionSteps = 0;  //how many steps until this confusion
    private int prevConfSteps = 0;

    //This is the TFRule that best matches the agent's previous int/ext sensing and
    // current ext sensing (i.e., the rule that best predicted the currently known outcome)
    // NOTE:  A newly created rule is excluded for consideration for this
    //        variable since it did not exist in the previous timestep
    private TFRule prevBestMatch = null;
    private double prevBestScore = 0.0;  //match score for the above

    //This is the TFRule that best matches the agent's current int/ext sensing
    //(i.e., the agent is most confident is predicting the future ext sensing)
    private TFRule currBestMatch = null;

    //This is the TFRule that was placed in prevBestMatch two timesteps ago
    private TFRule prevPrevBestMatch = null;

    //This is the PathRule that best matches the agent's experience
    //that concluded with the most recent external sensors.
    private PathRule prevBestPRMatch = null;

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
        this.actionList = actions;
        Rule.resetRuleIds();
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

        //update the PathRules now that the agent knows the ext. sensor outcome
        updatePathRules();

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

        // If multiple sensors are fired, they are added to the merge queue.
        this.mergeQueue.add(this.currInternal);

        //DEBUG:  Tell the human what time it is
        if (PhuJusAgent.DEBUGPRINTSWITCH) {
            debugPrintln("TIME STEP: " + this.now);
            printInternalSensors(this.currInternal);
            printExternalSensors(this.currExternal);
        }

        //DEBUG: breakpoint here to debug
        if(this.stepsSinceGoal >= 50) {
            debugPrintln("");
        }
        if (this.now >= 15) {
            debugPrintln("");
        }
        if (this.now >= 200) {
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

                debugPrintln("Path Rules:");
                for (PathRule pr : this.pathRules) {
                    debugPrintln("" + pr);
                }
                debugPrintln("NUM RULES: " + tfRules.size());
            }
        }

        //Update the agent's current path
        pathMaintenance(sensorData.isGoal());


        //extract next action
        char action = calcAction();

        //Use the selected action to make our best prediction about the future
        updateCurrBestMatch(action);

        //Use the selcted action to setup values for the next iteration
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
        loader.loadRules("./src/agents/phujus/res/rule_merge_testing.csv");
    }

//region PathRule Methods

    /**
     * update the value of this.prevBestMatch/Score
     *
     * Note:  call this method before new rules are added in the current time
     *        step since such rules can't have been the previous best.
     */
    private void updatePrevBestMatch() {
        this.prevPrevBestMatch = this.prevBestMatch;  //save for use by updatePathRules
        this.prevBestScore = 0.0;
        this.prevBestMatch = null;
        for (TFRule r : this.tfRules) {
            if (r.isExtMatch(this.prevAction, this.prevExternal, this.currExternal)) {
                double score = r.lhsMatchScore(this.prevAction, this.getPrevInternal(), this.prevExternal);
                if (score > this.prevBestScore) {
                    this.prevBestScore = score;
                    this.prevBestMatch = r;
                }
            }
        }
    }//updatePrevBestMatch

    /**
     * update the value of this.currBestMatch/Score
     *
     * TODO:  Instead of just the rule with the highest match score, should
     *        this be the highest match score rule that aligns with the
     *        bloc-voting result {@see TreeNode#predictExternalMark3}
     *
     * @param action  the action the agent has chosen for the current timestep
     */
    private void updateCurrBestMatch(char action) {
        double currBestScore = 0.0;
        this.currBestMatch = null;
        for (TFRule r : this.tfRules) {
            double score = r.lhsMatchScore(action, this.currInternal, this.currExternal);
            if (score > currBestScore) {
                currBestScore = score;
                this.currBestMatch = r;
            }
        }
    }//updatePrevBestMatch

    /**
     * getMatchingPathRules
     *
     * @return a list of all rules that match the agent's current most recent experience
     */
    private Vector<PathRule> getMatchingPathRules() {
        Vector<PathRule> matches = new Vector<>();
        for(PathRule pr : this.pathRules) {
            if (! pr.rhsMatch(this.currExternal)) continue; //RHS mismatch

            //check against the prev best matches
            Vector<TFRule> flatPR = pr.flatten();
            int tfIndex = flatPR.size() - 1;
            if (flatPR.get(tfIndex).ruleId != this.prevBestMatch.ruleId) continue; //LHS2 mismatch
            tfIndex--;

            //TODO: DEBUG REMOVE
            if (this.prevPrevBestMatch == null) {
                debugPrintln("oops");
            }


            if (flatPR.get(tfIndex).ruleId != this.prevPrevBestMatch.ruleId) continue; //LHS1 mismatch
            tfIndex--;

            //check against this.prevInternal
            int prevIndex = PhuJusAgent.MAX_TIME_DEPTH - 3;
            boolean match = true;
            while( (tfIndex >= 0) && (prevIndex >= 0) ) {
                int prId = flatPR.get(tfIndex).ruleId;
                if (! this.prevInternal.get(prevIndex).contains(prId)) {
                    match = false;
                    break;
                }
                tfIndex--;
                prevIndex--;
            }//while

            if (match) {
                matches.add(pr);  //Hooray!
            }
        }//for

        return matches;
    }//getMatchingPathRules


    /**
     * getBestMatchingPathRule
     *
     * @return the PathRule that best matches the agent's most recent experience
     */
    private PathRule getBestMatchingPathRule() {
        Vector<PathRule> matches = getMatchingPathRules();

        //Easy cases: 0 or 1 matches
        if (matches.size() == 0) {
            return null;
        } else if (matches.size() == 1) {
            return matches.firstElement();
        }

        //Break tie with longest match (Is longest best??  Using shorter
        // rules would yield a more matchable result.)
        PathRule best = matches.firstElement();
        int bestSize = best.size();
        for(PathRule pr : matches) {
            int size = pr.size();
            if (size > bestSize) {
                bestSize = size;
                best = pr;
            }
        }
        return best;
    }//getBestMatchingPathRule


    /**
     * updateMultiLevelPathRules
     *
     * finds the PathRules that matches the agent's most recent experiences
     * and increases their confidence.  The best matching of these rules is
     * placed in this.prevBestPRMatch
     *
     * Note:  this.prevBestMatch must have been updated this time step
     */
    private void updateMultiLevelPathRules() {

        //Can't create ML rule without this being set
        if (prevBestPRMatch != null) {

            //See if an existing multi-level pathrule matches the agent's current circumstances
            PathRule match = null;
            for (PathRule pr : this.pathRules) {
                if ((pr.rhsMatch(this.currExternal)
                        && pr.lhsMatch(this.prevBestPRMatch, this.prevBestMatch))) {
                    match = pr;
                    break;
                }
            }

            //If none exists, create one
            if (match == null) {
                match = new PathRule(this, this.prevBestPRMatch, this.prevBestMatch, this.currExternal);
                addRule(match);
            } else {
                match.increaseConfidence(1.0, 1.0);
            }
        }

        //Update the best PathRule match for next time
        this.prevBestPRMatch = getBestMatchingPathRule();
    }//updateMultiLevelPathRules

    /**
     * getLHSPRMatchesWithRHSMismatch
     *
     * is a helper method for {@link #updatePathRules()}.  It finds a list
     * of all the two-step PathRules that match the current prev and prevprev BestMatch.
     *
     * Caveat:  prevBestMatch and prevPrevBestMatch must be up to date and non-null
     *
     * Side Effect:  increases the confidence of the correct match if found
     * (or creates it if it doesn't exist).  This functionality should
     * probably be in separate methods but its tied together atm and not a big deal.
     *
     */
    private Vector<PathRule> getLHSPRMatchesWithRHSMismatch() {
        Vector<PathRule> incorrectLHSMatches = new Vector<>();
        PathRule correctPR = null;
        for(PathRule pr : this.pathRules) {
            if (pr.lhsMatch(this.prevPrevBestMatch, this.prevBestMatch)) {
                if (pr.rhsMatch(this.currExternal)) {
                    correctPR = pr;  //This should only happen once
                } else {
                    incorrectLHSMatches.add(pr);
                }
            }
        }

        //If no correct PR was found, create it
        if (correctPR == null) {
            correctPR = new PathRule(this, this.prevPrevBestMatch, this.prevBestMatch, this.currExternal);
            addRule(correctPR);
        } else {
            correctPR.increaseConfidence(1.0, 1.0);
        }

        return incorrectLHSMatches;
    }//getLHSPRMatchesWithRHSMismatch


    /**
     * getLHSPRMatchesWithRHSMismatch
     *
     * is a helper method for {@link #updatePathRules()}.  It finds the
     * PathRule that matches what the agent THOUGHT was the best
     * predicting rule but was wrong.  If no such PathRule exists
     * it is created.
     *
     * Caveat:  prevBestMatch and prevPrevBestMatch can't be null
     *
     * @return the found/created rule or null if currBestMatch was correct
     */
    private PathRule getPRForBadCurrBestMatch() {
        //If the currBestMatch predicted correctly, this method has nothing to do
        if (this.currBestMatch.getRHSExternal().equals(this.currExternal)) {
            return null;
        }

        //See if it already exists
        PathRule badPR = null;
        for(PathRule pr : this.pathRules) {
            if (pr.lhsMatch(this.prevPrevBestMatch, this.currBestMatch)
                    && (pr.rhsMatch(this.currBestMatch.getRHSExternal())) ) {
                badPR = pr;
                break;
            }
        }

        //If not found, add it
        if (badPR == null) {
            badPR = new PathRule(this, this.prevPrevBestMatch, this.currBestMatch, this.currBestMatch.getRHSExternal());
            addRule(badPR);
        }
        return badPR;
    }//getPRForBadCurrBestMatch


    /**
     * updatePathRules
     *
     * Performs maintenance on this.pathRules.  This includes:
     *  - confidence adjustments
     *  - creating new PathRules for new experiences
     *  - TODO: removing PathRules to stay below a given max
     *
     * Important:  this.prevBestMatch must be set
     */
    private void updatePathRules() {
        //Must have prev-prev and prev TFRules set to do anything
        updatePrevBestMatch();
        if (this.prevBestMatch == null) {
            this.prevPrevBestMatch = null;
            this.prevBestPRMatch = null;
            return;
        }
        if (prevPrevBestMatch == null) return;

        //Find all PRs with matching LHS but wrong RHS
        // (while rewarding the correct one with correct RHS)
        Vector<PathRule> incorrectLHSMatches = getLHSPRMatchesWithRHSMismatch();

        //If the agent's currBestMatch was incorrect, then the PathRule
        // reflecting that is also a mismatch
        PathRule badPR = getPRForBadCurrBestMatch();
        if (badPR != null) incorrectLHSMatches.add(badPR);

        //decrease confidences of all mismatching PRs
        for (PathRule pr : incorrectLHSMatches) {
            pr.decreaseConfidence(1.0, 1.0);  //max decrease
        }

        //also update multi-level PathRules in the same manner
        //TODO:  Nuxoll commented out for now as not quite working and needs to be rewritten anyway
        //updateMultiLevelPathRules();
    }//updatePathRules

//endregion

    /**
     * genNextInternal
     * <p>
     * calculates what the internal sensors will be on for the next timestep
     * by seeing which rules have a sufficient match score and correctly
     * predicted the current external sensors
     *
     * Caveat: At the moment, "sufficient" is any rule that exceeds the
     *         match cutoff.
     *
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

        // find the TFRule that best matches the given sensors/actions
        // IMPORTANT:  You can't use this.prevBestScore/Match for this because
        //             genNextInternal is also used extensively by TreeNode
        double bestScore = 0.0;
        TFRule bestRule = null; //useful for debugging but not strictly needed
        // Put all the scores in an array so we don't have to calc them twice (see next loop)
        double[] allScores = new double[this.tfRules.size()];
        for (int i = 0; i < this.tfRules.size(); ++i) {
            TFRule r = tfRules.get(i);
            // ignore rules that didn't predict external sensors correctly
            if (!r.isExtMatch(action, prevExt, currExt)) {
                allScores[i] = 0.0;
                continue;
            }
            allScores[i] = r.lhsMatchScore(action, prevInt, prevExt);
            if (allScores[i] > bestScore) {
                bestScore = allScores[i];
                bestRule = r;
            }
        }

        // Turn on internal sensors for all rules that are at or "near" the
        // highest match score.  "near" is currently hard-coded
        // TODO:  have agent adjust "near" based on its experience
        for (int i = 0; i < this.tfRules.size(); ++i) {
            if (allScores[i] == 0.0) continue;
            if (1.0 - (allScores[i] / bestScore )  <= TFRule.MATCH_NEAR){
                int ruleId = this.tfRules.get(i).getId();
                result.add(ruleId);
            }
        }
        return result;
    }//genNextInternal


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
        }

        this.pathTraversedSoFar.clear();

        //Find a new path to the goal
        TreeNode root = new TreeNode(this);

        
        this.pathToDo = root.findBestGoalPath();

        //The agent will not use paths whose confidence is less than its confusion level
        if ((this.pathToDo != null) && (this.pathToDo.lastElement().getConfidence() <= this.confusion)) {
            if (DEBUGPRINTSWITCH) {
                debugPrint("I'm too confused for this path: ");
                debugPrint(this.pathToDo.lastElement().getPathStr());
                debugPrint(" (conf: " + this.pathToDo.lastElement().getConfidence());
                debugPrintln(" cfsn: " + this.confusion + " steps: " + this.confusionSteps + ")");
            }
            this.pathToDo = null;
        }

        //DEBUG
        if (PhuJusAgent.DEBUGPRINTSWITCH && PhuJusAgent.DEBUGTREESWITCH) {
            root.printTree();
        }

        if (this.pathToDo == null) {
            debugPrintln("no path found");

            this.pathToDo = root.findMostUncertainPath();
            this.currPathRandom = true;


            //DEBUG
            if (this.pathToDo != null) {
                debugPrintln("using non-goal path with greatest uncertainty: " + this.pathToDo.lastElement().getPathStr());
            }
        }

        //DEBUG
        else {
            debugPrint("found path: " + this.pathToDo.lastElement().getPathStr());
            debugPrintln(String.format(" (conf=%.3f)", this.pathToDo.lastElement().getConfidence()));
        }


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
            //reset confusion info
            this.confusion = 0.0;
            this.prevConfSteps = 0;
            this.confusionSteps = 0;
            this.currPathRandom = false;

        } else if ((this.pathToDo.size() == 0)) {
            //DEBUG
            debugPrintln("Current path failed.");

            //TODO:  create a PathNode to record this failure using this.pathTraversedSoFar

            if (this.currPathRandom) {
                this.confusionSteps --;
                this.prevConfSteps ++;
                if (this.confusionSteps <= 0) {
                    this.confusion = 0.0;
                }
            } else {
                //The agent is now confused
                this.confusion = this.pathTraversedSoFar.lastElement().getConfidence();
                if (this.confusion > 0.0) {
                    this.confusionSteps = this.prevConfSteps + 1;
                }
            }
            this.currPathRandom = false;

            buildNewPath();
        } else {
            //If we reach this point we're partway through a path and no maint is needed

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
     * getPRMatches
     *
     * is a helper method for {@link #metaScorePath}.
     *
     * @param path          a candidate path the agent is considering
     * @param candidates    a set of PathRules, some of which may match the given path
     *
     * @return all the candidates that actually do match the given path
     */
    private Vector<PathRule> getPRMatches(Vector<TreeNode> path, Vector<PathRule> candidates) {
        Vector<PathRule> result = new Vector<>();

        //Compare each candidate to the path
        for(PathRule cand : candidates) {
            Vector<TFRule> flatCand = cand.flatten();
            boolean mismatch = false;

            //Iterate over each step of the path in reverse order from last to first
            for (int i = 1; i <= path.size(); ++i) {  //i.e., i-th from the end
                TreeNode currNode = path.get(path.size() - i);

                //Since the path may have more steps than the PathRule does, check
                // to make sure the PathRule has a step at this position.
                int lbIndex = flatCand.size() - i;
                if (lbIndex < 0) break;  //i.e., it's match as far as it goes

                //Compare this step of the path to corresponding step in the PathRule
                TFRule lhsBit = flatCand.get(lbIndex);
                if ( (lhsBit.getAction() != currNode.getAction())
                        || (! currNode.getCurrInternal().contains(lhsBit.ruleId)) ) {
                    mismatch = true;
                    break;
                }

            }//for

            //Match found, yay!
            if (!mismatch) {
                result.add(cand);
            }
        }//for

        return result;
    }//getPRMatches

    /**
     * metaScorePath
     *
     * provides an opinion about a given path based on the PathRule set
     *
     * @return a double 0.0..1.0 scoring the agent's opinion
     */
    public double metaScorePath(Vector<TreeNode> path) {
        //If there is no previous path then nothing can match
        if (this.prevPath == null) return 1.0;

        //Find all PathRules that have a matching RHS
        SensorData outcome = path.lastElement().getCurrExternal();
        Vector<PathRule> candidates = new Vector<>();
        for(PathRule pr : this.pathRules) {
            if (pr.rhsMatch(outcome)) {
                candidates.add(pr);
            }
        }
        Vector<PathRule> matches = getPRMatches(path, candidates);

        //No matches?  default opinion:  this path is good
        if (matches.size() == 0) return 1.0;

        //Only one match?  No more work need be done
        if (matches.size() == 1) return matches.firstElement().getConfidence();

        //For each match that is longer than the path compare it to the
        // present and past sensor values in order to winnow our match pool
        // TODO:  right now this only compares to currInternal because
        //        atm there are no PathRule with 3+ steps
        Vector<PathRule> keepers = new Vector<>();
        for(PathRule match : matches) {
            Vector<TFRule> flatMatch = match.flatten();
            int index = flatMatch.size() - path.size() - 1;
            if (index >= 0) {
                TFRule step = flatMatch.get(index);
                if (this.currInternal.contains(step.ruleId)) {
                    keepers.add(match);
                }
            }
        }


        //TODO:  break tie with external sensors??  Really not sure we should
        //       do this or not since partially-matching ext sensors maybe okay
        //       especially if some ext sensors are random.


        //TODO: If still a tie, break with longest match
        //      (Won't work at the moment since all PathRules are currently two steps.)


        //Return the average confidence
        //TODO:  Is this wise?  Perhaps the max is better?
        double sum = 0.0;
        for(PathRule match : keepers) { sum += match.getConfidence(); }
        return sum / keepers.size();

    }//metaScorePath

    /**
     * updateTFRuleConfidences
     *
     * updates the accuracy (called confidence) of the tfrules that
     * matched correctly in the prev timestep and decreases the rules that
     * didn't match correctly
     *
     * Caveat:  this.prevBestScore should be set correctly before this is called
     *
     */
    public void updateTFRuleConfidences() {
        //find the highest match score because the ration of a rule's match score
        //to the best score affects the amount of increase/decrease in confidence
        //NOTE:  Can't use this.bestPrevMatch for this because it rejects any rule
        //       with mismatching RHS.
        double bestScore = 0.0;
        TFRule bestRule = null;  //not needed but useful for debugging
        //Put all these scores in an array so we don't have to calc them twice (see next loop)
        double[] scores = new double[this.tfRules.size()];
        int scIndex = 0;
        for (TFRule r : this.tfRules) {
            double score = r.lhsMatchScore(this.prevAction, this.getPrevInternal(), this.prevExternal);
            if (score > bestScore) {
                bestScore = score;
                bestRule = r;
            }
            scores[scIndex] = score;
            scIndex++;
        }

        //Update confidences of all matching rules
        scIndex = 0;
        for(TFRule rule: this.tfRules) {
            double score = scores[scIndex];
            scIndex++;

            if(score > 0.0) {  //TODO: should this be something like (score > bestScore/2) instead?
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
     * TODO delete cullRules()?
     */
    private void cullRules() {
        mergeRules();
    }

    /**
     * mergeRules
     *
     * Merges similar rules.
     * <p>
     * When two or more rules activate at the same time, they are added to the MergeQueue. If the number of simultaneous
     * rule is greater than two, they're split up into pairs. For example, the input:
     * <p>
     * [3,16,8,19] would turn into -> [3,16] [3,8] [3,19]
     * <p>
     * The first rule in the pair is determined by whichever rule in the set has the lowest number of internal
     * sensors. The first rule is what all of the other rules will merge into if they qualify.
     * <p>
     * Whenever the agent reaches a goal, or the number of rules is too high, every pair of rules in the queue is
     * checked. If their externals match with the first number in the pair, then they're merged.
     */
    private void mergeRules() {

        // Don't bother if the queue is empty
        if (!this.mergeQueue.hasNext()) {
            return;
        }

        // The pair of rules being checked
        TFRule[] mergeThese = new TFRule[2];

        while (mergeQueue.hasNext()) {
            Integer[] poppedPair = mergeQueue.pop();
            mergeThese[0] = (TFRule) this.rules.get(poppedPair[0]);
            mergeThese[1] = (TFRule) this.rules.get(poppedPair[1]);

            if (mergeThese[0] == mergeThese[1]) continue;

            // Wildcard rules should never be merged into
            if (mergeThese[0].getOperator() == TFRule.RuleOperator.ALL) continue;
            if (mergeThese[1].getOperator() == TFRule.RuleOperator.ALL) continue;

            // We merge all of the rules into the rule with the smallest number of internal sensors, since it's
            // the simplest rule. This is an extra sanity check that could probably be removed.
            // Remember: The sets of potential merges are kept in pairs! e.g. [3,4] , [3,5], [3,17], etc...
            TFRule mergeParent = mergeThese[0];
            TFRule checkThis   = mergeThese[1];
            if (mergeThese[0].getLhsInternal().size() > mergeThese[1].getLhsInternal().size()) {
                mergeParent = mergeThese[1];
                checkThis   = mergeThese[0];
            }

            // If the rules match, merge them
            if (checkThis.isExtMatch(mergeParent.getAction(), mergeParent.getLHSExternal(), mergeParent.getRHSExternal())) {
                merge(mergeParent, checkThis);
            }
            // If they don't match, add them to the blacklist so they don't get checked again
            else {
                this.mergeQueue.blacklist(poppedPair);
            }
        }//while
    }//mergeRules


    /**
     * merge
     *
     * Helper method for merging two rules. It replaces all of the ruleIDs of the rule being replaced within
     * the MergeQueue.
     * TODO simply add this code to removeRule/MergeQueue?
     * @param rule1 the rule being merged into
     * @param rule2 the rule being replaced
     */
    private void merge(TFRule rule1, TFRule rule2) {
        debugPrintln("MERGED RULE #" + rule2.getId() + " -> #" + rule1.getId());

        // Firstly, we need to modify the merge queue's rule values.
        // We start by going through all the sets of rules in the queue
        mergeDuplicates(rule1, rule2, this.mergeQueue.queue);

        // Also do this with the blacklist
        mergeDuplicates(rule1, rule2, this.mergeQueue.blacklist);

        // TODO remove? This code handles gentle merging (internal sensors are combined). Doesn't seem to improve
        //      the performance of the agent. Might be useful in the future, though.
        /*
        // This part of the code merges the internal sensors from rule2 into the internal sensors of rule1.
        // Also switches the rule operator for rule1 to be ANDOR

        boolean switchOperatorFlag = false; // <- If there are sensors to be combined, then we switch the operator
                                            //    to ANDOR

        System.out.println("Rule #" + rule2.getId() + " internal sensors:");
        for (TFRule.Cond iSensor : rule2.getLhsInternal()) {

            // Ignore sensors which aren't relevant
            if (iSensor.data.getTF() == 0) {
                continue;
            }

            // Check if the sensor is already inside of rule1 before adding it
            if (!rule1.hasInternalSensor(iSensor.sName)) {
                rule1.getLhsInternal().add(iSensor);
                switchOperatorFlag = true;
            }
            System.out.println(iSensor);
        }

        if (switchOperatorFlag) rule1.setOperator(TFRule.RuleOperator.ANDOR);
*/
        removeRule(rule2, rule1);
    }//merge

    /**
     * mergeDuplicates
     *
     * Helper method for merge() which replaces all instances of rule2 in an Integer[] list with rule1.
     * @param rule1
     * @param rule2
     * @param list
     */
    private void mergeDuplicates(TFRule rule1, TFRule rule2, Vector<Integer[]> list) {
        for (Integer[] ruleSet : list) {

            // If any of the sets of rules contain rule2, we replace the Id with rule1's id
            for (int i = 0; i < ruleSet.length; i++) {
                if (ruleSet[i] == rule2.getId()) {
                    ruleSet[i] = rule1.getId();
                }
            }
        }
    }//mergeDuplicates


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
        // Sometimes, new rules are added which are identical to the previous one. This check is in place to prevent
        // this from happening.
        TFRule newRule = new TFRule(this);
        if (tfRules.size() > 0 && !tfRules.lastElement().isExtMatch(newRule.getAction(), newRule.getLHSExternal(), newRule.getRHSExternal())) {
            addRule(newRule);
        }
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

        // I'm assuming we don't want to add activations to pre-set rules. Could totally be wrong
        // about this
        if (!GENERATERULES) {
            newRule.addActivation(this.now, 1.0);
        }

        if (newRule instanceof TFRule) {
            this.tfRules.add((TFRule) newRule);
        }
        else if (newRule instanceof PathRule) {
            this.pathRules.add((PathRule) newRule);
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

        //update PathRules that used this rule
        Vector<PathRule> toChange = new Vector<>();
        for(PathRule pr : this.pathRules) {
            if (pr.uses(removeMe)) {
                toChange.add(pr);
            }
        }
        for(PathRule changeMe : toChange) {
            if (replacement == null) {
                this.pathRules.remove(changeMe);
            } else {
                changeMe.replace(removeMe, replacement);
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
