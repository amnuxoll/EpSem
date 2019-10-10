package agents.marzrules;

import framework.Action;
import framework.SensorData;

import java.util.ArrayList;

public interface ITreeNode extends Comparable{
    /**
     * FOR TESTING PURPOSES
     * @param action the action to update the frequency of
     * @return the new move frequency
     */
    public int incrementMoveFrequency(Action action);

    /**
     * Recursively calls for all its children. Used for caching purposes
     */
    public void reachedGoal();

    /**
     * Find the best move to make. May update cache
     * @return the best move
     */
    public ActionProposal getBestProposal();

    /**
     * Finds the best move to make. May not update cache.
     * @return the best move
     */
    public ActionProposal getCachedProposal();

    /**
     * @return how often this node has been visited
     */
    public int getFrequency();

    /**
     * Updates the move frequency of action and the frequency of all children
     * Can be thought of as extending this rule by the new episode
     * @param action the action taken
     * @param sensorData the sense after the action is taken
     * @return all children that now apply as a result of taking the action and sensing the sense
     */
    public ITreeNode[] updateExtend(Action action, SensorData sensorData);

    /**
     * Same as updateExtend but with no side effects.
     */
    public ITreeNode[] getNextChildren(Action action, SensorData sensorData);
    //TODO goal node interface?

    /**
     * Gets the goal node from performing an action
     * @param action the action performed
     * @return the goal node
     * Note: We may consider changing the return type to a goal node type in the future.
     */
    public ITreeNode getGoalChild(Action action);
}
