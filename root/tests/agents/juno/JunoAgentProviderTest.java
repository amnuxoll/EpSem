package agents.juno;

import agents.marz.MaRzAgent;
import agents.marz.nodes.SuffixNodeProvider;
import framework.IAgent;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class JunoAgentProviderTest {

    // getAgent Tests
    @Test
    public void getAgentReturnsMaRzAgents() {
        JunoAgentProvider provider = new JunoAgentProvider(new SuffixNodeProvider());
        IAgent agent = provider.getAgent();
        assertTrue(agent instanceof JunoAgent);
        assertTrue(agent instanceof MaRzAgent);
    }
}