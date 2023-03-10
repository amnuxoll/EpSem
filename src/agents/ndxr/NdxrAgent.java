package agents.ndxr;

import framework.Action;
import framework.IAgent;
import framework.IIntrospector;
import framework.SensorData;

import java.util.ArrayList;
import java.util.Random;

/**
 * NdxrAgent
 * <p>
 * This agent is intended to be like PhuJus but scalable.  In particular,
 * it allows the agent to find and merage similar rules with a minimal
 * loss of knowledge.
 */
public class NdxrAgent implements IAgent {
    /** maximum number of rules allowed */
    public static final int MAX_NUM_RULES = 1000;

    //a list of valid actions in the env
    private Action[] actions;
    //allows you to configure data gathered about agent performance (not used atm)
    private IIntrospector introspector;
    //Use this for all random number generation in this agent
    private final Random random = utils.Random.getFalse();


    //Rules that matched the agent's last N previous experiences (where N = Rule.MAX_DEPTH)
    private final ArrayList< ArrayList<Rule> > prevInternal = new ArrayList<>();
    //The rules which are matching the agent's current experience
    private ArrayList<Rule> currInternal = new ArrayList<>();

    //The external sensors the agent saw on the last time step
    private SensorData prevExternal = null;
    //The external sensors the agent saw on the current time step
    private SensorData currExternal = null;
    //The last action the agent took (prev time step)
    private char prevAction = '?';

    /** Rules are kept an index to that provides fast lookup and keeps the
     * total number of rules below a global maximum.
     * Note: this can't be initialized until after this.action is set. */
    private RuleIndex rules = null;

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
     * getNextAction
     * <p>
     * called by the environment each time step to request the agent's next action
     *
     * @param sensorData The agent's current sensing ({@link SensorData})
     * @return the agent's selected action
     */
    @Override
    public Action getNextAction(SensorData sensorData) throws Exception {

        //update the sensor logs
        this.prevInternal.add(this.currInternal);
        if (this.prevInternal.size() > Rule.MAX_DEPTH) this.prevInternal.remove(0);
        this.currInternal = new ArrayList<>();
        this.prevExternal = this.currExternal;
        this.currExternal = sensorData;

        ruleMaintenance();

        //DEBUG: print all rules
        //this.rules.printAll();

        //random action (for now)
        int actionIndex = this.random.nextInt(this.actions.length);
        Action action = this.actions[actionIndex];
        this.prevAction = action.toString().charAt(0);  //This trick may not work in the future...
        return action;
    }

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
    private Rule findEquiv(ArrayList<RuleIndex.MatchResult> matches,
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

        //Get the rules that match what the agent just experienced
        ArrayList<Rule> prevInt = new ArrayList<>();
        if (this.prevInternal.size() > 0) {
            prevInt = lastPrevInternal();
        }
        ArrayList<RuleIndex.MatchResult> matches =
                this.rules.findMatches( prevInt,
                                        this.prevExternal,
                                        this.prevAction,
                                        this.currExternal);
        for(RuleIndex.MatchResult mr : matches) {
            //TODO:  reward?
        }

        //Create new rules from the previous episode + current sensing
        // (also set this.currInternal)
        this.currInternal.clear();
        ArrayList<Rule> newRules = new ArrayList<>();
        if (this.prevExternal != null) {
            //new depth zero rule
            Rule r = findEquiv(matches, this.prevExternal, this.currExternal, 0);
            if (r == null) {
                r = new Rule(this.prevExternal, this.prevAction,
                        this.currExternal, null);
                newRules.add(r);
            }
            this.currInternal.add(r);

            //new rules for other depths
            ArrayList<Rule> lastPrevInt = lastPrevInternal();
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
                this.rules.addRule(newb);
            }
        }//create new rules with this episode

    }//ruleMaintenance


    private ArrayList<Rule> lastPrevInternal() {
        if (this.prevInternal.size() == 0) return new ArrayList<>();
        return this.prevInternal.get(this.prevInternal.size() - 1);
    }
    public Action[] getActions() { return this.actions; }
    public SensorData getCurrExternal() { return currExternal; }

    public RuleIndex getRules() { return this.rules; }
}//class NdxrAgent
