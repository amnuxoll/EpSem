package agents.popr;

import framework.*;
import agents.marzrules.Ruleset;
import utils.SequenceGenerator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;

/**
 * Created by Owen on 4/8/2019.
 */
public class RulesAgent implements IAgent {
    private Ruleset ruleset;
    private Action previousAction = null;
    private IIntrospector introspector;
    private RuleSetEvaluator ruleSetEvaluator;

    public RulesAgent() { }

    @Override
    public void initialize(Action[] actions, IIntrospector introspector) {
        this.introspector = introspector;
        this.ruleset = new Ruleset(actions, 1000);
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

        previousAction = ruleset.falsify();

        return previousAction;


    }

    @Override
    public void onTestRunComplete() {
        //System.out.println(ruleset);
    }

    @Override
    public String[] getStatisticTypes(){
        HashSet<String> statistics = new HashSet<>();
        statistics.addAll(Arrays.asList(IAgent.super.getStatisticTypes()));
        statistics.add("goal probability");
        statistics.add("explore");
        return statistics.toArray(new String[0]);
    }

    @Override
    public ArrayList<Datum> getGoalData() {
        ArrayList<Datum> data = new ArrayList<>();
        data.add(new Datum("goal probability", ruleset.getRoot().getIncreasedGoalProbability()));
        data.add(new Datum("explore", ruleset.getExplores()));
        return data;
    }
}
