package agents.dart;

import framework.Episode;
import framework.Heuristic;
import framework.Action;
import framework.SensorData;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.function.Function;

public class RuleNodeRoot extends RuleNode {
    public RuleNodeRoot(Action[] potentialActions, String[] sensors, Function<Integer, ActionSense> lookupEpisode) {
        super(0, potentialActions, 0, sensors, lookupEpisode);
    }

    @Override
    protected void initChildren() {
        children = new HashMap<>();
        for(String[] sensorKey:sensorKeys){
            ArrayList<RuleNode> value = new ArrayList<>();
            value.add(new RuleNodeGoal(potentialActions, depth + 1, lookupEpisode));
            for(Action a:potentialActions){
                children.put(new ChildKey(a, sensorKey), value);
            }
        }
    }

    @Override
    public int incrementMoveFrequency(Action action) {
        return frequency;
    }

    @Override
    public int getMoveFrequency(Action action) {
        return frequency;
    }

    @Override
    public void occurs(int index){
        frequency++;
        visited = true;
    }

    /**
     * Recursively calls for all its children and updates their cache.
     * Will not recurse if not visited since last goal.
     * @param heuristic the heuristic required for getBestProposal
     */
    public void reachedGoalRoot(Heuristic heuristic, int goalIndex) {
        if (visited){
            visited = false;
            for(String[] s:sensorKeys){
                for(RuleNode child:children.get(new ChildKey(potentialActions[0], s))){
                    child.getBestProposal(heuristic);
                    child.reachedGoal(goalIndex);
                }
            }
        }
    }

    @Override
    public void reachedGoal(int goalIndex) {
        throw new IllegalStateException("Call reachedGoalRoot instead!");
    }

    @Override
    public RuleNode[] updateExtend(Action action, SensorData sensorData, int episodeIndex) {
        RuleNode[] extensions = new RuleNode[sensorKeys.size()];
        for(int i = 0; i < sensorKeys.size(); i++) {
            String[] sensorKey = sensorKeys.get(i);
            int sense = sensorHash(sensorKey, sensorData);
            RuleNode child = getChild(potentialActions[0], sensorKey, sense);
            if (child == null){
                child = new RuleNode(sense, potentialActions, depth + 1, sensors, lookupEpisode);
                children.get(new ChildKey(potentialActions[0], sensorKey)).add(child);
            }
            child.occurs(episodeIndex);
            child.visited = true;
            extensions[i] = child;
        }
        return extensions;
    }

    @Override
    public RuleNodeGoal getGoalChild(Action action) {
        return super.getGoalChild(potentialActions[0]);
    }

    @Override
    public RuleNode getChild(Action action, String[] sensorKey, int sense) {
        return super.getChild(potentialActions[0], sensorKey, sense);
    }

    @Override
    public ActionProposal getBestProposal(Heuristic heuristic) {
        cache = ActionProposal.makeInfiniteProposal(potentialActions[0]);
        //Updates the caches of the children even though we do not need the result.
        for(String[] s:sensorKeys){
            for(RuleNode child:children.get(new ChildKey(potentialActions[0], s))){
                child.getBestProposal(heuristic);
            }
        }
        return cache;
    }

    public double getGoalProbability(){
        return getGoalChild(potentialActions[0]).frequency/(double)frequency;
    }
}
