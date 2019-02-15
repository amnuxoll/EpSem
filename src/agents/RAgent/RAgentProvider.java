package agents.RAgent;

import framework.IAgent;
import framework.IAgentProvider;

public class RAgentProvider implements IAgentProvider {
    @Override
    public IAgent getAgent() {
        return new RAgent(10000);
    }

    @Override
    public String getAlias() {
        return "RAgent";
    }
}
