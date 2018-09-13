package agents.marz;

import agents.marz.nodes.SuffixNodeProvider;
import framework.IAgent;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class MaRzAgentProviderTest {

    // getAgent Tests
    @Test
    public void getAgentReturnsMaRzAgents() {
        MaRzAgentProvider provider = new MaRzAgentProvider(new SuffixNodeProvider());
        IAgent agent = provider.getAgent();
        assertTrue(agent instanceof MaRzAgent);
    }
}
