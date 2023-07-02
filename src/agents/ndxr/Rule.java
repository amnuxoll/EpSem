package agents.ndxr;

import framework.SensorData;

import java.util.Vector;
import java.util.HashMap;

/** describes sensing+action->sensing */
public class Rule {
    private static int nextId = 1;  //next unique rule Id (use val then increment)
    public static int MAX_DEPTH = 7; //maximum rule depth allowed (see depth instance var)

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
    private Vector<Rule> prevRules = new Vector<>();

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
     * cardMatchRuleList
     * <p>
     * is a helper method for {@link #matchRuleList(Vector)}
     * and {@link #cardMatchScore(Vector, CondSet, CondSet)} .
     * It compares a lists of Rules to this.prevRules to find at
     * least one value in common.
     *
     * Note:  depth 0 rules are always considered a match
     *
     * @return true if common value found or this rule is depth 0
     */
    private boolean cardMatchRuleList(Vector<Rule> prevInt) {
        if (depth == 0) return true;
        if (prevInt.size() == 0) return false;

        //If the two lists have at least one value in common then
        //they are a full match
        for (Rule r : prevInt) {
            if (this.prevRules.contains(r)) {
                return true;
            }
        }
        return false;
    }   //cardMatchRuleList

    /**
     * matchRuleList            <!-- RECURSIVE -->
     * <p>
     * is a helper method for {@link #matchScore(Vector, CondSet, CondSet)}.
     * It compares a lists of Rules to this.prevRules for similarity
     */
    public double matchRuleList(Vector<Rule> prevInt) {
        if (cardMatchRuleList(prevInt) ) return 1.0;

        //TODO:  Consider adding this back in.  I'm taking it out for now to
        //       minimize the amount of differences between NDXR and PhuJus
        //A partial match score is the best match score of any two rules
        double bestScore = 0.0;
//        for(Rule r1 : prevInt) {
//            for (Rule r2 : this.prevRules) {
//                double score = r1.matchScore(r2);  //Note: recursion!
//                if (score > bestScore) {
//                    bestScore = score;
//                }
//            }
//        }

        return bestScore;
    }//matchRuleList

    /**
     * cardMatchScore
     * <p>
     * calculates the cardinality match score of this rule with given LHS.
     * In other words, this method does not do any partial matches and looks
     * explicitly for exactly matching conditions.  The action is presumed
     * to match already.
     *
     * @param rhs can be null or zero length if only a LHS score is desired
     * <p>
     */
    public double cardMatchScore(Vector<Rule> prevInt, CondSet lhs, CondSet rhs) {
        //Check external conditions first
        double score = this.lhs.cardMatch(lhs);
        if ((rhs != null) && (rhs.size() > 0)) {
            score *= this.rhs.cardMatch(rhs);
        }

        score *= matchRuleList(prevInt);

        return score;
    }//cardMatchScore

    /**
     * matchScore                <!-- RECURSIVE -->
     * <p>
     * calculates the match score of this rule with given LHS (+ RHS).
     * The action is presumed to match already.
     * <p>
     * Note: If the method detects that the final match score will be less than
     *       MIN_MATCH_SCORE then 0.0 is returned immediately (for efficiency)
     *
     * @param rhs can be null or zero length if only a LHS score is desired
     * <p>
     * Warning:  This method recurses by calling {@link #matchRuleList}!
     */
    public double matchScore(Vector<Rule> prevInt, CondSet lhs, CondSet rhs) {
        //Check external conditions first
        double score = this.lhs.matchScore(lhs);
        if ((rhs != null) && (rhs.size() > 0)) {
            score *= this.rhs.matchScore(rhs);
        }

        score *= matchRuleList(prevInt);   //indirect recursive call

        return score;
    }//matchScore
    
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
     *
     * Note:  This method does not update other Rules and PathRules that
     *         may be affected by this merge.
     */
    public void mergeWith(Rule other) {
        if (other.depth != this.depth) throw new IllegalArgumentException("Can not merge rules with mismattching depths");
        if (other.action != this.action) throw new IllegalArgumentException("Can not merge rules with mismattching depths");

        //Merge the external sensors
        this.lhs.mergeWith(other.lhs);
        this.rhs.mergeWith(other.rhs);

        //Merge the internal sensors by creating a union of the sets
        for(Rule r : other.prevRules) {
            if (! this.prevRules.contains(r)) this.prevRules.add(r);
        }

        //Remove any "bro" designation
        setBrother(null, 0.0);

    }//mergeWith

    /**
     * cleanup
     *
     * removes any references this rule has to encourage garbage collection.
     * This rule should not be used after this method is called
     */
    public void cleanup() {
        this.prevRules.clear();
        this.prevRules = null;
        this.rhs = null;
        this.lhs = null;
        this.brother = null;
    }//cleanup



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

        sb.append(this.lhs.wcBitString());
        sb.append(this.action);
        sb.append(" -> ");
        sb.append(this.rhs.wcBitString());

        //depth
        sb.append(" (depth: ");
        sb.append(this.depth);

        //brother
        sb.append(", bro: ");
        if (this.brother != null) {
            sb.append("#");
            sb.append(this.brother.getId());
            sb.append("=");
            sb.append(this.brotherScore);
        } else {
            sb.append("null");
        }
        sb.append(")");

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

    /** replace one rule in this.prevRules with another */
    public void replacePrevRule(Rule removeMe, Rule replacement) {
        RuleIndex.replaceInRuleList(replacement, removeMe, this.prevRules);
    }

}//class Rule

