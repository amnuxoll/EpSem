package framework;

import java.io.IOException;

/**
 *
 * @author Zachary Paul Faltersack
 * @version 0.95
 */
interface IGoalListener {
    //region Methods
    /**
     * Callback for receiving a goal {@link GoalEvent}.
     * @param event The event to receive.
     */
    void goalReceived(GoalEvent event) throws IOException;
    //endregion
}
