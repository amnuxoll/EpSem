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
 * is a test agent created as a proof of concept for doing two-phase matching
 * with EpMem rules.  Once this functionality is working, it's intended be
 * combined with the PhuJus agent to create new more scalable version of
 * PhuJus.
 *
 * A key intended approach is the use of Bloom Filters (or something like
 * them) to rapidly find similar rules but also as a way of merging rules
 * together with a minimal loss of knowledge.
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
        this.prevExternal = this.currExternal;
        this.currExternal = sensorData;

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


    public Action[] getActions() { return this.actions; }
    public SensorData getCurrExternal() { return currExternal; }

}//class NdxrAgent
