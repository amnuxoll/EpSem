package agents.phujus;

import framework.IAgent;
import framework.IAgentProvider;

public class PhuJusAgentProvider implements IAgentProvider {
    @Override
    public IAgent getAgent() {
        return new PhuJusAgent();
    }

    @Override
    public String getAlias() {
        return "PhuJusAgent";
    }
}
