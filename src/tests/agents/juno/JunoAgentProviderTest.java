package tests.agents.juno;

import agents.juno.JunoAgent;
import agents.juno.JunoAgentProvider;
import agents.marz.MaRzAgent;
import agents.marz.nodes.SuffixNodeProvider;
import framework.IAgent;

import tests.EpSemTest;
import tests.EpSemTestClass;
import static tests.Assertions.*;

@EpSemTestClass
public class JunoAgentProviderTest {

    // getAgent Tests
    @EpSemTest
    @SuppressWarnings("unchecked")
    public void getAgentReturnsMaRzAgents() {
        JunoAgentProvider provider = new JunoAgentProvider(new SuffixNodeProvider());
        IAgent agent = provider.getAgent();
        assertTrue(agent instanceof JunoAgent);
        assertTrue(agent instanceof MaRzAgent);
    }
}
