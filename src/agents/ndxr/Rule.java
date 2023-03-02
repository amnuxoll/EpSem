package agents.ndxr;

import framework.SensorData;

import java.util.HashSet;

/** describes sensing+action->sensing */
public class Rule {
    public static byte FQ_MAX = 8;  //max matches used by class FreqCount
    private static int nextId = 1;  //next unique rule Id (use val then increment)
    public static int MAX_DEPTH = 7; //maximum rule depth allowed (see depth instance var)

    /**
     * class FreqCount
     * <p>
     * is akin to {@link agents.phujus.TFRule.Cond} but only tracks the last N matches
     * so that the agent can adapt to changes in the env
     * <p>
     * Each time the rule matches a new '1' or '0' is added pushed into
     * the least significant position depending upon whether the condition
     * associated with this counter matched.  Thus the agent has a
     * record of the last N matches.  Right now count is a byte.  If a
     * longer record is needed, just use an int or long.  If an even
     * longer record is needed, a BitSet could be used but doing so would
     * make the shift-left operator unusable.
     */
    public class FreqCount {

        /**
         * MOST_SIG is the same len size as {@link #counter} with only most significant bit set.
         * This is used to efficiently keep {@link #size} up to date.
         */
        public static final byte MOST_SIG = (byte)0b10000000;

        /**
         * a bit string indicating the history (see the main comment on this class)
         * If counter's type changes, all the other vars must change to match and
         * {@link Rule#FQ_MAX} must also be updated.
         */
        private byte counter = 0;

        /** number of bits in counter that are in use */
        private byte size = 0;

        /**
         *
         * @param wasOn  is the sensor associated with this object currently on?
         */
        public FreqCount(boolean wasOn) {
            addBit(wasOn);
        }

        /** updates the object with a new bit */
        public void addBit(boolean b) {
            //Are we about to push off a '1' bit?
            if ((this.counter & MOST_SIG) != 0) this.counter--;  //Note:  if (size < FQ_MAX) this won't happen

            //shift and then add a new '1' if needed
            this.counter <<= 1;
            if (b) {
                this.counter++;
            }

            //update size as needed up to max
            if (size < FQ_MAX) {
                size++;
            }
        }

        /** calc percent of time this sensor is "on" for last N matches */
        public double getFreq() {
            return (double)Integer.bitCount(counter) / (double)size;
        }
    }//class FreqCount

    /*===========================================================================
     * Instance Variables
     ----------------------------------------------------------------------------*/

    private int id;         //unique id for this rule
    private WCBitSet lhs;   //sensor values on lhs*
    private char action;    //action taken by the agent
    private WCBitSet rhs;   //sensors values on the rhs*

    /*     ----===== An Important Warning About BitSet/WCBitSet ===----
     *
     *   the .size() of a bitset is how much ram is allocated (default 64)
     *   the .length() of a bitset varies depending upon what bits are set.
     *      for example if your bitset is "000000" then the size is zero!
     *
     *   If you want to know the "size" of lhs/rhs you will usually want
     *   to refer to SensorData.size() instead.
     *
     */

    //number of external sensor bits on each side of a Rule (same for all rules)
    private static int bitsLen = -1;

    //track the "on" frequency of each bit on the LHS and RHS
    private FreqCount[] lhsFreqs;
    private FreqCount[] rhsFreqs;

    //When building sequences of rules (paths) this rule may only be
    //preceded by a rule in this set.
    private HashSet<Rule> prevRules = new HashSet<>();

    //A rule's depth is how many
    private int depth = 0;

    /**
     * boring ctor
     */
    public Rule(SensorData initLHS, char initAction, SensorData initRHS,
                Rule initPrev) {
        this.id = Rule.nextId;
        Rule.nextId++;
        this.lhs = new WCBitSet(initLHS.toBitSet());
        this.action = initAction;
        this.rhs = new WCBitSet(initRHS.toBitSet());
        if (this.bitsLen == -1)  Rule.bitsLen = Math.max(initLHS.size(), initRHS.size());
        this.lhsFreqs = new FreqCount[bitsLen];
        this.rhsFreqs = new FreqCount[bitsLen];
        for(int i = 0; i < bitsLen; ++i) {
            this.lhsFreqs[i] = new FreqCount(this.lhs.wcget(i) != 0);
            this.rhsFreqs[i] = new FreqCount(this.rhs.wcget(i) != 0);
        }

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

    /** helper for toString that converts WCBitSet as a string of '1' and '0'
     * and '.' for wildcards.  The input is presumed to be of length this.bitsLen.  */
    private void bitsToString(StringBuilder addToMe, WCBitSet addThis) {
        for(int i = 0; i < this.bitsLen; ++i) {
            int val = addThis.wcget(i);
            char cVal = '.';
            if (val == 1) cVal = '1';
            else if (val == 0) cVal = '0';
            addToMe.append(cVal);
        }
    }

    /**
     * mergeWith
     *
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

        //TODO: Merge the frequency counts.  Can't do it now as no way to test it.

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

        bitsToString(result, this.lhs);
        result.append(this.action);
        result.append(" -> ");
        bitsToString(result, this.rhs);

        //depth
        result.append(" (depth: " + this.depth + ")");

        return result.toString();
    }//toString

    public WCBitSet getLHS() { return (WCBitSet)this.lhs.clone(); }
    public WCBitSet getRHS() { return (WCBitSet)this.rhs.clone(); }
    public int getDepth() { return this.depth; }
    public char getAction() { return this.action; }


}//class Rule

