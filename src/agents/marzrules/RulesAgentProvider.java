package agents.marzrules;

import framework.IAgent;
import framework.IAgentProvider;

/**
 * Created by Ryan on 2/11/2019.
 */
public class RulesAgentProvider implements IAgentProvider {
    private Heuristic heuristic;

    public RulesAgentProvider(Heuristic heuristic) {
        this.heuristic = heuristic;
    }

    @Override
    public IAgent getAgent(){
        return new RulesAgent(heuristic);
    }

    @Override
    public String getAlias() {return "RulesAgent";}
}
