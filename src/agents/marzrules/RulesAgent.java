package agents.marzrules;

import agents.marz.ISuffixNodeBaseProvider;
import agents.marz.MaRzAgent;
import agents.marz.SuffixNodeBase;
import framework.Datum;
import framework.IIntrospector;
import framework.Move;
import framework.SensorData;
import utils.RuleNode;
import utils.Ruleset;

import java.util.ArrayList;

/**
 * Created by Ryan on 2/11/2019.
 */
public class RulesAgent<TSuffixNode extends SuffixNodeBase<TSuffixNode>> extends MaRzAgent<TSuffixNode> {

    private Ruleset ruleset;
    private Move previousMove = null;
    private IIntrospector introspector;

    public RulesAgent(ISuffixNodeBaseProvider<TSuffixNode> nodeProvider){
        super(nodeProvider);
    }

    @Override
    public void initialize(Move[] moves, IIntrospector introspector){
        super.initialize(moves, introspector);
        this.introspector = introspector;
        ruleset = new Ruleset(moves, 4);
    }

    @Override
    public Move getNextMove(SensorData sensorData) {
        if (sensorData != null) {
            ruleset.update(previousMove, sensorData);
        }

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
