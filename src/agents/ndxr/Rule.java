package agents.ndxr;

import framework.SensorData;

import java.util.ArrayList;
import java.util.HashMap;

/** describes sensing+action->sensing */
public class Rule {
    private static int nextId = 1;  //next unique rule Id (use val then increment)
    public static int MAX_DEPTH = 7; //maximum rule depth allowed (see depth instance var)

    /** minimum match score that's worthwhile.  Is this a hyperparameters?  I think so.
     * TODO:  Find a way to remove this?
     */
    public static double MIN_MATCH_SCORE = 0.5;

    /*===========================================================================
     * Inner Classes
     ----------------------------------------------------------------------------*/

    /**
     * class RuleScore
     * <p>
     * stores the results of a match score calculation between two rules for later use.
     * @see #scoreHash
     */
    public static class RuleScore {
        public double score;
        public int timestamp;

        public RuleScore(double initScore) {
            this.score = initScore;
            this.timestamp = NdxrAgent.getTimeStep();
        }//ctor
    }//class RuleScore

    /*===========================================================================
     * Instance Variables
     ----------------------------------------------------------------------------*/

    private int id;         //unique id for this rule
    private CondSet lhs;    //LHS external sensor conditions
    private char action;    //action taken by the agent
    private CondSet rhs;    //RHS external sensor conditions

    //When building sequences of rules (paths) this rule may only be
    //preceded by a rule in this list.
    private ArrayList<Rule> prevRules = new ArrayList<>();

    //A rule's depth is how far back it's prevRules chain goes
    private int depth = 0;

    //Rules keep track of which other rule is most similar to them
    //Despite the name brotherhood is not a associative.
    private Rule brother = null;

    //The match score between 'this' and 'this.brother'
    private double brotherScore = 0.0;

    //match scores are cached for memoization
    //hash key:  "<id1>_<id2>" where id1 < id2  (example key:  "37_81")
    private static HashMap<String, RuleScore> scoreHash = new HashMap<>();

    /**
     * boring ctor
     */
    public Rule(SensorData initLHS, char initAction, SensorData initRHS, Rule initPrev) {
        this.id = Rule.nextId;
        Rule.nextId++;
        this.lhs = new CondSet(initLHS);
        this.action = initAction;
        this.rhs = new CondSet(initRHS);

        //Record previous rules and associated depth
        if (initPrev != null) {
            this.prevRules.add(initPrev);
            this.depth = initPrev.depth + 1;
            if (this.depth > MAX_DEPTH) {
                System.err.println("Max Rule Depth (" + MAX_DEPTH + ") Exceeded");
                System.exit(-1);
            }
        }
    }//ctor

    /**
     * matchRuleList            <!-- RECURSIVE -->
     * <p>
     * is a helper method for matchScore.  It compares a lists of Rules to 
     * this.prevRules for similarity
     */
    public double matchRuleList(ArrayList<Rule> prevInt) {
        if (depth == 0) return 1.0;  //base case: no comparison needed
        if (prevInt.size() == 0) return 0.0;  //no match possible
        
        //Do the two lists have at least one in common?
        for (Rule r : prevInt) {
            if (this.prevRules.contains(r)) {
                return 1.0;
            }
        }
        
        //Find the score of best matching rules
        double bestScore = 0.0;
        for(Rule r1 : prevInt) {
            for (Rule r2 : this.prevRules) {
                double score = r1.matchScore(r2);  //recursion
                if (score > bestScore) {
                    bestScore = score;
                }
            }
        }

        return bestScore;
    }//matchRuleList

    /**
     * matchScore                <!-- RECURSIVE -->
     * <p>
     * calculates the match score of this rule with given LHS.  The action
     * is presumed to match already.
     * <p>
     * Note: If the method detects that the final match score will be less than
     *       MIN_MATCH_SCORE then 0.0 is returned immediately (for efficiency)
     *
     * @param rhs can be null or zero length if only a LHS score is desired
     * <p>
     * Warning:  This method recurses by calling {@link #matchRuleList}!
     */
    public double matchScore(ArrayList<Rule> prevInt, CondSet lhs, CondSet rhs) {
        //Check external conditions first
        double score = this.lhs.matchScore(lhs);
        if (score < MIN_MATCH_SCORE) return 0.0;
        if ((rhs != null) && (rhs.size() > 0)) {
            score *= this.rhs.matchScore(rhs);
            if (score < MIN_MATCH_SCORE) return 0.0;
        }

        score *= matchRuleList(prevInt);   //indirect recusive call

        return score;
    }//matchScore
    
    //
    
    /** 
     * matchScore                  <!-- RECURSIVE -->
     * <p>
     * overload for comparing two rules.  This method caches its results for
     * faster comparison
     */
    public double matchScore(Rule other) {
        //See if this score is already known
        double score = getRuleScore(this, other);
        if (score >= 0.0) return score;

        //Generate the score
        score = this.matchScore(other.prevRules, other.lhs, other.rhs);

        //Cache the result for next time
        putRuleScore(this, other, score);

        return score;
    }//matchScore

    /**
     * tune
     * <p>
     * adjusts the CondSets of this rule based on a given prediction that the rule made
     */
    public void tune(SensorData prevExternal, SensorData currExternal) {
        this.lhs.update(prevExternal);
        this.rhs.update(currExternal);
    }//tune

    /**
     * mergeWith
     * <p>
     * merges a given rule into this one
     */
    public void mergeWith(Rule other) {
        if (other.depth != this.depth) throw new IllegalArgumentException("Can not merge rules with mismattching depths");
        if (other.action != this.action) throw new IllegalArgumentException("Can not merge rules with mismattching depths");

        //Merge the external sensors
        this.lhs.mergeWith(other.lhs);
        this.rhs.mergeWith(other.rhs);

        //Merge the internal sensors by creating a union of the sets
        for(Rule r : prevRules) {
            if (! this.prevRules.contains(r)) this.prevRules.add(r);
        }
    }//mergeWith

    /**
     * prefixString
     *
     * is a helper for {@link #toString()} and {@link #verboseString()} that
     * places the rule id and internal sensors into a given StringBuilder
     */
    private void prefixString(StringBuilder sb) {
        //#id number
        sb.append("#");
        if (this.id < 10) sb.append(" ");
        sb.append(this.id);
        sb.append(":");

        //prev rules
        sb.append("(");
        boolean first = true;
        for(Rule r : this.prevRules) {
            if (!first) sb.append(",");
            else first = false;
            sb.append(r.id);
        }
        sb.append(")");
    }

    /**
     * verboseString
     *
     * creates a string representation of this object that shows all the cond values
     */
    public String verboseString() {
        StringBuilder sb = new StringBuilder();
        prefixString(sb);

        sb.append(this.lhs.verboseString());
        sb.append(this.action);
        sb.append(" -> ");
        sb.append(this.rhs.verboseString());

        //depth
        sb.append(" (depth: " + this.depth + ")");

        return sb.toString();
    }//verboseString

    /** toString() override */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        prefixString(sb);

        sb.append(this.lhs.bitString());
        sb.append(this.action);
        sb.append(" -> ");
        sb.append(this.rhs.bitString());

        //depth
        sb.append(" (depth: " + this.depth + ")");

        return sb.toString();
    }//toString

    public int getId() { return this.id; }
    public CondSet getLHS() { return (CondSet)this.lhs.clone(); }
    public CondSet getRHS() { return (CondSet)this.rhs.clone(); }
    public int getDepth() { return this.depth; }
    public char getAction() { return this.action; }
    public int getBitsLen() { return this.lhs.size() + this.rhs.size(); }
    public void setBrother(Rule bro, double newScore) { this.brother = bro; this.brotherScore = newScore; }
    public Rule getBrother() { return this.brother; }
    public double getBrotherScore() { return this.brotherScore; }
    public static double getRuleScore(Rule r1, Rule r2) {
        String key;
        if (r1.id < r2.id) {
            key = "" + r1.id + "_" + r2.id;
        } else {
            key = "" + r2.id + "_" + r1.id;
        }
        RuleScore rs = Rule.scoreHash.get(key);
        if (rs == null) return -1.0; //not found
        if (rs.timestamp != NdxrAgent.getTimeStep()) return -1.0; //out of date
        return rs.score;
    }
    private void putRuleScore(Rule r1, Rule r2, double score) {
        String key;
        if (r1.id < r2.id) {
            key = "" + r1.id + "_" + r2.id;
        } else {
            key = "" + r2.id + "_" + r1.id;
        }
        RuleScore rs = new RuleScore(score);
        Rule.scoreHash.put(key, rs);
    }


}//class Rule

