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

    /** pairs an EpRule with a numerical value so it can be sorted by that value */
    private class RatedRule implements Comparable<RatedRule> {
        public EpRule rule;
        public double rating;

        public RatedRule(EpRule initRule, double initRating) {
            this.rule = initRule;
            this.rating = initRating;
        }

        @Override
        public int compareTo(RatedRule other) {
            //Special case: tie
            if (this.rating == other.rating) {
                //break ties arbitrarily with rule number
                return this.rule.getId() - other.rule.getId();
            }

            //regular case
            return (this.rating > other.rating) ? -1 : 1;
        }
    }//class RatedRule

    //Agent's current state and ruleset is needed to build the tree
    private final PhuJusAgent agent;

    // all rules in the system
    Hashtable<Integer, Rule> rules;

    //associated timestep for this node
    private final int episodeIndex;

    //sensor values for this node
    private final HashSet<Integer> currInternal;
    private final SensorData currExternal;

    //This is the parent node
    private final TreeNode parent;

    //child nodes
    private final Vector<TreeNode> children = new Vector<>();;

    //bool for if tree has children or not
    private boolean isLeaf = false;

    //This is the rule used to reach this node (use null for the root)
    private BaseRule rule;

    //This is the rule used to reach this node when using tfrules
    private TFRule termRule;

    //This is the path used to reach this node
    private final Vector<TreeNode> path;

    //The same path expressed as a String of actions
    private final String pathStr;

    //A measure from 0.0 to 1.0 of how confident the agent is that this path is correct
    private double confidence;

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
        this.rule = null;
        this.termRule = null;
        this.currInternal = agent.getCurrInternal();
        this.currExternal = agent.getCurrExternal();
        this.path = new Vector<>();
        this.pathStr = "";
        //full confidence because drawn directly from agent's present sensing
        this.confidence = 1.0;
    }

    public TreeNode(TreeNode parent, TFRule tfRule) {
        //initializing agent and its children
        this.agent = parent.agent;
        this.rules = parent.rules;
        this.parent = parent;
        this.episodeIndex = parent.episodeIndex + 1;
        this.termRule = tfRule;
        this.currInternal = agent.genNextInternalTF(tfRule.getAction(), parent.currInternal, parent.currExternal);
        this.currExternal = tfRule.getRHSExternal();
        this.path = new Vector<>(parent.path);
        this.path.add(this);
        this.pathStr = parent.pathStr + tfRule.getAction();
        this.confidence = tfRule.lhsMatchScore(tfRule.getAction(), parent.currInternal, parent.currExternal);
        this.confidence *= tfRule.getAccuracy();
    }

    /**
     * This child node constructor is built from a parent node and a matching rule
     *
     * Note:  caller is responsible for verifying the rule matches
     *
     */
    public TreeNode(TreeNode parent, BaseRule rule) {
        //initializing agent and its children
        this.agent = parent.agent;
        this.rules = parent.rules;
        this.parent = parent;
        this.episodeIndex = parent.episodeIndex + 1;
        this.rule = rule;
        this.currInternal = agent.genNextInternal(rule.getAction(), parent.currInternal, parent.currExternal);
        this.currExternal = rule.getRHSExternal();
        this.path = new Vector<>(parent.path);
        this.path.add(this);
        this.pathStr = parent.pathStr + rule.getAction();
        this.confidence = rule.lhsMatchScore(rule.getAction(), parent.currInternal, parent.currExternal);
        this.confidence *= rule.getAccuracy();
    }

    public TFRule findBestMatchingTFRule(char action) {
        HashSet<Integer> lhsInt = this.agent.getPrevInternal();
        SensorData lhsExt = this.agent.getPrevExternal();
        SensorData rhsExt = this.agent.getCurrExternal();

        TFRule maxRule = null;
        double max = 0.0;
        for (TFRule tr : this.agent.getTfRules()) {
            double temp = tr.totalMatchScore(action, lhsInt, lhsExt, rhsExt);
            System.out.println(tr.ruleId + ": " + temp);
            if( temp > max ){
                max = tr.totalMatchScore(action, lhsInt, lhsExt, rhsExt);
                maxRule = tr;
            }
        }

        return maxRule;
    }

    /**
     * findBestMatchingEpRule
     *
     * finds the EpRule that best matches this TreeNode
     *
     * @param action the action to use for the match
     * @return the best match or null if no match found
     */
    private EpRule findBestMatchingEpRule(char action) {
        double bestScore = 0.01;  //avoid "best" being 0.0 match
        double bestDepth = -1;
        EpRule bestRule = null;
        for (EpRule er : this.agent.getEpRules()) {
            if (er.getTimeDepth() < bestDepth) continue;
            double score = er.lhsMatchScore(action, this.currInternal, this.currExternal);
            score *= er.getAccuracy();
            if (score <= 0.0) continue;  //no match

            //A deeper match is always better, otherwise break ties with score
            if ( (er.getTimeDepth() > bestDepth) || (score > bestScore) ){
                bestScore = score;
                bestRule = er;
            }
        }//for each rule

        return bestRule;
    }//findBestMatchingEpRule

    /**
     * findBestMatchingBaseRule
     *
     * finds the BaseRule that best matches this TreeNode
     *
     * @param action the action to use for the match
     * @return the best match or null if no match found
     */
    private BaseRule findBestMatchingBaseRule(char action) {
        double bestScore = 0.01;  //avoid "best" being 0.0 match
        double bestDepth = -1;
        BaseRule bestRule = null;
        for (BaseRule br : this.agent.getBaseRules()) {
            double score = br.lhsMatchScore(action, this.currInternal, this.currExternal);
            score *= br.getAccuracy();
            if (score > bestScore){
                if (score == 1.0) return br;
                bestScore = score;
                bestRule = br;
            }
        }//for each rule

        return bestRule;
    }//findBestMatchingBaseRule



    /**
     * expand
     *
     * populates this.children
     */
    private void expand() {
        //check:  already expanded
        if (this.children.size() != 0) return;

        //Find the best matching Rule
        for(int i = 0; i < agent.getActionList().length; ++i) {
            char action = agent.getActionList()[i].getName().charAt(0);

            //Prefer a matching EpRule over any BaseRule
            BaseRule bestRule = findBestMatchingEpRule(action);
            if(bestRule == null) {
                bestRule = findBestMatchingBaseRule(action);
                if(bestRule == null) {
                    continue; //we can't expand with this action
                }
            }

            //Create a child node for this rule
            TreeNode child = new TreeNode(this, bestRule);
            this.children.add(child);

        }//for each action

    }//expand

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
                double foundScore = path.lastElement().confidence;

                //Adjust confidence based upon the PathRules eval
                foundScore *= agent.metaScorePath(path);

                if(foundScore > bestScore) {
                    bestPath = path;
                    bestScore = foundScore;
                }
            }
        }

        return bestPath;  //null if failed to find a path to goal
    }//findBestGoalPath


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
        if (isGoalNode()) {
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

            if ( foundPath != null) {
                double confidence = foundPath.lastElement().confidence;
                //TODO: Braeden is unsatisfied with the 1/length , think about another metric?
                double foundScore = (1.0 / (foundPath.size())) * confidence;
                if (foundScore > bestScore) {
                    bestPath = foundPath;
                    bestScore = foundScore;
                }
            }
        }//for

        return bestPath;

    }//fbgpHelper

    /**
     * findMostUncertainPath
     *
     * is called on a completed tree in which no path to GOAL was found.
     * It selects a relatively short path with a high degree of
     * uncertainty about the outcome.
     *
     * @return the path found
     */
    public Vector<TreeNode> findMostUncertainPath() {
        Vector<TreeNode> toSearch = new Vector<>();
        toSearch.add(this);

        double bestScore = 0.0;
        Vector<TreeNode> bestPath = null;
        while(toSearch.size() > 0) {
            TreeNode curr = toSearch.remove(0);
            double invConf = 1.0 - curr.confidence;
            double score = (1.0/((double)curr.path.size())) * invConf;
            if (score > bestScore) {
                bestScore = score;
                bestPath = curr.path;
            }

            //queue children to be searched
            for(TreeNode child : curr.children) {
                if (child != null) toSearch.add(child);
            }
        }//while

        return bestPath;
    }//findMostUncertainPath


    /**
     * sortedKeys
     *
     * @return a sorted Vector of the Integer keys in an internal sensor HashMap
     */
    private Vector<Integer> sortedKeys(HashSet<Integer> internal) {
        Vector<Integer> result = new Vector<>(internal);
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
    @Override
    public String toString() {
        StringBuilder result = new StringBuilder();

        //the actions that led to this node
        result.append(this.pathStr);
        result.append("->");

        //Internal Sensors
        result.append("(");
        int count = 0;
        for (Integer i : sortedKeys(this.currInternal)) {
            if (count > 0) {
                result.append(", ");
            }
            count++;
            result.append(i);
        }
        result.append(")|");

        //External Sensors
        result.append(TreeNode.extToString(this.currExternal));

        //Rule # and Match Score
        if (this.rule != null) {
            result.append(" #");
            result.append(this.rule.getId());
            result.append("=");
            double score = this.rule.lhsMatchScore(this.rule.getAction(),
                                                   this.parent.currInternal,
                                                   this.parent.currExternal);
            result.append(String.format("%.2f", score));
        }

        //Confidence
        result.append("\tc");
        //note:  replaceAll call removes extra trailing 0's to improve readability
        result.append(String.format("%.6f", this.confidence).replaceAll("0+$", "0"));

        return result.toString();
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
     * equals
     *
     * method is overridden to only compare sensors and actions
     */
    @Override
    public boolean equals(Object obj) {
        if (! (obj instanceof TreeNode) ) return false;
        TreeNode other = (TreeNode)obj;

        //compare actions
        if (! this.pathStr.equals(other.pathStr)) return false;

        //compare sensors
        if (! other.currExternal.equals(this.currExternal)) return false;
        if (! other.currInternal.equals(this.currInternal)) return false;

        //All tests passed
        return true;

    }//equals

    public char getAction() { return this.pathStr.charAt(this.pathStr.length() - 1); }
    public String getPathStr() { return this.pathStr; }
    public BaseRule getRule() { return this.rule; }
    public HashSet<Integer> getCurrInternal() { return this.currInternal; }
    public SensorData getCurrExternal() { return this.currExternal; }
    public TreeNode getParent() { return this.parent; }

}//class TreeNode
