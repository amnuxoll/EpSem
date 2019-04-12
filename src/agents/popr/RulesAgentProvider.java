package agents.popr;

import agents.marz.ISuffixNodeBaseProvider;
import framework.IAgent;
import framework.IAgentProvider;

/**
 * Created by Owen on 4/8/2019.
 */
public class RulesAgentProvider implements IAgentProvider {
    private ISuffixNodeBaseProvider nodeProvider;

    public RulesAgentProvider(ISuffixNodeBaseProvider nodeProvider) {
        this.nodeProvider = nodeProvider;
    }

    @Override
    public IAgent getAgent(){
        return new RulesAgent();
    }

    @Override
    public String getAlias() {return "RulesAgent";}
}
