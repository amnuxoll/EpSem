package agents.dart;

import framework.Heuristic;
import framework.IAgent;
import framework.IAgentProvider;

/**
 * Created by Ryan on 2/11/2019.
 */
public class DartAgentProvider implements IAgentProvider {
    private Heuristic heuristic;

    public DartAgentProvider(Heuristic heuristic) {
        this.heuristic = heuristic;
    }

    @Override
    public IAgent getAgent(){
        return new DartAgent(heuristic);
    }

    @Override
    public String getAlias() {return "DartAgent";}
}
