package agents.discr;

import framework.IAgent;
import framework.IAgentProvider;

/**
 *
 * @author Zachary Paul Faltersack
 * @version 0.95
 */
public class MaRzLearnerProvider implements IAgentProvider {
    //region MaRzAgentProvider<TSuffixNode> Overrides
    @Override
    public IAgent getAgent() {
        return new MaRzLearner();
    }

    @Override
    public String getAlias()
    {
        return "MaRzLearnerAgent";
    }
    //endregion
}
