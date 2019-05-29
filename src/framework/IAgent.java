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
     * Set the available {@link Action}s for the agent in the current environment.
     * @param actions An array of {@link Action} representing the actions available to the agent.
     */
    void initialize(Action[] actions, IIntrospector introspector);

    /**
     * Get the next move based on the provided sensorData.
     * @param sensorData The {@link SensorData} from the current move.
     * @return the next {@link Action} to attempt.
     * @throws Exception
     */
    Action getNextAction(SensorData sensorData) throws Exception;
    //endregion

    //region Defaulted Methods for gathering statistics
    default String[] getStatisticTypes() { return new String[0]; };
    default ArrayList<Datum> getGoalData() { return new ArrayList<>(); };
    default ArrayList<Datum> getAgentFinishedData() {return new ArrayList<>();};
    default void onGoalFound() { };
    default void onTestRunComplete() { };
    //endregion
}
