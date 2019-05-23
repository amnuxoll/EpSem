package agents.marzrules;

import framework.Move;

import java.util.ArrayList;
import java.util.Optional;

/**
 * Created by Ryan on 2/7/2019.
 */
public class RuleNodeGoal extends RuleNode{
    public RuleNodeGoal(Move[] potentialMoves, int currentDepth){
        super(potentialMoves, -1, 0, currentDepth);
        expectation = 0;
    }

    // BASE CASE 4
    @Override
    public double getGoalProbability(ArrayList<Move> moves, int moveIdx) {
        return 1;
    }

    @Override
    public Optional<Double> getExpectation(ArrayList<RuleNode> current, boolean top, Heuristic heuristic) {
        return Optional.of(0.0);
    }

    @Override
    public ArrayList<String> toStringArray(){
        ArrayList<String> result = new ArrayList<>();
        result.add("G: " + frequency);
        return result;
    }
}
