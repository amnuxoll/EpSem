package agents.marzrules;

import experiments.IHeuristic;
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
    private IHeuristic heuristic;

    public RulesAgent(IHeuristic heuristic) {
        this.heuristic = heuristic;
    }

    @Override
    public void initialize(Move[] moves, IIntrospector introspector) {
        this.introspector = introspector;
        this.ruleset = new Ruleset(moves, 500, heuristic);
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
        //System.out.println(previousMove);
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

    /*
    @Override
    public ArrayList<Datum> getData() {
        ArrayList<Datum> allData = super.getData();

        ArrayList<RuleNode> currentList = ruleset.getCurrent();
        ArrayList<Double> goalProbabilities = ruleset.getGoalProbabilities();
        for (int i = 0; i < currentList.size(); i++) {
            RuleNode ruleNode = currentList.get(i);
            double goalProbability = goalProbabilities.get(i);
        }

        return allData;
    }
    */

    @Override
    public String[] getStatisticTypes(){
        return new String[] {
                "heuristic",
                "explore"
        };
    }

    @Override
    public ArrayList<Datum> getGoalData() {
        ArrayList<Datum> data = new ArrayList<>();
        data.add(new Datum("heuristic", heuristic.getHeuristic(ruleset.getRoot())));
        data.add(new Datum("explore", ruleset.getExplores()));
        return data;
    }
}
