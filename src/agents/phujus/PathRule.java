package agents.phujus;

import framework.SensorData;

import java.util.Collection;
import java.util.HashSet;
import java.util.Vector;

/**
 * class PathRule
 *
 * Describes a rule that describes the expected outcome of a sequence of paths.
 *
 */
public class PathRule extends Rule {

    private HashSet<PathRule> lhs = new HashSet<>();  //can be empty but not null
    private Vector<TreeNode> rhs;  //must contain at least one TreeNode

    /** ctor
     *
     * note:  initLHS may be null
     * */
    public PathRule(PhuJusAgent initAgent, Collection<PathRule> initLHS, Vector<TreeNode> initRHS) {
        super(initAgent);

        if (initLHS != null) {
            this.lhs = new HashSet<>(initLHS);
        }

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
    public int matchLen(HashSet<PathRule> matLHS, Vector<TreeNode> matRHS) {
        //Match the RHS
        if (matRHS.size() != this.rhs.size()) return 0;
        for(int i = 0; i < this.rhs.size(); ++i) {
            TreeNode myNode = this.rhs.get(i);
            TreeNode otherNode = matRHS.get(i);
            //note:  a perfect match isn't required: nearEquals is used.
            if (! myNode.nearEquals(otherNode)) return 0;
        }

        //Nasty super-recursive compare (yikes!)
        int bestLen = 0;
        for(PathRule thisPR : this.lhs) {
            for(PathRule otherPR : matLHS) {
                int len = thisPR.matchLen(otherPR.lhs, otherPR.rhs);
                if (len > bestLen) {
                    bestLen = len;
                }
            }
        }

        return 1 + bestLen;
    }//matchLen

    /** helper for toString that just prints the LHS. */
    private void toStringLHS(StringBuilder result) {
        result.append("#");
        result.append(this.ruleId);
        result.append(": (");
        boolean first = true;
        for(PathRule pr : this.lhs) {
            result.append(first ? "" : ",");
            first = false;
            result.append(pr.ruleId);
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

        //See if we can save some time:
        if (this.ruleId == other.ruleId) return true;

        //If they match, consider them equal
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
        int bestLen = 0;
        for(PathRule pr : this.lhs) {
            int len = pr.length();
            if (len > bestLen) {
                bestLen = len;
            }
        }
        return 1 + bestLen;
    }//size

    /** get the final sensor data of this path */
    public SensorData getRHSExternal() { return this.rhs.lastElement().getCurrExternal(); }

}//class PathRule
