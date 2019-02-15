package framework;

import java.io.IOException;

/**
 * Listener for AgentFinishedEvents
 *
 * @Author Patrick Maloney
 * @Version 0.90
 */

public interface IAgentFinishedListener {

    void agentFinished(AgentFinishedEvent event) throws IOException;
}
