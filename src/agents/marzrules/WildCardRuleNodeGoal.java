package agents.marzrules;

import framework.Action;

public class WildCardRuleNodeGoal extends WildCardRuleNode{

    public WildCardRuleNodeGoal(Action[] potentialActions, int depth) {
        super(-1, potentialActions, depth, new String[] {});
    }
}
