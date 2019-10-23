package agents.marzrules;

import framework.Action;

public class WildCardActionProposal implements Comparable{
    public Action action;
    public String[] sensorKey;
    double cost;
    WildCardRuleNode ruleNode;
    boolean explore;
    boolean infinite = false;

    public WildCardActionProposal(Action action, String[] sensorKey, double cost, WildCardRuleNode ruleNode, boolean explore, boolean infinite) {
        this.action = action;
        this.sensorKey = sensorKey;
        this.cost = cost;
        this.ruleNode = ruleNode;
        this.explore = explore;
        this.infinite = infinite;
    }

    public static WildCardActionProposal makeInfiniteProposal(Action defaultAction){
        return new WildCardActionProposal(defaultAction, new String[] {}, Double.MAX_VALUE, null, false, true);
    }

    public static WildCardActionProposal makeGoalProposal(Action defaultAction){
        return new WildCardActionProposal(defaultAction, new String[] {}, 0, null, false, false);
    }

    /**
     * Compares expected values of proposals
     */
    @Override
    public int compareTo(Object o) {
        ActionProposal other = (ActionProposal) o;
        double difference = this.cost - other.expectation;
        if(difference < 0)
            return -1;
        else if(difference == 0)
            return 0;
        return 1;
    }
}
