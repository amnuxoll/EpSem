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

    //The last failed pathrule the agent was on
    private final PathRule prevPathRule; //this should maybe be a vector

    /** maintain a confidence in this PathRule */
    private final Conf confidence = new Conf(); //(byte)0b00111111

//endregion Instance Variables

//region ctors and initialization

    /** ctor for prRules init from given PathRule */
    public PathRule(NdxrAgent initAgent, Vector<Rule> initPrRules, PathRule initPathRule) {
        this.agent = initAgent;
        this.ruleId = PathRule.nextRuleId++;
        this.prRules = initPrRules;
        this.prevPathRule = initPathRule;
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
     *
     * the actions, length, the FIRST LHS, and the LAST RHS must match,
     * the other sensors in the middle are irrelevant.
     */
    public double prRulesMatch(Vector<TreeNode> matPrRules, PathRule lastPath) {
        if (matPrRules.size() != this.prRules.size()) return 0.0;  //unequal lengths

        //if currPathRule = x, then 'this' internal sensor can either match x, or be null
        //if currPathRule = null, then 'this' must have a null internal sensor
        if (lastPath == null) {
            if (this.prevPathRule != null) { return 0.0; } 
            //Otherwise both are null so they can be compared
        }
        else if (this.prevPathRule != null) {
            if (!(this.prevPathRule.equals(lastPath))) { return 0.0; } //if non null internal sensors are unequal
        }

        //CHECK:  the first rules LHS sensors match perfectly, if not return
        Rule checkRule = matPrRules.get(0).getRule();
        Rule tryRule = this.prRules.get(0);
        if (!(checkRule.getLHS().equals(tryRule.getLHS()))) {
            return 0.0;
        }

        //Actions must match
        for(int i = 0; i < matPrRules.size(); ++i) {
            checkRule = matPrRules.get(i).getRule();
            tryRule = this.prRules.get(i);
            //First checks the actions are the same
            if (checkRule.getAction() != tryRule.getAction()) { return 0.0; }
        }

        //CHECK: the last rules' RHS sensors match perfectly
        //NOTE:  checkRule and tryRule should already be set correctly as side effect of the loop above
        if (!(checkRule.getRHS().equals(tryRule.getRHS()))) { return 0.0; }

        //No mismatches found
        return 1.0;
    }//prRulesMatch

    /**
     * prPerfectMatch
     * <p>
     * returns if two pathrules have the same internal sensor, and have identical rules
     */
    public boolean prPerfectMatch(PathRule matPrRules) {
        //Makes sure this.prevPathRule is not null to avoid error
        //Then checks if the two objects are identical
        if (this.prevPathRule == null) {
            if (!(matPrRules.prevPathRule == null)) { return false; }
        }
        else if (matPrRules.prevPathRule == null) { return false;}
        else if (!(this.prevPathRule.equals(matPrRules.prevPathRule))) { return false; }


        if (matPrRules.getPrRules().size() != this.prRules.size()) return false;

        if (matPrRules.getId() == this.getId()) return false;

        for(int i = 0; i < matPrRules.getPrRules().size(); ++i) {
            Rule checkRule = matPrRules.getPrRules().get(i);
            Rule tryRule = this.prRules.get(i);

            if (checkRule.getId() != tryRule.getId()) { return false; } 
        }
        agent.debugPrintln("Found match: " + matPrRules + "  and  " + this);

        return true;
    }//prPerfectMatch

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

        if(this.prevPathRule != null) { result.append("(" + this.prevPathRule.getId() + ")  "); }
        else  {result.append("()  "); }

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