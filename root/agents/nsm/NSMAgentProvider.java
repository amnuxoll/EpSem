package agents.nsm;

import framework.IAgent;
import framework.IAgentProvider;

public class NSMAgentProvider implements IAgentProvider {
    @Override
    public IAgent getAgent() {
        return new NSMAgent();
    }
}
