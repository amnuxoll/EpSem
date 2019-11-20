package agents.dart;

import framework.Action;

public class ActionProposal implements Comparable{
    public final Action action;
    public final String[] sensorKey;
    public final double cost;
    public final RuleNode ruleNode;
    public final boolean explore;
    public final boolean infinite;

    public ActionProposal(Action action, String[] sensorKey, double cost, RuleNode ruleNode, boolean explore, boolean infinite) {
        this.action = action;
        this.sensorKey = sensorKey;
        this.cost = cost;
        this.ruleNode = ruleNode;
        this.explore = explore;
        this.infinite = infinite;
    }

    public static ActionProposal makeInfiniteProposal(Action defaultAction){
        return new ActionProposal(defaultAction, new String[] {}, Double.MAX_VALUE, null, false, true);
    }

    public static ActionProposal makeGoalProposal(Action defaultAction){
        return new ActionProposal(defaultAction, new String[] {}, 0, null, false, false);
    }

    /**
     * Compares expected values of proposals
     */
    @Override
    public int compareTo(Object o) {
        ActionProposal other = (ActionProposal) o;
        double difference = this.cost - other.cost;
        if(difference < 0)
            return -1;
        else if(difference == 0)
            return 0;
        return 1;
    }
}
