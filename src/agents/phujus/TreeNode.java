package agents.phujus;

import environments.fsm.FSMEnvironment;
import framework.Action;
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

    //This might be useful...
    Random rand = new Random();

    //Agent's current state and ruleset is needed to build the tree
    private final PhuJusAgent agent;

    // all rules in the system
    Hashtable<Integer, Rule> rules;

    //associated timestep for this node
    private final int episodeIndex;

    //the action that led to this node
    private char action;

    //sensor values for this node
    private final HashSet<Integer> currInternal;
    private final SensorData currExternal;

    //This is the parent node
    private final TreeNode parent;

    //child nodes
    private final Vector<TreeNode> children = new Vector<>();;

    //bool for if tree has children or not
    private boolean isLeaf = false;

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
        this.action = '\0';
        this.currInternal = agent.getCurrInternal();
        this.currExternal = agent.getCurrExternal();
        this.path = new Vector<>();
        this.pathStr = "";
        //full confidence because drawn directly from agent's present sensing
        this.confidence = 1.0;
    }

    /**
     * constructs a treenode with given sensors and action
     * @param parent
     * @param action action taken to reach this node
     * @param newExternal predicted external sensors for this node
     * @param confidence confidence in newExternal
     *
     */
    public TreeNode(TreeNode parent, char action, SensorData newExternal, double confidence) {
        //initializing agent and its children
        this.agent = parent.agent;
        this.rules = parent.rules;
        this.parent = parent;
        this.episodeIndex = parent.episodeIndex + 1;
        this.action = action;
        this.currInternal = agent.genNextInternal(action, parent.currInternal, parent.currExternal);
        this.currExternal = newExternal;
        this.path = new Vector<>(parent.path);
        this.path.add(this);
        this.pathStr = parent.pathStr + action;
        this.confidence = parent.confidence * confidence;
    }

    /**
     * findBestMatchingTFRule
     *
     * finds the TFRule that best matches this TreeNode
     *
     * @param action the action to use for the match
     * @return the best match or null if no match found
     */
    public TFRule findBestMatchingTFRule(char action) {
        TFRule maxRule = null;
        double max = 0.0;
        for (TFRule tr : this.agent.getTfRules()) {

            double score = tr.lhsMatchScore(action, this.currInternal, this.currExternal);
            score*= tr.getConfidence();
            if( score > max ){
                max = score;
                maxRule = tr;
            }
        }

        return maxRule;
    }

    /**
     * predicts what the externals sensors will be for a child node of this one
     *
     * TODO:  remove?  Currently this is replaced by the bloc-voting method but
     *        not sure it's better.  So we're keeping this method here for now.
     *
     * @param action  the action taken to reach the child
     * @param predicted  this object should have the desired external sensors
     *                   names.  The values will changed by the method based
     *                   on the rule votes.  (Note:  Probably just pass in
     *                   currExternal from an existing node.)
     * @return how confident we are in this prediction
     */
    public double predictExternalByVote(char action, SensorData predicted) {
        int numExtSensors = predicted.size();
        String[] sensorNames = predicted.getSensorNames().toArray(new String[0]);
        //first index: which sensor; second index: false/true
        double[][] votes = new double[numExtSensors][2];

        //Let each rule vote based on its match score
        for(TFRule rule: this.agent.getTfRules()) {
            if(rule.getAction() != action) continue;

            double score = rule.lhsMatchScore(action,this.currInternal, this.currExternal);

            for(int j = 0; j < sensorNames.length; j++){
                boolean on = (boolean) rule.getRHSExternal().getSensor(sensorNames[j]);
                votes[j][on ? 1 : 0] += score;
            }
        }

        //Calculate external values based on votes (winner take all)
        double confidence = 1.0;  //let's be optimistic to start...
        for(int j = 0; j < numExtSensors; j++) {
            predicted.setSensor(sensorNames[j], (votes[j][0] <= votes[j][1]));

            //indiv sensor confidence is based on fraction of votes received for
            // that sensor value (e.g., 3.61 "no" votes and 19.3 "yes" votes
            // = 0.84 confidence in "yes")
            //overall confidence is the product of confidence in each indiv sensor
            //TODO:  should we use an average instead of a product??
            double totalVotes = (votes[j][0] + votes[j][1]);
            if (totalVotes > 0.0) {
                confidence *= Math.max(votes[j][0], votes[j][1]) / totalVotes;
            } else {
                confidence = 0.0;  // no votes == no confidence
            }
        }

        return confidence;
    }//predictExternalByVote

    /**
     * predictExternalByBloc
     *
     * predicts what the externals sensors will be for a child node of this one
     * by going through the rule list and summing up each bloc's votes for external
     * sensors. The highest voted state for each sensor will be selected. A bloc is
     * determined by external sensors and action for a given rule. ie: 00a -> 01
     * and 00a -> 10 will be in the same bloc
     *
     * @param action  the action taken to reach the child
     * @param predicted  this object should have the desired external sensors
     *                   names.  The values will be changed by the method based
     *                   on the rule votes.  (Note:  Probably just pass in
     *                   currExternal from an existing node.)
     * @return how confident we are in this prediction
     */
    public double predictExternalByBloc(char action, SensorData predicted) {
        int numExtSensors = this.currExternal.size();

        // String is a String representation of a bloc (i.e "a00" or "b01")
        // TODO: Consider creating a BlocData object to make cleaner?
        HashMap<String, double[]> blocData = new HashMap<>();

        String[] sensorNames = predicted.getSensorNames().toArray(new String[0]);

        Vector<TFRule> tfRules = this.agent.getTfRules();

        // Go through each rule and add its vote to the blocData
        for (TFRule rule: tfRules) {
            // Ignore the rule if the action doesn't match
            if(rule.getAction() != action) continue;

            double score = rule.lhsMatchScore(action, this.currInternal,this.currExternal);
            score *= rule.getConfidence();

            String lhsKey = ""; // HashMap key String

            // Array containing data about the bloc in the following order:
            //first sensor on, first sensor off, second sensor on, second sensor off, bloc size
            double[] predictedExternal = new double[5];


            // Creates the key String for indexing into the HashMap and adds the rule's
            // vote for each lhs external sensor
            for(int j = 0; j < numExtSensors; j++){

                // Gets the value of the lhs external sensor
                boolean on = (boolean) rule.getLHSExternal().getSensor(sensorNames[j]);

                // Adds "1" or "0" to the key String depending on the sensor value
                lhsKey += on ? "1" : "0";

                // Gets the value of the prediction for the sensor
                on = (boolean) rule.getRHSExternal().getSensor(sensorNames[j]);

                // Adds the vote
                // Use a negative score for off and positive score for on to make summing the
                // total easier... maybe :)
                predictedExternal[on ? 2 * j : 2 * j + 1] += score;
            }

            // Set the bloc size to one, which is used to increment in later function calls
            predictedExternal[4] = 1.0;

            // If the bloc is already created, we update it with the new vote of the current rule
            if (blocData.containsKey(lhsKey)){
                double[] old = blocData.get(lhsKey);    // Gets the old data
                for(int i = 0; i < old.length; i++){

                    // Updates the data
                    // This also increments the bloc size by 1
                    predictedExternal[i] += old[i];
                }
            }
            // Puts the updated data back into the HashMap with the new value
            blocData.put(lhsKey, predictedExternal);

        }//for each tf rule

        // Go through the hashMap and calculate the highest vote for each action
        double confidence = 1.0;

        double[] totalVotes = new double[4];  //TODO:  shouldn't be hardcoded for two ext sensors

        // Counts up all the votes across the blocs, factoring in the size of the bloc
        for(double[] d: blocData.values()) {

            // Each sensor vote total is divided by the size of the bloc to ensure each
            // bloc has a single vote
            totalVotes[0] += d[0] / d[4];
            totalVotes[1] += d[1] / d[4];
            totalVotes[2] += d[2] / d[4];
            totalVotes[3] += d[3] / d[4];
        }

        //Construct the predicted SensorData
        for(int i = 0; i < numExtSensors; i++) {
            predicted.setSensor(sensorNames[i], totalVotes[2*i] >= totalVotes [2*i+1]);

            //calculate confidence in this sensor value
            double max = Math.max(totalVotes[2*i],totalVotes[2*i+1]);
            if(max > 0.0){
                confidence *= max / (totalVotes[2*i] + totalVotes[2*i+1]);
            } else {
                confidence = 0.0;
            }
        }

        return confidence;
    }

    /**
     * expand
     *
     * populates this.children
     */
    private void expand() {
        //check:  already expanded
        if (this.children.size() != 0) return;

        if (agent.getTfRules().size() < 1) return;

        int numActions = agent.getActionList().length;

        // bloc = lhs external sensors + action
        //  00a, 00b, 01a, 01b, etc.

        //Create predicted child node for each possible action
        for(int i = 0; i < numActions; i++) {

            char a = agent.getActionList()[i].getName().charAt(0);

            //Calculate the predicted external sensor values
            SensorData predictedExt = new SensorData(this.currExternal);
            double confidence = predictExternalByBloc(a, predictedExt);

            //create a child for this action
            TreeNode child = new TreeNode(this, a, predictedExt, confidence);
            this.children.add(child);
        }//for


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
                //TODO:  removed for now until we get PathRules back online
                //foundScore *= agent.metaScorePath(path);

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
     *          attempted to that but has been commented out
     * @return the path found
     */
    public Vector<TreeNode> findMostUncertainPath() {

        //pick the action (depth 1) with the greatest uncertainty
        char mostUncertain = '\0';
        double lowestConfSoFar = 1.1; //can't be this high
        for (TreeNode child : this.children) {
            //see if this child has more uncertainty
            if (child.confidence < lowestConfSoFar) {
                lowestConfSoFar = child.confidence;
                mostUncertain = child.action;
            }
        }

        //Just in case:  what if there are no children?
        if (mostUncertain == '\0') {
            mostUncertain = agent.getActionList()[rand.nextInt(agent.getActionList().length)].getName().charAt(0);
        }

        //Return a path with this action
        TreeNode tmpNode = new TreeNode(this, mostUncertain, this.currExternal, 1.0);
        return tmpNode.path;


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
    public HashSet<Integer> getCurrInternal() { return this.currInternal; }
    public SensorData getCurrExternal() { return this.currExternal; }
    public TreeNode getParent() { return this.parent; }

}//class TreeNode
