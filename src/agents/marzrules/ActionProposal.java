package agents.marzrules;

import framework.Action;


public class ActionProposal implements Comparable{
    public Action action;
    double expectation;
    RuleNode ruleNode;
    boolean explore;
    boolean infinite = false;

    public ActionProposal(Action action, double expectation, RuleNode ruleNode, boolean explore) {
        this.action = action;
        this.expectation = expectation;
        this.ruleNode = ruleNode;
        this.explore = explore;
    }

    public ActionProposal(Action action, double expectation, RuleNode ruleNode, boolean explore, boolean infinite) {
        this.action = action;
        this.expectation = expectation;
        this.ruleNode = ruleNode;
        this.explore = explore;
        this.infinite = infinite;
    }

    /**
     * Compares expected values of proposals
     */
    @Override
    public int compareTo(Object o) {
        ActionProposal other = (ActionProposal) o;
        double difference = this.expectation - other.expectation;
        if(difference < 0)
            return -1;
        else if(difference == 0)
            return 0;
        return 1;
    }
}
