package agents.ndxr;

import framework.Action;
import framework.IAgent;
import framework.IIntrospector;
import framework.SensorData;

import java.util.Collections;
import java.util.Random;
import java.util.Vector;

/**
 * NdxrAgent
 * <p>
 * This agent is intended to be like PhuJus but scalable.  In particular,
 * it allows the agent to find and merage similar rules with a minimal
 * loss of knowledge.
 */
public class NdxrAgent implements IAgent {
    /** maximum number of rules allowed */
    public static final int MAX_NUM_RULES = 5000;
    /** max depth of TreeNode Search */
    //TODO: replace with MAX_EXPANSIONS
    public static final int MAX_SEARCH_DEPTH = 7;

    /** turn on/off debug printlns */
    public static final boolean DEBUGPRINTSWITCH = true;

    //a list of valid actions in the env
    private Action[] actions;
    //allows you to configure data gathered about agent performance (not used atm)
    private IIntrospector introspector;
    //Use this for all random number generation in this agent
    public static final Random rand = utils.Random.getFalse();

    //Rules that matched the agent's last N previous experiences (where N = Rule.MAX_DEPTH)
    private final Vector< Vector<Rule> > prevInternal = new Vector<>();
    //The rules which are matching the agent's current experience
    private Vector<Rule> currInternal = new Vector<>();
    //The external sensors the agent saw on the last time step
    private SensorData prevExternal = null;
    //The external sensors the agent saw on the current time step
    private SensorData currExternal = null;
    //The last action the agent took (prev time step)
    private char prevAction = '?';

    //These are the rules that were most confident about the outcome of the agent's curr action
    Vector<RuleIndex.MatchResult> predictingRules = new Vector<>();

    //Rules are kept an index to that provides fast lookup and keeps the
    // total number of rules below a global maximum.
    //Note: this can't be initialized until after this.action is set.
    private RuleIndex rules = null;

    //Keep track of the total number of rules
    private int numRules;

    //Keep track of how many steps the agent has taken
    //Note:  This is staic because it's convenient for the Rule class.
    // If we ever have multiple simultaneous agents it will be a problem.
    private static int timeStep = 0;

    //The remaining steps on the path-to-goal that the agent is currently following
    private Vector<TreeNode> pathStepsRemaining = new Vector<>();

    //These variables track the success rate of random actions
    // (Using double instead of int, so we can calc pct success).
    // The agent uses the percentage to avoid following "hopeless" paths.
    private double numRand = 1.0;
    private double numRandSuccess = 1.0;
    private boolean lastActionRandom = true;

    /** a list of all the PathRules the agent is using
     * TODO:  create an index for these like RuleIndex? */
    private final Vector<PathRule> pathRules = new Vector<>();

    /** This is the PathRule that matches the agent's current path */
    private PathRule currPathRule = null;

    //ctor may be needed someday?
    public NdxrAgent() {
    }

    @Override
    public void initialize(Action[] actions, IIntrospector introspector) {
        this.actions = actions;
        this.introspector = introspector;
        this.rules = new RuleIndex(this);
    }

    /**
     * debugPrintln
     * <p>
     * is utilized as a helper method to print useful debug information to the console on a line
     * It can be toggled on and off using the DEBUGPRINT variable
     */
    public void debugPrintln(String db) {
        if(DEBUGPRINTSWITCH) {
            System.out.println(db);
        }
    }//debugPrintln

    /**
     * debugPrintln
     * <p>
     * is utilized as a helper method to print useful debug information to the console
     * It can be toggled on/off (true/false) using the DEBUGPRINT variable
     */
    public void debugPrint(String db) {
        if(DEBUGPRINTSWITCH) {
            System.out.print(db);
        }
    }//debugPrint

    /**
     * print what the agent expects at the next time step
     */
    private void debugPrintAgentExpectation() {
        if(!DEBUGPRINTSWITCH)  return;

        StringBuilder sb = new StringBuilder();
        sb.append("Agent is Expecting:  ");
        sb.append(RuleIndex.ruleIdListStr(this.currInternal));
        sb.append(new CondSet(this.currExternal).bitString());
        sb.append(this.prevAction);
        sb.append(" -> ");
        //which predicting rule has highest confidence
        double bestScore = 0.0;
        Rule bestRule = null;
        for(RuleIndex.MatchResult mr : this.predictingRules) {
            if (mr.score > bestScore) {
                bestScore = mr.score;
                bestRule = mr.rule;
            }
        }
        if (bestRule == null) {
            sb.append("??? (no predicting rule)");
        } else {
            sb.append(bestRule.getRHS().wcBitString());
            sb.append(" ( ");
            sb.append(bestRule);
            sb.append(")");
        }
        debugPrintln(sb.toString());
    }//debugPrintAgentExpectation


    /**
     * getNextAction
     * <p>
     * called by the environment each time step to request the agent's next action
     *
     * @param sensorData The agent's current sensing ({@link SensorData})
     * @return the agent's selected action
     */
    @Override
    public Action getNextAction(SensorData sensorData) throws Exception {
        NdxrAgent.timeStep++;

        //update the sensor logs
        updateSensors(sensorData);

        //DEBUG:  print the experience the agent just had
        if (this.prevExternal != null) {
            StringBuilder sb = new StringBuilder();
            sb.append("Agent Experienced: ");
            sb.append("  ");
            sb.append(RuleIndex.ruleIdListStr(lastPrevInternal()));
            sb.append(new CondSet(this.prevExternal).bitString());
            sb.append(this.prevAction);
            sb.append(" -> ");
            sb.append(new CondSet(this.currExternal).bitString());
            debugPrintln(sb.toString());
        }

        ruleMaintenance();

        //DEBUG:  Tell the human what the agent is feeling
        if (DEBUGPRINTSWITCH) {
            debugPrintln("TIME STEP: " + NdxrAgent.timeStep);
            printPrevCurrEpisode();
        }


        //DEBUG: print all rules
        System.out.println("\nRules:");
        this.rules.printAll();
        System.out.println("\nPathRules:");
        for(PathRule pr : this.pathRules) {
            System.out.println(pr);
        }

        //DEBUG: Put breakpoints below
        if (timeStep > 5) {
            boolean stop = true;
        }
        pathMaintenance(sensorData.isGoal());

        //Select the agent's next action
        //If the agent has a path, take the next step in that path
        Action action = calcAction();

        //Save the rules that are most confident about the outcome of this action
        this.predictingRules = this.rules.findMatches( this.currInternal,
                this.currExternal,
                this.prevAction,
                null);

        debugPrintAgentExpectation();

        debugPrintln("----------------------------------------------------------------------");
        return action;
    }//getNextAction


    /**
     * makeMatchingPathRule
     *
     * constructs a one-step PathRule with the rule that best matches a given
     * action and the agent's current sensing
     *
     * @return the new PathRule or null if it could not be constructed
     */
    private PathRule makeMatchingPathRule(char act) {
        //find all matching rules
        Vector<RuleIndex.MatchResult> matches =
                this.rules.findMatches(this.currInternal, this.currExternal, act, null);
        if (matches.size() == 0) return null; //none found

        //which one has the best score?
        double bestScore = 0.0;
        Rule bestRule = null;
        for(RuleIndex.MatchResult mr : matches) {
            if (mr.score > bestScore) {
                bestScore = mr.score;
                bestRule = mr.rule;
            }
        }

        //Build a new pathRule
        Vector<Rule> newRHS = new Vector<>();
        newRHS.add(bestRule);
        return new PathRule(this, null, newRHS);
    }//makeMatchingPathRule


    /**
     * calcAction
     *
     * determines what action the agent will take next
     */
    private Action calcAction() {
        Action action = null;
        if (this.pathStepsRemaining.size() > 0) {
            TreeNode step = this.pathStepsRemaining.remove(0);
            this.prevAction = step.getAction();
            action = new Action(this.prevAction + "");
            this.lastActionRandom = false;

            //DEBUG
            debugPrintln("Path action selected: " + this.prevAction);
        }
        else {
            //random action
            int actionIndex = this.rand.nextInt(this.actions.length);
            action = this.actions[actionIndex];
            this.prevAction = action.toString().charAt(0);  //This trick may not work in the future...
            this.numRand++;
            this.lastActionRandom = true;

            //Find or create a PathRule representing this one-step path
            PathRule match = getBestMatchingPathRule(this.prevAction);
            if (match == null) {
                match = makeMatchingPathRule(this.prevAction);
                if (match != null) this.pathRules.add(match);
            }
            this.currPathRule = match;

            //DEBUG
            debugPrintln("Random action selected: " + this.prevAction);
        }
        return action;
    }//calcAction

    /**
     * pathMaintenance
     *
     * updates the agent's current path information, particularly the pathToDo
     * and prevPath instance variables
     *
     * @param isGoal  did the agent just reach a goal?
     */
    private void pathMaintenance(boolean isGoal) {
        //If goal has been reached reset the path
        boolean goal = false;
        if (isGoal) {
            debugPrintln("FOUND GOAL");
            goal = true;
            if (this.lastActionRandom) this.numRandSuccess++;
            this.pathStepsRemaining.clear();
        }

        //If the agent's goal path is empty, try to find a new path
        if (this.pathStepsRemaining.size() == 0) {
            //log PathRule success/fail
            if (this.currPathRule != null) {
                if (goal) {
                    this.currPathRule.logSuccess();
                }
                else {
                    this.currPathRule.logFailure();
                    this.currPathRule = null; //don't use on LHS because of failure
                }
            }

            //Attempt to find a new path to the goal
            TreeNode root = new TreeNode(this);
            Vector<TreeNode> goalPath = root.findBestGoalPath();
            if (goalPath != null) {
                this.pathStepsRemaining = goalPath;

                //Find or create the PathRule that best matches this new path
                PathRule match = getBestMatchingPathRule(this.currPathRule, goalPath);
                if (match == null) {
                    //create a new PathRule that matches
                    Vector<Rule> newRHS = PathRule.nodePathToRulePath(goalPath);
                    match = new PathRule(this, this.currPathRule, newRHS);
                    if (match != null) this.pathRules.add(match);
                }
                this.currPathRule = match;


                //DEBUG
                debugPrintln("New Goal Path Found: " + goalPath.lastElement());
            }//path found
        }//try to find new path
    }//pathMaintenance


    /** helper method for {@link #printPrevCurrEpisode} */
    private void printIntHelper(Vector<Rule> printMe, StringBuilder sb) {
        int count = 0;
        for (Rule r : printMe) {
            if (count > 0) sb.append(", ");
            sb.append(r.getId());
            count++;
        }
    }//printIntHelper

    /** prints the agent's previous (just completed) and current (in progress)
     * episode to help the human better understand what's going on. */
    private void printPrevCurrEpisode() {
        StringBuilder sbPrev = new StringBuilder();
        sbPrev.append("  Completed Episode: ");

        //prev internal
        sbPrev.append("(");
        if (this.prevInternal.size() > 0) {
            printIntHelper(this.lastPrevInternal(), sbPrev);
        }
        sbPrev.append(")");
        int lhsBarPos = sbPrev.length();
        sbPrev.append("|");


        //Sort external sensor names alphabetical order with GOAL last
        Vector<String> sNames = new Vector<>(this.currExternal.getSensorNames());
        Collections.sort(sNames);
        int i = sNames.indexOf(SensorData.goalSensor);
        if (i > -1) {
            sNames.remove(i);
            sNames.add(SensorData.goalSensor);
        }

        //prev external
        if (this.prevExternal != null) {
            for (String sName : sNames) {
                int val = ((Boolean) this.prevExternal.getSensor(sName)) ? 1 : 0;
                sbPrev.append(val);
            }
        } else {
            sbPrev.append("xx");
        }

        //action
        sbPrev.append(this.prevAction);
        sbPrev.append(" -> ");
        int lhsLen = sbPrev.length();  //save this so we can line up the two eps

        //curr external
        for (String sName : sNames) {
            int val = ((Boolean)this.currExternal.getSensor(sName)) ? 1 : 0;
            sbPrev.append(val);
        }

        //print sensor names after
        sbPrev.append("\t\t");
        boolean comma = false;
        for (String sName : sNames) {
            if (comma) sbPrev.append(", ");
            comma = true;
            sbPrev.append(sName);
        }

        StringBuilder sbCurr = new StringBuilder();
        sbCurr.append("Episode in Progress: ");

        //curr internal
        sbCurr.append("(");
        printIntHelper(this.currInternal, sbCurr);
        sbCurr.append(")");

        //Lineup the '|' chars on the LHS
        while(sbCurr.length() < lhsBarPos) sbCurr.append(" ");
        while(sbCurr.length() > lhsBarPos) {
            sbPrev.insert(lhsBarPos, " ");
            lhsBarPos++;
        }
        sbCurr.append("|");

        //curr external
        for (String sName : sNames) {
            int val = ((Boolean)this.currExternal.getSensor(sName)) ? 1 : 0;
            sbCurr.append(val);
        }

        //action
        sbCurr.append("? -> ");

        //make the arrows line up
        while(sbCurr.length() < lhsLen) {
            sbCurr.insert(21, " ");
        }

        debugPrintln(sbPrev.toString());
        debugPrintln(sbCurr.toString());    }//printPrevCurrEpisode

    /**
     * updateSensors
     * <p>
     * called at the start of each time step the update the instance variables
     * that track what the agent's sensors feel.
     *
     * @param newExt the outcome of the just-executed action
     */
    private void updateSensors(SensorData newExt) {
        this.prevInternal.add(this.currInternal);
        if (this.prevInternal.size() > Rule.MAX_DEPTH) this.prevInternal.remove(0);
        this.currInternal = new Vector<>();
        this.prevExternal = this.currExternal;
        this.currExternal = newExt;
    }//updateSensors

    /**
     * findEquiv
     * <p>
     * searches a list of MatchResult for one that contains a rule that
     * is at a given depth and completely matches a set of external sensors.
     * <p>
     * The list is presumed to be in sorted order by depth
     * <p>
     * @return the found rule or null.  If there are multiple matches the
     *         best match is returned.
     *
     *  TODO:  Should this return multiple matches?
     */
    private Rule findEquiv(Vector<RuleIndex.MatchResult> matches,
                           SensorData lhs,
                           SensorData rhs,
                           int depth) {
        //Convert the SensorData to CondSet for matching
        if (lhs == null) lhs = SensorData.createEmpty();
        CondSet lhsConds = new CondSet(lhs);
        CondSet rhsConds = new CondSet(rhs);

        Rule result = null;
        double bestScore = 0.0;
        for(RuleIndex.MatchResult mr : matches) {
            if (mr.rule.getDepth() < depth) continue;
            if (mr.rule.getDepth() > depth) break;  //no more at req'd depth
            if (! mr.rule.getLHS().equals(lhsConds)) continue;
            if (! mr.rule.getRHS().equals(rhsConds)) continue;
            if (mr.score > bestScore) result = mr.rule;
        }

        return result;
    }//findEquiv

    /**
     * ruleMaintenance
     * <p>
     * updates the data in the rules that match the experience the agent just had
     */
    public void ruleMaintenance() {
        //Skip first timestep
        if (this.prevAction == '?') return;

        //Tune rules that predicted the current state last timestep
        for(RuleIndex.MatchResult mr : this.predictingRules) {
            mr.rule.tune(this.prevExternal, this.currExternal);
        }

        //Get the rules that match what the agent just experienced
        Vector<Rule> prevInt;
        if (this.prevInternal.size() == 0) {  // i.e., timestep 0
            prevInt = new Vector<>();
        }
        else {
            prevInt = lastPrevInternal();
        }
        Vector<RuleIndex.MatchResult> matches =
                this.rules.findMatches( prevInt,
                                        this.prevExternal,
                                        this.prevAction,
                                        this.currExternal);

        //Create new rules from the previous episode + current sensing
        // (also set this.currInternal)
        this.currInternal.clear();
        Vector<Rule> newRules = new Vector<>();
        //new depth zero rule
        Rule r = findEquiv(matches, this.prevExternal, this.currExternal, 0);
        if (r == null) {
            r = new Rule(this.prevExternal, this.prevAction,
                    this.currExternal, null);
            newRules.add(r);
        }
        this.currInternal.add(r);

        //new rules for other depths
        Vector<Rule> lastPrevInt = lastPrevInternal();
        for (int i = 0; i < lastPrevInt.size(); ++i) {    //for-each causes concurrent mod exceptoin.  idk why
            Rule pr = lastPrevInt.get(i);
            if (pr.getDepth() >= Rule.MAX_DEPTH) break;
            r = findEquiv(matches, this.prevExternal, this.currExternal, pr.getDepth() + 1);
            if (r == null) {
                r = new Rule(this.prevExternal, this.prevAction,
                        this.currExternal, pr);
                newRules.add(r);
            }
            this.currInternal.add(r);
        }//if

        //insert all the new rules into the index
        //(This is done at the end so the new rules don't end up being
        //   internal sensors for each other.)
        for(Rule newb : newRules) {
            //DEBUG
            debugPrintln("ADDING rule: " + newb);

            this.rules.addRule(newb);
            this.numRules++;
        }

        //Merge rules to keep under the limit
        while (this.numRules > MAX_NUM_RULES) {
            boolean success = this.rules.reduce(this.numRules - MAX_NUM_RULES);
            if (!success) break;
            this.numRules--;
        }//rule merging


    }//ruleMaintenance

    //region PathRule methods

    /**
     * getBestMatchingPathRule
     *
     * determines which PathRule in this.pathRules best matches a given path
     *
     * @return the matching PR or null if not found
     */
    public PathRule getBestMatchingPathRule(PathRule prev, Vector<TreeNode> path) {
        double bestScore = 0.0;
        PathRule bestPR = null;
        for (PathRule pr : this.pathRules) {
            double score = pr.matchScore(prev, path);
            if (score > bestScore) {
                bestScore = score;
                bestPR = pr;
            }
        }

        return bestPR;
    }//getBestMatchingPathRule

    /**
     * getBestMatchingPathRule
     *
     * determines which PathRule in this.pathRules best matches a given action.
     * In particular, this must be a PathRule with one-step RHS that has the
     * given action.  The match scores with current external sensors are used
     * to find the best match.
     *
     * @return the matching PR or null if not found
     */
    public PathRule getBestMatchingPathRule(char act) {
        double bestScore = 0.0;
        PathRule bestPR = null;
        for (PathRule pr : this.pathRules) {
            //LHS must be empty
            if (pr.getLHS().size() > 0) continue;

            //only one-step paths can match
            if (pr.getRHS().size() != 1) continue;

            //action must match
            Rule step = pr.getRHS().get(0);
            if (step.getAction() != act) continue;

            //Note:  no need to check depth since it must be depth 0 given the other requirements

            //Score is based on LHS ext match with the step
            double score = step.getLHS().matchScore(this.currExternal);
            if (score > bestScore) {
                bestScore = score;
                bestPR = pr;
            }
        }

        return bestPR;
    }//getBestMatchingPathRule

    //endregion PathRule Methods

    public Vector<Rule> lastPrevInternal() {
        if (this.prevInternal.size() == 0) return new Vector<>();
        return this.prevInternal.get(this.prevInternal.size() - 1);
    }
    public Action[] getActionList() { return this.actions; }
    public SensorData getCurrExternal() { return currExternal; }
    public RuleIndex getRules() { return this.rules; }
    public static int getTimeStep() { return NdxrAgent.timeStep; }
    public double getRandSuccessRate() { return this.numRandSuccess / this.numRand; }
    public PathRule getCurrPathRule() { return this.currPathRule; }
    public Vector<PathRule> getPathRules() { return this.pathRules; }
}//class NdxrAgent
