package agents.tfsocket;

import framework.IAgent;
import framework.IAgentProvider;
import utils.RandomFactory;

public class TFSocketAgentProvider implements IAgentProvider {

    @Override
    public IAgent getAgent() {
        return new TFSocketAgent(RandomFactory.getTrue());
    }

    @Override
    public String getAlias() {
        return "TFSocketAgent1";
    }
}
