package agents.ndxr;

import java.util.HashSet;
import java.util.Vector;

/**
 * class PathRule
 *
 * This class is descended from {@link PathRule}.
 * A PathRule describes a path that the agent could take to reach a goal.
 * Each PathRule has a confidence level indicating how often it has
 * been successful.
 *
 */
public class PathRule {
    //to assign a unique id to each rule this shared variable is incremented by the ctor
    private static int nextRuleId = 1;

//region Instance Variables

    //The agent using this rule
    protected final NdxrAgent agent;

    //each rule has a unique integer id
    protected final int ruleId;

    /** The LHS is a set of existing PathRules.  One of which must precede this one */
    private HashSet<PathRule> lhs = new HashSet<>();  //can be empty but not null

    /** The RHS is a sequence of rules that describe a path.  The LHS of each
     * rule must match the RHS of the preceding rule. */
    private final Vector<Rule> rhs;  //must contain at least one step

    /** track the rule's success rate with this variables.  An initial success is given so that overall
     * confidence won't drop instantly to zero on the first fail. */
    private double numTries = 1.0;
    private double numSuccesses = 1.0;

//endregion Instance Variables

//region ctors and initialization

    /** ctor for lhs init from given PathRule */
    public PathRule(NdxrAgent initAgent, PathRule initLHS, Vector<Rule> initRHS) {
        this.agent = initAgent;
        this.ruleId = PathRule.nextRuleId++;
        if (initLHS != null) this.lhs.add(initLHS);
        this.rhs = initRHS;
    }

    /** converts a Vector<TreeNode> into a Vector<Rule> */
    public static Vector<Rule> nodePathToRulePath(Vector<TreeNode> path) {
        Vector<Rule> result = new Vector<>();
        for(int i = 0; i < path.size(); ++i) {
            TreeNode node = path.get(i);
            if (node == null) continue;
            Rule r = node.getRule();
            if (r == null) continue;
            result.add(r);
        }

        return result;
    }//nodePathToRulePath


//endregion ctors and initialization

    //TODO:  Should rhsMatch and lhsMatch be doing partial matches??
    // The nearEquals() method lets us dodge this for now but not sure we should.

    /**
     * rhsMatch
     *
     * Determines if a given Vector<TreeNode> matches this rule's rhs.
     */
    public double rhsMatch(Vector<TreeNode> matRHS) {
        if (matRHS.size() != this.rhs.size()) return 0.0;  //unequal lengths

        //Comparison
        double score = 1.0;
        for(int i = 0; i < matRHS.size(); ++i) {
            Rule matRule = matRHS.get(i).getRule();
            Rule myRule = this.rhs.get(i);
            //actions *must* match
            if (matRule.getAction() != myRule.getAction()) return 0.0;
            score *= matRule.matchScore(myRule);
            if (score <= 0.0) break;
        }

        return score;
    }//rhsMatch

    /**
     * rhsMatch
     *
     * compares the rhs of a given PathRule to this one
     *
     * @return a match score:
     */
    public double rhsMatch(PathRule other) {
        //If other is null then this rule must also be empty
        if (other == null) return 0.0;
        if (other.rhs.size() != this.rhs.size()) {
            return 0.0;
        }

        //Comparison
        double score = 1.0;
        for(int i = 0; i < other.rhs.size(); ++i) {
            Rule matRule = other.rhs.get(i);
            Rule myRule = this.rhs.get(i);
            //actions *must* match
            if (matRule.getAction() != myRule.getAction()) return 0.0;
            score *= matRule.matchScore(myRule);
            if (score <= 0.0) break;
        }

        return score;

    }//rhsMatch

    /**
     * lhsMatch
     *
     * determines if a given PathRule is contained in this rule's LHS
     * If a 'null' then an empty LHS is considered  match
     *
     * @return 1.0 if found, 0.0 if not
     */
    public double lhsMatch(PathRule lhs) {
        //If the parameter is null then this rule's lhs must also be empty
        if (lhs == null)  {
            if (this.lhsSize() == 0) return 1.0;
            else return 0.0;
        }

        //One overlapping rule is sufficient
        if (this.lhs.contains(lhs)) return 1.0;
        return 0.0;

    }//lhsMatch

    /**
     * matchScore
     *
     * calculate a match score for this PathRule with a given lhs and rhs
     */
    public double matchScore(PathRule lhs, Vector<TreeNode> rhs) {
        double score = lhsMatch(lhs);
        if (score > 0.0) {
            score *= rhsMatch(rhs);
        }
        return score;
    }//matchScore


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

    /** adds a short version of the RHS to a given SB */
    private void rhsToStringShort(StringBuilder result) {

        //first append all the actions
        int indexOfLastAction = -1;
        for(Rule step : this.rhs) {
            result.append(step.getAction());
        }

        //now append the final ext sensors
        result.append(":");
        result.append(this.rhs.lastElement().getRHS().wcBitString());
    }//rhsToStringShort

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder();
        toStringLHS(result);

        //print a short version first
        rhsToStringShort(result);

        //print stats
        result.append(String.format(" ^  conf=%.5f", getConfidence()).replaceAll("0+$", "0"));

        //now print the full version
        result.append("  [");
        boolean first = true;
        for (Rule r : this.rhs) {
            if (!first) result.append(",");
            first = false;
            result.append(r.toString());
        }
        result.append("]");

        return result.toString();
    }//toString

    /** a shorter string format designed to be used inline */
    public String toStringShort() {
        StringBuilder result = new StringBuilder();
        toStringLHS(result);
        rhsToStringShort(result);

        return result.toString();
    }//toStringShort


    public boolean equals(Object obj) {
        if (! (obj instanceof PathRule)) return false;
        PathRule other = (PathRule) obj;
        return (this.ruleId == other.ruleId);
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
    }//length

    /** get the final sensor data of this path */
    public int getId() { return this.ruleId; }
    public int lhsSize() { return this.lhs.size(); }
    public boolean lhsContains(PathRule other) { return this.lhs.contains(other); }
    public HashSet<PathRule> getLHS() { return this.lhs; }
    public Vector<Rule> getRHS() { return this.rhs; }
    public void logSuccess() { this.numTries++; this.numSuccesses++; }
    public void logFailure() { this.numTries++; }
    public double getConfidence() { return this.numSuccesses / this.numTries; }

}//class PathRule

