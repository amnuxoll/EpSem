package agents.discr;

import agents.marz.ISuffixNodeBaseProvider;
import agents.marz.MaRzAgentProvider;
import agents.marz.SuffixNodeBase;
import framework.IAgent;
import framework.IAgentProvider;

public class MaRzLearnerProvider<TSuffixNode extends SuffixNodeBase<TSuffixNode>> extends MaRzAgentProvider<TSuffixNode> {

    public MaRzLearnerProvider(ISuffixNodeBaseProvider<TSuffixNode> nodeProvider) {
        super(nodeProvider);
    }

    @Override
    public IAgent getAgent() {
        return new MaRzLearner<>(super.nodeProvider);
    }
}
