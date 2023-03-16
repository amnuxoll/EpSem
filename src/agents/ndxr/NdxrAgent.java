package agents.ndxr;

import agents.phujus.PhuJusAgent;
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
        if (PhuJusAgent.DEBUGPRINTSWITCH) {
            debugPrintln("TIME STEP: " + NdxrAgent.timeStep);
            printPrevCurrEpisode();
        }

        //DEBUG: Put breakpoints below
        if (timeStep > 150) {
            boolean stop = true;
        }

        //DEBUG: print all rules
        this.rules.printAll();

        pathMaintenance(sensorData.isGoal());

        //Select the agent's next action
        //If the agent has a path, take the next step in that path
        Action action = calcAction();

        //DEBUG:  print the experience the agent is expecting
        StringBuilder sb = new StringBuilder();
        sb.append("Agent is Expecting: ");
        sb.append("  ");
        sb.append(RuleIndex.ruleIdListStr(this.currInternal));
        sb.append(new CondSet(this.currExternal).bitString());
        sb.append(this.prevAction);
        sb.append(" -> ????");
        debugPrintln(sb.toString());


        //Save the rules that are most confident about the outcome of this action
        this.predictingRules = this.rules.findMatches( this.currInternal,
                                                        this.currExternal,
                                                        this.prevAction,
                                                        null);

        debugPrintln("----------------------------------------------------------------------");
        return action;
    }//getNextAction

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
        if (isGoal) {
            debugPrintln("FOUND GOAL");
            if (this.lastActionRandom) this.numRandSuccess++;
            this.pathStepsRemaining.clear();
        }

        //If the agent's goal path is empty, try to find a new path
        if (this.pathStepsRemaining.size() == 0) {
            //Attempt to find a new path to the goal
            TreeNode root = new TreeNode(this);
            Vector<TreeNode> goalPath = root.findBestGoalPath();
            if (goalPath != null) {
                this.pathStepsRemaining = goalPath;

                //DEBUG
                debugPrintln("New Goal Path Found: " + goalPath.lastElement());
            }
        }
    }


    /** helper method for {@link #printPrevCurrEpisode} */
    private void printIntHelper(Vector<Vector<Rule>> printMe, StringBuilder sb) {
        int count = 0;
        for(Vector<Rule> subSet : printMe) {
            for (Rule r : subSet) {
                if (count > 0) sb.append(", ");
                sb.append(r.getId());
                count++;
            }
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
            printIntHelper(this.prevInternal, sbPrev);
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
        Vector<Vector<Rule>> tempVec = new Vector<>();
        tempVec.add(this.currInternal);
        printIntHelper(tempVec, sbCurr);
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

        //At timestep 1, no further work to do
        if (this.prevExternal == null) return;

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


    public Vector<Rule> lastPrevInternal() {
        if (this.prevInternal.size() == 0) return new Vector<>();
        return this.prevInternal.get(this.prevInternal.size() - 1);
    }
    public Action[] getActionList() { return this.actions; }
    public SensorData getCurrExternal() { return currExternal; }

    public RuleIndex getRules() { return this.rules; }
    public static int getTimeStep() { return NdxrAgent.timeStep; }


    public double getRandSuccessRate() {
        return this.numRandSuccess / this.numRand;
    }

}//class NdxrAgent
