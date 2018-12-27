package agents.nsm;

import framework.IAgent;
import framework.IAgentProvider;

/**
 *
 * @author Zachary Paul Faltersack
 * @version 0.95
 */
public class NSMAgentProvider implements IAgentProvider {
    //region IAgentProvider Members
    @Override
    public IAgent getAgent() {
        return new NSMAgent();
    }

    @Override
    public String getAlias() {
        return "NSMAgent";
    }
    //endregion
}
