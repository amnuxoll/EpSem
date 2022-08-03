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

    //-----------------------------//
    //===== Hyper-Parameters ===== //   <-- we hate these :@
    //-----------------------------//
    public static final int MAXNUMRULES = 4000;
    public static final int MAX_SEARCH_DEPTH = 5; //TODO: get the search to self prune again
    public static final int MAX_TIME_DEPTH = 7;  //size of short term memory

    /** How similar two match scores need to be to each other to be "near."
     * Some prelimiary research shows that the best value for this parameter
     * varies from FSM to FSM, from run to run in the same FSM, and even
     * during a single run on a single FSM. MATCH_NEAR badly needs to be
     * replaced with some sort of self-tuning mechanism. */
    public static final double MATCH_NEAR = 0.1;

    //-----------------------------//
    //=====   DEBUG FLAGS    ===== //
    //-----------------------------//

    // DEBUG variable to toggle println statements (on/off = true/false)
    public static final boolean DEBUGPRINTSWITCH = true;

    // DEBUG variable to toggle printing the tree (on/off = true/false)
    public static final boolean DEBUGTREESWITCH = true;

    // FLAG variable to toggle updating of TFIDF values
    public static final boolean TFIDF = true;

    // FLAG variabale to toggle rule generation
    // if this is false, addPredeterminedRules will be called at timestep 0
    public static final boolean GENERATERULES = true;

//region InnerClasses

    /**
     * class Tuple describes a Tuple object for storing three values of different type
     * modified from:
     * https://commons.apache.org/proper/commons-lang/apidocs/org/apache/commons/lang3/tuple/Triple.html
     */
    public static class Tuple<F, S> {
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
    }//class Tuple

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
        private final Vector<Integer[]> queue;

        //Pairs of rules that have demonstated they are distinct and should not be merged
        private final Vector<Integer[]> blacklist;

        public MergeQueue() {
            this.queue = new Vector<>();
            this.blacklist = new Vector<>();
        }

        public boolean hasNext() {
            return this.queue.size() > 0;
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

                    if (Objects.equals(convertedSet[i], convertedSet[0])) continue;

                    Integer[] newPair = new Integer[] {convertedSet[0], convertedSet[i]};

                    if (this.queue.contains(newPair)) {
                        continue;
                    }

                    this.queue.add(newPair);
                }
            } else {
                this.queue.add(convertedSet);
            }
        }//add

        /**
         * Removes the entry from the start of the queue and returns it (FIFO)
         */
        public Integer[] pop() {
            if (this.queue.size() == 0) return null;
            return this.queue.remove(0);
        }

        /**
         * Adds a set of ruleIDs to be blocked from being added to the queue.
         */
        public void blacklist(Integer[] block) {
            this.blacklist.add(block);
        }
    }//class MergeQueue

//endregion Inner Classes

//region Instance Variables

    /**
     * Always use this generator for any random numbers used by this agent.
     * By doing so, any output is reproducible with the same seed.
     *
     * Note:  If you wish to change the FSM you have to change the seed
     * in src/utils/Random.java not here.  Notable seeds:
     *   13 - we've used this to develop PhuJusAgent
     *   14 - used for presentation slides
     *   21 - particularly nasty
     */
    public static Random rand = new Random(2);

    /** list of available actions in current FSM */
    private Action[] actionList;

    /** current timestep 't' */
    private int now = 0;

    //The agent keeps lists of the rules it is using
    private final Vector<PathRule> pathRules = new Vector<>();
    private final Vector<TFRule> tfRules = new Vector<>();
    private final Hashtable<Integer, Rule> rules = new Hashtable<>();  //all rules

    /** this tracks rules that "fire together" and therefore may need
     *  to be merged (i.e., "wire together")
     */
    private final MergeQueue mergeQueue = new MergeQueue();

    /** internal sensors are rule ids of rules that fired in the prev time step */
    private HashSet<Integer> currInternal = new HashSet<>();

    /** external sensors come from the environment */
    private SensorData currExternal;

    /** sensor values from the previous timesteps */
    private final Vector<HashSet<Integer>> prevInternal = new Vector<>();
    private SensorData prevExternal = null;
    private char prevAction = '?';  //last action the agent took

    /** This is the PathRule that best matched at the end of the last completed path */
    private PathRule prevPRMatch = null;

    //These variables track the agent's progress along its currently selected
    // path to goal.  A path is a sequence of TreeNode objs.
    private Vector<TreeNode> pathToDo = null;  //steps yet to take
    private final Vector<TreeNode> pathTraversedSoFar = new Vector<>();  //steps taken

    /** Log the actual path the agent experienced (which can differ from above) */
    private final Vector<TreeNode> actualPath = new Vector<>();

    //Tracks agent performance for debugging  (DEBUG)
    private int stepsSinceGoal = 0;
    private int numGoals = 0;
    Vector<Integer> last10Goals = new Vector<>();
    private String goalPath = ""; //The path the agent has taken since the last goal
    private static long totalTime = 0L;  //track the total time the agent spends calculating



    //Stores the percentage that each sensor is on and relative info to calculate it.
    //HashMap maps sensor name (String) to Tuple containing creation
    // time step (Integer) and percentage (Double).  This is the 'df' for
    // a TFRule's tf-idf calculation.
    private final HashMap<String, Tuple<Integer, Double>> externalPercents = new HashMap<>();
    private final HashMap<String, Tuple<Integer, Double>> internalPercents = new HashMap<>();
    /**
     * It turns out that getting the DF value for an internal sensor is
     * where the agent was spending about 50% of total computing time.
     * So we cache those values in this array for quick access.
     */
    public double[] intPctCache = new double[Rule.getNextRuleId()];


    //These variables track the success rate of random actions
    // (Using double instead of int, so we can calc pct success).
    // The agent uses the percentage to avoid following "hopeless" paths.
    private boolean currPathRandom = false;
    private double numRand = 1.0;
    private double numRandSuccess = 1.0;

//endregion Instance Variables

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
        long startTime = System.currentTimeMillis();  //to monitor total calc time
        this.now++;
        this.currExternal = sensorData;

        //Calculate the internal sensors based upon what the agent just experienced
        this.currInternal = genNextInternal();

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

        // If multiple rules fired together, they are added to the merge queue.
        this.mergeQueue.add(this.currInternal);

        //Regular update to the PathRules
        updatePathRules();

        //DEBUG:  Tell the human what the agent is feeling
        if (PhuJusAgent.DEBUGPRINTSWITCH) {
            debugPrintln("TIME STEP: " + this.now);
            printPrevCurrEpisode();
            printPrevMatchingPathRules();
        }

        //DEBUG: put breakpoints below to debug
        if(this.stepsSinceGoal >= 40) {
            debugPrintln("");
        }
        if (this.now >= 800) {
            debugPrintln("");
        }

        if(TFIDF) {
            //Calculate percentage that each sensor is on
            updateInternalPercents();
            updateExternalPercents();
        }

        if (DEBUGPRINTSWITCH) printAllRules();

        //Update the agent's current path
        pathMaintenance(sensorData.isGoal());

        //extract next action from current path
        char action = calcAction();

        //Use the selected action to set up values for the next iteration
        updateSensors(action);

        //DEBUG
        debugPrintln("----------------------------------------------------------------------");

        //report time spent every 10 goals
        PhuJusAgent.totalTime += System.currentTimeMillis() - startTime;
        if( (stepsSinceGoal == 0) && (numGoals % 10 == 0) ) {
            long hours = PhuJusAgent.totalTime / 3600000;
            long mins = (PhuJusAgent.totalTime % 3600000) / 60000;
            long secs = (PhuJusAgent.totalTime % 60000) / 1000;
            System.out.print("elapsed time: " + totalTime + "ms ");
            System.out.println(hours + ":" + mins + ":" + secs);
        }

        return new Action(action + "");
    }//getNextAction

    /**
     * printAllRules
     *
     * is a DEBUG method to print all the rules the agent has
     */
    private void printAllRules() {
        if (this.now > 2) {
            debugPrintln("TF Rules:");
            for (TFRule tfRule : tfRules) {
                debugPrintln("" + tfRule);
            }

            debugPrintln("Path Rules:");
            for (PathRule pr : this.pathRules) {
                debugPrintln("" + pr);
            }
            debugPrintln("NUM RULES: " + tfRules.size());
        }
    }//printAllRules

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

    /**
     * getBestMatchingWildcardTFRule
     *
     * determines which Operator.ALL TFRule best matches the episode that
     * the agent just completed
     */
    private TFRule getBestMatchingWildcardTFRule() {
        double bestScore = -2.0; //impossibly low score serves as -infinity
        TFRule bestRule = null;
        for(TFRule r : this.tfRules) {
            if (r.getOperator() != TFRule.RuleOperator.ALL) continue;
            double rhsScore = r.rhsMatchScore(this.currExternal);
            if (rhsScore < 0.0) continue;
            //Note:  an empty lhs internal parameter is fine since this is a wildcard rule
            double lhsScore = r.lhsMatchScore(this.prevAction, new HashSet<>(), this.prevExternal);
            double score = rhsScore * lhsScore;
            if (score > bestScore) {
                bestScore = score;
                bestRule = r;
            }
        }

        return bestRule;

    }//getBestMatchingWildcardTFRule

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
        if (this.rules.size() == 0) return result; //no rules yet

        // find the TFRule that best matches the given sensors/actions
        double bestScore = 0.0;
        // Put all the scores in an array, so we don't have to calc them twice (see next loop)
        double[] allScores = new double[this.tfRules.size()];
        for (int i = 0; i < this.tfRules.size(); ++i) {
            TFRule r = tfRules.get(i);
            // ignore rules that didn't predict external sensors correctly
            // TODO:  I don't like using isExtMatch.  This should be a regular
            //        match scoreso that random external sensors can be handled.
            //        Leave it in for now.
            if (!r.isExtMatch(action, prevExt, currExt)) {
                allScores[i] = 0.0;
                continue;
            }
            allScores[i] = r.lhsMatchScore(action, prevInt, prevExt);
            if (allScores[i] > bestScore) {
                bestScore = allScores[i];
            }
        }//for

        //If best score is a mismatch then there will be no internal sensors
        if (bestScore <= 0.0) return result;

        // Turn on internal sensors for all rules that are at or "near" the
        // highest match score.  "near" is currently hard-coded
        // TODO:  have agent adjust "near" based on its experience
        for (int i = 0; i < this.tfRules.size(); ++i) {
            if (allScores[i] <= 0.0) continue;
            if (1.0 - (allScores[i] / bestScore )  <= PhuJusAgent.MATCH_NEAR){
                int ruleId = this.tfRules.get(i).getId();
                result.add(ruleId);
            }
        }

        //If possible least one wildcard rule should be in each internal set
        TFRule bestWildcard = getBestMatchingWildcardTFRule();
        if (bestWildcard != null) {
            result.add(bestWildcard.getId());
        }

        return result;
    }//genNextInternal

    /** convenience version that uses the agent's action + sensors */
    public HashSet<Integer> genNextInternal() {
        HashSet<Integer> result = new HashSet<>();
        if (this.rules.size() == 0) return result; //no rules yet

        return genNextInternal(this.prevAction,
                               this.getPrevInternal(),
                               this.prevExternal,
                               this.currExternal);
    }//genNextInternal

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

    //region PathRule methods

    /**
     * getBestMatchingPathRule
     *
     * determines which PathRule in this.pathRule best matches a given rule
     *
     * @return the matching PR or null if not found
     */
    public PathRule getBestMatchingPathRule(PathRule compareMe) {
        int bestScore = -1;
        PathRule bestPR = null;
        for (PathRule pr : this.pathRules) {
            if (pr.rhsMatch(compareMe)) {
                int score = pr.lhsMatch(compareMe);
                if (score > bestScore) {
                    bestScore = score;
                    bestPR = pr;
                }
            }
        }

        return bestPR;
    }//getBestMatchingPathRule

    /**
     * getBestMatchingPathRule
     *
     * determines which PathRule in this.pathRule best matches a given path
     *
     * @return the matching PR or null if not found
     */
    public PathRule getBestMatchingPathRule(Vector<TreeNode> path) {
        int bestScore = -1;
        PathRule bestPR = null;
        for (PathRule pr : this.pathRules) {
            if (pr.rhsMatch(path)) {
                int score = -1;
                if (pr.lhsSize() == 0) {
                    score = 0;
                } else if (pr.lhsContains(this.prevPRMatch)) {
                    score = 1;
                }
                if (score > bestScore) {
                    bestScore = score;
                    bestPR = pr;
                }
            }
        }

        return bestPR;
    }//getBestMatchingPathRule

    /**
     * incorporateNewPathRule
     *
     * adds a new PathRule to the agent's database as either an active
     * PathRule or a nascent rule in an existing PathRule.
     *
     * @param newbie  the new PR to incoporate
     * @param prMatch the existing PR that newbie is an example for (or null)
     * @param correctPredict  whether this PR reflects a path that was correct or not
     */
    private void incorporateNewPathRule(PathRule newbie, PathRule prMatch, boolean correctPredict) {

        //Case #1: If no existing pathRule matches the newbie then newbie becomes
        // a new base PathRule for the agent
        if (prMatch == null) {
            //if the newbie has an empty LHS then it is already a base rule
            if (newbie.lhsSize() == 0) {
                prMatch = newbie;  //newbie is already a base rule so just use it
            }

            //otherwise, newbie becomes the first example for a new base rule
            else {
                prMatch = new PathRule(this, null, newbie.cloneRHS());
                prMatch.addExample(newbie, correctPredict);
            }

            addRule(prMatch);

        }//if

        //Case #2: the existing match has already split so newbie is a new
        // branch of that split
        else if (prMatch.hasSplit()) {
            prMatch.addExample(newbie, correctPredict);
            addRule(newbie);
        }

        //Case #3: newbie becomes a new example for the existing match
        else {
            prMatch.addExample(newbie, correctPredict);

            //If this rule has a conflict then it needs to be split
            if (prMatch.hasConflict()) {
                prMatch.split();
            }

        }//else

        //Adjust confidence of the active, matching rule
        if (correctPredict) {
            prMatch.adjustConfidence(1.0);
        } else {
            prMatch.adjustConfidence(-1.0);
        }

    }//incorporateNewPathRule

    /**
     * updatePathRules
     *
     * should be called each time step before the agent builds a new path to
     * keep the agent's PathRule set up to date
     */
    private void updatePathRules() {
        //If the agent isn't on a path then there is nothing to do
        if (this.pathTraversedSoFar.size() == 0) return;  //should only happen at this.now==1

        //log what just happened for future use by PathRules
        TreeNode actualTN = new TreeNode(this.actualPath.lastElement(),
                                         this.prevAction,
                                         this.currExternal);
        this.actualPath.add(actualTN);

        //The actualPath begins with the root note in place, so it can be used
        // by the TreeNode ctor.  Once it's been used, it is discarded so that
        // actualPath and pathTraversedSoFar stay in sync
        if (this.actualPath.firstElement().getParent() == null) {
            this.actualPath.remove(0);
        }
        if (this.actualPath.size() != this.pathTraversedSoFar.size()) {
            debugPrintln("ERROR: PJA.actualPath and  PJA.pathTraversedSoFar are not the same length!");
            throw new java.lang.IllegalArgumentException();
        }

        //Create a new PathRule based upon the path steps taken so far
        //Note:  these steps may not match the agent's actual experience!
        PathRule newbie = new PathRule(this, this.prevPRMatch, this.pathTraversedSoFar);
        PathRule prMatch = getBestMatchingPathRule(newbie);
        boolean correctPredict = (this.pathTraversedSoFar.lastElement().getCurrExternal().equals(this.currExternal));
        incorporateNewPathRule(newbie, prMatch, correctPredict);

        //If newbie was wrong, create a second PathRule to reflect what actually happened
        PathRule correctPRMatch = getBestMatchingPathRule(this.actualPath);
        if (!correctPredict) {
            PathRule correctNewbie = new PathRule(this, this.prevPRMatch, this.actualPath);
            incorporateNewPathRule(correctNewbie, correctPRMatch, true);
        }

        //Once the agent's path has been completed, then log the PathRule that
        // matched what actually occurred, so we can use it as the LHS of the
        // next PathRule
        if (this.pathToDo.size() == 0) {
            this.prevPRMatch = correctPRMatch;
        }
    }//updatePathRules

    //endregion PathRule Methods

    //region Path Maintenance Methods

    /**
     * buildNewPath
     *
     * uses a search tree to find a path to goal.  The resulting path is
     * placed in this.pathToDo if found.  Otherwise, this.pathToDO is
     * set to null.
     *
     */
    private void buildNewPath() {

        //Reset the agent's log of the actual path and traversed path
        this.pathTraversedSoFar.clear();
        TreeNode root = new TreeNode(this);

        //Reset the log of actual events that happen (compare to pathTraversedSoFar)
        this.actualPath.clear();
        this.actualPath.add(root); //needed to create subsequent nodes

        //See if we can beat the baseline with the TFRules
        this.pathToDo = root.findBestGoalPath();

        //DEBUG
        if (PhuJusAgent.DEBUGPRINTSWITCH && PhuJusAgent.DEBUGTREESWITCH) {
            root.printTree();
        }
        debugPrintln("random action success rate: " + this.getRandSuccessRate());

        if (this.pathToDo == null) {
            debugPrintln("no path found");

            this.pathToDo = root.findMostUncertainPath();  //this is actually random atm
            this.currPathRandom = true;

            //DEBUG
            debugPrintln("using non-goal path with greatest uncertainty: " + this.pathToDo.lastElement());
        }

        //DEBUG
        else {
            debugPathReport(root, this.pathToDo);

        }//else DEBUG

    }//buildNewPath

    /**
     * debugPathReport
     *
     * provides detailed information about a given path.  This method is
     * used to help humans debug and does nothing for the agent.
     */
    public void debugPathReport(TreeNode root, Vector<TreeNode> path) {
        debugPrintln("found path: " + path.lastElement().getPathStr());
        debugPrintln("\t" + root);
        int count = 1;
        for(TreeNode tn : path) {
            for(Integer ruleId : tn.getCurrInternal()) {
                TFRule tfRule = (TFRule)getRuleById(ruleId);
                for(int i = 0; i < count; ++i) debugPrint("\t");
                debugPrintln("  " + tfRule.toString());
            }
            count++;
            for(int i = 0; i < count; ++i) debugPrint("\t");
            debugPrintln(tn.toString(false));
        }
        debugPrintln(String.format("\t path confidence=%.3f", path.lastElement().getConfidence()));
        debugPrintln("\t adjusted by PathRule: " + path.lastElement().getPathRule());
    }//debugPathReport

    /**
     * pathMaintenance
     *
     * updates the agent's current path information, particularly the pathToDo
     * and prevPath instance variables
     *
     * @param isGoal  did the agent just reach a goal?
     */
    private void pathMaintenance(boolean isGoal) {
        //DEBUG: keep track of agent's steps
        this.goalPath += this.prevAction + "->" + this.currExternal.toStringShort();

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
            System.out.println("Goal Path: " + this.goalPath);
            this.goalPath = "";
            rewardRulesForGoal();

            //Track random success
            if (this.currPathRandom) {
                this.numRand++;
                this.numRandSuccess++;
                this.currPathRandom = false;
            }

            this.stepsSinceGoal = 0;

            buildNewPath();
        } else if ((this.pathToDo.size() == 0)) {
            //DEBUG
            debugPrintln("Current path failed.");

            //Track random action count
            if (this.currPathRandom) {
                this.numRand++;
                this.currPathRandom = false;
            }


            buildNewPath();
        }
        //else {
            //If we reach this point we're partway through a path and no maint is needed

            //TODO: path validation?
            //If the agent is partway through a path but the predicted outcome
            //of it's most recent step has already not been met, then it may
            //make sense to abort the path now and build a new one.  There are two
            //big hurdles for this:
            // 1.  We've tried this before with other agent it caused the agent
            //     to miss some key exploratory experiences and, thus, made it
            //     more vulnerable to getting into loops.
            // 2.  How to be sure a path has failed?  I don't want to rely on
            //     any external sensor being 100% accurate except, perhaps,
            //     the goal sensor.
            //
            // If we can address these issues, then this is the right thing to
            // do.
        //}

    }//pathMaintenance

//endregion Path Maintenance Methods

    //region Sensor Update Methods
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

            for (String s : nameArr) {
                boolean sensorVal = (Boolean) this.currExternal.getSensor(s);
                Tuple<Integer, Double> sensorTuple = this.externalPercents.get(s);

                if (sensorTuple != null) { //We need to adjust the old percent value
                    double newPercent = calculateSensorPercent(sensorTuple.first, sensorTuple.second, sensorVal);
                    sensorTuple.setSecond(newPercent);
                } else { //We just set the percent to either 1.0 or 0 and set the current time
                    double percentage = sensorVal ? 1.0 : 0.0;
                    externalPercents.put(s, new Tuple<>(this.now, percentage));
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

            //This cache is used to speedup lookup
            //TODO:  If this works better to replace the entire HashSet with an array?
            intPctCache = new double[Rule.getNextRuleId()];

            for(Rule rule: rules.values()){
                boolean wasActive = this.currInternal.contains(rule.ruleId);

                Tuple<Integer, Double> sensorTuple = this.internalPercents.get(Integer.toString(rule.ruleId));

                if (sensorTuple != null) { //We need to adjust the old percent value
                    double newPercent = calculateSensorPercent(sensorTuple.first, sensorTuple.second, wasActive);
                    sensorTuple.setSecond(newPercent);
                    intPctCache[rule.ruleId] = newPercent;
                } else { //We just set the percent to either 1.0 or 0 and set the current time
                    double percentage = wasActive ? 1.0 : 0.0;
                    internalPercents.put(Integer.toString(rule.ruleId), new Tuple<>(this.now, percentage));
                    intPctCache[rule.ruleId] = percentage;
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
        double newPercentage;

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

    /**
     * calcAdjustmentScore
     *
     * compares a given rule to the episode the agent just completed to see if
     * it is "close enough" of a match that it should be adjusted as a result.
     *
     * There are series of tests this method tries see inline comments.
     *
     * TODO:  Not sure if this is too aggressive or not enough.  Could be even
     *        more aggressive via a self-tuning hyper-param for min match score.
     *        Not sure how to self-tune.
     *
     * @return a confidence score [-1.0..1.0] of how right/wrong this rule is.
     *         A 0.0 return value means that no adjustment is recommended.
     */
    private double calcAdjustmentScore(TFRule tfRule) {
        //actions must match, of course
        if (tfRule.getAction() != this.prevAction) return 0.0;

        //there has to be some sort of match on the LHS
        if ((tfRule.getOperator() != TFRule.RuleOperator.ALL)
                && (! tfRule.hasOneLHSMatch(this.getPrevInternal())) ) {
            return 0.0;
        }

        //Calculate the match score for each component of the match and
        //make sure they all agree
        double lhsIntScore = tfRule.lhsIntMatchScore(this.getPrevInternal());
        double lhsExtScore = tfRule.lhsExtMatchScore(this.prevExternal);
        double rhsScore = tfRule.rhsMatchScore(this.currExternal);
        if ( (lhsIntScore > 0.0) && (lhsExtScore > 0.0) && (rhsScore > 0.0)) {
            return lhsIntScore * lhsExtScore * rhsScore; //all positive
        }
        if ( (lhsIntScore < 0.0) && (lhsExtScore < 0.0) && (rhsScore < 0.0)) {
            return lhsIntScore * lhsExtScore * rhsScore; //all negative
        }

        return 0.0;
    }//calcAdjustmentScore

    /**
     * updateTFRuleConfidences
     *
     * updates the accuracy (called confidence) of the tfrules that
     * matched correctly in the prev timestep and decreases the rules that
     * didn't match correctly
     *
     */
    public void updateTFRuleConfidences() {
        for(TFRule tfRule : this.tfRules) {
            double adjAmount = calcAdjustmentScore(tfRule);
            if (adjAmount != 0.0) {
                tfRule.adjustConfidence(adjAmount);
            }
        }//for
    }//updateTFRuleConfidences

    //endregion Sensor Update Methods

    //region Rule Maintenance

    /**
     * cullRules
     *
     * if MAXNUMRULES has been reached, this method culls the set down
     * by removing the worst rules.
     *
     * a rules score is calculated by multiplying its activation with its accuracy
     *
     */
    private void cullRules() {
        mergeRules();

        //TODO:  code here to remove rules if there are still too many
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
     * sensors. The first rule is what all the other rules will merge into if they qualify.
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

            // We merge all the rules into the rule with the smallest number of internal sensors, since it's
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
            // If they don't match, add them to the blacklist, so they don't get checked again
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

        removeRule(rule2, rule1);
    }//merge

    /**
     * mergeDuplicates
     *
     * Helper method for merge() which replaces all instances of rule2 in an Integer[] list with rule1.
     */
    private void mergeDuplicates(TFRule rule1, TFRule rule2, Vector<Integer[]> list) {
        for (Integer[] ruleSet : list) {

            // If any of the sets of rules contain rule2, we replace its id with rule1's id
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
     */
    public void updateRuleSet() {
        //Can't do anything in the first timestep because there is no previous timestep
        if (this.now == 1) return;

        //updates the accuracies of our tf rules
        updateTFRuleConfidences();

        //Update the TF values for all extant TFRules
        updateAllTFValues();

        //TODO:  consider this revision to how TFRules are created and used:
        //       Trust Operator.ALL TFRules until their confidence drops. Such rules
        //       would not be limited to MIN_MATCH.  Competing Operator.ANDOR rules would also
        //       not be created until their confidence drops.

        //Create new TF Rule(s) for this latest experience
        // Prevent a new rule being added which is identical to a previous one.
        TFRule newRule = new TFRule(this);
        if (tfRules.size() > 0 && !tfRules.lastElement().isExtMatch(newRule.getAction(), newRule.getLHSExternal(), newRule.getRHSExternal())) {
            addRule(newRule);
        }
        if(! baseRuleExists()) {
            //create a base-event rule
            //TODO: review if these are still needed
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
     */
    private void updateAllTFValues() {
        for(TFRule rule : this.tfRules) {
            double adjScore = calcAdjustmentScore(rule);
            if (adjScore != 0.0) {
                rule.updateTFVals(this.getPrevInternal());
            }
        }
    }//updateAllTFValues

    /** @return whether a base-event rule exists for the agent's most
     * recently completed experience */
    private boolean baseRuleExists() {
        for(TFRule rule : this.tfRules) {
            if (rule.isExtMatch()) {
                if (rule.getOperator() == TFRule.RuleOperator.ALL) {
                    return true;
                }
            }
        }
        return false;
    }//baseRuleExists


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
        if (DEBUGPRINTSWITCH) {
            String prefix = "TF";
            if (newRule instanceof PathRule) {
                prefix = "PR";
            }
            debugPrintln("added " + prefix + ": " + newRule);
        }
    }//addRule

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
        tfRules.remove(removeMe);


        //DEBUGGING
        if (replacement == null) debugPrint("removed: ");
        else debugPrint("replaced: ");

        //Removes the data from the sensor percentage HashMap
        removeInternalSensorPercent(removeMe.getId());

        // If any rule has a condition that test for 'removeMe' then that
        // condition must also be removed or replaced
        for (Rule rule : this.rules.values()) {
            if(rule instanceof TFRule) {
                TFRule r = (TFRule) rule;
                if (r.testsIntSensor(removeMe.getId())) {
                    int replId = (replacement == null) ? -1 : replacement.getId();
                    r.replaceIntSensor(removeMe.getId(), replId);
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

        //update PathRules that used this rule (old version below)
        for(PathRule pr : this.pathRules) {
            pr.replaceAll(removeMe, replacement);
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
        double actBoost = 1.0;
        for(int i = this.pathTraversedSoFar.size() - 1; i >= 0; --i) {
            TreeNode tn = this.pathTraversedSoFar.get(i);
            for(TFRule r : tn.getSupporters()) {
                r.addActivation(this.now, actBoost);
            }
            actBoost *= Rule.DECAY_RATE;
        }
    }//rewardRulesForGoal

//endregion Rule Maintenance

    //region Debug Printing Methods

    /** helper method for {@link #printPrevCurrEpisode} */
    private void printIntHelper(HashSet<Integer> printMe, StringBuilder sb) {
        int count = 0;
        for (Integer i : printMe) {
            if (count > 0) sb.append(", ");
            sb.append(i);
            count++;
        }
    }

    /** prints the agent's previous (just completed) and current (in progress)
     * episode to help the human better understand what's going on. */
    private void printPrevCurrEpisode() {
        StringBuilder sbPrev = new StringBuilder();
        sbPrev.append("  Completed Episode: ");

        //prev internal
        sbPrev.append("(");
        if (this.prevInternal.size() > 0) {
            printIntHelper(getPrevInternal(), sbPrev);
        }
        sbPrev.append(")");
        int lhsBarPos = sbPrev.length();
        sbPrev.append("|");


        //Sort external sensor names alphabetical order with GOAL last
        Vector<String> sNames = new Vector<>(this.currExternal.getSensorNames());
        Collections.sort(sNames);
        int i = sNames.indexOf(SensorData.goalSensor);
        if (i > -1) {
            sNames.remove(i);
            sNames.add(SensorData.goalSensor);
        }

        //prev external
        if (this.prevExternal != null) {
            for (String sName : sNames) {
                int val = ((Boolean) this.prevExternal.getSensor(sName)) ? 1 : 0;
                sbPrev.append(val);
            }
        } else {
            sbPrev.append("xx");
        }

        //action
        sbPrev.append(this.prevAction);
        sbPrev.append(" -> ");
        int lhsLen = sbPrev.length();  //save this so we can line up the two eps

        //curr external
        for (String sName : sNames) {
            int val = ((Boolean)this.currExternal.getSensor(sName)) ? 1 : 0;
            sbPrev.append(val);
        }

        //print sensor names after
        sbPrev.append("\t\t");
        boolean comma = false;
        for (String sName : sNames) {
            if (comma) sbPrev.append(", ");
            comma = true;
            sbPrev.append(sName);
        }

        StringBuilder sbCurr = new StringBuilder();
        sbCurr.append("Episode in Progress: ");

        //curr internal
        sbCurr.append("(");
        printIntHelper(this.currInternal, sbCurr);
        sbCurr.append(")");

        //Lineup the '|' chars on the LHS
        while(sbCurr.length() < lhsBarPos) sbCurr.append(" ");
        while(sbCurr.length() > lhsBarPos) {
            sbPrev.insert(lhsBarPos, " ");
            lhsBarPos++;
        }
        sbCurr.append("|");

        //curr external
        for (String sName : sNames) {
            int val = ((Boolean)this.currExternal.getSensor(sName)) ? 1 : 0;
            sbCurr.append(val);
        }

        //action
        sbCurr.append("? -> ");

        //make the arrows line up
        while(sbCurr.length() < lhsLen) {
            sbCurr.insert(21, " ");
        }

        debugPrintln(sbPrev.toString());
        debugPrintln(sbCurr.toString());


    }//printPrevCurrEpisode


    /** DEBUG: prints the matching PathRule */
    private void printPrevMatchingPathRules () {
        debugPrint("  Matching PathRule: ");
        if (this.prevPRMatch == null) {
            debugPrintln("none");
        } else {
            debugPrintln("" + this.prevPRMatch.getId());
        }
    }

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
    public Rule getRuleById(int id) { return this.rules.get(id); }
    public Vector<TFRule> getTfRules() { return tfRules; }
    public int getNow() { return now; }
    public HashSet<Integer> getCurrInternal() { return this.currInternal; }
    /** returns the most recent */
    public HashSet<Integer> getPrevInternal() { return this.prevInternal.lastElement(); }
    public SensorData getCurrExternal() { return this.currExternal; }
    public void setCurrExternal(SensorData curExtern) { this.currExternal = curExtern; }
    public SensorData getPrevExternal() { return this.prevExternal; }
    public Action[] getActionList() { return actionList; }
    public char getPrevAction() { return prevAction; }
    public HashMap<String, Tuple<Integer, Double>> getExternalPercents() {return this.externalPercents;}
    public double getRandSuccessRate() { return this.numRandSuccess / this.numRand; }
    //endregion

}//class PhuJusAgent
