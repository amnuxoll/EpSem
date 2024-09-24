package agents.pytorchDQN;

import framework.IAgent;
import framework.IAgentProvider;
import utils.Random;

public class pytorchDQNAgentProvider implements IAgentProvider {

    @Override
    public IAgent getAgent() {
        return new pytorchDQNAgent(Random.getTrue());
    }

    @Override
    public String getAlias() {
        return "pytorchDQNAgent1";
    }
}
