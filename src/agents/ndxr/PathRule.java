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
    private final PathRule prevPathRule;

    //Used for debugging purposes to tell what caused this pathrule to be created
    private boolean isRand = false; //created after a random action
    public boolean isBackwards = false; //created through backwards chaining in NdxrAgent.pathMaintenence

    //Individual pathrule metrics used to calculate utility
    private int numAttempts = 0;
    private int numSuccess = 0;
    private int numSteps = 0;

    //Contains refernces to all the pathrules that use this pathrule as an internal sensor
    private Vector<PathRule> descendents = new Vector<>();

    //Remembers last goal this pathrule failed so it can try not to use it again
    private int lastFailure;
    private int numLastFail = 0;

    /** maintain a confidence in this PathRule */
    private final Conf confidence = new Conf(); //(byte)0b00111111

//endregion Instance Variables

//region ctors and initialization

    /** ctor for prRules init from given PathRule */
    public PathRule(NdxrAgent initAgent, Vector<Rule> initPrRules, PathRule initInternalSensor) {
        this.agent = initAgent;
        this.ruleId = PathRule.nextRuleId++;
        this.prRules = initPrRules;
        this.prevPathRule = initInternalSensor;
        if (this.getPrevPathRule() != null) this.getPrevPathRule().addChild(this);
    }

    // ctor for copying a pathrule with a new internal sensor
    public PathRule(PathRule initPr, PathRule initInternalSensor) {
        this.agent = initPr.agent;
        this.ruleId = PathRule.nextRuleId++;
        this.prRules = initPr.prRules;  //note:  shallow copy (seems ok)
        this.prevPathRule = initInternalSensor;
        if (this.getPrevPathRule() != null) this.getPrevPathRule().addChild(this);
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
     * The internal sensor must also match or be empty.
     */
    public double prRulesMatch(Vector<TreeNode> matPrRules, PathRule lastPath) {
        //If lengths are unequal they can not match
        if (matPrRules.size() != this.prRules.size()) return 0.0;  //unequal lengths

        //TODO: make these comments better?
        //Current logic for matching internal sensors is the newly created pathrule can either have the current pathrule as its internal sensor,
        //or it can have no internal sensor. It can not have a different internal sensor.

        //if currPathRule = x, then 'this' internal sensor can either match x, or be null
        if (lastPath == null) { //if currPathRule = null, then 'this' must have a null internal sensor
            if (this.prevPathRule != null) { return 0.0; } 
            //Otherwise both are null so they can be compared
        }
        else if (this.prevPathRule != null) { //sanity check
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
        if (this.prevPathRule != null && this.prevPathRule.equals(lastPath)) return 1.0; // if pathRule does not have lastpath as its internal
        return 0.9;
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

        result.append(" Utility: " + this.getUtility() + " Success: " + this.numSuccess + " Attempts: " + this.numAttempts + " SR: " + this.getSuccessRate() + " ");

        //Old printline for confidence
        //result.append(String.format(" ^  conf=%.5f", getConfidence()).replaceAll("0+$", "0"));

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

    public int length() { return prRules.size(); }
    public int getId() { return this.ruleId; }
    public Vector<Rule> getPrRules() { return this.prRules; }
    public PathRule getPrevPathRule() { return this.prevPathRule; }
    public void logSuccess() { 
        this.confidence.adj(true); 
        this.numSuccess++; 
        this.numAttempts++;
        resetNumLastFail();
    }
    public void logFailure() { 
        this.confidence.adj(false); 
        this.numAttempts++;
        this.lastFailure = agent.getGoalCount();
        this.numLastFail++;
    }
    //Sets all pathrule's number of failures for this goal to zero
    public void resetNumLastFail() {
        for(PathRule pr : agent.getPrsToGoal()){
            pr.numLastFail = 0;
        }
    }
    public void setRand() { this.isRand = true; }
    public boolean isRand() { return this.isRand; }
    public void addSuccess() { this.numSuccess++;} 
    public void addTries() { this.numAttempts++; }
    public void addStep() { this.numSteps++; }
    public void addChild(PathRule child) { this.descendents.add(child); }
    public double getConfidence() { return this.confidence.dval(); }
    public int getNumSuccess() { return this.numSuccess; }
    public int getNumAttempts() { return this.numAttempts; }
    public int getNumSteps() { return this.numSteps; }
    public Vector<PathRule> getDescendents() { return this.descendents; }
    public double getSuccessRate() { return (double)this.numSuccess / this.numAttempts; } // Refernce P_pr in the doc
    public double getAveSteps() { return (double)this.numSteps / this.numAttempts; } // Reference T_pr in the doc

    //Reference C_pr in the doc = T_Pr * rho
    //Right now this uses the total agent success rate times the average number of step of this pathrule
    //This calculation currently causes a performace decrease and is not optimal
    public double calcCost() { return (agent.getAgentSuccessRate() * getAveSteps()); } 

    //Calculates the average utility of all descendents, weighted by how many times they have been tried
    public double getDescendentsUtility() {
        double sumUtility = 0.0;
        int weightDenomitor = 0;
        for(PathRule pr : descendents) {
            sumUtility += pr.getUtility() * pr.getNumAttempts();
            weightDenomitor += pr.getNumAttempts();
        }
        return (sumUtility / this.descendents.size()) / weightDenomitor;
    }//getDescendentsUtility

    /**
     * getUtility
     * 
     * Cacluates the utlility of this pathrule, using success rate, average utility of descendents
     *  and whether or not this pathrule has failed for this goal already.
     * 
     * Used a cost calculation to account for longer pathrules, but we have not found a good solution
     *  for this yet.
    */
    public double getUtility() { 

        //P_PR (SR) Each pathrules successes / tries
        double utility = this.getSuccessRate();

        //TODO P_PR - C_PR -- The following show consistant decrease in ave of aves
        //utility -= this.calcCost();    //maybe use a dampening weight

        //Calculates the total number of times descendent pathrule have been tried
        int totalDecTries = 0;
        for(PathRule dec : this.descendents) {
            totalDecTries += dec.getNumAttempts();
        }

        //P_PR + (Q_next * weight of descendents)
        //This weighing takes into account the chance that a pathrule is not followed by a descendent
        // but is instead followed by a random action or a pathrule with no internal sensor
        if (this.descendents.size() > 0) {
            utility += ((1-this.getSuccessRate()) * this.getDescendentsUtility() * this.numAttempts/totalDecTries);
        }

        //Punished a pathrule if it has already failed this goal (resets after goal)
        //TODO use agent.prsToGoal to make this easier
        if (this.lastFailure == agent.getGoalCount()) {
            for (int i = 0; i < this.numLastFail; ++i) {
                utility *= 0.5;
            }
        }

        //Return the final calculated value
        return utility;
    }//getUtility

    public String getActions() {
        StringBuilder result = new StringBuilder();
        for(Rule step : this.prRules) {
            result.append(step.getAction());
        }
        return result.toString();
    }//getActions

}//class PathRule