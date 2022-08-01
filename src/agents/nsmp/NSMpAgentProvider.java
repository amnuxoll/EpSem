package agents.nsmp;

import agents.nsm.NSMAgent;
import agents.nsm.QLearningConfiguration;
import framework.IAgent;
import framework.IAgentProvider;

public class NSMpAgentProvider implements IAgentProvider {
    //region IAgentProvider Members
    @Override
    public IAgent getAgent() {
        return new NSMpAgent(new QLearningConfiguration());
    }

    @Override
    public String getAlias() {
        return "NSMpAgent";
    }
    //endregion
}
