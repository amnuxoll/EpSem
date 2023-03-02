package agents.ndxr;

import framework.*;
import utils.EpisodicMemory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Random;

/**
 * NdxrAgent
 *
 * This agent is intended to be like PhuJus but scalable.  In particular,
 * it allows the agent to find and merage similar rules with a minimal
 * loss of knowledge.
 */
public class NdxrAgent implements IAgent {
    /** maximum number of rules allowed */
    public static final int MAX_NUM_RULES = 1000;

    //a list of valid actions in the env
    private Action[] actions;
    //allows you to configure data gathered about agent performance (not used atm)
    private IIntrospector introspector;
    //Use this for all random number generation in this agent
    private Random random = utils.Random.getFalse();


    //The rules which matched the agent's previous experience
    private ArrayList<Rule> prevInternal = new ArrayList<>();
    //The rules which are matching the agent's current experience
    private ArrayList<Rule> currInternal = null;

    //The external sensors the agent saw on the last time step
    private SensorData prevExternal = null;
    //The external sensors the agent saw on the current time step
    private SensorData currExternal = null;
    //The last action the agent took (prev time step)
    private char prevAction = '?';
    //The rule used to select the next action
    private ArrayList<Rule> prevRules = new ArrayList<>();

    /** Rules are kept an index to that provides fast lookup and keeps the
     * total number of rules below a global maximum.
     * Note: this can't be initialized until after this.action is set. */
    private RuleIndex rules = null;

    //ctor may be needed someday?
    public NdxrAgent() {
    }

    @Override
    public void initialize(Action[] actions, IIntrospector introspector) {
        this.actions = actions;
        this.introspector = introspector;
        this.rules = new RuleIndex(this);
    }

    /**
     * getNextAction
     *
     * called by the environment each time step to request the agent's next action
     *
     * @param sensorData The agent's current sensing ({@link SensorData})
     * @return the agent's selected action
     */
    @Override
    public Action getNextAction(SensorData sensorData) throws Exception {

        //update the sensor logs
        this.prevInternal = this.currInternal;
        this.prevExternal = this.currExternal;
        this.currExternal = sensorData;

        ruleMaintenance();

        //Create new rules from the previous episode + current sensing
        if (this.prevExternal != null) {
            //new depth zero rule
            Rule newRule = new Rule(this.prevExternal, this.prevAction,
                    this.currExternal, null);
            this.rules.addRule(newRule);
            ArrayList<Rule> newPRSet = new ArrayList<>();
            newPRSet.add(newRule);

            //new rules for other depths
            for (Rule pr : this.prevRules) {
                if (pr.getDepth() >= Rule.MAX_DEPTH) break;
                newRule = new Rule(this.prevExternal, this.prevAction,
                        this.currExternal, pr);
                this.rules.addRule(newRule);
                newPRSet.add(newRule);
            }

            //set prevRules for next iteration
            this.prevRules = newPRSet;
        }//create new rules with this episode

        //print all rules
        this.rules.printAll();

        //random action (for now)
        int actionIndex = this.random.nextInt(this.actions.length);
        Action action = this.actions[actionIndex];
        this.prevAction = action.toString().charAt(0);  //This trick may not work in the future...
        return action;
    }

    /**
     * ruleMaintenance
     *
     * updates the data in the rules that match the experience the agent just had
     */
    public void ruleMaintenance() {
        //Skip first timestep
        if (this.prevAction == '?') return;

        //Get the rules that match what the agent just experienced
        this.currInternal = this.rules.findMatches(this.prevInternal,
                                                    this.prevExternal,
                                                    this.prevAction,
                                                    this.currExternal);
    }//ruleMaintenance


    public Action[] getActions() { return this.actions; }
    public SensorData getCurrExternal() { return currExternal; }

}//class NdxrAgent
