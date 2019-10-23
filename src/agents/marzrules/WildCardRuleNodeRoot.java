package agents.marzrules;

import framework.Action;
import framework.SensorData;

import java.util.ArrayList;
import java.util.HashMap;

public class WildCardRuleNodeRoot extends WildCardRuleNode{
    public WildCardRuleNodeRoot(int sense, Action[] potentialActions, int depth, String[] sensors) {
        super(sense, potentialActions, depth, sensors);
    }

    @Override
    protected void initChildren() {
        children = new HashMap<>();
        for(String[] sensorKey:sensorKeys){
            ArrayList<WildCardRuleNode> value = new ArrayList<>();
            value.add(new WildCardRuleNodeGoal(potentialActions, depth + 1));
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
    protected int getMoveFrequency(Action action) {
        return frequency;
    }

    public void occurs(){
        frequency++;
    }

    /**
     * Recursively calls for all its children and updates their cache.
     * Will not recurse if not visited since last goal.
     * @param heuristic the heuristic required for getBestProposal
     */
    public void reachedGoal(Heuristic heuristic) {
        if (visited){
            visited = false;
            for(String[] s:sensorKeys){
                for(WildCardRuleNode child:children.get(new ChildKey(potentialActions[0], s))){
                    child.getBestProposal(heuristic, true);
                }
            }
        }
    }

    public WildCardRuleNode[] updateExtend(SensorData sensorData) {
        return super.updateExtend(potentialActions[0], sensorData);
    }

    public WildCardRuleNodeGoal getGoalChild() {
        return super.getGoalChild(potentialActions[0]);
    }

    @Override
    public WildCardActionProposal getBestProposal(Heuristic heuristic) {
        cache = WildCardActionProposal.makeInfiniteProposal(potentialActions[0]);
        return cache;
    }
}
