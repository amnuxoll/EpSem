package agents.phujus;

import framework.SensorData;

import java.util.*;

/**
 * class TreeNode
 * <p>
 * Each instance is a node in an N-ary tree where N is the number of actions (i.e., the FSM's
 * alphabet) that tries to predict outcomes of sequences of actions.  Thus, we can find a
 * "best" (shortest, most confident) sequence of actions to reach the goal
 */

public class TreeNode {

    //Agent's current state and ruleset is needed to build the tree
    private final PhuJusAgent agent;

    // all rules in the system
    Hashtable<Integer, Rule> rules;

    //associated timestep for this node
    private final int episodeIndex;

    //the action that led to this node
    private final char action;

    //sensor values for this node
    private final Vector<HashSet<TFRule>> currInternal;
    private final SensorData currExternal;

    //The TreeNode that preceded this one in time
    private final TreeNode parent;

    //child nodes
    private final Vector<TreeNode> children = new Vector<>();

    //bool for if tree has children or not
    private boolean isLeaf = false;

    //This is the path used to reach this node
    private final Vector<TreeNode> path;

    //The same path expressed as a String of actions
    private final String pathStr;

    //A measure from 0.0 to 1.0 of how confident the agent is that this path is correct
    private final double confidence;

    //A list of all possible external sensor combinations for this agent
    // e.g., 000, 001, 010, etc.
    private static String[] comboArr = null;  //init'd by 1st ctor call

    //These are TFRules which most strongly supported the existence of this TreeNode
    private HashSet<TFRule> supporters;

    //The PathRule best matches the path described by this TreeNode, presuming
    // it is the last step in its path.  This can be 'null' if no PathRule applies.
    private PathRule pathRule;

    //The time depth of this tree node is determined by the rules used to create it
    private int timeDepth;


    /**
     * This root node constructor is built from the agent.
     *
     */
    public TreeNode(PhuJusAgent initAgent) {
        //initializing agent and its children
        this.agent = initAgent;
        this.rules = agent.getRules();
        this.parent = null;
        this.episodeIndex = agent.getNow();
        this.action = '\0';
        this.currInternal = agent.getCurrInternal();
        this.currExternal = agent.getCurrExternal();
        this.path = new Vector<>();
        this.pathStr = "";
        //full confidence because drawn directly from agent's present sensing
        this.confidence = 1.0;
        supporters = new HashSet<>(); //none
        this.pathRule = null;
        this.timeDepth = 0;  //this is a root node

        if (TreeNode.comboArr == null) {
            TreeNode.initComboArr(this.currExternal.size());
        }
    }

    /**
     * constructs a TreeNode with given sensors and action
     * @param parent the TreeNode that precedes this one in a path
     * @param action action taken to reach this node
     * @param newExternal predicted external sensors for this node
     * @param confidence confidence in newExternal
     * @param initSupporters  which TFRules strongly support this TreeNode
     *
     */
    public TreeNode(TreeNode parent,
                    char action,
                    SensorData newExternal,
                    double confidence,
                    HashSet<TFRule> initSupporters) {
        //initializing agent and its children
        this.agent = parent.agent;
        this.rules = parent.rules;
        this.parent = parent;
        this.episodeIndex = parent.episodeIndex + 1;
        this.action = action;
        this.currInternal = agent.genNextInternal(action, parent.currInternal, parent.currExternal, newExternal);
        this.currExternal = newExternal;
        this.path = new Vector<>(parent.path);
        this.path.add(this);
        this.pathStr = parent.pathStr + action;
        this.confidence = parent.confidence * confidence;
        this.supporters = initSupporters;
        this.pathRule = null;
        this.timeDepth = parent.timeDepth + 1;
    }//child ctor

    /** convenience version of the child ctor that fills in some default values */
    public TreeNode(TreeNode parent,  char action, SensorData newExternal) {
        this(parent, action, newExternal, 1.0, new HashSet<>());
    }

    /**
     * copy ctor
     */
    public TreeNode(TreeNode orig) {
        //initializing agent and its children
        this.agent = orig.agent;
        this.rules = orig.rules;
        this.parent = orig.parent;
        this.episodeIndex = orig.episodeIndex;
        this.action = orig.action;
        this.currInternal = new Vector<>();
        for(int depth = 0; depth < orig.currInternal.size(); ++depth) {
            this.currInternal.add(new HashSet<>(orig.getCurrInternal(depth)));
        }
        this.currExternal = new SensorData(orig.currExternal);
        this.path = new Vector<>(orig.path);
        this.pathStr = orig.pathStr;
        this.confidence = orig.confidence;
        this.supporters = new HashSet<>(orig.supporters);
        this.pathRule = orig.pathRule;
        this.timeDepth = orig.timeDepth;
    }//copy ctor

    /**
     * initComboArr
     *
     * initializes TreeNode.comboArr static variable
     *
     * @param numExt  number of external sensors the agent has
     */
    private static void initComboArr(int numExt) {
        //Create arrays to store a string rep and confidence value for each possible sensor combination
        int numCombos = (int)Math.pow(2, numExt);
        TreeNode.comboArr = new String[numCombos];
        for(int i = 0; i < numCombos; ++i) {
            StringBuilder combo = new StringBuilder(Integer.toBinaryString(i));
            while (combo.length() < numExt) {
                combo.insert(0, "0");
            }
            TreeNode.comboArr[i] = combo.toString();
        }
    }//initComboArr


    /**
     * class BlocData
     *
     * One instance of this class corresponds to a single sensor in the
     * external sensor array.  It tracks the total votes from each voting
     * bloc as well as the max voter from each block.
     */
    public static class BlocData {
        public String sensorName;

        //Each bloc (e.g., 00a, 10b, 11a, etc.) its most confident constituent's vote stored here
        //There is a separate map for on vs. off votes
        public HashMap<String, Double> onVotes = new HashMap<>();
        public HashMap<String, Double> offVotes = new HashMap<>();

        public int offVoteCount = 0;
        public int onVoteCount = 0;

        //The TFRule with the best match score voting for on and off respectively
        public TFRule maxOnVoter = null;
        public TFRule maxOffVoter = null;

        public BlocData(String initName) {
            this.sensorName = initName;
        }

        /** scales a vote's weight on an exponential scale so that (near)
         * perfect match scores get much more weight than partial matches */
        //TODO:  revisit this.  Given the new match method it may be extraneous or even harmful.
        public double scale(double matchScore) {
            //We want higher math scores to have exponentially higher weight so
            //that a (near-)perfect match will outweigh many partial matches but
            //partial matches can still have enough weight to make a difference
            //The formula 2^(2*r) - 1 gives an appropriately steep curve that
            // starts at zero.
            double result = matchScore * 2.0;  //doing this makes the max resulting score 1.0
            result = Math.pow(2.0, result);
            result -= 1.0;  //This makes min score 0.0
            return result;
        }//BlocData.scale

        /** registers one vote */
        public void vote(TFRule voter, double matchScore) {
            if (matchScore <= 0.0) return;  //ignore weightless vote

            //scale the score for a vote weight
            double weight = scale(matchScore);

            //Calculate the voting bloc this rule is in
            String bloc = voter.getLHSExternal().toString(false);

            //Add the vote to the proper vote sum
            boolean on = (boolean) voter.getRHSExternal().getSensor(this.sensorName);
            HashMap<String, Double> ballotBox = on ? this.onVotes : this.offVotes;
            if (on) { this.onVoteCount++; } else { this.offVoteCount++; }
            double oldVal = 0.0;

            //is the new match score the best yet?
            boolean replace = true;
            if (ballotBox.containsKey(bloc)) {
                oldVal = ballotBox.get(bloc);
                if (weight < oldVal) replace = false;
            }

            //Put the new max vote in
            if (replace) {
                if (on) {maxOnVoter = voter;} else {maxOffVoter = voter;}
                ballotBox.put(bloc, weight);
            }

        }//BlocData.vote


        /**
         * Calculates how confident the node is that this sensor is on or off
         *
         * @return an array containing the confidence values for off and on respectively
         */
        public double[] getOutcomes() {
            //0=off confidence,  1=on confidence
            double[] outArr = new double[2];

            //Calculate the vote totals and max vote for on and off
            double onVotes = 0.0;
            double offVotes = 0.0;
            double onVoteMax = 0.0;
            double offVoteMax = 0.0;
            for (double d : this.onVotes.values()) {
                onVotes += d;
                if (d > onVoteMax) onVoteMax = d;
            }
            for (double d : this.offVotes.values()) {
                offVotes += d;
                if (d > offVoteMax) offVoteMax = d;
            }

            //Calculate the confidences
            if(onVotes + offVotes == 0) {
                outArr[0] = 0;
                outArr[1] = 0;
            } else {
                int totalVotes = this.onVoteCount + this.offVoteCount;
                if (totalVotes == 0) {
                    outArr[0] = 0;
                    outArr[1] = 0;
                } else {
                    outArr[0] = (offVoteMax * this.offVoteCount) / totalVotes;
                    outArr[1] = (onVoteMax * this.onVoteCount) / totalVotes;
                }
            }
            return outArr;
        }//BlocData.getOutcomes


    }//Class BlocData

    /**
     * class VoteOutcome
     *
     * is a tiny public class to describe the outcome of a TFRules voting for
     * the predicted external sensor values in {@link #predictExternalMark3 }
     */
    private class VoteOutcome {
        public SensorData ext = new SensorData(false);  //the predicted data
        public double confidence = 1.0;                       //how confident the agent should be in this
        public HashSet<TFRule> supporters = new HashSet<>();     //The TFRules that most strongly supported this
    }

    /**
     * castVotes
     *
     * casts votes for various possible external sensor combinations given
     * an action and a required rule depth
     *
     */
    private HashMap<String, BlocData> castVotes(char action, int depth) {
        //Get a list of external sensor names
        String[] sensorNames = currExternal.getSensorNames().toArray(new String[0]);

        //Create a BlocData object to track the votes for each sensor
        HashMap<String, BlocData> votingData = new HashMap<>();
        for(String sName : sensorNames) {
            votingData.put(sName, new BlocData(sName));
        }

        Vector<Vector<TFRule>> tfRules = this.agent.getTfRules();
        Vector<TFRule> ruleSubList = tfRules.get(depth);
        //The flat version is needed for matching
        HashSet<Integer> flatCurrInt = PhuJusAgent.flattenRuleSet(this.currInternal);
        for (TFRule rule : ruleSubList) {
            if (rule.getAction() != action) continue;
            double score = rule.lhsMatchScore(action, flatCurrInt, this.currExternal);
            score *= rule.getConfidence();
            if (score <= 0.0) continue;  //skip rules with unsupportive match scores

            //Record this rule's vote for each sensor
            for (String sName : sensorNames) {
                BlocData bd = votingData.get(sName);
                bd.vote(rule, score);
            }
        }//for

        return votingData;
    }//castVotes

    /**
     * getVoteOutcomes
     *
     * examines the voting data from {@link #castVotes} and creates a resulting
     * array of VoteOutcome objects
     */
    private VoteOutcome[] getVoteOutcomes(HashMap<String, BlocData> votingData) {
        //Create an array to store a confidence value for each possible sensor combination
        VoteOutcome[] confArr = new VoteOutcome[TreeNode.comboArr.length];
        for(int i = 0; i < confArr.length; ++i) {
            confArr[i] = new VoteOutcome();
        }

        //Create a VoteOutcome object for each external sensor combination
        String[] sensorNames = currExternal.getSensorNames().toArray(new String[0]);
        for (int sId = 0; sId < sensorNames.length; ++sId) {
            //get the confidences in this sensor's values
            String sName = sensorNames[sId];
            BlocData bd = votingData.get(sName);
            double[] outcomes = bd.getOutcomes();

            //Update all sensor combos for this sensor
            for (int i = 0; i < confArr.length; ++i) {
                if (TreeNode.comboArr[i].charAt(sId) == '0') { //false
                    confArr[i].ext.setSensor(sName, false);
                    confArr[i].confidence *= outcomes[0];
                    if (bd.maxOffVoter != null) confArr[i].supporters.add(bd.maxOffVoter);
                } else {  //true
                    confArr[i].ext.setSensor(sName, true);
                    confArr[i].confidence *= outcomes[1];
                    if (bd.maxOnVoter != null) confArr[i].supporters.add(bd.maxOnVoter);
                }
            }//for

            //TODO:  experiment with giving a 1.0 score to nodes for which
            //       only base rules have voted to encourage curiosity


        }//for

        //Scale the confidence values to the range [0.0..1.0]
        //This makes the agent more likely to consider longer paths
        //which ends up being an important nudge toward learning.
        //TODO:  re-test this theory once agent is performing well overall
        double max = 0.0;
        for(VoteOutcome outcome : confArr) {
            max = Math.max(max, outcome.confidence);
        }
        for(int i = 0; i < confArr.length; ++i) {
            if (max == 0) {
                confArr[i].confidence = 0.0;
            } else {
                confArr[i].confidence /= max;
            }
        }
        return confArr;
    }//getVoteOutcomes

    /**
     * predictExternalMark3
     *
     * This is a re-revised version of a method to predict the external sensors
     * that will result from taking a selected action.  It calculates the
     * confidence for all possible ext sensor combos as defined in
     * TreeNode.comboArray.  This method is super complicated, but I think
     * it provides the best confidence calculation (at least until Mark 4...)
     *
     * CAVEAT:  The sensor combos are based on the sensor names in alphabetical
     *          order so GOAL may not be the last sensor as it appears in the toString
     *
     * Note:  This method assumes at least one rule exists at the current time depth
     *
     * @param action  the action taken to reach the child
     * @return an array of {@link VoteOutcome} objects for each candidate
     *         external sensor combo
     */
    public VoteOutcome[] predictExternalMark3(char action) {
        // Record the vote from each matching rule at this depth for each sensor
        HashMap<String, BlocData> votingData = castVotes(action, this.timeDepth);

        //Generate a summary of the outcome
        return getVoteOutcomes(votingData);

    }//predictExternalMark3



    /**
     * expand
     *
     * populates this.children
     *
     * a(1) -> 00
     *   ..
     *     ..
     * a(2) -> 10
     *   ..
     *     ..
     *       .. -> 01*
     */
    private void expand() {
        //check:  already expanded
        if (this.children.size() != 0) return;

        //check: no rules available at this depth
        if (agent.getTfRules().size() <= this.timeDepth) return;

        int numActions = agent.getActionList().length;

        // bloc = lhs external sensors + action
        //  00a, 00b, 01a, 01b, etc.

        //Create predicted child nodes for each possible action
        for(int actId = 0; actId < numActions; actId++) {

            char act = agent.getActionList()[actId].getName().charAt(0);

            //Calculate the confidence in each combination
            VoteOutcome[] voteOutcomes = predictExternalMark3(act);

            for(VoteOutcome outcome : voteOutcomes) {
                //agent must be more confident in this path than just taking a random action
                if (outcome.confidence <= this.agent.getRandSuccessRate()) continue;

                //create a child for this action + ext sensor combo
                TreeNode child = new TreeNode(this, act, outcome.ext,
                                               outcome.confidence, outcome.supporters);
                this.children.add(child);
            }//for
        }//for

    }//expand

    /**
     * cullWeakChildren
     *
     * remove any children from this node whose overall confidence (after adjustment by a PathRule)
     */
    private void cullWeakChildren() {
        //Find the matching pathRule for each child and use it to calc overall conf
        Vector<TreeNode> removeThese = new Vector<>();  //children to be removed
        Vector<TreeNode> partialPath = new Vector<>();
        TreeNode tmp = this;
        while(tmp.parent != null) {
            partialPath.add(tmp);
            tmp = tmp.parent;
        }
        for(TreeNode child : this.children) {
            //Find the matching pathrule
            partialPath.add(child);
            child.pathRule = agent.getBestMatchingPathRule(partialPath);
            //note: don't use partialPath.remove(child) as it relies on equals()
            double overallScore = calcOverallScore(partialPath);
            partialPath.remove(partialPath.size() - 1);

            //If the overall confidence is too low, mark the child for removal
            if (overallScore < agent.getRandSuccessRate()) {
                removeThese.add(child);
            }
        }//for

        //Remove the too-weak children.  Gruesome, no?
        for(TreeNode child : removeThese) {
            this.children.remove(child);
        }
    }//cullWeakChildren

    /**
     * calcOverallScore
     *
     * A path has an overall score based on these factors:
     * 1.  the path confidence (based on the TFRule confidences)
     * 2.  the matching PathRule's confidence
     * 3.  the path's length (longer paths are less certain)
     *
     */
    private double calcOverallScore(Vector<TreeNode> foundPath) {
        //the score starts with a base confidence
        TreeNode lastEl = foundPath.lastElement();
        double foundScore = lastEl.confidence;

        //adjust score using the matching PathRule
        if (lastEl.pathRule != null) {
            foundScore *= lastEl.pathRule.getConfidence();
        }

        //Adjust based on path length.  This is based on the Sunrise problem in probability.
        foundScore *= (1.0 / (foundPath.size()));

        return foundScore;
    }//calcOverallScore


    /**
     * fbgpHelper
     *
     * recursive helper method for {@link #findBestGoalPath}
     *
     * @param depth  depth of this node
     * @param maxDepth maximum depth allowed
     *
     * @return a path to the goal (or null if not found)
     */
    private Vector<TreeNode> fbgpHelper(int depth, int maxDepth) {
        //base case:  found goal
        if (this.isGoalNode()) {
            this.isLeaf = true;
            return this.path;
        }

        //base case:  max depth
        if (depth >= maxDepth) {
            this.isLeaf = true;
            return null;
        }

        /* ====================================
           Recursive case: examine child nodes
           ------------------------------------ */

        //Keeps track of the best path we've seen so far
        Vector<TreeNode> bestPath = null;
        double bestScore = 0.0;

        //Create the child nodes if they don't exist yet
        if (this.children.size() == 0) {
            expand();
            //Remove children that aren't better than random
            cullWeakChildren();
        } else {
            for (TreeNode child : this.children) {
                child.isLeaf = false; //reset from any prev use of this child
            }
        }

        for(TreeNode child : this.children) {
            //Recursive case: test all children and return shortest path
            Vector<TreeNode> foundPath = child.fbgpHelper(depth + 1, maxDepth);

            if ( foundPath != null ) {
                double foundScore = calcOverallScore(foundPath);

                //best so far?
                if (foundScore > bestScore) {
                    bestPath = foundPath;
                    bestScore = foundScore;
                }
            }
        }//for

        return bestPath;

    }//fbgpHelper

    /**
     * findBestGoalPath
     * <p>
     * uses an iterative deepening search tree to find a path to the goal
     *
     * //TODO:  Could do A*Search instead if this is too slow
     */
    public Vector<TreeNode> findBestGoalPath() {
        double bestScore = 0.0;
        Vector<TreeNode> bestPath = null;
        for(int max = 1; max <= PhuJusAgent.MAX_SEARCH_DEPTH; ++max) {
            Vector<TreeNode> path = fbgpHelper(0, max);
            if (path != null) {
                path.lastElement().pathRule = agent.getBestMatchingPathRule(path);
                double foundScore = calcOverallScore(path);
                if (foundScore < agent.getRandSuccessRate()) continue;

                if(foundScore > bestScore) {
                    bestPath = path;
                    bestScore = foundScore;
                }
            }
        }

        return bestPath;  //null if failed to find a path to goal
    }//findBestGoalPath

    /**
     * findMostUncertainPath
     *
     * is called on a completed tree in which no path to GOAL was found.
     * It selects a relatively short path with a high degree of
     * uncertainty about the outcome.
     *
     * TODO:  This code is only looking at paths of length 1 right now
     *        Consider this case:
     *           ->(3, 20, 21, 33, 34)|00	c1.0
     *              a->()|00	c0.563817
     *                  aa->()|00	c0.317515
     *                  ab->(3, 20, 21, 33, 34)|00	c0.447692
     *               b->(3, 15, 20, 21, 33, 34, 35)|00	c0.817221
     *                   ba->()|00	c0.005266
     *                   bb->(3, 15, 34, 35)|00	c0.670597
     *          Shouldn't path 'ba' be selected?  Some code below
     *          attempted to do that but has been commented out
     * @return the path found
     */
    public Vector<TreeNode> findMostUncertainPath() {

        int i = PhuJusAgent.rand.nextInt(agent.getActionList().length);
        char action = agent.getActionList()[i].getName().charAt(0);
        TreeNode random = new TreeNode(this, action, this.currExternal);
        return random.path;

//        //pick the action (depth 1) with the greatest uncertainty
//        char mostUncertain = '\0';
//        double lowestConfSoFar = 1.1; //can't be this high
//        for (TreeNode child : this.children) {
//            //see if this child has more uncertainty
//            if (child.confidence < lowestConfSoFar) {
//                lowestConfSoFar = child.confidence;
//                mostUncertain = child.action;
//            }
//        }
//
//        //Just in case:  what if there are no children?
//        if (mostUncertain == '\0') {
//            mostUncertain = agent.getActionList()[rand.nextInt(agent.getActionList().length)].getName().charAt(0);
//        }
//
//        //Return a path with this action
//        TreeNode tmpNode = new TreeNode(this, mostUncertain, this.currExternal, 1.0);
//        return tmpNode.path;


        // TODO: Make code function how we want. Currently commented out until we get it working
       /* Vector<TreeNode> toSearch = new Vector<>();
        toSearch.add(this);

        double bestScore = -99.9;  //no score can be this low
        Vector<TreeNode> bestPath = null;
        while(toSearch.size() > 0) {
            TreeNode curr = toSearch.remove(0);
            double invConf = 1.0 - curr.confidence;
            if (curr.path.size() > 0) {  //node with no path can't be selected
                double score = (1.0 / ((double) curr.path.size())) * invConf;
                if (score > bestScore) {
                    bestScore = score;
                    bestPath = curr.path;
                }
                else if(score == bestScore) {

                    if(rand.nextBoolean()){
                        bestScore = score;
                        bestPath = curr.path;
                    }
                }
            }

            if (curr.children.size() == this.agent.getActionList().length) {
                //queue children to be searched
                for (TreeNode child : curr.children) {
                    if (child != null) toSearch.add(child);
                }
            //else if not at a leaf node...
            } else if (curr.path.size() < PhuJusAgent.MAX_SEARCH_DEPTH) {
                //If we reach this point, there is an action that has never been
                // taken in this scenario.  So find that action and build a path
                // with it.
                ArrayList<String> unusedActionsArr = new ArrayList<String>();
                int numUnusedActions = 0;
                for (int i = 0; i < agent.getActionList().length; i++) {
                    char action = agent.getActionList()[i].getName().charAt(0);
                    boolean found = false;
                    for(TreeNode child : curr.children) {
                        if (child.getAction() == action) {
                            found = true;
                            break;
                        }
                    }
                    //found an unused action
                    if(!found) {
                        unusedActionsArr.add(action + "");
                        numUnusedActions++;
                    }
                }
                if (numUnusedActions > 0) {
                    //create a fake tree node with this unused action so
                    // that there will be a path that uses it
                    // TODO:  Is this a mistake?

                    char action = unusedActionsArr.get(rand.nextInt(numUnusedActions)).charAt(0);
                    TreeNode fake = new TreeNode(curr, action, this.currExternal, 1.0);
                    return fake.path;

                }
            }

        }//while

        return bestPath; */
    }//findMostUncertainPath


    /**
     * sortedKeys
     *
     * @return a sorted sorted Vector of the rule ids of a given set of TFRules
     */
    private Vector<Integer> sortedKeys(HashSet<TFRule> internal) {
        Vector<Integer> result = new Vector<>();
        for(TFRule r : internal) {
            result.add(r.getId());
        }
        Collections.sort(result);
        return result;
    }

    /**
     * sortedKeys
     *
     * @return a sorted Vector of the Integer keys in an external sensor SensorData.
     * The GOAL sensor is always put at the end, however.
     */
    private static Vector<String> sortedKeys(SensorData external) {
        Vector<String> result = new Vector<>(external.getSensorNames());
        Collections.sort(result);

        //Move the goal to the end
        if (result.contains(SensorData.goalSensor)) {
            result.remove(SensorData.goalSensor);
            result.add(SensorData.goalSensor);
        }

        return result;
    }//sortedKeys

    /**
     * extToString
     *
     * a shortened bit-based toString() method for SensorData
     */
    public static String extToString(SensorData stringMe) {
        StringBuilder result = new StringBuilder();
        for (String s : sortedKeys(stringMe)) {
            Boolean val = (Boolean) stringMe.getSensor(s);
            result.append(val ? "1" : "0");
        }
        return result.toString();
    }//extToString

    /**
     * toString
     * <p>
     * creates a terse ASCII representation of this node.  The child nodes are not depicted.
     */
    public String toString(boolean includeConf) {
        StringBuilder result = new StringBuilder();

        //the actions that led to this node
        result.append(this.pathStr);
        result.append("->");

        //Internal Sensors
        result.append("(");
        int count = 0;
        for(int depth = 0; depth < this.currInternal.size(); ++depth) {
            HashSet<TFRule> subSet = getCurrInternal(depth);
            for (int i : sortedKeys(subSet)) {
                if (count > 0) {
                    result.append(", ");
                }
                count++;
                result.append(i);
            }
        }
        result.append(")|");

        //External Sensors
        result.append(TreeNode.extToString(this.currExternal));

        //Confidence
        if (includeConf) {
            result.append("\tc");
            //note:  replaceAll call removes extra trailing 0's to improve readability
            result.append(String.format("%.6f", this.confidence).replaceAll("0+$", "0"));
        }

        return result.toString();
    }

    @Override
    public String toString() {
        return toString(true);
    }


    /**
     * prints the tree in an "easily" readable format.  This method is the front facing interface.
     * {@link #printTreeHelper} is the recursive helper method that traverses the tree
     */
    public void printTree() {
        //print the nodes recursively
        printTreeHelper("");
    }

    /**
     * printTreeHelper
     * <p>
     * is the helper method for {@link #printTree}
     *
     * @param indent how much to indent any output from this method
     */
    private void printTreeHelper(String indent) {
        System.out.print(indent + "  " + this);

        //base case #1: Goal Node found (not at root)
        if ( isGoalNode() ) {
            System.out.println("*");
            return;
        }

        //base case #2:  Leaf Node (no goal found)
        if (this.isLeaf) {
            System.out.println();
            return;
        }

        System.out.println();

        //recursive case: print child nodes
        for(TreeNode child : this.children) {
            child.printTreeHelper(indent + "   ");
        }
    }//printTreeHelper

    /**
     * isGoalNode
     *
     * @return true if this node's external sensors include GOAL=true
     */
    public boolean isGoalNode() {
        return ( (this.currExternal.getSensor(SensorData.goalSensor) != null)  //goal sensor is present
                && (this.currExternal.isGoal())  //goal sensor is true
                && (this.parent != null) );   //this isn't the root
    }

    /**
     * nearEquals
     *
     * This method is like {@link #equals } but it only requires that the
     * other node have at least one LHS internal sensor in common.  This method
     * is used directly by {@link PathRule#matchLen}
     *
     * @param other  TreeNode obj to compare to this one
     *
     * TODO:  should this be a tfidf-style partial match instead?
     */
    public boolean nearEquals(TreeNode other) {

        //compare actions
        if (! this.pathStr.equals(other.pathStr)) return false;

        //compare external sensors.
        // Note: if they are both goal nodes, the other ext sensors don't
        //       need to match.
        if ( (!other.isGoalNode()) || (!this.isGoalNode()) ) {
            if (!other.currExternal.equals(this.currExternal)) {
                return false;
            }
        }

        //An empty internal set is treated as a wildcard match
        //(not 100% sure about this)
        if ((this.currInternal.size() == 0) || (other.currInternal.size() == 0)) {
            return true;
        }

        //look for one internal sensor in common
        for(int depth = 0; depth < this.currInternal.size(); ++depth) {
            HashSet<TFRule> subset = getCurrInternal(depth);
            HashSet<TFRule> otherSubSet = other.getCurrInternal(depth);
            for (TFRule r : subset) {
                if (otherSubSet.contains(r)) return true;
            }
        }
        return false; //no luck

    }//nearEquals

    public char getAction() { return this.pathStr.charAt(this.pathStr.length() - 1); }
    public String getPathStr() { return this.pathStr; }
    public Vector<HashSet<TFRule>> getCurrInternal() { return this.currInternal; }
    public HashSet<TFRule> getCurrInternal(int timeDepth) {
        if (this.currInternal.size() > timeDepth) {
            return this.currInternal.get(timeDepth);
        }
        return new HashSet<>();
    }
    public SensorData getCurrExternal() { return this.currExternal; }
    public TreeNode getParent() { return this.parent; }
    public double getConfidence() { return this.confidence; }
    public HashSet<TFRule> getSupporters() { return this.supporters; }
    public PathRule getPathRule() { return this.pathRule; }


}//class TreeNode
