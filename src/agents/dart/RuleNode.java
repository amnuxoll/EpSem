package agents.dart;

import framework.Heuristic;
import agents.marzrules.ITreeNode;
import framework.Action;
import framework.SensorData;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

public class RuleNode {
    final int sense;//number describing the values of a set of sensors. This set is determined by the parent
    final int[] moveFrequencies;//how often each move is made leaving this node. The index corresponds to potentialActions
    final Action[] potentialActions;//the alphabet
    private static final int DEPTH_LIMIT = 500;
    int frequency = 0;//how often this node has been visited
    protected final int depth;//how deep this node is in the tree
    boolean visited = false;//whether the node has been reached since the goal was last reached. If false, the cache is used in expected value
    ActionProposal cache;//the best move found last time expected value was calculated.
    protected final String[] sensors;//the array of all sensors
    ArrayList<String[]> sensorKeys;//combinations of sensors used for child nodes
    /**
     * All nodes that can be reached from this node.
     * In the arraylist of nodes, the zeroth index is guaranteed to be the goal node.
     * This goal node is the same for any choice of sensors.
     * This can be thought of as a list of rules.
     */
    protected HashMap<ChildKey, ArrayList<RuleNode>> children;

    public RuleNode(int sense, Action[] potentialActions, int depth, String[] sensors) {
        this.sense = sense;
        this.potentialActions = potentialActions;
        this.moveFrequencies = new int[potentialActions.length];
        this.depth = depth;
        this.cache = ActionProposal.makeInfiniteProposal(potentialActions[0]);
        this.sensors = sensors;
        this.sensorKeys = new ArrayList<>(sensors.length);

        sensorKeys.add(new String[] {});
        for(String sensor:sensors){
            if(sensor.equals(SensorData.goalSensor))
                continue;
            sensorKeys.add(new String[] {sensor});
        }

        initChildren();
    }

    protected void initChildren(){
        children = new HashMap<>();
        for(Action a:potentialActions){
            RuleNodeGoal goal = new RuleNodeGoal(potentialActions, depth+1);
            for(String[] key:sensorKeys){
                ArrayList<RuleNode> value = new ArrayList<>();
                value.add(goal);
                children.put(new ChildKey(a, key), value);
            }
        }
    }

    /**
     * FOR TESTING PURPOSES AND INTERNAL USE
     * Use updateExtend instead
     * @param action the action to update the frequency of
     * @return the new move frequency
     */
    public int incrementMoveFrequency(Action action){
        int index = getActionIndex(action);
        return ++moveFrequencies[index];
    }

    protected int getMoveFrequency(Action action){
        int index = getActionIndex(action);
        return moveFrequencies[index];
    }

    /**
     * Finds the index of the given action
     * @throws IllegalArgumentException if the action is not in the alphabet
     * @return index
     */
    private int getActionIndex(Action action){
        int index = Arrays.asList(potentialActions).indexOf(action);
        if (index == -1)
            throw new IllegalArgumentException("Action not valid");
        return index;
    }

    /**
     * Find the best move to make. May update cache
     * @param heuristic the heuristic for exploring
     * @param isGoal true if the agent has just reached the goal.
     *               After true is passed, expected value will be served from the cache until the node is visited.
     * @return the best move
     */
    protected ActionProposal getBestProposal(Heuristic heuristic, boolean isGoal){
        if(!visited)
            return cache;
        //TODO make sure allowing current does not result in looping
        if(depth == DEPTH_LIMIT) {
            visited = !isGoal;
            cache = ActionProposal.makeInfiniteProposal(potentialActions[0]);
            return cache;
        }

        ActionProposal best = ActionProposal.makeInfiniteProposal(potentialActions[0]);
        boolean noChildren = true;
        for(int i = 0; i < potentialActions.length; i++){
            Action a = potentialActions[i];
            int f = getMoveFrequency(a);
            if(f == 0){
                ActionProposal proposal = new ActionProposal(a, new String[] {}, heuristic.getHeuristic(depth), this, true, false);
                if(proposal.compareTo(best) < 0)
                    best = proposal;
            }
            else{
                noChildren = false;
                for(String[] sensorKey:sensorKeys) {
                    double entropy = 0;
                    for (RuleNode child : children.get(new ChildKey(a, sensorKey))){
                        if(child.frequency == 0)
                            continue;
                        entropy += child.frequency * (child.getBestProposal(heuristic, isGoal).cost+ 1 +
                                                    (Math.log(f/(double)child.frequency)/Math.log(potentialActions.length)));
                    }
                    entropy /= f;
                    if (entropy < best.cost) {
                        best = new ActionProposal(a, sensorKey, entropy, this, false, false);
                    }
                }
            }
        }
        if (noChildren) {
            cache = ActionProposal.makeInfiniteProposal(potentialActions[0]);
            visited = !isGoal;
            return cache;
        }
        cache = best;
        visited = !isGoal;
        return best;
    }

    public ActionProposal getBestProposal(Heuristic heuristic){
        return getBestProposal(heuristic, false);
    }

    /**
     * Finds the best move to make. May not update cache.
     * @return the best move
     */
    public ActionProposal getCachedProposal(){
        return cache;
    }

    /**
     * @return how often this node has been visited
     */
    public int getFrequency(){
        return frequency;
    }

    /**
     * Updates the move frequency of action and the frequency of all children
     * Can be thought of as extending this rule by the new episode
     * Will create a new RuleNode if it does not exist
     * Visits child
     * However, no children are returned if we are at the depth limit
     * @param action the action taken
     * @param sensorData the sense after the action is taken
     * @return all children that now apply as a result of taking the action and sensing the sensorData
     */
    public RuleNode[] updateExtend(Action action, SensorData sensorData){
        incrementMoveFrequency(action);
        if(depth == DEPTH_LIMIT)
            return new RuleNode[] {};
        RuleNode[] extensions = new RuleNode[sensorKeys.size()];
        for(int i = 0; i < sensorKeys.size(); i++) {
            String[] sensorKey = sensorKeys.get(i);
            int sense = sensorHash(sensorKey, sensorData);
            RuleNode child = getChild(action, sensorKey, sense);
            if (child == null){
                child = new RuleNode(sense, potentialActions, depth + 1, sensors);
                children.get(new ChildKey(action, sensorKey)).add(child);
            }
            child.frequency++;
            child.visited = true;
            extensions[i] = child;
        }
        return extensions;
    }

    public void updateExtendGoal(Action action){
        incrementMoveFrequency(action);
        if(depth != DEPTH_LIMIT){
            getGoalChild(action).frequency++;
        }
    }

    /**
     * Converts sensorData into a sense
     * @param sensorKey which sensors to use
     * @param sensorData
     * @return sense
     */
    public static int sensorHash(String[] sensorKey, SensorData sensorData){
        int sense = 0;
        for (String sensor : sensorKey) {
            int value = (boolean) sensorData.getSensor(sensor) ? 1 : 0;
            sense *= 2;
            sense += value;
        }
        return sense;
    }

    /**
     * Gets the child with the corresponding characteristics or null if that child does not exist
     * @return child or null
     */
    public RuleNode getChild(Action action, String[] sensorKey, int sense){
        ArrayList<RuleNode> matchingChildren = children.get(new ChildKey(action, sensorKey));
        for(int i = 1; i < matchingChildren.size(); i++) {
            RuleNode child = matchingChildren.get(i);
            if (sense == child.sense)
                return child;
        }
        return null;
    }

    /**
     * Same as updateExtend but with no side effects.
     */
    /*public ITreeNode[] getNextChildren(Action action, SensorData sensorData){
        throw new NotImplementedException();
    }*/
    //TODO goal node interface?

    /**
     * Gets the goal node from performing an action
     * @param action the action performed
     * @return the goal node
     * Note: We may consider changing the return type to a goal node type in the future.
     */
    public RuleNodeGoal getGoalChild(Action action){
        return (RuleNodeGoal)children.get(new ChildKey(action, new String[]{})).get(0);
    }

    public static class ChildKey{
        public Action action;
        public String[] sensors;

        ChildKey(Action action, String[] sensors) {
            this.action = action;
            this.sensors = sensors;
        }

        @Override
        public int hashCode() {
            return Arrays.hashCode(sensors) * 13 + action.hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            if(obj instanceof ChildKey) {
                ChildKey key = (ChildKey) obj;
                return this.action.equals(key.action) && Arrays.equals(this.sensors, key.sensors);
            }
            return false;
        }
    }

}
