package experiments;

import utils.RuleNodeRoot;

public class Heuristic implements IHeuristic {
    @Override
    public double getHeuristic(RuleNodeRoot root) {
        return 1 / root.getIncreasedGoalProbability();
    }
}
