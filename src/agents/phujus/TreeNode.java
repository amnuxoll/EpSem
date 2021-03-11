package agents.phujus;

import environments.fsm.FSMEnvironment.Sensor;
import framework.SensorData;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Arrays;

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
    ArrayList<Rule> rulesList = new ArrayList<>();  // all rules in the system

    private int episodeIndex; //associated timestep for this node

    //sensor values for this node
    private int[] currInternal = new int[PhuJusAgent.NUMINTERNAL];

    private SensorData currExternal;

    //Agent
    private PhuJusAgent agent;

    //child nodes
    private TreeNode[] children;

    //bool for if tree has child or not
    private boolean childBool = true;

    //weights for predicting the external sensors
    private HashMap<String, double[]> predictedExternal = new HashMap<>();


    /** root node constructor */
    public TreeNode(PhuJusAgent initAgent, ArrayList<Rule> initRulesList, int initEpisodeIndex, int[] initCurrInternal, SensorData initCurrExternal) {
        //initializing agent and its children
        this.agent = initAgent;
        this.rulesList = initRulesList;
        children = new TreeNode[agent.getNumActions()]; // actionSize
        this.episodeIndex = initEpisodeIndex;
        this.currInternal = initCurrInternal;
        this.currExternal = initCurrExternal;

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
            int[] nextInternal = new int[PhuJusAgent.NUMINTERNAL];

            /*
            Example:
            x: ....1..a->..1.  (act=0.8172)
            y: .....0.a->..0.  (act=0.7115)
            z: 01.....a->..0.  (act=0.2005)

            {0.0, 0.0, 0.9120, 0.0}, {0.0, 0.0, 0.8172, 0.0}

            */


            //initialize nextInternal (all zeroes)
            for (int j = 0; j < nextInternal.length; j++) {
                nextInternal[j] = 0;
            }

            // create separate predictedExternal entries for on vs off
            for(Rule r : rulesList) {
                int sIndex = r.getAssocSensor();
                if (r.matches(action, this.currExternal, this.currInternal)) {
                    if (sIndex != -1) {
                        nextInternal[sIndex] = 1;  //on
                    }

                    //get the current votes so far
                    double[] votes = predictedExternal.get(r.getRHSSensorName());
                    if (votes == null) {
                        votes = new double[]{0.0, 0.0};
                        //Create a sensor data to initialize this child node
                        predictedExternal.put(r.getRHSSensorName(), votes);
                    }
                    //update predicted external value (currently a placeholder value for r.getActivation arg)
                    votes[r.getRHSValue()] += r.getActivation(this.episodeIndex);
                } else {
                    if (sIndex != -1) {
                        nextInternal[sIndex] = 0;  //off
                    }
                }
            }//for

            //Create a sensor data to initialize this child node
            SensorData childSD = new SensorData(this.currExternal);
            for(String sensorName : predictedExternal.keySet()) {
                double[] votes = predictedExternal.get(sensorName);
                boolean newSensorVal = votes[1] > votes[0];
                childSD.setSensor(sensorName, new Boolean(newSensorVal)); // off
            }

            this.children[i] = new TreeNode(this.agent, this.rulesList,
                    this.episodeIndex + 1, nextInternal, childSD);

            //future:  decide here if it's worth looking at this child's successors?

            //Create the successors of this child
            this.children[i].genSuccessors(depth - 1);

        }//for



    }//genSuccessor

    public void printTree(){
        for (int i = 0; i < this.currInternal.length; i++) {
            System.out.print(this.currInternal[i]);
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
     * fbgpHelper
     *
     * recurisve helper method for findBestGoalPath. Kills the path
     *
     */
    public boolean fbgpHelper(TreeNode tree) {
        if (tree.currExternal.isGoal()) {
            return true;
        }

        for (int i = 0; i < agent.getNumActions(); i++) {
            if (tree.children[i] != null) {
                return fbgpHelper(tree.children[i]);
            }
        }
        return false;
    }

    /**
     * findBestGoalPath
     *
     * searches this tree for the best path to the goal and returns the first action
     * in the sequence of actions on that path
     */
    public String findBestGoalPath(TreeNode tree) {
        //base case
        if (!tree.childBool) {
            return "";
        }

        int goalPath = 0;
        for (int i = 0; i < agent.getNumActions(); i++) {
            if (fbgpHelper(tree.children[i])) {
                goalPath = i;
                break;
            }
        }

        for(Rule r: rulesList) {
            if (r.matches(this.currExternal, this.currInternal)) {
                return r.getChar() + findBestGoalPath(tree.children[goalPath]);
            }
        }

        return "";
    }


}//class TreeNode
