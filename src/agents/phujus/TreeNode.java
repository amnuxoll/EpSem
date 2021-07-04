package agents.phujus;

import framework.SensorData;

import java.util.HashMap;
import java.util.Vector;

/**
 * class TreeNode
 * <p>
 * Each instance is a node in an N-ary tree where N is the number of actions (i.e., the FSM's
 * alphabet) that tries to predict outcomes of sequences of actions.  Thus, we can "find" a
 * best (shortest, most confident) sequences of actions to reach the goal
 */

public class TreeNode {
    Vector<Rule> rules = new Vector<Rule>();  // all rules in the system

    private int episodeIndex; //associated timestep for this node

    //sensor values for this node
    private HashMap<Integer, Boolean> currInternal = new HashMap<>();

    private SensorData currExternal;

    //Agent
    private PhuJusAgent agent;

    //child nodes
    private TreeNode[] children;

    //bool for if tree has child or not
    private boolean childBool = true;

    //Character that the tree
    private char characterAction = '\0';

    private String path = "";


    /**
     * root node constructor
     */
    public TreeNode(PhuJusAgent initAgent, Vector<Rule> initRulesList, int initEpisodeIndex,
                    HashMap<Integer, Boolean> initCurrInternal, SensorData initCurrExternal, char action, String path) {
        //initializing agent and its children
        this.agent = initAgent;
        this.rules = initRulesList;
        this.children = new TreeNode[agent.getNumActions()]; // actionSize
        this.episodeIndex = initEpisodeIndex;
        this.currInternal = initCurrInternal;
        this.currExternal = initCurrExternal;
        this.characterAction = action;
        this.path = path + this.characterAction;
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
    private HashMap<String, double[]> pneHelper(char action, boolean updateActivation) {
        HashMap<String, double[]> result = new HashMap<String, double[]>();

        for (Rule r : rules) {
            //we only care about matching rules
            if (!r.matches(action, this.currExternal, this.currInternal)) {
                continue;
            }

            //get the current votes so far
            double[] votes = result.get(r.getRHSSensorName());
            if (votes == null) {
                votes = new double[]{0.0, 0.0};
                //Create a sensor data to initialize this child node
                result.put(r.getRHSSensorName(), votes);
            }

            //update predicted external value (currently a placeholder value for r.getActivation arg)
            double activationSum = 0.0;
            if (updateActivation) {
                r.setActivationLevel(this.episodeIndex, false, false, this.agent.getMatchArray(), this.agent.getMatchIdx(), this.agent.getRuleMatchHistoryLen());
                activationSum = r.calculateActivation(this.episodeIndex);
            } else {
                activationSum = 0.2;  //TODO: wtf is this?
            }

            votes[r.getRHSValue()] += activationSum;
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
     * @param updateActivation whether or not to update rule activation
     */
    private SensorData predictNextExternal(char action, boolean updateActivation) {
        //Gather the votes (and activate rules if asked)
        HashMap<String, double[]> nextExternal = pneHelper(action, updateActivation);

        //Create a sensor data to initialize this child node
        SensorData childSD = new SensorData(this.currExternal);
        for (String sensorName : childSD.getSensorNames()) {
            double[] votes = nextExternal.get(sensorName);
            if (votes == null) {
                childSD.setSensor(sensorName, false);
            } else {
                boolean newSensorVal = (votes[1] > votes[0]); //are yea's greater than nay's?
                childSD.setSensor(sensorName, Boolean.valueOf(newSensorVal)); // off
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
            this.setChildBool(false);
            return;
        }

        for (int i = 0; i < agent.getNumActions(); ++i) {
            char action = (char) ('a' + i);  //TODO: do this more elegantly

            //predict the sensor values for the next timestep
            // create separate predictedExternal entries for on vs off
            HashMap<Integer, Boolean> childInternal = agent.genNextInternal(action, this.currInternal, this.currExternal);
            SensorData childExternal = predictNextExternal(action, false);

            //Create the successors of this node
            //future?:  use a heuristic to decide here if it's worth looking at this node's successors ala A*Search
            this.children[i] = new TreeNode(this.agent, this.rules,
                    this.episodeIndex + 1, childInternal, childExternal, action, this.path);
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
        String result = this.characterAction + "->";

        //internal sensors
        for (Integer i : this.currInternal.keySet()) {
            result += (this.currInternal.get(i)) ? "1" : "0";
        }
        result += "|";

        //external sensors
        for (String s : this.currExternal.getSensorNames()) {
            Boolean val = (Boolean) this.currExternal.getSensor(s);
            result += val.booleanValue() ? "1" : "0";
        }

        return result;
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
        System.out.println(indent + "  " + this.toString());

        //base case: no children
        if (!this.childBool) {
            return;
        }

        //recursive case: print child nodes
        for (int i = 0; i < this.agent.getNumActions(); i++) {
            this.children[i].printTreeHelper(indent + "   ");
        }
    }//printTreeHelper


    /**
     * fbgpHelper
     * <p>
     * recurisve helper method for findBestGoalPath. Returns true if a path to the
     * goal is found
     */
    public boolean fbgpHelper(TreeNode tree) {
        if (tree.currExternal.isGoal()) {
            return true;
        }

        //recursively check if any of the children contain a goal path
        for (int i = 0; i < agent.getNumActions(); i++) {
            if (tree.children[i] != null) {
                if (fbgpHelper(tree.children[i])) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * findBestGoalPath
     * <p>
     * searches this tree for the best path to the goal and returns the first action
     * in the sequence of actions on that path
     */
    public String dfFindBestGoalPath(TreeNode tree) {
        //base case
        if (!tree.childBool) {
            return tree.getCharacterAction() + "";
        }

        //look for a path with the goal
        int goalPath = -1;
        for (int i = agent.getNumActions() - 1; i >= 0; i--) {
            if (fbgpHelper(tree.children[i])) {
                goalPath = i;
                break;
            }
        }

        //if no goal path is found, return nothing
        if (goalPath == -1) {
            return "";
        }

        return tree.getCharacterAction() + dfFindBestGoalPath(tree.children[goalPath]);
    }

    /**
     * bfFindBestGoalPath
     * <p>
     * searches this tree for the best path to the goal and returns the first action
     * in the sequence of actions on that path by using depth-first search
     */
    public String bfFindBestGoalPath(TreeNode tree) {
        //base case
        if (tree.currExternal.isGoal()) {
            return "";
        }

        //look for a path with the goal
        String paths = bfFbgpHelper(tree);
        if (paths.length() == 0) {
            return "";
        }

        String[] listOfPaths = paths.split(" ");
        String smallestPath = listOfPaths[0];
        for (int i = 1; i < listOfPaths.length; i++) {
            if (listOfPaths[i].length() < smallestPath.length()) {
                smallestPath = listOfPaths[i];
            }
        }

        return smallestPath;
    }

    /**
     * bfFindBestGoalPath
     * <p>
     * searches this tree for the best path to the goal and returns the first action
     * in the sequence of actions on that path by using breadth-first search
     */
    public String bfFbgpHelper(TreeNode tree) {
        if (tree == null) {
            return "";
        }

        //look for a path with the goal
        String pathsFound = "";
        for (int i = agent.getNumActions() - 1; i >= 0; i--) {
            if (tree.children[i] != null) {
                if (tree.children[i].currExternal.isGoal()) {
                    pathsFound = pathsFound + tree.children[i].getPath() + " ";
                }
            } else {
                return "";
            }
        }

        if (tree.children[0] == null) {
            return "";
        }
        return pathsFound + bfFbgpHelper(tree.children[0]) + bfFbgpHelper(tree.children[1]);
    }

    /**
     * setChildBool
     * <p>
     * set childBool to false when there are no children
     */
    public void setChildBool(boolean childFalse) {
        this.childBool = childFalse;
    }

    /**
     * getCharacterAction
     * <p>
     * get the character action instance variable
     */
    public char getCharacterAction() {
        return this.characterAction;
    }

    /**
     * getPath
     * <p>
     * get the character action instance variable
     */
    public String getPath() {
        return this.path;
    }


}//class TreeNode
