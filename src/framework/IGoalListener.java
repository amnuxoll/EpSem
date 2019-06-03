package framework;

import java.io.IOException;

/**
 * Defines operations for any class that wishes to be notified when an {@link IAgent} hits a goal.
 *
 * @author Zachary Paul Faltersack
 * @version 0.95
 */
public interface IGoalListener {

    //region Methods

    /**
     * Callback for receiving a goal {@link GoalEvent}.
     *
     * @param event The event to receive.
     */
    void goalReceived(GoalEvent event) throws IOException;

    //endregion
}
