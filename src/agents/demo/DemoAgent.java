package agents.demo;

import framework.*;
import utils.EpisodicMemory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Random;

/**
 *
 * This agent was written as a quick study for creating a new agent with a basic
 * heuristic for randomly selecting between a random action and the last
 * action taken before a goal.
 *
 * @author Zachary Faltersack
 */
public class DemoAgent implements IAgent {

    private EpisodicMemory<Episode> episodicMemory;

    private Action[] actions;
    private IIntrospector introspector;
    private Random random;
    private Action lastGoalAction;

    public DemoAgent(Random random)
    {
        // For unit testing purposes we inject a Random object that can be seeded to produce
        // specific, repeating behaviors.
        this.random = random;

        // This data structure will retain our history of episodes that we generate.
        this.episodicMemory = new EpisodicMemory<>();
    }

    @Override
    public void initialize(Action[] actions, IIntrospector introspector) {
        // Initialize is called prior to running an Experiment in an environment.
        // The available actions for the environment are provided
        this.actions = actions;

        // The introspector can be used to query for information related to the
        // environment. This is strictly for analytics during data collection.
        // DO NOT CHEAT and use this to "discover" optimal action sequences.
        this.introspector = introspector;
        this.lastGoalAction = actions[0];
    }

    @Override
    public Action getNextAction(SensorData sensorData) throws Exception {

        // The very first experience in the environment is treated as though
        // the agent has just located the GOAL, so special boundary checks are
        // required, such as confirming that Episodes exist.
        if (sensorData.isGoal() && this.episodicMemory.any())
            this.lastGoalAction = this.episodicMemory.current().getAction();

        if (this.random.nextBoolean())
            return this.lastGoalAction;
        else {
            int actionIndex = this.random.nextInt(this.actions.length);
            Action action = this.actions[actionIndex];

            this.episodicMemory.add(new Episode(sensorData, action));

            return action;
        }
    }

    @Override
    public String[] getStatisticTypes() {
        // Additional data can be gathered for analysis by declaring the types of
        // analytics gathered prior to running the Experiment.
        HashSet<String> statistics = new HashSet<>();
        statistics.addAll(Arrays.asList(IAgent.super.getStatisticTypes()));
        statistics.add("actionConfidence");
        return statistics.toArray(new String[0]);
    }

    /**
     * Gets the actual statistical data for the most recent iteration of hitting a goal.
     *
     * @return the collection of {@link Datum} that indicate the agent statistical data to track.
     */
    @Override
    public ArrayList<Datum> getGoalData() {
        // When the goal is hit, report the additional analytics the agent should be tracking.
        ArrayList<Datum> data = new ArrayList<>();
        data.add(new Datum("actionConfidence", this.random.nextDouble()));
        return data;
    }
    //for landon
}
