package agents.marzrules;

import agents.marz.ISuffixNodeBaseProvider;
import agents.marz.MaRzAgent;
import agents.marz.SuffixNodeBase;
import framework.IIntrospector;
import framework.Move;
import framework.SensorData;
import utils.Ruleset;

/**
 * Created by Ryan on 2/11/2019.
 */
public class RulesAgent<TSuffixNode extends SuffixNodeBase<TSuffixNode>> extends MaRzAgent<TSuffixNode> {

    private Ruleset ruleset;
    private Move previousMove = null;

    public RulesAgent(ISuffixNodeBaseProvider<TSuffixNode> nodeProvider){
        super(nodeProvider);
    }

    @Override
    public void initialize(Move[] moves, IIntrospector introspector){
        super.initialize(moves, introspector);
        ruleset = new Ruleset(moves, 3);
    }

    @Override
    public Move getNextMove(SensorData sensorData) {
        ruleset.update(previousMove, sensorData);
        previousMove = super.getNextMove(sensorData);
        return previousMove;
    }

    @Override
    public void onTestRunComplete() {
        System.out.println(ruleset);
        super.onTestRunComplete();
    }
}
