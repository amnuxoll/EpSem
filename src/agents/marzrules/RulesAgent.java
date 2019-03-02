package agents.marzrules;

import agents.marz.ISuffixNodeBaseProvider;
import agents.marz.MaRzAgent;
import agents.marz.SuffixNodeBase;
import framework.*;
import utils.RuleNode;
import utils.Ruleset;
import utils.SequenceGenerator;

import java.util.ArrayList;

/**
 * Created by Ryan on 2/11/2019.
 */
public class RulesAgent<TSuffixNode extends SuffixNodeBase<TSuffixNode>> extends MaRzAgent<TSuffixNode> {

    private Ruleset ruleset;
    private Move previousMove = null;
    private IIntrospector introspector;
    private RuleSetEvaluator ruleSetEvaluator;

    public RulesAgent(ISuffixNodeBaseProvider<TSuffixNode> nodeProvider){
        super(nodeProvider);
    }

    @Override
    public void initialize(Move[] moves, IIntrospector introspector) {
        super.initialize(moves, introspector);
        this.introspector = introspector;
        this.ruleset = new Ruleset(moves, 4);
        SequenceGenerator generator = new SequenceGenerator(moves);
        ArrayList<Sequence> evaluationSuffixes = new ArrayList<>();
        for (int i = 1; i <= 15; i++)
        {
            evaluationSuffixes.add(generator.nextPermutation(i));
        }
        this.ruleSetEvaluator = new RuleSetEvaluator(evaluationSuffixes.toArray(new Sequence[0]));
    }

    @Override
    public Move getNextMove(SensorData sensorData) {
        if (sensorData != null) {
            ruleset.update(previousMove, sensorData);
        }

        this.ruleSetEvaluator.evaluate(this.ruleset);

        previousMove = super.getNextMove(sensorData);
        return previousMove;
    }

    @Override
    public void onTestRunComplete() {
        System.out.println(ruleset);
        super.onTestRunComplete();
    }

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
}
