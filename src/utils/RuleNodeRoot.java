package utils;

import framework.Move;

import java.util.ArrayList;
import java.util.Map;

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
    }

    @Override
    protected int getMoveFrequency(Move move) {
        return frequency;
    }

    @Override
    protected int incrementMoveFrequency(Move move) {
        return ++frequency;
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
        return (childArray.get(0).getFrequency()+1)/((double)frequency+1);
    }
}
