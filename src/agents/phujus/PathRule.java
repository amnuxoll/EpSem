package agents.phujus;

import framework.SensorData;
import java.util.Vector;

/**
 * class PathRule
 *
 * Describes a rule that looks like this:  rule + rule -> external sensors
 * Since a rule can either be a TFRule or a PathRule, this allows the agent
 * to build a recursive hierarchy of expectations.
 *
 */
public class PathRule extends Rule {

    private final Rule lhs1;
    private final Rule lhs2;
    private final SensorData rhsExternal;

    /** ctor */
    public PathRule(PhuJusAgent initAgent, Rule initLHS1, Rule initLHS2, SensorData initRHS) {
        super(initAgent);

        this.lhs1 = initLHS1;
        this.lhs2 = initLHS2;
        this.rhsExternal = initRHS;
    }

    /**
     * flatten
     *
     * While a PathRule's lhs1 and lhs2 values can also be a PathRule,
     * ultimately, any PathRule's LHS can be thought of as a sequence of
     * TFRules.  This method returns that sequence
     *
     *  Note:  This method is recursive.
     */
    public Vector<TFRule> flatten() {
        Vector<TFRule> result = new Vector<>();
        if (this.lhs1 instanceof TFRule) {
            result.add((TFRule)this.lhs1);
        } else {
            result.addAll( ((PathRule)this.lhs1).flatten() );
        }
        if (this.lhs2 instanceof TFRule) {
            result.add((TFRule)this.lhs2);
        } else {
            result.addAll( ((PathRule)this.lhs2).flatten() );
        }

        return result;
    }//flatten

    /** helper method for {@link #toString()} to format the LHS of a given Rule as a string */
    protected void toStringRule(StringBuilder result, Rule r) {
        if (r instanceof TFRule) {
            TFRule tf = (TFRule) r;
            tf.toStringShortLHS(result);
        } else {
            result.append(r.toString());
        }

    }//toStringRule

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder();
        result.append("#");
        result.append(this.ruleId);
        result.append(": [");
        toStringRule(result, lhs1);
        result.append("->");
        toStringRule(result, lhs2);
        result.append("]");
        result.append(" -> ");
        result.append(TreeNode.extToString(this.rhsExternal));
        result.append(String.format(" ^  conf=%.5f", getConfidence()).replaceAll("0+$", "0"));

        return result.toString();
    }//toString

    /** a shorter string format designed to be used inline */
    @Override
    public String toStringShort() {
        StringBuilder result = new StringBuilder();
        result.append("(#");
        result.append(lhs1.ruleId);
        result.append(",#");
        result.append(")");
        result.append("->");
        if (this.rhsExternal == null) {
            result.append("null");
        } else {
            result.append(TreeNode.extToString(this.rhsExternal));
        }

        return result.toString();
    }

    @Override
    public boolean equals(Object obj) {
        if (! (obj instanceof PathRule)) return false;
        PathRule other = (PathRule) obj;

        if (other.lhs1.getId() != this.lhs1.getId()) return false;
        if (other.lhs2.getId() != this.lhs2.getId()) return false;
        return (other.rhsExternal.equals(this.rhsExternal));
    }

    /**
     * @return 'true' if this PathRule matches given LHS rules
     */
    public boolean lhsMatch(Rule compLHS1, Rule compLHS2) {
        return (this.lhs1.ruleId == compLHS1.ruleId) && (this.lhs2.ruleId == compLHS2.ruleId);
    }

    /**
     * @return 'true' if this PathRule matches given RHS sensors
     */
    public boolean rhsMatch(SensorData compRHS) {
        return (this.rhsExternal.equals(compRHS));
    }


}//class PathRule
