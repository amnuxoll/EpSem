package agents.dart;

import framework.Heuristic;
import framework.Action;

import java.util.function.Function;

public class RuleNodeGoal extends RuleNode {

    public RuleNodeGoal(Action[] potentialActions, int depth, Function<Integer, ActionSense> lookupEpisode) {
        super(-1, potentialActions, depth, new String[] {}, lookupEpisode);
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

    @Override
    public void reachedGoal(int goalIndex) {
        visited = false;
    }
}
