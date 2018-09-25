package agents.juno;

import agents.marz.ISuffixNodeBaseProvider;
import agents.marz.MaRzAgentProvider;
import framework.IAgent;

public class JunoAgentProvider extends MaRzAgentProvider {


    public JunoAgentProvider(ISuffixNodeBaseProvider nodeProvider) {
        super(nodeProvider);
    }

    @Override
    public IAgent getAgent() {return new JunoAgent(this.nodeProvider);}
}
