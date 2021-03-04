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
    public TreeNode(PhuJusAgent initAG, ArrayList<Rule> initRL, int initEI, int[] initCI, SensorData initCE) {
        //initializing agent and its children
        this.agent = initAG;
        this.rulesList = initRL;
        children = new TreeNode[agent.getNumActions()]; // actionSize
        this.episodeIndex = initEI;
        this.currInternal = initCI;
        this.currExternal = initCE;

    }

    /** constructor for creating children */
    public TreeNode(PhuJusAgent agt, ArrayList<Rule> rulesList, int initEI, int[] nextInt,
                    HashMap<String, Integer> nextExt) {
        //Passes in the agent that is being used by the other tree nodes
        this.agent = agt;
        this.rulesList = rulesList;
        this.episodeIndex = initEI;
        this.currInternal = nextInt;
        this.currExternal = new SensorData(false); //TODO how to set SensorData goal sensor
        children = new TreeNode[agent.getNumActions()];
        for(HashMap.Entry<String, Integer> entry : nextExt.entrySet()) {
            String key = entry.getKey();
            int value = entry.getValue();
            if (value == 0) {
                this.currExternal.setSensor(key, false);
            } else {
                this.currExternal.setSensor(key, true);
            }
        }
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
                    double[] activation = {0.0, 0.0};
                    activation[0] = r.getActivation(this.episodeIndex)[0] + r.getLastActivation()[0];
                    activation[1] = r.getActivation(this.episodeIndex)[1] + r.getLastActivation()[1];

                    r.setLastActivation(activation);

                    //update predicted external value (currently a placeholder value for r.getActivation arg)
                    predictedExternal.put(r.getRHSSensorName(), activation);
                } else {
                    if (sIndex != -1) {
                        nextInternal[sIndex] = 0;  //off
                    }
                }
            }//for


            // calculate nextExternal based on winner takes all from predictedExternal
            HashMap<String, Integer> nextExternal = new HashMap<>();
            for(HashMap.Entry<String, double[]> entry : predictedExternal.entrySet()) {
                int value = 0;
                String key = entry.getKey();
                double[] arr = entry.getValue();
                if(arr[1] > arr[0]) {
                    value = 1;
                    nextExternal.put(key, value);
                }
                else {
                    nextExternal.put(key, value);
                }
            }




            this.children[i] = new TreeNode(this.agent, this.rulesList,
                    this.episodeIndex + 1,
                    nextInternal,
                    nextExternal);

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
     * recurisve helper method for findBestGoalPath
     *
     */
    public void fbgpHelper() {
        //%%%% We'll pick up where we left off here %%%
    }

    /**
     * findBestGoalPath
     *
     * searches this tree for the best path to the goal and returns the first action
     * in the sequence of actions on that path
     */
    public char findBestGoalPath() {
        //base case
        //if (this.currExternal[GOALSENSOR] == 1) return ??;

        //recursive case where you
        return ' ';
    }


}//class TreeNode
