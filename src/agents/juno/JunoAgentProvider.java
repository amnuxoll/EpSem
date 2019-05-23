package agents.juno;

import agents.marz.MaRzAgentProvider;
import framework.IAgent;

public class JunoAgentProvider extends MaRzAgentProvider {
    //region Class Variables
    private JunoConfiguration config;
    //endregion

    //region Constructors
    public JunoAgentProvider(JunoConfiguration config) {
        this.config= config;
    }

    public JunoAgentProvider() {
        this(JunoConfiguration.DEFAULT);
    }
    //endregion

    //region MaRzAgentProvider Overrides
    @Override
    public IAgent getAgent() { return new JunoAgent(config); }

    @Override
    public String getAlias()
    {
        return "JunoAgent";
    }
    //endregion
}
