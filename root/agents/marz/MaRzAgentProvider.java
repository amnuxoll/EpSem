package agents.marz;

import framework.IAgent;
import framework.IAgentProvider;

/**
 * MaRzAgentProvider
 *
 * Builds instances of a MaRz agent.
 *
 * @author Zachary Paul Faltersack
 * @version 0.95
 */
public class MaRzAgentProvider<TSuffixNode extends SuffixNodeBase<TSuffixNode>> implements IAgentProvider {

    private ISuffixNodeBaseProvider<TSuffixNode> nodeProvider;

    public MaRzAgentProvider(ISuffixNodeBaseProvider<TSuffixNode> nodeProvider) {
        this.nodeProvider = nodeProvider;
    }

    /**
     * Gets a MaRz agent.
     * @return the {@link MaRzAgent}.
     */
    @Override
    public IAgent getAgent() {
        return new MaRzAgent<>(this.nodeProvider);
    }
}
