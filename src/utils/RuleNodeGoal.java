package utils;

import framework.Move;

/**
 * Created by Ryan on 2/7/2019.
 */
public class RuleNodeGoal extends RuleNode{

    public RuleNodeGoal(Move[] potentialMoves){
        super(potentialMoves, -1, 0);
    }
}
