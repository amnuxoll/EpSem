package agents.phujus;

import framework.SensorData;

import java.util.ArrayList;
import java.util.HashMap;

/**
 *
 * class TreeNode
 *
 * Each instance is a node in an N-ary tree where N is the number of actions (i.e., the FSM's
 * alphabet) that tries to predict outcomes of sequences of actions.  Thus, we can "find" a
 * best (shortest, most confident) sequences of actions to reach the goal
 *
 */

public class TreeNode {
    private PhuJusAgent agent; //Agent

    private ArrayList<Rule> rulesList = new ArrayList<>();  // all rules in the system

    private int episodeIndex;     //associated timestep for this node

    //sensor values for this node
    private HashMap<Integer, Boolean> currInternal = new HashMap<>();
    private SensorData currExternal;

    //child nodes
    private TreeNode[] children;

    //bool for if tree has child or not
    private boolean childBool = true;

    //Character that the tree
    private char characterAction = '\0';

    /** root node constructor */
    public TreeNode(PhuJusAgent initAgent, ArrayList<Rule> initRulesList, int initEpisodeIndex,
                    HashMap<Integer, Boolean> initCurrInternal, SensorData initCurrExternal, char action) {
        //initializing agent and its children
        this.agent = initAgent;
        this.rulesList = initRulesList;
        children = new TreeNode[agent.getNumActions()]; // actionSize
        this.episodeIndex = initEpisodeIndex;
        this.currInternal = initCurrInternal;
        this.currExternal = initCurrExternal;
        this.characterAction = action;
    }

    /** recursively generate successor nodes */
    public void genSuccessors(int depth) {
        //base case
        if (depth == 0) {
            this.setChildBool(false);
            return;
        }

        for(int i = 0; i < agent.getNumActions(); ++i) {
            char action = (char) ('a' + i);

            //predicted sensor values for t+1
            HashMap<Integer, Boolean> nextInternal = new HashMap<>();

            // create separate predictedExternal entries for off(0) vs on(1)
            HashMap<String, double[]> predictedExternal = new HashMap<>();

            genNextSensors(action, nextInternal, predictedExternal, false);

            //Create a sensor data to initialize this child node
            SensorData childSD = new SensorData(this.currExternal);
            for(String sensorName : predictedExternal.keySet()) {
                double[] votes = predictedExternal.get(sensorName);
                boolean newSensorVal = votes[1] > votes[0];
                childSD.setSensor(sensorName, new Boolean(newSensorVal)); // off
            }

            this.children[i] = new TreeNode(this.agent, this.rulesList,
                    this.episodeIndex + 1, nextInternal, childSD, action);

            //future:  decide here if it's worth looking at this child's successors?

            //Create the successors of this child
            this.children[i].genSuccessors(depth - 1);

        }//for
    }//genSuccessor

    /**
     * genNextSensors
     *
     * generates the nextInternal and predictedExternal sensor values for a
     * given action for this rule
     *
     * CAVEAT:  predictedExternal and nextInternal should not be null!

     */
    public void genNextSensors(char action, HashMap<Integer, Boolean> nextInternal,
                               HashMap<String, double[]> predictedExternal, boolean updateActivation ) {
        for(Rule r : this.rulesList) {
            int sIndex = r.getRHSInternal();
            if (r.matches(action, this.currExternal, this.currInternal)) {
                if (sIndex != -1) {
                    nextInternal.put(sIndex, true); //on
                }

                //get the current votes so far
                double[] votes = predictedExternal.get(r.getRHSExternalSensorName());
                if (votes == null) {
                    votes = new double[]{0.0, 0.0};
                    //Create a sensor data to initialize this child node
                    predictedExternal.put(r.getRHSExternalSensorName(), votes);
                }
                //update predicted external value (currently a placeholder value for r.getActivation arg)
                if(updateActivation){
                    r.setActivation(this.episodeIndex);
                }
                votes[r.getRHSExternalValue()] += r.getActivation();
            }
            else {
                if (sIndex != -1) {
                    nextInternal.put(sIndex, false); //off
                }
                if(updateActivation) {
                    r.reduceActivation();
                }
            }
        }//for
    }//genNextSensors

    /**
     * findBestGoalPath
     *
     * searches this tree for the best path to the goal and returns the first action
     * in the sequence of actions on that path
     */
    public String findBestGoalPath(TreeNode tree) {
        //base case
        if (!tree.childBool) {
            return tree.getCharacterAction() + "";
        }

        //look for a path with the goal
        int goalPath = -1;
        for (int i = agent.getNumActions() -1; i >= 0; i--) {
            if (fbgpHelper(tree.children[i])) {
                goalPath = i;
                break;
            }
        }

        //if no goal path is found, return nothing
        if(goalPath == -1) {
            return "";
        }

        return tree.getCharacterAction() + findBestGoalPath(tree.children[goalPath]);
    }

    /**
     * fbgpHelper
     *
     * recurisve helper method for findBestGoalPath. Returns true if a path to the
     * goal is found
     *
     */
    public boolean fbgpHelper(TreeNode tree) {
        if (tree.currExternal.isGoal()) {
            return true;
        }

        //recursively check if any of the children contain a goal path
        for (int i = 0; i < agent.getNumActions(); i++) {
            if (tree.children[i] != null) {
                if(fbgpHelper(tree.children[i])) {
                    return true;
                }
            }
        }

        return false;
    }

    //DEBUG
    public void printTree(){
        System.out.print(this.characterAction);
        for(Integer i : this.currInternal.keySet()){
            System.out.print('(' + String.valueOf(i.intValue()) + ',' + this.currInternal.get(i) + ')');
        }
        System.out.print("|");
        for (String s : this.currExternal.getSensorNames()) {
            System.out.print(this.currExternal.getSensor(s) + " ");
        }
        if(!this.childBool){
            return;
        } else {
            System.out.print(" -> ");
        }
        for (int i = 0; i < this.agent.getNumActions(); i++) {
            System.out.print("( ");
            this.children[i].printTree();
            System.out.print(") ");
        }
    }

    //Getters and Setters for TreeNode instance variables
    /**
     * setChildBool
     *
     * set childBool to false when there are no children
     *
     */
    public void setChildBool(boolean childFalse) {
        this.childBool = childFalse;
    }

    /**
     * getCharacterAction
     *
     * get the character action instance variable
     *
     */
    public char getCharacterAction() {
        return this.characterAction;
    }



}//class TreeNode
