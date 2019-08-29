package agents.marzrules;

import framework.IAgent;
import framework.IAgentProvider;

/**
 * Created by Ryan on 2/11/2019.
 */
public class RulesAgentProvider implements IAgentProvider {
    private Heuristic heuristic;
    private int depthLimit;

    public RulesAgentProvider(Heuristic heuristic, int depthLimit) {
        this.heuristic = heuristic;
        this.depthLimit = depthLimit;
    }

    public RulesAgentProvider(Heuristic heuristic) {
        this.heuristic = heuristic;
        this.depthLimit = 10000;
    }

    @Override
    public IAgent getAgent(){
        return new RulesAgent(heuristic, depthLimit);
    }

    @Override
    public String getAlias() {return "RulesAgent";}
}
