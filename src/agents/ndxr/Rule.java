package agents.ndxr;

import framework.SensorData;

import java.util.BitSet;
import java.util.HashSet;

/** describes sensing+action->sensing */
public class Rule {
    public static byte FQ_MAX = 8;  //max matches used by class FreqCount
    private static int nextId = 1;

    /**
     * class FreqCount
     *
     * is akin to TFRule.Cond but only tracks the last N matches
     * so that the agent can adapt to changes in the env
     */
    public class FreqCount {
        /**
         *  Each time the rule matches a new '1' or '0' is added pushed into
         *  the least significant position depending upon whether the condition
         *  associated with this counter matched.  Thus the agent has a
         *  record of the last N matches.  Right now counte is a byte.  If a
         *  longer record is needed, just use an int or long.  If an even
         *  longer record is needed, a BitSet could be used but doing so would
         *  make the shift-left operator unusable.
         */
        private byte counter = 0;
        private byte size = 0;  //number of bits in counter that are in use


        /**
         *
         * @param wasOn  is the sensor associated with this object currently on?
         */
        public FreqCount(boolean wasOn) {
            addBit(wasOn);
        }

        public void addBit(boolean on) {
            this.counter <<= 1;
            if (on) counter++;
            if (size < FQ_MAX) size++;
        }

        /** calc percent of time this sensor is "on" for last N matches */
        public double getFreq() {
            return (double)Integer.bitCount(counter) / (double)size;
        }
    }//class FreqCount

    private int id;         //unique id for this rule
    private int bitsLen;    //number of bits on each side of this rule
    private BitSet lhs;     //sensor values on lhs
    private char action;    //action taken by the agent
    private BitSet rhs;     //sensors values on the rhs

    //track the "on" frequency of each bit on the LHS and RHS
    private FreqCount[] lhsFreqs;
    private FreqCount[] rhsFreqs;

    //When building sequences of rules (paths) this rule may only be
    //preceded by a rule in this set.
    private HashSet<Rule> prevRules = new HashSet<>();

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

        if (initPrev != null) {
            this.prevRules.add(initPrev);
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

        return result.toString();
    }//toString

}//class Rule

