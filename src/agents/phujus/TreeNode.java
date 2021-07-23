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
    Vector<Rule> rules;

    //associated timestep for this node
    private final int episodeIndex;

    //sensor values for this node
    private final HashMap<Integer, Boolean> currInternal;
    private final SensorData currExternal;

    //child nodes
    private final TreeNode[] children;

    //bool for if tree has children or not
    private boolean isLeaf = false;

    //This is the path used to reach this node (the root should be "")
    private final String path;


    /**
     * root node constructor
     */
    public TreeNode(PhuJusAgent initAgent, Vector<Rule> initRulesList, int initEpisodeIndex,
                    HashMap<Integer, Boolean> initCurrInternal, SensorData initCurrExternal, String path) {
        //initializing agent and its children
        this.agent = initAgent;
        this.rules = initRulesList;
        this.children = new TreeNode[agent.getNumActions()]; // actionSize
        this.episodeIndex = initEpisodeIndex;
        this.currInternal = initCurrInternal;
        this.currExternal = initCurrExternal;
        this.path = path;
    }

    /**
     * pneHelper
     * <p>
     * helper method for {@link #predictNextExternal}.  This method gathers "votes" for
     * either a true or false value for each sensor based on rules that predict
     * either value.  If no value is predicted, then no vote entry is created
     * in the result.
     *
     * @return a hash map of sensor name to true and false vote tallies
     * (each as a two-cell double[])
     */
    private HashMap<String, double[]> pneHelper(char action) {
        HashMap<String, double[]> result = new HashMap<>();

        for (Rule r : rules) {
            //we only care about matching rules
            if (!r.matches(action, this.currExternal, this.currInternal)) {
                continue;
            }

            //get the current votes so far
            double[] votes = result.computeIfAbsent(r.getRHSSensorName(), k -> new double[]{0.0, 0.0});

            //each rule votes with its accuracy
            votes[r.getRHSIntValue()] += r.getAccuracy();
        }//for

        return result;
    }//pneHelper

    /**
     * predictNextExternal
     * <p>
     * generates the next external sensor values for a given action for this
     * rule.  This method will also update the activation of rules that match
     * if asked.
     *
     * @param action           the action selected by the agent
     */
    private SensorData predictNextExternal(char action) {
        //Gather the votes (and activate rules if asked)
        HashMap<String, double[]> nextExternal = pneHelper(action);

        //Create a sensor data to initialize this child node
        SensorData childSD = SensorData.createEmpty();
        for (String sensorName : nextExternal.keySet()) {
            double[] votes = nextExternal.get(sensorName);
            if (votes != null) {
                boolean newSensorVal = (votes[1] > votes[0]); //are yea's greater than nay's?
                childSD.setSensor(sensorName, newSensorVal);
            }
        }
        return childSD;
    }//predictNextExternal

    /**
     * genSuccessors
     * <p>
     * recursively generates the tree in an iterative deepending fashion
     * stopping when a goal node is found (or max depth is reached)
     */
    public void genSuccessors() {
        for(int max = 1; max <= PhuJusAgent.MAXDEPTH; ++max) {
            Vector<TreeNode> visited = new Vector<>();
            int goalDepth = genSuccHelper(0, max, visited);
            if (goalDepth < max)
                break;
        }
    }


    /**
     * genSuccHelper
     *
     * recursive helper method for {@link #genSuccessors}
     *
     * @param depth  depth of this node
     * @param maxDepth maximum depth allowed
     * @param visited nodes we've visited so far
     *
     * @return the depth that a goal was found at (or maxDepth if not found)
     */
    public int genSuccHelper(int depth, int maxDepth, Vector<TreeNode> visited) {
        //base case:  found goal
        if (isGoalNode()) {
            this.isLeaf = true;
            return depth;
        }

        //base case:  max depth
        if (depth >= maxDepth) {
            this.isLeaf = true;
            return maxDepth;
        }

        //Recursive case: examine child nodes
        for(int i = 0; i < agent.getActionList().length; ++i) {
            //Create the child node for this action if it doesn't exist yet
            TreeNode child = this.children[i];
            if (child == null) {
                char action = agent.getActionList()[i].getName().charAt(0);

                //predict the sensor values for the next timestep
                // create separate predictedExternal entries for on vs off
                HashMap<Integer, Boolean> childInternal = agent.genNextInternal(action, this.currInternal, this.currExternal);
                SensorData childExternal = predictNextExternal(action);

                child = new TreeNode(this.agent, this.rules,
                          this.episodeIndex + 1, childInternal,
                                        childExternal, this.path + action);
                this.children[i] = child;
            } //if create new child node
            else {
                child.isLeaf = false; //reset from any prev use of this child
            }

            //Reasons not to expand this child node (effectively more base cases):
            // 1.  no external sensors are set in the child
            // 2.  we've seen this node elsewhere in the tree
            if ( (child.currExternal.size() == 0) || (visited.contains(child)) ) {
                child.isLeaf = true;
                continue;
            }

            //Recursive case
            visited.add(child);
            int newDepth = child.genSuccHelper(depth + 1, maxDepth, visited);
            if (newDepth < maxDepth){
                maxDepth = newDepth;
            }

        }//for

        return maxDepth;

    }//genSuccessors


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
     * findBestGoalPath
     * <p>
     * searches this tree for the best path to the goal and returns the first action
     * in the sequence of actions on that path
     *
     * @return a goal path if found or empty string ("") if no goal path was found.
     */
    public String findBestGoalPath() {
        //base case #1: Goal Node found (not at root)
        if (isGoalNode()) return this.path;

        //base case #2:  Leaf Node (no goal found)
        if (this.isLeaf) return "";

        //search all possible actions
        String bestPath = "";
        for(TreeNode child : this.children) {
            String candPath = child.findBestGoalPath();
            if ((!candPath.equals("")) && ((bestPath.equals("")) || (bestPath.length() > candPath.length()))) {
                bestPath = candPath;
            }
        }

        return bestPath;

    }//findBestGoalPath

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

    /**
     * isValidPath
     *
     * checks to see if a previous path is still valid after an action from it is taken
     *
     * @return true is the previous path is still valid, false if new path is needed
     */
    public boolean isValidPath(String prevPath) {
        // Check to make sure there is a path
        if(prevPath.equals("")) {
            return false;
        }

        // Path-to-take and Path-to-Nodes are reversed
        // So compare first char to in Path-to-take to last char in Path-to-Node
        char nextStep = prevPath.charAt(0);
        for(TreeNode child : this.children) {
            if(child.path.charAt(child.path.length()-1) == nextStep) {
                if(child.isGoalNode()) {
                    return true;
                } else if(child.isLeaf) {
                    //this likely happens because the tree was truncated by
                    // genSuccessors() when it found a shorter path.  We don't
                    //want to switch to a shorter path mid-stream (causes loops)
                    //so stick with this one and assume it's okay
                    return true;
                } else {
                    child.isValidPath(prevPath.substring(1));
                }
            }
        }

        // If we don't find children, return false
        return false;

    }//isValidPath

    /**
     * equals
     *
     * method is overridden to only compare sensors and most recent action in the path
     */
    @Override
    public boolean equals(Object obj) {
        if (! (obj instanceof TreeNode) ) return false;
        TreeNode other = (TreeNode)obj;

        //compare actions
        if (! other.path.equals(this.path)) {
            if (other.path.equals("")) return false;
            if (this.path.equals("")) return false;
            char thisAct = this.path.charAt(this.path.length() - 1);
            char otherAct = other.path.charAt(other.path.length() - 1);
            if (thisAct != otherAct) return false;
        }

        //compare sensors
        if (! other.currExternal.equals(this.currExternal)) return false;
        if (! other.currInternal.equals(this.currInternal)) return false;

        //All tests passed
        return true;

    }//equals

    public boolean matches(Rule r) {
        // Special case: Looking at the root of the tree
        if(this.path.equals("")) {
            return false;
        }
        return r.matches(this.path.charAt(path.length()-1), this.currExternal, this.currInternal);
    }//matches

    public TreeNode[] getChildren() {
        return this.children;
    }

    public boolean isLeaf() {
        return this.isLeaf;
    }

    public boolean hasRHS(String sName, boolean val) {
        return (this.currExternal.hasSensor(sName) &&
                ((Boolean) this.currExternal.getSensor(sName)) == val);
    }

}//class TreeNode
