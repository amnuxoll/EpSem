package agents.dart;

import framework.Episode;
import framework.Heuristic;
import framework.Action;
import framework.SensorData;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.function.Function;

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
    private int episodeIndex;//the index where this node first occurs in episodic memory
    private int goalIndex = -1;//the index of the first goal after episodeIndex in episodic memory. Not set if node occurs multiple times before reaching goal
    protected final Function<Integer, ActionSense> lookupEpisode;
    /**
     * All nodes that can be reached from this node.
     * In the arraylist of nodes, the zeroth index is guaranteed to be the goal node.
     * This goal node is the same for any choice of sensors.
     * This can be thought of as a list of rules.
     */
    protected HashMap<ChildKey, ArrayList<RuleNode>> children;

    public RuleNode(int sense, Action[] potentialActions, int depth, String[] sensors, Function<Integer, ActionSense> lookupEpisode) {
        this.sense = sense;
        this.potentialActions = potentialActions;
        this.moveFrequencies = new int[potentialActions.length];
        this.depth = depth;
        this.cache = ActionProposal.makeInfiniteProposal(potentialActions[0]);
        this.sensors = sensors;
        this.sensorKeys = new ArrayList<>(sensors.length);
        this.lookupEpisode = lookupEpisode;

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
            RuleNodeGoal goal = new RuleNodeGoal(potentialActions, depth+1, lookupEpisode);
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
     * @return the best move
     */
    public ActionProposal getBestProposal(Heuristic heuristic){
        if(!visited)
            return cache;
        //TODO make sure allowing current does not result in looping
        if(depth == DEPTH_LIMIT) {
            cache = ActionProposal.makeInfiniteProposal(potentialActions[0]);
            return cache;
        }
        if(frequency == 1){
            if(goalIndex == -1){
                return ActionProposal.makeInfiniteProposal(potentialActions[0]);
            }
            int stepsToGoal = goalIndex - episodeIndex;
            Action taken = null;
            for(Action a:potentialActions){
                if(getMoveFrequency(a) > 0){
                    taken = a;
                    break;
                }
            }
            if(taken == null)
                throw  new IllegalStateException("Illegal move frequencies.");
            double explore = heuristic.getHeuristic(depth);
            if(explore <= stepsToGoal){
                Action exploreAction = taken != potentialActions[0] ? potentialActions[0] : potentialActions[1];//First action not taken
                return new ActionProposal(exploreAction, new String[] {}, explore, this, true, false);
            }else{
                return new ActionProposal(taken, new String[] {}, stepsToGoal, this, false, false);
            }
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
                        entropy += child.frequency * (child.getBestProposal(heuristic).cost+ 1 +
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
            return cache;
        }
        cache = best;
        return best;
    }

    public void reachedGoal(int goalIndex) {
        if(visited){
            visited = false;
            if(frequency == 1){
                this.goalIndex = goalIndex;
            }
            for(Action a:potentialActions){
                for(String[] sensorKey:sensorKeys){
                    for(RuleNode node:children.get(new ChildKey(a, sensorKey))){
                        node.reachedGoal(goalIndex);
                    }
                }
            }
        }
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
    public RuleNode[] updateExtend(Action action, SensorData sensorData, int episodeIndex){
        if(depth == DEPTH_LIMIT)
            return new RuleNode[] {};
        incrementMoveFrequency(action);
        if(frequency <= 1)
            return new RuleNode[] {};
        return extend(action, sensorData, episodeIndex);
    }

    private RuleNode[] extend(Action action, SensorData sensorData, int episodeIndex){
        RuleNode[] extensions = new RuleNode[sensorKeys.size()];
        for(int i = 0; i < sensorKeys.size(); i++) {
            String[] sensorKey = sensorKeys.get(i);
            int sense = sensorHash(sensorKey, sensorData);
            RuleNode child = getChild(action, sensorKey, sense);
            if (child == null){
                child = new RuleNode(sense, potentialActions, depth + 1, sensorKey, lookupEpisode);
                children.get(new ChildKey(action, sensorKey)).add(child);
            }
            child.occurs(episodeIndex);
            child.visited = true;
            extensions[i] = child;
        }
        return extensions;
    }

    protected void occurs(int episodeIndex){
        frequency++;
        if(frequency == 1){
            this.episodeIndex = episodeIndex;
        }
        else if(frequency == 2){
            ActionSense actionSense = lookupEpisode.apply(this.episodeIndex);
            extend(actionSense.action, actionSense.sensorData, this.episodeIndex + 1);
        }
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
