package agents.ndxr;

import framework.SensorData;

import javax.naming.LimitExceededException;
import java.util.BitSet;
import java.util.HashSet;

/** describes sensing+action->sensing */
public class Rule {
    public static byte FQ_MAX = 8;  //max matches used by class FreqCount
    private static int nextId = 1;  //next unique rule Id (use val then increment)
    public static int MAX_DEPTH = 7; //maximum rule depth allowed (see depth instance var)

    /**
     * class FreqCount
     *
     * is akin to {@link agents.phujus.TFRule.Cond} but only tracks the last N matches
     * so that the agent can adapt to changes in the env
     */
    public class FreqCount {
        /**
         *  Each time the rule matches a new '1' or '0' is added pushed into
         *  the least significant position depending upon whether the condition
         *  associated with this counter matched.  Thus the agent has a
         *  record of the last N matches.  Right now count is a byte.  If a
         *  longer record is needed, just use an int or long.  If an even
         *  longer record is needed, a BitSet could be used but doing so would
         *  make the shift-left operator unusable.
         */
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
    private int bitsLen;    //number of external sensor bits on each side of this rule
    private BitSet lhs;     //sensor values on lhs
    private char action;    //action taken by the agent
    private BitSet rhs;     //sensors values on the rhs

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
        this.lhs = initLHS.toBitSet();
        this.action = initAction;
        this.rhs = initRHS.toBitSet();
        this.bitsLen = Math.max(initLHS.size(), initRHS.size());
        this.lhsFreqs = new FreqCount[bitsLen];
        this.rhsFreqs = new FreqCount[bitsLen];
        for(int i = 0; i < bitsLen; ++i) {
            this.lhsFreqs[i] = new FreqCount(this.lhs.get(i));
            this.rhsFreqs[i] = new FreqCount(this.rhs.get(i));
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

    /** helper for toString that converst BitSet as a string of '1' and '0'
     *  The input is presumed to be of length this.bitsLen.  */
    private void bitsToString(StringBuilder addToMe, BitSet addThis) {
        for(int i = 0; i < this.bitsLen; ++i) {
            addToMe.append(addThis.get(i) ? '1' : '0');
        }
    }

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

    public int getDepth() { return this.depth; }
    public char getAction() { return this.action; }


}//class Rule

