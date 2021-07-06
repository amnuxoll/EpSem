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

            //each rule votes with its activation level
            votes[r.getRHSIntValue()] += r.calculateActivation(agent.getNow());
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
        SensorData childSD = new SensorData(this.currExternal);
        for (String sensorName : childSD.getSensorNames()) {
            double[] votes = nextExternal.get(sensorName);
            if (votes == null) {
                childSD.setSensor(sensorName, false);
            } else {
                boolean newSensorVal = (votes[1] > votes[0]); //are yea's greater than nay's?
                childSD.setSensor(sensorName, newSensorVal); // off
            }
        }
        return childSD;
    }//predictNextExternal

    /**
     * genSuccessors
     * <p>
     * recursively generates successor nodes up to a given depth.  These
     * new nodes are placed in this.children (creating a tree)
     */
    public void genSuccessors(int depth) {
        //base case
        if (depth == 0) {
            this.isLeaf = true;
            return;
        }

        for(int i = 0; i < agent.getActionList().length; ++i) {
            char action = agent.getActionList()[i].getName().charAt(0);

            //predict the sensor values for the next timestep
            // create separate predictedExternal entries for on vs off
            HashMap<Integer, Boolean> childInternal = agent.genNextInternal(action, this.currInternal, this.currExternal);
            SensorData childExternal = predictNextExternal(action);

            //Create the successors of this node
            //future?:  use a heuristic to decide here if it's worth looking at this node's successors ala A*Search
            this.children[i] = new TreeNode(this.agent, this.rules,
                    this.episodeIndex + 1, childInternal, childExternal, this.path + action);
            this.children[i].genSuccessors(depth - 1);

        }//for

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
        if ( (this.currExternal.isGoal()) && (! this.path.equals("")) ) {
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
        if ( (this.currExternal.isGoal()) && (! this.path.equals("")) ) return this.path;

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


}//class TreeNode
