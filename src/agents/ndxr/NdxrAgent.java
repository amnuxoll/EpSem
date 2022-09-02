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
    private Rule prevRule = null;


    public NdxrAgent() {
    }

    @Override
    public void initialize(Action[] actions, IIntrospector introspector) {
        this.actions = actions;
        this.introspector = introspector;
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

        //Create a rule from the previous episode + current sensing
        if (this.prevExternal != null) {
            Rule newRule = new Rule(this.prevExternal, this.prevAction,
                                    this.currExternal, this.prevRule);
            this.prevRule = newRule;
            System.out.println(newRule);
        }

        //random action (for now)
        int actionIndex = this.random.nextInt(this.actions.length);
        Action action = this.actions[actionIndex];
        this.prevAction = action.toString().charAt(0);
        return action;
    }

}//class NdxrAgent
