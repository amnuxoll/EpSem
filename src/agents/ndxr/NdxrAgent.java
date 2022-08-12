package agents.ndxr;

import framework.*;
import utils.EpisodicMemory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Random;

public class NdxrAgent implements IAgent {

    private Action[] actions;
    private IIntrospector introspector;
    private Random random = utils.Random.getFalse();
    private Action lastGoalAction;

    public NdxrAgent() {
    }

    @Override
    public void initialize(Action[] actions, IIntrospector introspector) {
        this.actions = actions;
        this.introspector = introspector;
        this.lastGoalAction = actions[0];
    }

    @Override
    public Action getNextAction(SensorData sensorData) throws Exception {

        //random action
        int actionIndex = this.random.nextInt(this.actions.length);
        Action action = this.actions[actionIndex];
        return action;

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
}
