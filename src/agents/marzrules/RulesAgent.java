package agents.marzrules;

import framework.*;
import utils.EpisodicMemory;
import utils.SequenceGenerator;

import java.util.ArrayList;

/**
 * Created by Ryan on 2/11/2019.
 */
public class RulesAgent implements IAgent {
    //private ActionEvaluator actionEvaluator;
    private Ruleset ruleset;
    private Action previousAction;
    private IIntrospector introspector;
    private RuleSetEvaluator ruleSetEvaluator;
    private Heuristic heuristic;
    private int maxDepth;
    private int explores = 0;
    private int actionsSinceGoal = 0;
    private boolean independentDrivers;
    private boolean resetWithAnyOffroad;
    private boolean singleDriver;
    private EpisodicMemory<Episode> episodicMemory;
    private static final int LOOKBACK_GOALS = 5;
    //ArrayDeque<Integer> goalIndicies;

    public RulesAgent(Heuristic heuristic, int maxDepth, boolean independentDrivers, boolean resetWithAnyOffroad, boolean singleDriver) {
        this.heuristic = heuristic;
        this.maxDepth = maxDepth;
        this.independentDrivers = independentDrivers;
        this.resetWithAnyOffroad = resetWithAnyOffroad;
        this.singleDriver = singleDriver;
    }

    @Override
    public void initialize(Action[] actions, IIntrospector introspector) {
        this.introspector = introspector;
        this.ruleset = new Ruleset(actions, maxDepth, heuristic);
        SequenceGenerator generator = new SequenceGenerator(actions);
        ArrayList<Sequence> evaluationSuffixes = new ArrayList<>();
        for (int i = 1; i <= 15; i++) {
            evaluationSuffixes.add(generator.nextPermutation(i));
        }
        this.ruleSetEvaluator = new RuleSetEvaluator(evaluationSuffixes.toArray(new Sequence[0]));
        episodicMemory = new EpisodicMemory<>();
    }

    @Override
    public Action getNextAction(SensorData sensorData) {
        if (sensorData != null) {
            ruleset.update(previousAction, ActionEvaluator.ALL_SENSOR_HASH.apply(sensorData), sensorData.isGoal());
        }
        actionsSinceGoal++;

        ActionProposal proposal = ruleset.getBestMove();
        if(proposal.explore){
            explores++;
        }
        if(proposal.infinite){
            System.out.println("Performing infinite cost action!");
            if(!proposal.explore)
                explores++;
        }
        previousAction = proposal.action;
        //System.out.print(previousAction);
        episodicMemory.add(new Episode(sensorData, previousAction));
        return previousAction;
    }

    @Override
    public void onTestRunComplete() {
        //System.out.println(ruleset);
    }

    @Override
    public String[] getStatisticTypes(){
        return new String[] {
                "goal probability",
                "explore",
                "no sensor rate",
                "avg bit machine size",
                //"max bit machine size"
        };
    }

    @Override
    public ArrayList<Datum> getGoalData() {
        //System.out.println();
        ArrayList<Datum> data = new ArrayList<>();
        data.add(new Datum("goal probability", heuristic.getHeuristic(0)));
        data.add(new Datum("explore", explores));
        //data.add(new Datum("no sensor rate", actionEvaluator.getNoSensorSteps()/(double)actionsSinceGoal));
        actionsSinceGoal = 0;
        //data.add(new Datum("avg bit machine size", actionEvaluator.getAverageNoSensorBits()));
        //data.add(new Datum("max bit machine size", ruleset.getMaxBitStateEstimate()));
        return data;
    }
}
