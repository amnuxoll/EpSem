package agents.dart;

import framework.Heuristic;
import framework.Action;

public class RuleNodeGoal extends RuleNode {

    public RuleNodeGoal(Action[] potentialActions, int depth) {
        super(-1, potentialActions, depth, new String[] {});
    }

    @Override
    protected void initChildren() {
        children = null;
    }

    @Override
    public ActionProposal getBestProposal(Heuristic heuristic) {
        cache = ActionProposal.makeGoalProposal(potentialActions[0]);
        return cache;
    }
}
