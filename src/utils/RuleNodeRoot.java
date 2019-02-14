package utils;

import framework.Move;

import java.util.ArrayList;

/**
 * Created by Ryan on 2/7/2019.
 */
public class RuleNodeRoot extends RuleNode {

    private ArrayList<RuleNode> children;
    private RuleNodeGoal goalChild;

    public RuleNodeRoot(Move[] potentialMoves, int maxDepth){
        super(potentialMoves, -1, maxDepth);
        if (maxDepth  <= 0 ){
            throw new IllegalArgumentException("Must have max depth of at least 1");
        }
        children = new ArrayList<>();
    }

    @Override
    public RuleNode getNextChild(Move move, int nextSense){
        if (maxDepth == 0){
            return null;
        }

        return getChildBySense(children, nextSense);
    }

    @Override
    public RuleNodeGoal getGoalChild(Move move){
        return goalChild;
    }
}
