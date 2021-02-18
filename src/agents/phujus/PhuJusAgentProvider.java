package agents.phujus;

import agents.phujus.PhuJusAgent;
import framework.IAgent;
import framework.IAgentProvider;
import utils.Random;

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
