package agents.marzrules;

import agents.marz.ISuffixNodeBaseProvider;
import framework.IAgent;
import framework.IAgentProvider;

/**
 * Created by Ryan on 2/11/2019.
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
