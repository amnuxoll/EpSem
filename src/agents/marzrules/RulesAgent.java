package agents.marzrules;

import framework.*;
import utils.SequenceGenerator;

import java.util.ArrayList;

/**
 * Created by Ryan on 2/11/2019.
 */
public class RulesAgent implements IAgent {
    private Ruleset ruleset;
    private Action previousAction;
    private IIntrospector introspector;
    private RuleSetEvaluator ruleSetEvaluator;
    private Heuristic heuristic;
    private int maxDepth;

    public RulesAgent(Heuristic heuristic, int maxDepth) {
        this.heuristic = heuristic;
        this.maxDepth = maxDepth;
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

    }

    @Override
    public Action getNextAction(SensorData sensorData) {
        if (sensorData != null) {
            ruleset.update(previousAction, sensorData);
        }

        //this.ruleSetEvaluator.evaluate(this.ruleset);

        previousAction = ruleset.getBestMove();
        System.out.print(previousAction);
        return previousAction;

        /*
        previousAction = super.getNextAction(sensorData);
        return previousAction;
        */
    }

    @Override
    public void onTestRunComplete() {
        //System.out.println(ruleset);
    }

    @Override
    public String[] getStatisticTypes(){
        return new String[] {
                "goal probability",
                "explore"
        };
    }

    @Override
    public ArrayList<Datum> getGoalData() {
        System.out.println();
        ArrayList<Datum> data = new ArrayList<>();
        data.add(new Datum("goal probability", heuristic.getHeuristic(0)));
        data.add(new Datum("explore", ruleset.getExplores()));
        return data;
    }
}
