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
    //region Class Variables
    protected ISuffixNodeBaseProvider<TSuffixNode> nodeProvider;
    //endregion

    //region Constructors
    public MaRzAgentProvider(ISuffixNodeBaseProvider<TSuffixNode> nodeProvider) {
        this.nodeProvider = nodeProvider;
    }
    //endregion

    //region IAgentProvider Members
    /**
     * Gets a MaRz agent.
     * @return the {@link MaRzAgent}.
     */
    @Override
    public IAgent getAgent() {
        return new MaRzAgent<>(this.nodeProvider);
    }

    @Override
    public String getAlias() {
        return "MaRzAgent";
    }
    //endregion
}
