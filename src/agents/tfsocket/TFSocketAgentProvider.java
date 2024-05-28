package agents.tfsocket;

import framework.IAgent;
import framework.IAgentProvider;
import utils.Random;

public class TFSocketAgentProvider implements IAgentProvider {

    @Override
    public IAgent getAgent() {
        return new TFSocketAgent(Random.getTrue());
    }

    @Override
    public String getAlias() {
        return "TFSocketAgent1";
    }
}
