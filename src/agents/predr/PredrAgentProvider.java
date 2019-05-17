package agents.predr;

import framework.*;

/**
 * provider for the Predictor agent
 *
 * @author Faltersack and Nuxoll
 */
public class PredrAgentProvider implements framework.IAgentProvider {

    //region methods
    public IAgent getAgent() {
        return new PredrAgent();
    }

    public String getAlias() { return "PredrAgent"; }
    //endregion

    
}//class PredrAgentProvider
