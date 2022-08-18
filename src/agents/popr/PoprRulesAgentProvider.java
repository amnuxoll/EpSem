package agents.popr;

import framework.IAgent;
import framework.IAgentProvider;

/**
 * Created by Owen on 4/8/2019.
 */
public class PoprRulesAgentProvider implements IAgentProvider {

    @Override
    public IAgent getAgent(){
        return new PoprRulesAgent();
    }

    @Override
    public String getAlias() {return "RulesAgent";}
}
