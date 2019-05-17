package tests.agents.marz;

import agents.marz.MaRzAgent;
import agents.marz.MaRzAgentProvider;
import agents.marz.nodes.SuffixNodeProvider;
import framework.IAgent;

import tests.EpSemTest;
import tests.EpSemTestClass;
import static tests.Assertions.*;

@EpSemTestClass
public class MaRzAgentProviderTest {

    // getAgent Tests
    @EpSemTest
    @SuppressWarnings("unchecked")
    public void getAgentReturnsMaRzAgents() {
        MaRzAgentProvider provider = new MaRzAgentProvider(new SuffixNodeProvider());
        IAgent agent = provider.getAgent();
        assertTrue(agent instanceof MaRzAgent);
    }
}
