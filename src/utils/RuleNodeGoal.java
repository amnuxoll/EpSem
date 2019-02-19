package utils;

import framework.Move;

import java.util.ArrayList;

/**
 * Created by Ryan on 2/7/2019.
 */
public class RuleNodeGoal extends RuleNode{
    public RuleNodeGoal(Move[] potentialMoves){
        super(potentialMoves, -1, 0);
    }

    // BASE CASE 4
    @Override
    public double getGoalProbability(ArrayList<Move> moves, int moveIdx) {
        return 1;
    }

    @Override
    public ArrayList<String> toStringArray(){
        ArrayList<String> result = new ArrayList<>();
        result.add("G: " + frequency);
        return result;
    }
}
