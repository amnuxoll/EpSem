package agents.wfc;

import framework.IAgent;
import framework.IAgentProvider;

public class WFCAgentProvider implements IAgentProvider {

    @Override
    public IAgent getAgent() {
        return new WFCAgent();
    }

    @Override
    public String getAlias() {
        return "WFCAgent";
    }
}
