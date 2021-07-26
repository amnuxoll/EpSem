package agents.phujus;

import framework.SensorData;
import java.util.HashMap;
import java.util.Vector;

/**
 * class TreeNode
 * <p>
 * Each instance is a node in an N-ary tree where N is the number of actions (i.e., the FSM's
 * alphabet) that tries to predict outcomes of sequences of actions.  Thus, we can "find" a
 * best (shortest, most confident) sequence of actions to reach the goal
 */

public class TreeNode {
    //Agent
    private final PhuJusAgent agent;

    // all rules in the system
    Vector<EpRule> rules;

    //associated timestep for this node
    private final int episodeIndex;

    //sensor values for this node
    private final HashMap<Integer, Boolean> currInternal;
    private final SensorData currExternal;

    //child nodes
    private final TreeNode[] children;

    //bool for if tree has children or not
    private boolean isLeaf = false;

    //This is the rule used to reach this node (use null for the root)
    private EpRule rule;

    //This is the path used to reach this node (the root should be an empty list)
    private final Vector<TreeNode> path;


    /**
     * This root node constructor is built from the agent.
     *
     */
    public TreeNode(PhuJusAgent initAgent) {
        //initializing agent and its children
        this.agent = initAgent;
        this.rules = agent.getRules();
        this.children = new TreeNode[agent.getNumActions()]; // actionSize
        this.episodeIndex = agent.getNow();
        this.rule = null;
        this.currInternal = agent.getCurrInternal();
        this.currExternal = agent.getCurrExternal();
        this.path = new Vector<TreeNode>();

    }

    /**
     * This child node constructor is built from a parent node and a rule
     *
     */
    public TreeNode(TreeNode parent, EpRule rule) {
        //initializing agent and its children
        this.agent = parent.agent;
        this.rules = parent.rules;
        this.children = new TreeNode[agent.getNumActions()]; // actionSize
        this.episodeIndex = parent.episodeIndex + 1;
        this.rule = rule;
        this.currInternal = rule.getLHSInternal();
        this.currExternal = rule.getLHSExternal();
        this.path = (Vector<TreeNode>)parent.path.clone();
        this.path.add(this);
    }

    /**
     * calcBestMatchingRule
     *
     * calculates which rule best matches this node with a given action
     */
    private EpRule calcBestMatchingRule(char action) {
        //Find the best matching rule
        double bestScore = -1.0;
        EpRule bestRule = null;

        //Find the best matching rule
        //TODO:  should it consider other top rules that are almost as good?
        for (EpRule r : this.rules) {
            double score = r.matchScore(action, this.currInternal, this.currExternal);
            if (score > bestScore) {
                bestScore = score;
                bestRule = r;
            }
        }

        return bestRule;

    }//calcBestMatchingRule

    /**
     * findBestGoalPath
     * <p>
     * uses an iterative deepening search tree to find a path to the goal
     *
     * //TODO:  consider longer paths that are more reliable?  Hopefully rule confidence adjustments will make this unncessary.
     * //TODO:  A*Search instead?
     */
    public Vector<TreeNode> findBestGoalPath() {
        for(int max = 1; max <= PhuJusAgent.MAXDEPTH; ++max) {
            Vector<TreeNode> visited = new Vector<>();
            Vector<TreeNode> path = fbgpHelper(0, max, visited);
            if ( (path != null) && (path.size() < max) )
                return path;
        }

        return null;  //failed to find a path to goal
    }//findBestGoalPath


    /**
     * fbgpHelper
     *
     * recursive helper method for {@link #findBestGoalPath}
     *
     * @param depth  depth of this node
     * @param maxDepth maximum depth allowed
     * @param visited nodes we've visited so far
     *
     * @return a path to the goal (or null if not found)
     */
    private Vector<TreeNode> fbgpHelper(int depth, int maxDepth, Vector<TreeNode> visited) {
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

        //Recursive case: examine child nodes
        for(int i = 0; i < agent.getActionList().length; ++i) {
            //Create the child node for this action if it doesn't exist yet
            TreeNode child = this.children[i];
            if (child == null) {
                char action = agent.getActionList()[i].getName().charAt(0);

                EpRule bestRule = calcBestMatchingRule(action);
                child = new TreeNode(this, bestRule);
                this.children[i] = child;
            } //if create new child node
            else {
                child.isLeaf = false; //reset from any prev use of this child
            }

            //don't expand this child node if we've seen it before
            if (visited.contains(child)) {
                child.isLeaf = true;
                continue;
            }

            //Recursive case
            visited.add(child);
            Vector<TreeNode> path = child.fbgpHelper(depth + 1, maxDepth, visited);
            if ( path != null) {
                if (path.size() < maxDepth) {
                    maxDepth = path.size();
                }
                return path;  //TODO:  test all children and return shortest path (or path with highest confidence)
            }
        }//for

        return null;

    }//fbgpHelper


    /**
     * toString
     * <p>
     * creates a terse ASCII representation of this node.  The child nodes are not depicted.
     */
    @Override
    public String toString() {
        StringBuilder result = new StringBuilder(this.path + "->");

        //internal sensors
        for (Integer i : this.currInternal.keySet()) {
            result.append((this.currInternal.get(i)) ? "1" : "0");
        }
        result.append("|");

        //external sensors
        for (String s : this.currExternal.getSensorNames()) {
            Boolean val = (Boolean) this.currExternal.getSensor(s);
            result.append(val ? "1" : "0");
        }

        return result.toString();
    }

    /**
     * prints the tree in an "easily" readable format.  This method is the front facing interface.
     * {@link #printTreeHelper} is the recursive helper method that traverses the tree
     */
    public void printTree() {
        //Print the sensor names
        System.out.println("  Internal Sensors: 0-" + (this.currInternal.keySet().size() - 1));
        System.out.print("  External Sensors: ");
        for (String s : this.currExternal.getSensorNames()) {
            System.out.print(s + " ");
        }
        System.out.println();

        //print the rules recursively
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
        for (int i = 0; i < this.agent.getNumActions(); i++) {
            this.children[i].printTreeHelper(indent + "   ");
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
                && (! this.path.equals("")) );   //this isn't the root
    }

    //TODO: get this working with EpRules.  Mothballed for now
    /**
     * isValidPath
     *
     * checks to see if a previous path is still valid after an action from it is taken
     *
     * @return true is the previous path is still valid, false if new path is needed
     */
//    public boolean isValidPath(String prevPath) {
//        // Check to make sure there is a path
//        if(prevPath.equals("")) {
//            return false;
//        }
//
//        // Path-to-take and Path-to-Nodes are reversed
//        // So compare first char to in Path-to-take to last char in Path-to-Node
//        char nextStep = prevPath.charAt(0);
//        for(TreeNode child : this.children) {
//            if(child.path.charAt(child.path.length()-1) == nextStep) {
//                if(child.isGoalNode()) {
//                    return true;
//                } else if(child.isLeaf) {
//                    //this likely happens because the tree was truncated by
//                    // genSuccessors() when it found a shorter path.  We don't
//                    //want to switch to a shorter path mid-stream (causes loops)
//                    //so stick with this one and assume it's okay
//                    return true;
//                } else {
//                    child.isValidPath(prevPath.substring(1));
//                }
//            }
//        }
//
//        // If we don't find children, return false
//        return false;
//
//    }//isValidPath

    /**
     * equals
     *
     * method is overridden to only compare sensors and action
     */
    @Override
    public boolean equals(Object obj) {
        if (! (obj instanceof TreeNode) ) return false;
        TreeNode other = (TreeNode)obj;

        //compare actions
        if (this.getAction() != other.getAction()) return false;

        //compare sensors
        if (! other.currExternal.equals(this.currExternal)) return false;
        if (! other.currInternal.equals(this.currInternal)) return false;

        //All tests passed
        return true;

    }//equals

    public char getAction() { return this.rule.getAction(); }

    public EpRule getRule() { return this.rule; }

}//class TreeNode
