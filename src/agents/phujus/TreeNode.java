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
    private EpRule rule;

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
        this.children = new TreeNode[agent.getNumActions()]; // actionSize
        this.episodeIndex = agent.getNow();
        this.rule = null;
        this.currInternal = agent.getCurrInternal();
        this.currExternal = agent.getCurrExternal();
        this.path = new Vector<>();
        this.pathStr = "";
        //full confidence because drawn directly from agent's present sensing
        this.confidence = 1.0;
    }

    /**
     * This child node constructor is built from a parent node and a selected action
     *
     */
    public TreeNode(TreeNode parent, char action) {
        //initializing agent and its children
        this.agent = parent.agent;
        this.rules = parent.rules;
        this.parent = parent;
        this.children = new TreeNode[agent.getNumActions()]; // actionSize
        this.episodeIndex = parent.episodeIndex + 1;
        this.rule = null;  //not used
        this.currInternal = agent.genNextInternal(action, parent.currInternal, parent.currExternal);
        this.currExternal = softmaxSelect(action);
        this.path = new Vector<>(parent.path);
        this.path.add(this);
        this.pathStr = parent.pathStr + action;
        //Note: this.confidence is set by voteRHSExternal()
    }

    /**
     * softmaxSelect
     *
     * randomly selects one rule to predict the currExternal for this node.
     * The selection is weighted by the product of:
     *  - the rule's match score to the parent's sensors
     *  - the rule's accuracy
     *  - TODO: should we also use confidence in RHS values?
     *
     * Algorithm:  https://en.wikipedia.org/wiki/Softmax_function
     * Side effect:  this.rule and this.confidence are also set by this method
     *
     * @param action the selected action for this node
     * @return the selected rule's SensorData
     */
    private SensorData softmaxSelect(char action) {
        double sum = 0.0;

        //Calculate the weight of each rule
        double[] weights = new double[this.rules.size()];
        int index = 0;
        for(EpRule rule : this.rules) {
            double score = rule.lhsMatchScore(action, parent.currInternal, parent.currExternal);
            double acc = rule.getAccuracy();
            double weight = score * acc * 10;
            if (weight > 0.0) weight = Math.pow(Math.E, weight);
            weights[index] = weight;
            sum += weight;
            index++;
        }//for

        //Special case:  no helpful rules
        if (sum == 0.0) {
            this.rule = null;
            this.confidence *= 0.01; //TODO: calc this value based baseline goal probability
            SensorData result = SensorData.createEmpty();
            result.setSensor(SensorData.goalSensor, true);
            return result;
        }

        //Weighted, random selection
        double random = PhuJusAgent.rand.nextDouble();
        for(int i = 0; i < weights.length; ++i) {
            random -= weights[i]/sum;
            if (random <= 0.0) {
                this.rule = this.rules.get(i);
                this.confidence = Math.log(weights[i]) / 10;
                return this.rule.getRHSExternal();
            }
        }

        return null; //should never be reached
    }//softmaxSelect



    /**
     * Calculates the best guess expected RHS values if a given action is
     * selected when the agent reaches a state corresponding to this node.
     * The calculation is done via a voting process:  every rule votes
     * for particular sensor values and each vote is weighted by the
     * product of:
     *  - the rule's match score to the parent's sensors
     *  - the rule's confidence of the RHS value
     *  - the rule's accuracy
     *
     * Side effect:  this.confidence is also set by this method
     *
     * @param action the selected action for this node
     * @return SensorData that is the agent's
     */
    //TODO:  this is currently unused but I want to replace softmaxSelect() with this.  It's creating infinite loops atm.
    private SensorData voteRHSExternal(char action) {
        //Tally the votes from each rule
        HashMap<String, Double> rhsTrue = new HashMap<>();  //votes for 'true'
        HashMap<String, Double> rhsFalse = new HashMap<>(); //votes for 'false'
        for(EpRule r : this.rules) {
            double score = r.lhsMatchScore(action, parent.currInternal, parent.currExternal);
            if (score == 0.0) continue; //no vote
            for(EpRule.ExtCond eCond : r.getRHSConds()) {
                //calculate the 'weight' of this rule's vote
                double adj = score * eCond.getConfidence() * r.getAccuracy();

                //Is this a 'true' vote or a 'false' vote
                HashMap<String, Double> target = rhsTrue;
                if (!eCond.val) {
                    target = rhsFalse;
                }

                //record the vote
                if (!target.containsKey(eCond.sName)) {
                    target.put(eCond.sName, adj);
                } else {
                    double currVal = target.get(eCond.sName);
                    target.put(eCond.sName, currVal + adj);
                }
            }//for each cond
        }//for each rule

        //Set confidence and build a SensorData based on the tallied votes
        this.confidence = parent.confidence;
        SensorData result = SensorData.createEmpty();
        TreeSet<String> allKeys = new TreeSet<>(rhsTrue.keySet());
        allKeys.addAll(rhsFalse.keySet());
        for(String sName : allKeys) {
            double trueVotes = (rhsTrue.containsKey(sName)) ? rhsTrue.get(sName) : 0.0;
            double falseVotes = (rhsFalse.containsKey(sName)) ? rhsFalse.get(sName) : 0.0;

            if (trueVotes != falseVotes) {  //tie means sensor is excluded
                //set SensorData value
                result.setSensor(sName, trueVotes > falseVotes);

                //confidence based on vote totals
                this.confidence *= Math.max(trueVotes, falseVotes) / (trueVotes + falseVotes);
            }

        }//for

        //Special case:  empty SensorData
        //   assume goal at nominal probability
        if (result.getSensorNames().size() == 0) {
            result.setSensor(SensorData.goalSensor, true);
            this.confidence *= 0.01; //TODO: calc this value based baseline goal probability
        }

        return result;
    }//voteRHSExternal

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
        double bestScore = 0.0;
        Vector<TreeNode> bestPath = null;
        for(int max = 1; max <= PhuJusAgent.MAXDEPTH; ++max) {
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

        //Recursive case: examine child nodes

        //Keeps track of the best path we've seen so far
        Vector<TreeNode> bestPath = null;
        double bestScore = 0.0;

        for(int i = 0; i < agent.getActionList().length; ++i) {
            //Create the child node for this action if it doesn't exist yet
            TreeNode child = this.children[i];
            if (child == null) {
                char action = agent.getActionList()[i].getName().charAt(0);
                this.children[i] = new TreeNode(this, action);
                child = this.children[i];
            } //if create new child node
            else {
                child.isLeaf = false; //reset from any prev use of this child
            }

            //Recursive case: test all children and return shortest path
            // TODO: path confidence should play a role not just path length
            Vector<TreeNode> foundPath = child.fbgpHelper(depth + 1, maxDepth);
            if ( foundPath != null) {
                double confidence = foundPath.lastElement().confidence;
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
        result.append(TreeNode.extToString(this.currExternal));

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

        //Confidence
        result.append(" c");
        if ((this.confidence == 1.0) || (this.confidence == 0.0) || (this.confidence == 0.1)){
            result.append(this.confidence);
        } else {
            result.append(String.format("%.6f", this.confidence));
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
    public EpRule getRule() { return this.rule; }
    public HashMap<Integer, Boolean> getCurrInternal() { return this.currInternal; }
    public SensorData getCurrExternal() { return this.currExternal; }
    public TreeNode getParent() { return this.parent; }

}//class TreeNode
