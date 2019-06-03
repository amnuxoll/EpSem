package framework;

import java.util.ArrayList;

/**
 * An IAgent is an implementation of an agent in this test framework.
 *
 * @author Zachary Paul Faltersack
 * @version 0.95
 */
public interface IAgent {

    //region Methods

    /**
     * Set the available {@link Action}s for the agent in the current environment.
     *
     * @param actions An array of {@link Action} representing the actions available to the agent.
     * @param introspector an {@link IIntrospector} that can be used to request metadata for tracking {@link IAgent} data.
     */
    void initialize(Action[] actions, IIntrospector introspector);

    /**
     * Get the next move based on the provided sensorData.
     *
     * @param sensorData The {@link SensorData} from the current move.
     * @return the next {@link Action} to attempt.
     * @throws Exception
     */
    Action getNextAction(SensorData sensorData) throws Exception;

    //endregion

    //region Defaulted Methods for gathering statistics

    /**
     * Gets the names of statistical data the agent intends to track. This allows the framework to generate
     * CSV files for these data. Only statistical data should be tracked here. Nonstatistical data can be tracked
     * using the {@link NamedOutput}.
     *
     * @return an array of string names for statistical data.
     */
    default String[] getStatisticTypes() { return new String[0]; }

    /**
     * Gets the actual statistical data for the most recent iteration of hitting a goal.
     *
     * @return the collection of {@link Datum} that indicate the agent statistical data to track.
     */
    default ArrayList<Datum> getGoalData() { return new ArrayList<>(); }

    /**
     * This callback is invoked when the agent finds a goal. If you wish to output or track any non-statistical data
     * this is one opportunity to flush to your {@link NamedOutput} stream.
     */
    default void onGoalFound() { }

    /**
     * This callback is invoked when the agent completes N goals (as defined be the test parameters). If you wish to
     * output or track any non-statistical data this is one opportunity to flush to your {@link NamedOutput} stream.
     */
    default void onTestRunComplete() { }

    //endregion

}
