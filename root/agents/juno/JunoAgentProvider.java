package agents.juno;

import agents.marz.ISuffixNodeBaseProvider;
import agents.marz.MaRzAgentProvider;
import framework.IAgent;

public class JunoAgentProvider extends MaRzAgentProvider {
    //region Class Variables
    private JunoConfiguration config;
    //endregion

    //region Constructors
    public JunoAgentProvider(ISuffixNodeBaseProvider nodeProvider, JunoConfiguration config) {
        super(nodeProvider);
        this.config= config;
    }

    public JunoAgentProvider(ISuffixNodeBaseProvider nodeProvider) {
        this(nodeProvider, JunoConfiguration.DEFAULT);
    }
    //endregion

    //region MaRzAgentProvider Overrides
    @Override
    public IAgent getAgent() {return new JunoAgent(this.nodeProvider, config);}

    @Override
    public String getAlias()
    {
        return "JunoAgent";
    }
    //endregion
}
