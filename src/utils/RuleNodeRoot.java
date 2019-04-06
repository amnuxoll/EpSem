package utils;

import framework.Move;

import java.util.ArrayList;
import java.util.Map;
import java.util.Optional;

/**
 * Created by Ryan on 2/7/2019.
 */
public class RuleNodeRoot extends RuleNode {
    private ArrayList<RuleNode> childArray;

    public RuleNodeRoot(Move[] potentialMoves, int maxDepth){
        super(potentialMoves, -1, maxDepth);
        if (maxDepth  <= 0 ){
            throw new IllegalArgumentException("Must have max depth of at least 1");
        }
        childArray = new ArrayList<>();
        childArray.add(new RuleNodeGoal(potentialMoves));
        for (Map.Entry<Move,ArrayList<RuleNode>> entry : children.entrySet()){
            entry.setValue(childArray);
        }
        visited = true;
    }

    @Override
    protected int getMoveFrequency(Move move) {
        return frequency;
    }

    @Override
    public int incrementMoveFrequency(Move move) {
        //Don't actually increase frequency, since all moves are the same to root, and it will already by incremented by occurs() call
        return frequency;
    }

    @Override
    public RuleNode getNextChild(Move move, int nextSense) {
        if (move == null){
            return super.getChildBySense(childArray, nextSense);
        }
        return super.getNextChild(move, nextSense);
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
        //Number of goals + 1 / freqency + 1
        return (childArray.get(0).getFrequency()+1)/((double)frequency+1);
    }

    public void reachedGoal(){
        visited = false;
        for (RuleNode child : childArray){
            child.unvisit();
        }
    }

    @Override
    protected Optional<Double> getEVRecursive(ArrayList<RuleNode> current, double h){
        if (frequency == 0){
            expectation = -1;
            explore = true;
            return Optional.empty();
        }

        Optional<Double> best = getMoveEV(childArray, frequency, current, h);

        if (best.isPresent()){
            expectation = best.get();
        } /*else {
            expectation = -1;
            this.bestMove = null;
        }*/ // Makes lack of side effects consistent
        return best;
    }
}
