package agents.ndxr;

import framework.SensorData;

import java.util.ArrayList;
import java.util.HashSet;

/** describes sensing+action->sensing */
public class Rule {
    private static int nextId = 1;  //next unique rule Id (use val then increment)
    public static int MAX_DEPTH = 7; //maximum rule depth allowed (see depth instance var)

    /*===========================================================================
     * Instance Variables
     ----------------------------------------------------------------------------*/

    private int id;         //unique id for this rule
    private CondSet lhs;    //LHS external sensor conditions
    private char action;    //action taken by the agent
    private CondSet rhs;    //RHS external sensor conditions

    //When building sequences of rules (paths) this rule may only be
    //preceded by a rule in this set.
    private HashSet<Rule> prevRules = new HashSet<>();

    //A rule's depth is how many
    private int depth = 0;

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
     * matchScore
     * <p>
     * calculates the match score of this rule with given LHS.  The action
     * is presumed to match already.
     *
     * @param rhs can be null or zero length if only a LHS score is desired
     */
    public double matchScore(ArrayList<Rule> prevInt, CondSet lhs, CondSet rhs) {
        //Unless depth 0, at least one prevInt must match this rule
        if (depth > 0) {
            boolean mat = false;
            for (Rule r : prevInt) {
                if (this.prevRules.contains(r)) {
                    mat = true;
                    break;
                }
            }
            if (!mat) return 0.0;
        }

        //compare external sensors
        double score = this.lhs.matchScore(lhs);
        if ((rhs != null) && (rhs.size() > 0)) {
            score *= this.rhs.matchScore(rhs);
        }
        return score;
    }//matchScore

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
        this.prevRules.addAll(other.prevRules);
    }//mergeWith



    /** toString() override */
    @Override
    public String toString() {
        StringBuilder result = new StringBuilder();
        //#id number
        result.append("#");
        if (this.id < 10) result.append(" ");
        result.append(this.id);
        result.append(":");

        //prev rules
        result.append("(");
        boolean first = true;
        for(Rule r : this.prevRules) {
            if (!first) result.append(",");
            else first = false;
            result.append(r.id);
        }
        result.append(")");

        result.append(this.lhs.bitString());
        result.append(this.action);
        result.append(" -> ");
        result.append(this.rhs.bitString());

        //depth
        result.append(" (depth: " + this.depth + ")");

        return result.toString();
    }//toString

    public int getId() { return this.id; }
    public CondSet getLHS() { return (CondSet)this.lhs.clone(); }
    public CondSet getRHS() { return (CondSet)this.rhs.clone(); }
    public int getDepth() { return this.depth; }
    public char getAction() { return this.action; }
    public int getBitsLen() { return this.lhs.size() + this.rhs.size(); }


}//class Rule

