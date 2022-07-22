package agents.wfc;


import java.util.Vector;

/**
 * class WFCPathRule
 * <p>
 * Represents a series of TreeNodes which has led the agent to the goal.
 */
public class WFCPathRule extends WFCRule {

    private Vector<PathNode> nodes;



    public WFCPathRule(WFCAgent agent, Vector<PathNode> nodes) {
        super(agent);

        this.nodes = new Vector<>(nodes);
    }

    public Vector<PathNode> getNodes() {
        return nodes;
    }

    @Override
    public String toString() {
        return this.toString(true);
    }

    public String toString(boolean includeRuleID) {
        StringBuilder result = new StringBuilder();
        if (includeRuleID) {
            result.append("#" + this.ruleId + ": ");
        }
        for (PathNode node : this.nodes) {
            result.append(node);
            result.append(" -> ");
        }
        result.append("GOAL");
        return result.toString();
    }

    public static String toString(Vector<PathNode> nodes) {
        StringBuilder result = new StringBuilder();

        int c = 1;
        for (PathNode node : nodes) {
            result.append(node);
            if (c < nodes.size()) {
                result.append(" -> ");
            }
            c++;
        }
        return result.toString();
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof WFCPathRule)) return false;

        WFCPathRule other = (WFCPathRule) obj;

        if (other.getNodes().size() != this.getNodes().size()) return false;

        // If the strings match, they're the same!
        if (other.toString(false).equals(this.toString(false))) {
            return true;
        }

        return false;
    }

    @Override
    public String toStringShort() {
        return this.toString();
    }
}
