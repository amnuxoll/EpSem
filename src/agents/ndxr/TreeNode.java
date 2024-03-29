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
    private static NdxrAgent agent;

    //the rule that was used to create this node.  (null for root node)
    private final Rule rule;

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

    /**
     * This root node constructor is built from the agent.
     *
     */
    public TreeNode(NdxrAgent initAgent) {
        //initializing agent and its children
        TreeNode.agent = initAgent;
        this.parent = null;
        this.rule = null;
        this.path = new Vector<>();
        this.pathStr = "";
        //full confidence because drawn directly from agent's present sensing
        this.confidence = 1.0;
    }//ctor

    /**
     * constructs a child TreeNode with given sensors and action
     * @param parent the TreeNode that precedes this one in a path
     * @param initRule rule used to create this node
     * @param score match score for this rule
     */
    public TreeNode(TreeNode parent, Rule initRule, double score) {
        this.parent = parent;
        this.rule = initRule;
        this.path = new Vector<>(parent.path);
        this.path.add(this);
        this.pathStr = parent.pathStr + initRule.getAction();
        this.confidence = parent.confidence * score;
    }//child ctor

    /**
     * expand
     * <p>
     * populates this.children
     */
    private void expand() {
        //check:  already expanded
        if (this.children.size() != 0) return;

        int numActions = agent.getActionList().length;

        //Create predicted child nodes for each possible action
        for(int actId = 0; actId < numActions; actId++) {

            char act = agent.getActionList()[actId].getName().charAt(0);

            //Calculate the expected outcome for this action
            Vector<Rule> prevRules = new Vector<>();
            Vector<MatchResult> results;
            CondSet rhsBits = new CondSet(SensorData.createEmpty());  //only matching LHS
            if (this.rule == null) { //root note
                CondSet lhsBits = new CondSet(agent.getCurrExternal());
                results = agent.getRules().findMatches(prevRules, lhsBits,
                                                       act, rhsBits);
            }
            else {  //non-root node
                prevRules.add(this.rule);
                results = agent.getRules().findMatches(prevRules, this.rule.getLHS(),
                                                       act, rhsBits);
            }

            //create a child for this action + ext sensor combo
            for (MatchResult mr : results) {
                //agent must be more confident in this path than just taking a random action
                if (mr.score <= agent.getRandSuccessRate()) continue;

                //Create a child node
                Vector<Rule> newPrev = new Vector<>();
                newPrev.add(mr.rule);
                TreeNode child = new TreeNode(this, mr.rule, mr.score);
                this.children.add(child);
            }//for each match result
        }//for each action

    }//expand

    /**
     * calcOverallScore
     * <p>
     * A path has an overall score based on these factors:
     * 1.  the path confidence (based on the Rule confidences)
     * 2.  the matching PathRule's confidence
     * 3.  the path's length (longer paths are less certain)
     *
     */
    private static double calcOverallScore(Vector<TreeNode> foundPath) {
        //the score starts with a base confidence
        TreeNode lastEl = foundPath.lastElement();
        double foundScore = lastEl.confidence;

        //Adjust with the best matching PathRule (if it exists)
        //Note:  the fact that no adjustment is made for mismatch makes the agent explore more.  TODO: too curious?
        PathRule match = agent.getBestMatchingPathRule(agent.getCurrPathRule(), foundPath);
        if (match != null) {
            foundScore *= match.getConfidence();
        }

        //Adjust based on path length.  This is based on the Sunrise problem in probability.
        foundScore *= (1.0 / (foundPath.size()));

        return foundScore;
    }//calcOverallScore


    /**
     * fbgpHelper
     * <p>
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
     * <p>
     * TODO:  Could do A*Search instead if this is too slow.
     *        Also likely better to limit search by max # of expansions instead of a max depth.
     */
    public Vector<TreeNode> findBestGoalPath() {
        double bestScore = 0.0;
        Vector<TreeNode> bestPath = null;
        for(int max = 1; max <= NdxrAgent.MAX_SEARCH_DEPTH; ++max) {
            Vector<TreeNode> path = fbgpHelper(0, max);

            //DEBUG
            if (path != null) {
                PathRule matchPR = agent.getBestMatchingPathRule(agent.getCurrPathRule(), path);
                agent.debugPrintln("    Cand Path Found: " + path.lastElement());
                if (matchPR != null) {
                    agent.debugPrintln("             adj by: " + matchPR + " c" + matchPR.getConfidence());
                }
            }

            if (path != null) {
                //ignore scores that are worse than random
                double score = calcOverallScore(path);
                if (score < agent.getRandSuccessRate()) {
                    path = null;
                }

                //Is this the best so far?
                else if (score > bestScore) {
                    bestScore = score;
                    bestPath = path;
                }
            }
        }//for

        return bestPath;  //null if failed to find a path to goal
    }//findBestGoalPath

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
        result.append(this.rule.getId());
        result.append(")|");

        //external sensors
        result.append(this.rule.getRHS().wcBitString());

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
        if (this.rule == null) return false;  //root node
        CondSet rhs = this.rule.getRHS();
        return (rhs.getBit(rhs.size() - 1) == 1);
    }

    public char getAction() { return this.pathStr.charAt(this.pathStr.length() - 1); }

    public Rule getRule() { return this.rule; }
    public double getConfidence() { return this.confidence; }
}//class TreeNode
