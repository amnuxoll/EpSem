package agents.ndxr;

import framework.IAgent;
import framework.IAgentProvider;

/**
 *
 * This agent was designed as a tester for a new external sensor partial
 * matching approach for Phujus Agent
 *
 * @author Andrew Nuxoll
 */
public class NdxrAgentProvider implements IAgentProvider {


    @Override
    public IAgent getAgent() {
        return new NdxrAgent();
    }

    @Override
    public String getAlias() {
        return "NdxrAgent1";
    }
}
