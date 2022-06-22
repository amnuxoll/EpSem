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

    private final Rule[] lhs = new Rule[2];
    private final SensorData rhsExternal;

    /** ctor */
    public PathRule(PhuJusAgent initAgent, Rule initLHS1, Rule initLHS2, SensorData initRHS) {
        super(initAgent);

        this.lhs[0] = initLHS1;
        this.lhs[1] = initLHS2;
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
        for(Rule r : this.lhs) {
            if (r instanceof TFRule) {
                result.add((TFRule) r);
            } else {
                result.addAll(((PathRule) r).flatten());
            }
        }

        return result;
    }//flatten

    /** helper method for {@link #toString()} to format the LHS of a given Rule as a string */
    protected void toStringRule(StringBuilder result, Rule r) {
        if (r instanceof TFRule) {
            TFRule tf = (TFRule) r;
            tf.toStringShortLHS(result);
        } else if (r instanceof PathRule){
            ((PathRule)r).toStringLHS(result);
        } else {
            result.append(r.toString()); //should never happen
        }

    }//toStringRule

    /** helper for toString that just prints the LHS.  This
     *   code is split out so it can be also be used by {@link #toStringRule} */
    private void toStringLHS(StringBuilder result) {
        result.append("#");
        result.append(this.ruleId);
        result.append(": [");
        toStringRule(result, lhs[0]);
        result.append("->");
        toStringRule(result, lhs[1]);
        result.append("]");
    }//toStringConf

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder();
        toStringLHS(result);
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
        result.append(lhs[0].ruleId);
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

        if (other.lhs[0].getId() != this.lhs[0].getId()) return false;
        if (other.lhs[1].getId() != this.lhs[1].getId()) return false;
        return (other.rhsExternal.equals(this.rhsExternal));
    }

    /**
     * @return 'true' if this PathRule matches given LHS rules
     */
    public boolean lhsMatch(Rule compLHS1, Rule compLHS2) {
        return (this.lhs[0].ruleId == compLHS1.ruleId) && (this.lhs[1].ruleId == compLHS2.ruleId);
    }

    /**
     * @return 'true' if this PathRule matches given RHS sensors
     */
    public boolean rhsMatch(SensorData compRHS) {
        return (this.rhsExternal.equals(compRHS));
    }


    /** @return true if a given Rule is used anywhere on the LHS of this PathRule */
    public boolean uses(Rule removeMe) {
        for(Rule r : this.lhs) {
            if (r instanceof TFRule) {
                if (removeMe.ruleId == r.ruleId) return true;
            } else if (r instanceof PathRule) {
                if (((PathRule) r).uses(removeMe)) {  //recursion!
                    return true;
                }
            }
        }
        return false;
    }//uses

    /** replaces one TFRule with another in this PathRule */
    public void replace(TFRule removeMe, TFRule replacement) {
        for(int i = 0; i < 2; ++i) {
            if (this.lhs[i] instanceof TFRule) {
                if (removeMe.ruleId == lhs[i].ruleId) {
                    lhs[i] = replacement;
                }
            } else if (lhs[i] instanceof PathRule) {
                ((PathRule) lhs[i]).replace(removeMe, replacement);  //recursion!
            }
        }
    }//replace

    /** @return number of TFRules this PathRule uses */
    public int size() {
        int result = 0;
        for(Rule r : this.lhs) {
            if (r instanceof TFRule) {
                result++;
            } else if (r instanceof PathRule) {
                result += ((PathRule) r).size();  //recursion!
            }
        }

        return result;
    }//size

}//class PathRule
