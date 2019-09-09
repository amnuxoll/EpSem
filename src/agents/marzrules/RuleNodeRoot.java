package agents.marzrules;

import framework.Action;

import java.util.ArrayList;
import java.util.Map;
import java.util.Optional;

/**
 * Created by Ryan on 2/7/2019.
 */
public class RuleNodeRoot extends RuleNode {
    private ArrayList<RuleNode> childArray;

    public RuleNodeRoot(Action[] potentialActions, int maxDepth){
        super(potentialActions, -1, maxDepth, 0);
        if (maxDepth  <= 0 ){
            throw new IllegalArgumentException("Must have max depth of at least 1");
        }
        childArray = new ArrayList<>();
        childArray.add(new RuleNodeGoal(potentialActions, 1));
        for (Map.Entry<Action,ArrayList<RuleNode>> entry : children.entrySet()){
            entry.setValue(childArray);
        }
        visited = true;
    }

    @Override
    protected int getMoveFrequency(Action action) {
        return frequency;
    }

    @Override
    public int incrementMoveFrequency(Action action) {
        //Don't actually increase frequency, since all actions are the same to root, and it will already by incremented by occurs() call
        return frequency;
    }

    @Override
    public RuleNode getNextChild(Action action, int nextSense) {
        if (action == null){
            return super.getChildBySense(childArray, nextSense);
        }
        return super.getNextChild(action, nextSense);
    }


    @Override
    protected ArrayList<String> toStringArray(){
        ArrayList<String> result = new ArrayList<>();
        result.add("ROOT -> " + frequency);
        for (RuleNode child : childArray) {
            for (String childItem : child.toStringArray()) {
                result.add("ROOT -> " + childItem);
            }
        }
        return result;
    }

    public double getIncreasedGoalProbability(){

        //NOTE: We no longer add 1, since the framework was modified to show a goal at the very first step
        return  childArray.get(0).getFrequency()/(double)frequency;

        //Number of goals + 1 / freqency + 1
        //return (childArray.get(0).getFrequency()+1)/((double)frequency+1);
    }

    //NOTE: Uncomment to enable caching
    public void reachedGoal(){
        visited = false;
        for (RuleNode child : childArray){
            child.unvisit();
        }
    }

    @Override
    protected Optional<Double> getEVRecursive(ArrayList<RuleNode> current, Heuristic heuristic){
        if (frequency == 0){
            expectation = -1;
            explore = true;
            return Optional.empty();
        }

        double h = heuristic.getHeuristic(currentDepth);
        Optional<Double> best = getMoveEV(childArray, frequency, current, heuristic);

        if (best.isPresent()){
            expectation = best.get();
        } /*else {
            expectation = -1;
            this.bestMove = null;
        }*/ // Makes lack of side effects consistent
        return best;
    }

    @Override
    protected double getAverageBits() {
        if(frequency == 0)
            return 0;
        double bits = 0;
        for(RuleNode child:childArray){
            double childBits = child.getAverageBits();
            double p = child.getFrequency()/(double)frequency;
            bits += p*(childBits + Math.log(1/p));
        }
        return bits;
    }

    @Override
    public double getMaxBits() {
        if(frequency == 0)
            return 0;
        double maxBits = 0;
        for(RuleNode child:childArray){
            double childBits = child.getAverageBits();
            double p = child.getFrequency()/(double)frequency;
            childBits += Math.log(1/p);
            maxBits = Math.max(maxBits, childBits);
        }
        return maxBits;
    }

    @Override
    public RuleNodeGoal getGoalChild(Action action) {
        return (RuleNodeGoal)childArray.get(0);
    }
}
