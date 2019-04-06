package agents.marzrules;

import agents.marz.ISuffixNodeBaseProvider;
import experiments.Heuristic;
import framework.IAgent;
import framework.IAgentProvider;

/**
 * Created by Ryan on 2/11/2019.
 */
public class RulesAgentProvider implements IAgentProvider {
    private Heuristic heuristic;
    private ISuffixNodeBaseProvider nodeProvider;

    public RulesAgentProvider(ISuffixNodeBaseProvider nodeProvider, Heuristic heuristic) {
        this.nodeProvider = nodeProvider;
        this.heuristic = heuristic;
    }

    @Override
    public IAgent getAgent(){
        return new RulesAgent(heuristic);
    }

    @Override
    public String getAlias() {return "RulesAgent";}
}
