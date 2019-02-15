package framework;

import java.util.ArrayList;

/**
 * An IAgent is an implementation of an agent in this test framework.
 * @author Zachary Paul Faltersack
 * @version 0.95
 */
public interface IAgent {
    //region Methods
    /**
     * Set the available {@link Move}s for the agent in the current environment.
     * @param moves An array of {@link Move} representing the moves available to the agent.
     */
    void initialize(Move[] moves, IIntrospector introspector);

    /**
     * Get the next move based on the provided sensorData.
     * @param sensorData The {@link SensorData} from the current move.
     * @return the next {@link Move} to attempt.
     * @throws Exception
     */
    Move getNextMove(SensorData sensorData) throws Exception;
    //endregion

    //region Defaulted Methods for gathering statistics
    default String[] getStatisticTypes() { return new String[0]; };
    default ArrayList<Datum> getGoalData() { return new ArrayList<>(); };
    default ArrayList<Datum> getAgentFinishedData() {return new ArrayList<>();};
    default void onGoalFound() { };
    default void onTestRunComplete() { };
    //endregion
}
