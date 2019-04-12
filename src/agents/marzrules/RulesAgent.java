package agents.marzrules;

import utils.Heuristic;
import framework.*;
import utils.Ruleset;
import utils.SequenceGenerator;

import java.util.ArrayList;

/**
 * Created by Ryan on 2/11/2019.
 */
public class RulesAgent implements IAgent {
    private Ruleset ruleset;
    private Move previousMove = null;
    private IIntrospector introspector;
    private RuleSetEvaluator ruleSetEvaluator;
    private Heuristic heuristic;

    public RulesAgent(Heuristic heuristic) {
        this.heuristic = heuristic;
    }

    @Override
    public void initialize(Move[] moves, IIntrospector introspector) {
        this.introspector = introspector;
        this.ruleset = new Ruleset(moves, 1000, heuristic);
        SequenceGenerator generator = new SequenceGenerator(moves);
        ArrayList<Sequence> evaluationSuffixes = new ArrayList<>();
        for (int i = 1; i <= 15; i++) {
            evaluationSuffixes.add(generator.nextPermutation(i));
        }
        this.ruleSetEvaluator = new RuleSetEvaluator(evaluationSuffixes.toArray(new Sequence[0]));
    }

    @Override
    public Move getNextMove(SensorData sensorData) {
        if (sensorData != null) {
            ruleset.update(previousMove, sensorData);
        }

        //this.ruleSetEvaluator.evaluate(this.ruleset);

        previousMove = ruleset.getBestMove();
        //System.out.print(previousMove);
        return previousMove;

        /*
        previousMove = super.getNextMove(sensorData);
        return previousMove;
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
        //System.out.println();
        ArrayList<Datum> data = new ArrayList<>();
        data.add(new Datum("goal probability", ruleset.getRoot().getIncreasedGoalProbability()));
        data.add(new Datum("explore", ruleset.getExplores()));
        return data;
    }
}
