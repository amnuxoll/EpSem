package agents.juno;

import agents.marz.ISuffixNodeBaseProvider;
import agents.marz.MaRzAgentProvider;
import agents.marz.SuffixNodeBase;
import framework.IAgent;

public class JunoAgentProvider<TSuffixNode extends SuffixNodeBase<TSuffixNode>> extends MaRzAgentProvider<TSuffixNode> {
    //region Class Variables
    private JunoConfiguration config;
    //endregion

    //region Constructors
    public JunoAgentProvider(ISuffixNodeBaseProvider<TSuffixNode> nodeProvider, JunoConfiguration config) {
        super(nodeProvider);
        this.config= config;
    }

    public JunoAgentProvider(ISuffixNodeBaseProvider<TSuffixNode> nodeProvider) {
        this(nodeProvider, JunoConfiguration.DEFAULT);
    }
    //endregion

    //region MaRzAgentProvider Overrides
    @Override
    public IAgent getAgent() { return new JunoAgent<>(this.nodeProvider, config); }

    @Override
    public String getAlias()
    {
        return "JunoAgent";
    }
    //endregion
}
