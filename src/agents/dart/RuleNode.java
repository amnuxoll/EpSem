package agents.dart;

import framework.Heuristic;
import framework.Action;
import framework.SensorData;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.function.Function;

public class RuleNode {
    private final int sense;//number describing the values of a set of sensors. This set is determined by the parent
    private final int[] moveFrequencies;//how often each move is made leaving this node. The index corresponds to potentialActions
    protected final Action[] potentialActions;//the alphabet
    private final int[] goalFrequencies;
    private static final String[] empty = new String[] {};
    public static final int DEPTH_LIMIT = 500;
    int frequency = 0;//how often this node has been visited
    protected final int depth;//how deep this node is in the tree
    boolean visited = false;//whether the node has been reached since the goal was last reached. If false, the cache is used in expected value
    ActionProposal cache;//the best move found last time expected value was calculated.
    protected final String[] sensors;//the array of all sensors
    ArrayList<String[]> sensorKeys;//combinations of sensors used for child nodes
    private int episodeIndex;//the index where this node first occurs in episodic memory
    private int goalIndex = -1;//the index of the first goal after episodeIndex in episodic memory. Not set if node occurs multiple times before reaching goal

    //The function to access the action of an episode at a given index and the next observation.
    //Used to "replay" episode when creating children
    protected final Function<Integer, ActionSense> lookupEpisode;

    /**
     * All nodes that can be reached from this node.
     * In the arraylist of nodes, the zeroth index is guaranteed to be the goal node.
     * This goal node is the same for any choice of sensors.
     * This can be thought of as a list of rules.
     */
    protected HashMap<ChildKey, ArrayList<RuleNode>> children;

    /**
     * Creates the RuleNode.
     * Initializes a goal child.
     * Initializes sensor keys to no sensors and all singelton sensors.
     * @param sense The sense of the node - a hash generated with a sensor key picked by the parent.
     * @param potentialActions The alphabet of the machine.
     * @param depth How deep the node is, with root being depth 0. Will not create children if this reaches the depth limit.
     * @param sensors The different sensor data keys. Used to create sensor keys.
     * @param lookupEpisode The function to get the action and following observation for an episode at a given index.
     */
    public RuleNode(int sense, Action[] potentialActions, int depth, String[] sensors, Function<Integer, ActionSense> lookupEpisode) {
        if(potentialActions == null || potentialActions.length < 2)
            throw new IllegalArgumentException("Cannot have null alphabet or alphabet with fewer than two actions");
        if(sensors == null)
            throw new IllegalArgumentException("Cannot have null sensors");
        if(lookupEpisode == null)
            throw new IllegalArgumentException("Cannot have null lookupEpisode");
        if(depth < 0)
            throw new IllegalArgumentException("Cannot have negative node depth");
        if(depth > DEPTH_LIMIT)
            throw new IllegalArgumentException("Cannot have depth greater than depth limit");
        this.sense = sense;
        this.potentialActions = potentialActions;
        this.moveFrequencies = new int[potentialActions.length];
        this.goalFrequencies = new int[potentialActions.length];
        this.depth = depth;
        this.cache = ActionProposal.makeInfiniteProposal(potentialActions[0]);
        this.sensors = sensors;
        this.sensorKeys = new ArrayList<>(sensors.length);
        this.lookupEpisode = lookupEpisode;

        //Creates sensor keys
        //One key for no sensors
        sensorKeys.add(empty);
        //One key for using each sensor individually
        for(String sensor:sensors){
            if(sensor.equals(SensorData.goalSensor))
                continue;
            sensorKeys.add(new String[] {sensor});
        }
        if(depth < DEPTH_LIMIT)
            initChildren();
    }

    /**
     * Creates children and puts a goal node in the first item of the array.
     */
    protected void initChildren(){
        children = new HashMap<>();
        for(Action a:potentialActions){
            //RuleNodeGoal goal = new RuleNodeGoal(potentialActions, depth+1, lookupEpisode);
            for(String[] key:sensorKeys){
                ArrayList<RuleNode> value = new ArrayList<>();
                //value.add(goal);
                children.put(new ChildKey(a, key), value);
            }
        }
    }

    /**
     * FOR INTERNAL USE AND TESTING PURPOSES - do not call.
     * Increases the frequency for a move and returns the new frequency.
     * @param action the action to update the frequency of
     * @return the new move frequency
     */
    public void incrementMoveFrequency(Action action){
        int index = getActionIndex(action);
        moveFrequencies[index]++;
    }

    /**
     * Gets the current move frequency
     * @param action The action to get the frequency of
     * @return The number of times that action has been performed
     */
    public int getMoveFrequency(Action action){
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
     * Find the best move to make. May update cache.
     * If a best move cannot be found, will return an infinite move proposal.
     *
     * Best move is defined as follows:
     *      If no move has been made, no solution.
     *      If at the depth limit, no solution.
     *      Compute the information entropy of each move taken, with a base of the size of the alphabet. Find  the best one.
     *          Ask someone for more details on this computation. This is confusing.
     *          This computation is "cheated" with a frequency of one by using the last goal index.
     *      If there is a move that has not been taken, compare the heuristic to the best entropy, then return the corresponding move.
     *
     * If the node has not been visited since the goal, will return the cache.
     * Otherwise, the cache will be updated to the return value of this function.
     *
     *
     *
     * @param heuristic the heuristic for exploring
     * @return the best move
     */
    public ActionProposal getBestProposal(Heuristic heuristic){

        //Base case: cached.
        //All other cases must update cache to new value.
        if(!visited)
            return cache;

        //Base case: is goal
        //Covered with virtual dispatch

        //Base case: At depth limit
        //Return infinity
        if(depth == DEPTH_LIMIT) {
            cache = ActionProposal.makeInfiniteProposal(potentialActions[0]);
            return cache;
        }

        //Base case: Visited exactly once.
        //In this case, we find expected value non-recursively.
        if(frequency == 1) {
            //Get the action taken after this node was created.
            ActionSense next = lookupEpisode.apply(episodeIndex);

            //If this is a new node, it is not allowed to make a proposal.
            if (next == null){
                cache = ActionProposal.makeInfiniteProposal(potentialActions[0]);
                return cache;
            }

            Action taken = next.action;

            Action exploreAction = taken != potentialActions[0] ? potentialActions[0] : potentialActions[1];//First action not taken
            double explore = heuristic.getHeuristic(depth);

            //Cost of an exploit
            //Meaningless if goal not reached
            int stepsToGoal = goalIndex - episodeIndex;

            //Never reached goal after being visited or an explore is better than repeating previous actions
            if(goalIndex == -1 || explore <= stepsToGoal){
                cache = new ActionProposal(exploreAction, empty, explore, this, true, false);
                return cache;
            }

            //Otherwise, we exploit
            cache = new ActionProposal(taken, empty, stepsToGoal, this, false, false);
            return cache;
        }

        //Recursive case: check expected information entropy from every move and take best one
        ActionProposal best = ActionProposal.makeInfiniteProposal(potentialActions[0]); //Best proposal found so far
        for(int i = 0; i < potentialActions.length; i++){
            Action a = potentialActions[i];
            int f = getMoveFrequency(a);

            //For an action never taken, make heuristic guess
            if(f == 0){
                ActionProposal proposal = new ActionProposal(a, empty, heuristic.getHeuristic(depth), this, true, false);
                if(proposal.compareTo(best) < 0)
                    best = proposal;
            }
            else{
                int goalF = goalFrequencies[i];
                double goalEntropy = 0;
                if (goalF != 0){
                    goalEntropy = goalF * (1 + Math.log(f/(double)goalF)/Math.log(potentialActions.length));
                }
                keyLoop:
                for(String[] sensorKey:sensorKeys) {
                    double entropy = goalEntropy;

                    //Non-goal entropy
                    for (RuleNode child : children.get(new ChildKey(a, sensorKey))){
                        //if(child.frequency == 0) //Skip goal nodes with frequency 0.
                            //continue;
                        ActionProposal childProp = child.getBestProposal(heuristic);
                        if (childProp.infinite){
                            continue keyLoop; //If any child infinite, cost of move/key pair is infinite, so we just continue
                        }
                        entropy += child.frequency * (childProp.cost+ 1 +
                                                    (Math.log(f/(double)child.frequency)/Math.log(potentialActions.length)));
                    }
                    //Rather than multiply by child probabilities to get expected value, we multiply by child frequencies than divide by total frequency
                    entropy /= f;
                    if (entropy < best.cost) {
                        best = new ActionProposal(a, sensorKey, entropy, this, false, false);
                    }
                }
            }
        }
        cache = best;
        return best;
    }

    /**
     * Called when a goal has been reached.
     * Will recursively call for all visited children.
     * Unvisits and saves the goal index.
     * @param goalIndex The episode index for the goal
     * @param h The heuristic. Needed to force a cache update on the tree fringes when a goal is first reached.
     */
    public void reachedGoal(int goalIndex, Heuristic h) {
        if(visited){
            if(frequency == 1){
                this.goalIndex = goalIndex;
                getBestProposal(h);
            }
            visited = false;
            if (depth < DEPTH_LIMIT) {
                for (Action a : potentialActions) {
                    for (String[] sensorKey : sensorKeys) {
                        for (RuleNode node : children.get(new ChildKey(a, sensorKey))) {
                            node.reachedGoal(goalIndex, h);
                        }
                    }
                }
            }
        }
    }

    /**
     * Finds the best move to make.
     * Simply returns the cache - cache cannot update, and no side effects.
     * @return the best proposal.
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
     * DO NOT call for goal. Use updateExtendGoal instead.
     *
     * Assuming this rule currently applies, get array of new rules that will apply after action is taken and sensorData is sensed.
     * Updates the move frequency of action and has all the children occur.
     * Can be thought of as extending this rule by the new episode.
     * Will create a new RuleNode if it does not exist, this node has occured at least twice, and not at the depth limit.
     * Visits child, invalidating their cache.
     *
     * This method simply updates move frequencies, then calls extend only if we are allowed to create children.
     * Extend can be called at a later time with the same information to "retroactively" create children.
     *
     * @param action the action taken
     * @param sensorData the sense after the action is taken
     * @param episodeIndex The index of the episode where this node occured. Used to let this node create children at a later time by replaying episode.
     * @return all children that now apply as a result of taking the action and sensing the sensorData
     */
    public RuleNode[] updateExtend(Action action, SensorData sensorData, int episodeIndex){
        if(depth == DEPTH_LIMIT)
            return new RuleNode[] {};
        if(frequency <= 1)
            return new RuleNode[] {};
        return extend(action, sensorData, episodeIndex);
    }

    /**
     * Finds or creates children using the given action and sensorData, has them occur, and returns them.
     * Note that this function does not check depth limit or other restrictions before creating children, but will fail if we are at the depth limit
     *
     * @param action The action taken
     * @param sensorData The next sensorData
     * @param episodeIndex The index of the episode where this node applied.
     * @return The new rules that apply.
     */
    private RuleNode[] extend(Action action, SensorData sensorData, int episodeIndex){
        incrementMoveFrequency(action);
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
            extensions[i] = child;
        }
        return extensions;
    }

    /**
     * Called when this node first applies.
     * FOR TESTING AND INTERNAL USE ONLY
     * Visits this node.
     * Increments node frequency.
     * When visited for the second time, replays past episode to "retroactively" create child.
     * @param episodeIndex The index of the episode where this node applies.
     */
    public void occurs(int episodeIndex){
        visited = true;
        frequency++;
        if(frequency == 1){
            this.episodeIndex = episodeIndex;
        }
        else if(frequency == 2 && depth != DEPTH_LIMIT){ //The second time this is visited
            ActionSense actionSense = lookupEpisode.apply(this.episodeIndex);
            if (actionSense == null){
                throw new IllegalStateException("Node occurs twice in same episode");
            }
            if (actionSense.sensorData.isGoal()){
                updateExtendGoal(actionSense.action);
            } else {
                extend(actionSense.action, actionSense.sensorData, this.episodeIndex + 1);

                Action last = lookupEpisode.apply(this.episodeIndex).action;
                if (frequency == 2 && last == actionSense.action){
                    for (String[] key: sensorKeys){
                        RuleNode child = getChild(last, key, sensorHash(key, actionSense.sensorData));
                        if (child != null){
                            child.goalIndex = this.goalIndex;
                        }
                    }
                }
            }
        }
    }

    /**
     * Assuming this node applies, updates move frequency and goal frequency when a goal occurs.
     * Since goal extensions are worthless, return is void.
     * @param action Action taken from this node to reach the goal.
     */
    public void updateExtendGoal(Action action){
        incrementMoveFrequency(action);
        int index = getActionIndex(action);
        goalFrequencies[index]++;
        /*if(depth != DEPTH_LIMIT){
            getGoalChild(action).frequency++;
        }*/
    }

    /**
     * Converts sensorData into a sense.
     * This is a pure function.
     * Two calls to this function with different sensor datas will return
     *      the same value exactly when their sensor datas differ only by sensors not in the sensor key.
     * @param sensorKey which sensors to use
     * @param sensorData the data used to compute the sense
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
        if(depth == DEPTH_LIMIT)
            return null;
        ArrayList<RuleNode> matchingChildren = children.get(new ChildKey(action, sensorKey));
        for(int i = 0; i < matchingChildren.size(); i++) {
            RuleNode child = matchingChildren.get(i);
            if (sense == child.sense)
                return child;
        }
        return null;
    }

    public ArrayList<RuleNode> getChildren(Action action, String[] sensorKey){
        return children.get(new ChildKey(action, sensorKey));
    }

    public String[] getSensors(){
        return sensors;
    }

    /**
     * Gets the goal node from performing an action.
     * @param action the action performed
     * @return the goal node
     */
    /*public RuleNodeGoal getGoalChild(Action action){
        if(depth == DEPTH_LIMIT)
            return null;
        return (RuleNodeGoal)children.get(new ChildKey(action, new String[]{})).get(0);
    }*/

    /**
     * Used as a key for children.
     * Effectively a value type consisting of an action and a sensorKey
     */
    public static class ChildKey{
        public Action action;
        public String[] sensorKey;

        ChildKey(Action action, String[] sensorKey) {
            this.action = action;
            this.sensorKey = sensorKey;
        }

        @Override
        public int hashCode() {
            return Arrays.hashCode(sensorKey) * 13 + action.hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            if(obj instanceof ChildKey) {
                ChildKey key = (ChildKey) obj;
                return this.action.equals(key.action) && Arrays.equals(this.sensorKey, key.sensorKey);
            }
            return false;
        }
    }

}
