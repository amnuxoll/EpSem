package framework;

/**
 *
 * @author Zachary Paul Faltersack
 * @version 0.95
 */
public interface IResultWriter {

    void beginNewRun();

    void logStepsToGoal(int stepsToGoal);

    void complete();
}
