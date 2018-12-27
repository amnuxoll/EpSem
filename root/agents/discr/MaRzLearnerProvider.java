package agents.discr;

import agents.marz.ISuffixNodeBaseProvider;
import agents.marz.MaRzAgentProvider;
import agents.marz.SuffixNodeBase;
import framework.IAgent;

/**
 *
 * @author Zachary Paul Faltersack
 * @version 0.95
 */
public class MaRzLearnerProvider<TSuffixNode extends SuffixNodeBase<TSuffixNode>> extends MaRzAgentProvider<TSuffixNode> {
    //region Constructors
    public MaRzLearnerProvider(ISuffixNodeBaseProvider<TSuffixNode> nodeProvider) {
        super(nodeProvider);
    }
    //endregion

    //region MaRzAgentProvider<TSuffixNode> Overrides
    @Override
    public IAgent getAgent() {
        return new MaRzLearner<>(super.nodeProvider);
    }

    @Override
    public String getAlias()
    {
        return "MaRzLearnerAgent";
    }
    //endregion
}
