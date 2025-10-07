package agents.demosocketagent;

import framework.IAgent;
import framework.IAgentProvider;
import utils.Random;

/**
 *
 * This agent was written as a quick study for creating a new agent with a basic
 * heuristic for randomly selecting between a random action and the last
 * action taken before a goal.
 *
 * @author Zachary Faltersack
 */
public class DemoSocketAgentProvider implements IAgentProvider {

    @Override
    public IAgent getAgent() {
        return new Pytorch2SocketAgent(Random.getTrue());
    }

    @Override
    public String getAlias() {
        return "DemoAgent1";
    }
}
