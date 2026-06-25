package agents.ndxr;

import java.util.Vector;

/**
 * class PathRule
 * <p>
 * This class is descended from the class with the same name in PhuJus.
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

    /** The prRules is a sequence of rules that describe a path. */
    private final Vector<Rule> prRules;  //must contain at least one step

    /** maintain a confidence in this PathRule */
    private final Conf confidence = new Conf();

//endregion Instance Variables

//region ctors and initialization

    /** ctor for prRules init from given PathRule */
    public PathRule(NdxrAgent initAgent, Vector<Rule> initPrRules) {
        this.agent = initAgent;
        this.ruleId = PathRule.nextRuleId++;
        this.prRules = initPrRules;
    }

    /** converts a Vector<TreeNode> into a Vector<Rule> */
    public static Vector<Rule> nodePathToRulePath(Vector<TreeNode> path) {
        Vector<Rule> result = new Vector<>();
        for (TreeNode node : path) {
            if (node == null) continue;
            Rule r = node.getRule();
            if (r == null) continue;
            result.add(r);
        }

        return result;
    }//nodePathToRulePath


//endregion ctors and initialization

    /**
     * prRulesMatch
     * <p>
     * Determines if a given Vector<TreeNode> matches this rule's prRules.
     */
    public double prRulesMatch(Vector<TreeNode> matPrRules) {
        if (matPrRules.size() != this.prRules.size()) return 0.0;  //unequal lengths

        //Comparison
        for(int i = 0; i < matPrRules.size(); ++i) {
            Rule matRule = matPrRules.get(i).getRule();
            Rule myRule = this.prRules.get(i);
            if (matRule.getId() != myRule.getId()) { return 0.0; }
        }

        return 1.0;
    }//prRulesMatch

    /** adds a short version of the prRules to a given SB */
    private void prRulesToStringShort(StringBuilder result) {

        //first append all the actions
        for(Rule step : this.prRules) {
            result.append(step.getAction());
        }

        //now append the final ext sensors
        result.append(":");
        result.append(this.prRules.lastElement().getRHS().wcBitString());
    }//prRulesToStringShort

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder();
        
        result.append("#pr");
        result.append(this.ruleId + "  ");

        //print a short version first
        prRulesToStringShort(result);

        //print stats
        result.append(String.format(" ^  conf=%.5f", getConfidence()).replaceAll("0+$", "0"));

        //now print the full version
        result.append("  [");
        boolean first = true;
        for (Rule r : this.prRules) {
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
        result.append("#pr");
        result.append(this.ruleId + "  ");
        prRulesToStringShort(result);

        return result.toString();
    }//toStringShort


    public boolean equals(Object obj) {
        if (! (obj instanceof PathRule)) return false;
        PathRule other = (PathRule)obj;
        return (this.ruleId == other.ruleId);
    }


    /** get the final sensor data of this path */
    public int getId() { return this.ruleId; }
    public Vector<Rule> getPrRules() { return this.prRules; }
    public void logSuccess() { this.confidence.adj(true); }
    public void logFailure() { this.confidence.adj(false); }
    public double getConfidence() { return this.confidence.dval(); }

}//class PathRule

