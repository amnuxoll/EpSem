package agents.phujus;

import environments.fsm.FSMEnvironment.Sensor;
import framework.SensorData;

import java.util.ArrayList;

/**
 * class TreeNode
 *
 * Each instance is a node in an N-ary tree where N is the number of actions (i.e., the FSM's
 * alphabet) that tries to predict outcomes of sequences of actions.  Thus, we can "find" a
 * best (shortest, most confident) sequences of actions to reach the goal
 */
public class TreeNode {
    Rule[] rulesList = new Rule[PhuJusAgent.MAXNUMRULES];  // all rules in the system

    private int episodeIndex; //associated timestep for this node

    //sensor values for this node
    private int[] currInternal = new int[PhuJusAgent.NUMINTERNAL];


    //TODO check to see if we actually need to change currExternal's type from SensorData[] to SensorData
    //private SensorData[] currExternal = new SensorData[PhuJusAgent.NUMEXTERNAL];
    private SensorData currExternal;

    //Agent
    private PhuJusAgent agent;

    //child nodes
    private TreeNode[] children;

    /** root node constructor */
    public TreeNode(PhuJusAgent initAG, ArrayList<Rule> initRL, int initEI, int[] InitCI, SensorData initCE) {
        //TODO: code here to init the instance variables

        //initializing agent and its children
        this.agent = initAG;
        children = new TreeNode[agent.getNumActions()]; // actionSize


    }

    public TreeNode(Rule[] rulesList, int initEI, byte[] nextInternal, double[][] predictedExternal) {

    }

    /** recursively generate successor nodes */
    public void genSuccessors(int depth) {
        //base case
        if (depth == 0) return;

        for(int i = 0; i < agent.getNumActions(); ++i) {
            char action = (char) ('a' + i);

            //predicted sensor values for t+1
            byte[] nextInternal = new byte[PhuJusAgent.NUMINTERNAL];
            double[][] predictedExternal = { {0.0, 0.0, 0.0, 0.0}, {0.0, 0.0, 0.0, 0.0} };
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

            for(Rule r : rulesList) {
                int sIndex = r.getAssocSensor();
                if (r.matches(action, this.currExternal, this.currInternal)) {
                    if (sIndex != -1) {
                        nextInternal[sIndex] = 1;  //on
                    }

                    //update predicted external value (currently a placeholder value for r.getActivation arg)
                    predictedExternal[r.getRHSValue()][r.getRHSIndex()] += r.getActivation(2);

                } else {
                    if (sIndex != -1) {
                        nextInternal[sIndex] = 0;  //off
                    }
                }
            }//for

            //Calculate final predicted values for the external sensors
            // (winner take all)
            /*
            Sensor[] nextExternal = new Sensor[PhuJusAgent.NUMEXTERNAL];
            for(int j = 0; j < PhuJusAgent.NUMEXTERNAL; ++j) {
                byte val = 0;
                if (predictedExternal[1][j] > predictedExternal[0][j]) {
                    val = 1;
                }

                //TODO fix this error
                nextExternal[j] = new Sensor(val);
            }

             */


            this.children[i] = new TreeNode(this.rulesList,
                    this.episodeIndex + 1,
                    nextInternal,
                    predictedExternal);

            //future:  decide here if it's worth looking at this child's successors?

            //Create the successors of this child
            this.children[i].genSuccessors(depth - 1);

        }//for



    }//genSuccessor

    /**
     * fbgpHelper
     *
     * recurisve helper method for findBestGoalPath
     *
     * @return a tuple containing: action to take, and confidence in that action.
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
