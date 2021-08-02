package agents.phujus;

import framework.SensorData;

import java.util.Collections;
import java.util.HashMap;
import java.util.TreeSet;
import java.util.Vector;

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

    //Agent
    private final PhuJusAgent agent;

    // all rules in the system
    Vector<EpRule> rules;

    //associated timestep for this node
    private final int episodeIndex;

    //sensor values for this node
    private final HashMap<Integer, Boolean> currInternal;
    private final SensorData currExternal;

    //This is the parent node
    private final TreeNode parent;

    //child nodes
    private final TreeNode[] children;

    //bool for if tree has children or not
    private boolean isLeaf = false;

    //This is the rule used to reach this node (use null for the root)
    private final EpRule rule;

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
        this.parent = null;
        this.children = new TreeNode[agent.getNumActions()]; // actionSize
        this.episodeIndex = agent.getNow();
        this.rule = null;
        this.currInternal = agent.getCurrInternal();
        this.currExternal = agent.getCurrExternal();
        this.path = new Vector<>();

    }

    /**
     * This child node constructor is built from a parent node and a rule
     *
     */
    public TreeNode(TreeNode parent, EpRule rule) {
        //initializing agent and its children
        this.agent = parent.agent;
        this.rules = parent.rules;
        this.parent = parent;
        this.children = new TreeNode[agent.getNumActions()]; // actionSize
        this.episodeIndex = parent.episodeIndex + 1;
        this.rule = rule;
        this.currInternal = agent.genNextInternal(rule.getAction(), parent.currInternal, parent.currExternal);
        this.currExternal = rule.getRHSExternal();
        this.path = new Vector<>(parent.path);
        this.path.add(this);
    }

    /**
     * isBetter
     *
     * compares two matching rules to determine which is better.  The base
     * response is based on match score but there are some tie breakers.
     * This method is a helper for {@link #calcBestMatchingRule(char)}.
     *
     * @param r1  first rule
     * @param score1 first rule's matchs score
     * @param r2 second rule
     * @param score2 second rule's match score
     * @return 'true' if r1 is better than r2
     */
    private boolean isBetter(EpRule r1, double score1, EpRule r2, double score2) {
        //first test:  match scores
        if (score1 > score2) { return true; }
        if (score2 > score1) { return false; }

        //First tie breaker:  activation
        double r1Act = r1.calculateActivation(agent.getNow());
        double r2Act = r2.calculateActivation(agent.getNow());
        if (r1Act > r2Act) return true;
        if (r2Act > r1Act) return false;

        //Second tie breaker: random chance
        return (PhuJusAgent.rand.nextDouble() > 0.5);
    }//isBetter

    /**
     * rankRulesByMatchScore
     *
     * @param action the action the agent intends to take
     * @return a TreeSet of the agent's rules ordered from highest to lowest match score
     */
    private TreeSet<RatedRule> rankRulesByMatchScore(char action) {
        TreeSet<RatedRule> treeResult = new TreeSet<>();
        for(EpRule r : this.rules) {
            double score = r.lhsMatchScore(action, this.currInternal, this.currExternal);
            treeResult.add(new RatedRule(r, score));
        }

        return treeResult;
    }//rankRulesByMatchScore

    /**
     * rankRulesByActivation
     *
     * same as {@link #rankRulesByMatchScore(char)} but for activation
     */
    private TreeSet<RatedRule> rankRulesByActivation() {
        TreeSet<RatedRule> treeResult = new TreeSet<>();
        for(EpRule r : this.rules) {
            double activation = r.calculateActivation(agent.getNow());
            treeResult.add(new RatedRule(r, activation));
        }

        return treeResult;
    }//rankRulesByActivation

    /**
     * calcBestRuleForAction
     *
     * calculates which rule has the best combination of match score and
     * activation to be used to expand this TreeNode with a given action.
     *
     * //TODO:  should rule accuracy also be a factor here?
     * //TODO:  Do we need hyper-parameter weights on the factors? :(  Better if it was a self-tuning value.
     *
     * @return the best EpRule or null if there is no acceptable rule
     */
    private EpRule calcBestRuleForAction(char action) {
        //First get the rules listed in rank order by each criteria
        TreeSet<RatedRule> matchRanked = rankRulesByMatchScore(action);
        TreeSet<RatedRule> actRanked = rankRulesByActivation();

        //Each ranking yields a score from [0.0..1.0].
        //The best product of these scores determines the overall best
        double bestScore = 0.00001; //set to slightly above zero so the method will return null when the "best" match isn't a null
        EpRule bestRule = null;
        int matchCount = 0;
        int actCount = 0;

        //Find the best rule
        //TODO:  should we consider other top rules that are almost as good?  I'm thinking no since activation is a factor in this ranking
        for (RatedRule rMatch : matchRanked) {
            if (rMatch.rating == 0.0) break;
            double matchScore = ((double)(matchRanked.size() - matchCount)) / ((double)matchRanked.size());
            actCount = 0;
            for(RatedRule rAct : actRanked) {  //TODO: how to take advantage of TreeSet's nlogn search time?
                if (rAct.rating == 0.0) break;
                if (rAct.rule != rMatch.rule) {  //ok to use '==' here b/c same object
                    actCount++;
                    continue;
                }
                double actScore = ((double)(actRanked.size() - actCount)) / ((double)actRanked.size());

                //Is this the best so far?
                double overallScore = matchScore * actScore;
                if (overallScore > bestScore) {
                    bestScore = overallScore;
                    bestRule = rAct.rule;
                }
                break;  //exit once rule is found
            }//for actRanked
            matchCount++;
        }//for matchRanked

        //Special case:  no matching rules activated yet
        if ((bestRule == null) && (matchRanked.size() > 0) && (matchRanked.first().rating > 0.0)){
            bestRule = matchRanked.first().rule;
        }

        return bestRule;
    }//calcBestRuleForAction

    /**
     * calcBestMatchingRule
     *
     * calculates which rule best matches this node with a given action
     */
    private EpRule calcBestMatchingRule(char action) {
        //Find the best matching rule
        double bestScore = 0.00001; //set to slightly above zero so the "best" match isn't a non-match
        EpRule bestRule = null;

        //Find the best matching rule
        //TODO:  should it consider other top rules that are almost as good?
        for (EpRule r : this.rules) {
            double score = r.lhsMatchScore(action, this.currInternal, this.currExternal);
            if (isBetter(r, score, bestRule, bestScore)) {
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
            if ( (path != null) && (path.size() <= max) )
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
        Vector<TreeNode> bestPath = null;  //used to store best path found

        for(int i = 0; i < agent.getActionList().length; ++i) {
            //Create the child node for this action if it doesn't exist yet
            TreeNode child = this.children[i];
            if (child == null) {
                char action = agent.getActionList()[i].getName().charAt(0);

                EpRule bestRule = calcBestRuleForAction(action);
                if (bestRule != null) {
                    child = new TreeNode(this, bestRule);
                    this.children[i] = child;
                } else {
                    this.children[i] = null;
                    continue;
                }
            } //if create new child node
            else {
                child.isLeaf = false; //reset from any prev use of this child
            }

            //don't expand this child node if we've seen it before
            //TODO:  I don't think we can do this.  It's preventing us from finding goals
//            if (visited.contains(child)) {
//                child.isLeaf = true;
//                continue;
//            }

            //Recursive case: test all children and return shortest path
            // TODO: path confidence should play a role not just path length
            visited.add(child);
            Vector<TreeNode> foundPath = child.fbgpHelper(depth + 1, maxDepth, visited);
            if ( foundPath != null) {
                if ((bestPath == null) || (foundPath.size() < bestPath.size())) {
                    bestPath = foundPath;
                }
            }
        }//for

        return bestPath;

    }//fbgpHelper

    /**
     * sortedKeys
     *
     * @return a sorted Vector of the Integer keys in an internal sensor HashMap
     */
    private Vector<Integer> sortedKeys(HashMap<Integer, Boolean> internal) {
        Vector<Integer> result = new Vector<>(internal.keySet());
        Collections.sort(result);
        return result;
    }

    /**
     * sortedKeys
     *
     * @return a sorted Vector of the Integer keys in an external sensor SensorData.
     * The GOAL sensor is always put at the end, however.
     */
    private Vector<String> sortedKeys(SensorData external) {
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
    @Override
    public String toString() {
        StringBuilder result = new StringBuilder();

        //create a path string of actions that led to this node
        for (TreeNode node : this.path) {
            result.append(node.getAction());
        }

        result.append("->");

        //internal sensors
        result.append("(");
        int count = 0;
        for (Integer i : sortedKeys(this.currInternal)) {
            if (((Boolean)this.currInternal.get(i))) {
                if (count > 0) {
                    result.append(", ");
                }
                count++;
                result.append(i);
            }
        }
        result.append(")|");

        //external sensors
        for (String s : sortedKeys(this.currExternal)) {
            Boolean val = (Boolean) this.currExternal.getSensor(s);
            result.append(val ? "1" : "0");
        }

        //Rule # and match score
        if (this.rule != null) {
            result.append(" #");
            result.append(this.rule.getId());
            result.append("=");
            double score = this.rule.lhsMatchScore(this.rule.getAction(),
                                                   this.parent.currInternal,
                                                   this.parent.currExternal);
            result.append(String.format("%.2f", score));
        }

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
        for (int i = 0; i < this.agent.getNumActions(); i++) {
            if (this.children[i] != null) {
                this.children[i].printTreeHelper(indent + "   ");
            }
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
                && (this.rule != null) );   //this isn't the root
    }

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
    public HashMap<Integer, Boolean> getCurrInternal() { return this.currInternal; }
    public SensorData getCurrExternal() { return this.currExternal; }
    public TreeNode getParent() { return this.parent; }

}//class TreeNode
