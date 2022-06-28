package agents.phujus;

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

    private final PathRule lhs;  //can be 'null'
    private final Vector<TreeNode> rhs;

    /** ctor
     *
     * note:  initLHS may be null
     * */
    public PathRule(PhuJusAgent initAgent, PathRule initLHS, Vector<TreeNode> initRHS) {
        super(initAgent);

        this.lhs = initLHS;

        //must make a copy because initRHS is often PJA.pathTraversedSoFar which changes
        this.rhs = new Vector<>(initRHS);
    }

    /**
     * matchLen
     *
     * calculates how well this PathRule matches given data.  Since PathRule
     * is a recursive data structure, the match score is how far back into
     * the past the match persists.
     *
     * Caveat:  this is a recursive method!
     *
     * @return the length of the match
     */
    public int matchLen(PathRule matLHS, Vector<TreeNode> matRHS) {
        //Match the RHS
        if (matRHS.size() != this.rhs.size()) return 0;
        for(int i = 0; i < this.rhs.size(); ++i) {
            TreeNode myNode = this.rhs.get(i);
            TreeNode otherNode = matRHS.get(i);
            //note:  a perfect match isn't required: nearEquals is used.
            if (! myNode.nearEquals(otherNode)) return 0;
        }

        //Match the LHS (base cases)
        if (matLHS == null) return 1;
        if (this.lhs == null) return 1;

        //If lhs is the same rule we don't need to do any more matching (sort-of base case)
        if (this.lhs.ruleId == matLHS.ruleId) {
            return 1 + this.lhs.length();
        }

        //compare LHS (recursive case)
        return 1 + this.lhs.matchLen(matLHS.lhs, matLHS.rhs);
    }//matchLen

    /** helper for toString that just prints the LHS. */
    private void toStringLHS(StringBuilder result) {
        result.append("#");
        result.append(this.ruleId);
        result.append(": (");
        if (this.lhs != null) {
            result.append(this.lhs.ruleId);
        }
        result.append(") -> ");
    }//toStringConf

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder();
        toStringLHS(result);

        result.append("[");
        for(TreeNode node : this.rhs) {
            result.append(node.toString(false));
            result.append("; ");
        }
        result.append("]");
        result.append(String.format(" ^  conf=%.5f", getConfidence()).replaceAll("0+$", "0"));

        return result.toString();
    }//toString

    /** a shorter string format designed to be used inline */
    @Override
    public String toStringShort() {
        StringBuilder result = new StringBuilder();
        toStringLHS(result);
        result.append(this.rhs.lastElement().getPathStr());
        result.append(":");
        result.append(this.rhs.lastElement().getCurrExternal().toStringShort());

        return result.toString();
    }//toStringShort


    public boolean equals(Object obj) {
        if (! (obj instanceof PathRule)) return false;
        PathRule other = (PathRule) obj;

        int matchLen = this.matchLen(other.lhs, other.rhs);
        return (matchLen == this.length());
    }

    /**
     * length
     *
     * Caveat:  this is a recursive method!
     *
     * @return the time depth of this rule
     */
    public int length() {
        //Base Case
        if (this.lhs == null) return 1;

        //Recursive Case
        return 1 + this.lhs.length();
    }//size

}//class PathRule
