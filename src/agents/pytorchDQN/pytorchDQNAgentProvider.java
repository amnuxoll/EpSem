package agents.pytorchDQN;

import framework.IAgent;
import framework.IAgentProvider;
import utils.RandomFactory;

public class pytorchDQNAgentProvider implements IAgentProvider {

    @Override
    public IAgent getAgent() {
        return new pytorchDQNAgent(RandomFactory.getTrue());
    }

    @Override
    public String getAlias() {
        return "pytorchDQNAgent1";
    }
}
