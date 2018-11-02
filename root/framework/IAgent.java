package framework;

/**
 * An IAgent is an implementation of an agent in this test framework.
 * @author Zachary Paul Faltersack
 * @version 0.95
 */
public interface IAgent {
    /**
     * Set the available {@link Move}s for the agent in the current environment.
     * @param moves An array of {@link Move} representing the moves available to the agent.
     */
    void initialize(Move[] moves);

    /**
     * Get the next move based on the provided sensorData.
     * @param sensorData The {@link SensorData} from the last move.
     * @return the next {@link Move} to attempt.
     * @throws Exception
     */
    Move getNextMove(SensorData sensorData) throws Exception;

    void addAgentListener(IAgentListener listener);

    default String getMetaData(){
        return "None";
    }
}
