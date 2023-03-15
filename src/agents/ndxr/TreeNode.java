package agents.ndxr;

import java.util.*;
import framework.SensorData;
import agents.ndxr.RuleIndex.MatchResult;

/**
 * class TreeNode
 * <p>
 * This class is a descendent of NdxrAgent.TreeNode
 * <p>
 * Each instance is a node in an N-ary tree where N is the number of actions (i.e., the FSM's
 * alphabet) that tries to predict outcomes of sequences of actions.  Thus, we can find a
 * "best" (shortest, most confident) sequence of actions to reach the goal
 */

public class TreeNode {
    //Agent's current state
    private final NdxrAgent agent;

    // all rules in the system
    RuleIndex rules;

    //associated timestep for this node (i.e., "now")
    private final int episodeIndex;

    //the action that led to this node
    private final char action;

    //sensor values for this node
    private final Vector<Rule> prevRules;
    private final SensorData currExternal;

    //The TreeNode that preceded this one in time
    private final TreeNode parent;

    //child nodes
    private final Vector<TreeNode> children = new Vector<>();

    //bool for if tree has children or not
    private boolean isLeaf = false;

    //This is the path used to reach this node
    private final Vector<TreeNode> path;

    //The same path as above expressed as a String of action chars
    private final String pathStr;

    //A measure from 0.0 to 1.0 of how confident the agent is that this path is correct
    private final double confidence;

    //A list of all possible external sensor combinations for this agent
    // e.g., 000, 001, 010, etc.
    //TODO:  this is not sustainable.  Do something else.
    private static String[] comboArr = null;  //init'd by 1st ctor call

    //These are Rules which most strongly supported the existence of this TreeNode
    private Vector<Rule> supporters;

    //The PathRule best matches the path described by this TreeNode, presuming
    // it is the last step in its path.  This can be 'null' if no PathRule applies.
    //TODO:  Re-insert PathRule later?  Removed for now.  I've put %PR% as a sentinel around relevant code
    // private PathRule pathRule = null;

    //The time depth of this tree node is determined by the rules used to create it
    //This can be present or future
    private int timeDepth;


    /**
     * This root node constructor is built from the agent.
     *
     */
    public TreeNode(NdxrAgent initAgent) {
        //initializing agent and its children
        this.agent = initAgent;
        this.rules = agent.getRules();
        this.parent = null;
        this.episodeIndex = NdxrAgent.getTimeStep();
        this.action = '\0';
        this.prevRules = agent.lastPrevInternal();
        this.currExternal = agent.getCurrExternal();
        this.path = new Vector<>();
        this.pathStr = "";
        //full confidence because drawn directly from agent's present sensing
        this.confidence = 1.0;
        supporters = new Vector<>(); //none

        //Note:  if you set timeDepth to -1 that causes the TreeNode to search
        //       all rules instead of just depth 1 (for root notes only)
        //       See the 06 Aug 2022 journal entry for more about this.
        this.timeDepth = 0;

        if (TreeNode.comboArr == null) {
            TreeNode.initComboArr(this.currExternal.size());
        }
    }//ctor

    /**
     * constructs a TreeNode with given sensors and action
     * @param parent the TreeNode that precedes this one in a path
     * @param action action taken to reach this node
     * @param newExternal predicted external sensors for this node
     * @param confidence confidence in newExternal
     * @param initSupporters  which Rules strongly support this TreeNode
     * @param initDepth  the depth of this TreeNode
     *
     */
    public TreeNode(TreeNode parent, char action, SensorData newExternal,
                    double confidence, Vector<Rule> initSupporters,
                    int initDepth) {
        //initializing agent and its children
        this.agent = parent.agent;
        this.rules = parent.rules;
        this.parent = parent;
        this.episodeIndex = parent.episodeIndex + 1;
        this.action = action;
        Vector<RuleIndex.MatchResult> matches = this.rules.findMatches( parent.prevRules,
                parent.currExternal,
                action,
                newExternal);
        this.prevRules = new Vector<>();
        for(MatchResult mr : matches) {
            prevRules.add(mr.rule);
        }
        this.currExternal = newExternal;
        this.path = new Vector<>(parent.path);
        this.path.add(this);
        this.pathStr = parent.pathStr + action;
        this.confidence = parent.confidence * confidence;
        this.supporters = initSupporters;
        this.timeDepth = initDepth;
    }//child ctor

    /** convenience version of the child ctor that fills in some default values */
    public TreeNode(TreeNode parent,  char action, SensorData newExternal, int initDepth) {
        this(parent, action, newExternal, 1.0, new Vector<>(), initDepth);
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
        this.prevRules = new Vector<>(orig.prevRules);
        this.currExternal = new SensorData(orig.currExternal);
        this.path = new Vector<>(orig.path);
        this.pathStr = orig.pathStr;
        this.confidence = orig.confidence;
        this.supporters = new Vector<>(orig.supporters);
        //%PR% this.pathRule = orig.pathRule;
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
        public int sensorIndex;  //sensor asociated with this Bloc

        //Each bloc (e.g., 00a, 10b, 11a, etc.) its most confident constituent's vote stored here
        //There is a separate map for on vs. off votes
        public HashMap<String, Double> onVotes = new HashMap<>();
        public HashMap<String, Double> offVotes = new HashMap<>();

        public int offVoteCount = 0;
        public int onVoteCount = 0;

        //The Rule with the best match score voting for on and off respectively
        public Rule maxOnVoter = null;
        public Rule maxOffVoter = null;

        public BlocData(int initIndex) {
            this.sensorIndex = initIndex;
        }

        /** registers one vote */
        public void vote(Rule voter, double matchScore) {
            if (matchScore <= 0.0) return;  //ignore weightless vote

            //Calculate the voting bloc this rule is in
            String bloc = voter.getLHS().toString();

            //Add the vote to the proper vote sum
            boolean on = (1 == voter.getRHS().getBit(this.sensorIndex));
            HashMap<String, Double> ballotBox = on ? this.onVotes : this.offVotes;
            if (on) { this.onVoteCount++; } else { this.offVoteCount++; }
            double oldVal = 0.0;

            //is the new match score the best yet?
            boolean replace = true;
            if (ballotBox.containsKey(bloc)) {
                oldVal = ballotBox.get(bloc);
                if (matchScore < oldVal) replace = false;
            }

            //Put the new max vote in
            if (replace) {
                if (on) {maxOnVoter = voter;} else {maxOffVoter = voter;}
                ballotBox.put(bloc, matchScore);
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
     * castVotes
     *
     * casts votes for various possible external sensor combinations given
     * an action and a required rule depth.
     *
     */
    private HashMap<String, BlocData> castVotes(char action, int depth) {
        //Create a BlocData object to track the votes for each sensor
        HashMap<Integer, BlocData> votingData = new HashMap<>();
        for(int i = 0; i < currExternal.size(); ++i) {
            votingData.put(i, new BlocData(i));
        }

        return null;
        //TODO: Re-examine this approach.  I think the RuleIndex makes this easier.
//        //Extract matching rules
//        Vector<Rule> voters = new Vector<>();
//        if (depth > 0) {
//            for(Rule r : this.getCurrInternal(depth)) {
//                voters.addAll(agent.findRules(action, this.currExternal, r));
//            }
//        } else {  //depth 0
//            voters = Rules.get(depth);
//        }
//
//        //Record the votes
//        for (Rule rule : voters) {
//            if (rule.getAction() != action) continue;
//            double score = rule.lhsMatchScore(action, flatCurrInt, this.currExternal);
//            score *= rule.getConfidence();
//            if (score <= 0.0) continue;  //skip rules with unsupportive match scores
//
//            //Record this rule's vote for each sensor
//            for(int i = 0; i < currExternal.size(); ++i) {
//                BlocData bd = votingData.get(i);
//                bd.vote(rule, score);
//            }
//        }//for
//
//        return votingData;
    }//castVotes

    /**
     * class VoteOutcome
     *
     * is a tiny public class to describe the outcome of a Rules voting for
     * the predicted external sensor values in {@link #predictExternal }
     */
    private class VoteOutcome {
        public SensorData ext = new SensorData(false);  //the predicted data
        public double confidence = 1.0;                       //how confident the agent should be in this
        public HashSet<Rule> supporters = new HashSet<>();  //The Rules that most strongly supported this
    }

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

        //Calculate the results for each VoteOutcome object
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
        }//for

        //Scale the confidence values to the range [0.0..1.0]
        //This makes the agent more likely to consider longer paths
        //which ends up being an important nudge toward learning.
        //Note:  this was retested in Aug 2022 and it still helps
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
     * @return a 2D array of {@link VoteOutcome} objects indexed by time depth
     *         and then for each candidate external sensor combo.
     *         Note!  Some internal 1D arrays can be null for depths that
     *                don't apply for this TreeNode
     */
    public VoteOutcome[][] predictExternal(char action) {

        //init an empty array
        VoteOutcome[][] result = new VoteOutcome[Rule.MAX_DEPTH + 1][0];
        for(int depth = 0; depth <= Rule.MAX_DEPTH; ++depth) {
            result[depth] = null;
        }

        //for root notes, all the time depths
        //TODO: this needs re-architecting
//        if (this.timeDepth == -1) {
//            for (int depth = 0; depth <= Rule.MAX_DEPTH; ++depth) {
//                if (agent.getRules().size() <= depth) break;  //no rules to vote
//
//                HashMap<String, BlocData> voteData = castVotes(action, depth);
//                result[depth] = getVoteOutcomes(voteData);
//            }
//        } else {
//            //For non-root nodes, stick to a consistent depth
//            HashMap<String, BlocData> voteData = castVotes(action, this.timeDepth);
//            result[this.timeDepth] = getVoteOutcomes(voteData);
//
//            //Note:  You may be tempted to use the depth 0 rules if no votes
//            // were cast.  I tried this (Aug 2022) and it made it worse. -Nux
//        }//else (timeDepth != -1)

        //Generate a summary of the outcome
        return result;

    }//predictExternalMark3



    /**
     * expand
     *
     * populates this.children
     */
    private void expand() {
        //check:  already expanded
        if (this.children.size() != 0) return;

        //TODO: revamp
//        //check: no rules available at this depth
//        if (agent.getRules().size() <= this.timeDepth) return;
//
//        int numActions = agent.getActionList().length;
//
//        // bloc = lhs external sensors + action
//        //  00a, 00b, 01a, 01b, etc.
//
//        //Create predicted child nodes for each possible action
//        for(int actId = 0; actId < numActions; actId++) {
//
//            char act = agent.getActionList()[actId].getName().charAt(0);
//
//            //Calculate the confidence in each combination
//            VoteOutcome[][] voteOutcomes = predictExternal(act);
//
//            for(int depth = 0; depth <= NdxrAgent.MAX_TIME_DEPTH; ++depth) {
//                if (voteOutcomes[depth] == null) continue;
//                for (VoteOutcome outcome : voteOutcomes[depth]) {
//                    //agent must be more confident in this path than just taking a random action
//                    if (outcome.confidence <= this.agent.getRandSuccessRate())
//                        continue;
//
//                    //create a child for this action + ext sensor combo
//                    TreeNode child = new TreeNode(this, act, outcome.ext,
//                            outcome.confidence, outcome.supporters, depth + 1);
//                    this.children.add(child);
//                }//for
//            }//for
//        }//for

    }//expand

    /**
     * cullWeakChildren
     *
     * remove any children from this node whose overall confidence (after adjustment by a PathRule)
     */
    private void cullWeakChildren() {
        //%PR%
//        //Find the matching pathRule for each child and use it to calc overall conf
//        Vector<TreeNode> removeThese = new Vector<>();  //children to be removed
//        Vector<TreeNode> partialPath = new Vector<>();
//        TreeNode tmp = this;
//        while(tmp.parent != null) {
//            partialPath.add(tmp);
//            tmp = tmp.parent;
//        }
//        for(TreeNode child : this.children) {
//            //Find the matching pathrule
//            partialPath.add(child);
//            child.pathRule = agent.getBestMatchingPathRule(partialPath);
//            //note: don't use partialPath.remove(child) as it relies on equals()
//            double overallScore = calcOverallScore(partialPath);
//            partialPath.remove(partialPath.size() - 1);
//
//            //If the overall confidence is too low, mark the child for removal
//            if (overallScore < agent.getRandSuccessRate()) {
//                removeThese.add(child);
//            }
//        }//for
//
//        //Remove the too-weak children.  Gruesome, no?
//        for(TreeNode child : removeThese) {
//            this.children.remove(child);
//        }
    }//cullWeakChildren

    /**
     * calcOverallScore
     *
     * A path has an overall score based on these factors:
     * 1.  the path confidence (based on the Rule confidences)
     * 2.  the matching PathRule's confidence
     * 3.  the path's length (longer paths are less certain)
     *
     */
    private double calcOverallScore(Vector<TreeNode> foundPath) {
        //the score starts with a base confidence
        TreeNode lastEl = foundPath.lastElement();
        double foundScore = lastEl.confidence;

        //%PR%: adjust score using the matching PathRule
//        if (lastEl.pathRule != null) {
//            foundScore *= lastEl.pathRule.getConfidence();
//        }

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
     * TODO:  Could do A*Search instead if this is too slow.
     *        Also likely better to limit search by max # of expansions instead of a max depth.
     */
    public Vector<TreeNode> findBestGoalPath() {
        double bestScore = 0.0;
        Vector<TreeNode> bestPath = null;
        for(int max = 1; max <= NdxrAgent.MAX_SEARCH_DEPTH; ++max) {
            Vector<TreeNode> path = fbgpHelper(0, max);
            if (path != null) {
                //%PR%
//                path.lastElement().pathRule = agent.getBestMatchingPathRule(path);
//                double foundScore = calcOverallScore(path);
//                if (foundScore < agent.getRandSuccessRate()) continue;

//                if(foundScore > bestScore) {
//                    bestPath = path;
//                    bestScore = foundScore;
//                }
            }
        }

        return bestPath;  //null if failed to find a path to goal
    }//findBestGoalPath

    /**
     * sortedKeys
     *
     * @return a sorted sorted Vector of the rule ids of a given set of Rules
     */
    private Vector<Integer> sortedKeys(HashSet<Rule> internal) {
        Vector<Integer> result = new Vector<>();
        for(Rule r : internal) {
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

        //prev rules
        result.append("(");
        boolean first = true;
        for(Rule r : this.prevRules) {
            if (!first) result.append(",");
            else first = false;
            result.append(r.getId());
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

    public char getAction() { return this.pathStr.charAt(this.pathStr.length() - 1); }

}//class TreeNode
