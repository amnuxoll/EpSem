package agents.popr;

import framework.IAgent;
import framework.IAgentProvider;

/**
 * Created by Owen on 4/8/2019.
 */
public class RulesAgentProvider implements IAgentProvider {

    @Override
    public IAgent getAgent(){
        return new RulesAgent();
    }

    @Override
    public String getAlias() {return "RulesAgent";}
}
