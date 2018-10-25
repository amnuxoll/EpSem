package agents.juno;

import agents.marz.ISuffixNodeBaseProvider;
import agents.marz.MaRzAgentProvider;
import framework.IAgent;

public class JunoAgentProvider extends MaRzAgentProvider {

    private JunoConfiguration config;
    public JunoAgentProvider(ISuffixNodeBaseProvider nodeProvider, JunoConfiguration config) {
        super(nodeProvider);
        this.config= config;
    }

    public JunoAgentProvider(ISuffixNodeBaseProvider nodeProvider) {
        super(nodeProvider);
        this.config= JunoConfiguration.DEFAULT;
    }

    @Override
    public IAgent getAgent() {return new JunoAgent(this.nodeProvider, config);}
}
