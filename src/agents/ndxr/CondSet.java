package agents.ndxr;

import framework.SensorData;

import java.util.BitSet;

/**
 * class CondSet
 * <p>
 * represents a sequence of conditions of a rule that can be used to
 * calculate a match score with a given sensor array. (Could be LHS or RHS.)
 * <p>
 * Each condition is either a '1' or '0' depending upon what experience (episode)
 * was used to create it.  Each condition also has a confidence that is
 * adjusted based upon subsequent experiences.  A condition with zero
 * confidence is effectively a wildcard bit (matches either '0' or '1').
 *
 */
public class CondSet implements Cloneable {
    /**
     * a convenient array used to extract individual bits from this.base
     */
    public static final byte[] singleBits = {(byte)0b00000001, (byte)0b00000010, (byte)0b00000100, (byte)0b00001000,
                                             (byte)0b00010000, (byte)0b00100000, (byte)0b01000000 };

    //The initial value of each bit is represented in this value which never
    // changes once set.  If an agent ever has more than 7 sensors, this will
    // have to become a different type to accommodate
    private byte base = 0;

    //The agent's confidence in each bit is represented by a corresponding
    //value in this array.  This confidence is used as the TF value  in
    //TF-IDF calculations
    private final Conf[] confs;

    /**
     * ctor
     *
     * @param sensors values to init the conditions with
     */
    public CondSet(SensorData sensors) {
        confs = new Conf[sensors.size()];
        BitSet bits = sensors.toBitSet();
        for(int i = 0; i < sensors.size(); ++i) {
            //initialize base
            if (bits.get(i)) {
                base |= CondSet.singleBits[i];
            }

            confs[i] = new Conf();
        }//for
    }//ctor

    /** copy ctor */
    public CondSet(CondSet orig) {
        this.base = orig.base;
        this.confs = new Conf[orig.confs.length];
        for(int i = 0; i < orig.confs.length; ++i) {
            this.confs[i] = new Conf(orig.confs[i]);
        }
    }

    /**
     * update
     * <p>
     * updates all the confidences with a given SensorData set
     */
    public void update(SensorData sensors) {
        BitSet bits = sensors.toBitSet();
        for(int i = 0; i < sensors.size(); ++i) {
            boolean sensorVal = bits.get(i);
            boolean myVal = (this.getBit(i) == 1);
            this.confs[i].adj(sensorVal == myVal);
        }
    }//update

    /**
     * cardMatch
     *
     * calculations the cardinality match between this set and another, given set.
     * This is based entirely upon the bit values.  No partial matching is performed.
     *
     * Note:  If all you care about is whether it is an exact match or not then use
     * {@link #equals(Object)}
     *
     * @return fraction of bits that match
     */
    public double cardMatch(CondSet other) {
        double count = 0.0;
        double sum = 0.0;
        for(int i = 0; i < this.size(); ++i) {
            count++;
            int myBit = this.getBit(i);
            int otherBit = other.getBit(i);
            if (myBit != otherBit) sum++;
        }

        return sum / count;
    }//cardMatch

    /**
     * matchScore
     * <p>
     * calculates how closely this CondSet matches a given SensorData set
     * as a TF-IDF (ish) match score.
     * NOTE:  sensors.size() should equal this.confs.length
     *
     * @return a TF-IDF (ish) match score in the range [0.0..1.0]
     */
    public double matchScore(SensorData sensors) {
        BitSet bits = sensors.toBitSet();
        double sum = 0.0; //sum of tfidf values for the sensors
        double max = 0.0; //max tfidf score possible
        for(int i = 0; i < sensors.size(); ++i) {
            boolean sensorVal = bits.get(i);
            boolean myVal = (this.getBit(i) == 1);
            double tf = this.confs[i].dval();
            double df = NdxrAgent.getDocFrequency(i);
            double relevance = Math.abs(tf - df);
            if (sensorVal == myVal) sum += tf * relevance;
            max += relevance;
        }

        return sum / max;
    }//matchScore

    /**
     * matchScore
     * <p>
     * calculates how closely this CondSet matches another CondSet
     * NOTE:  the other set should be same size
     *
     * @return a TF-IDF (ish) match score in the range [0.0..1.0]
     */
    public double matchScore(CondSet other) {
        double sum = 0.0; //sum of tfidf values for the sensors
        double max = 0.0; //max tfidf score possible
        for(int i = 0; i < this.size(); ++i) {
            int myBit = this.getBit(i);
            int otherBit = other.getBit(i);
            double tf = this.confs[i].dval();
            double df = NdxrAgent.getDocFrequency(i);
            double relevance = Math.abs(tf - df);
            if (myBit == otherBit) sum += tf * relevance;
            max += relevance;
        }

        //catch divide by zero (complete cardinality mismatch)
        if (max == 0.0) return 0.0;

        return sum/max;
    }//matchScore

    /**
     * mergeWith
     * <p>
     * destructively merges this CondSet with a given one.
     */
    public void mergeWith(CondSet other) {
        for(int i = 0; i < this.confs.length; ++i) {
            int myBit = this.getBit(i);
            int otherBit = other.getBit(i);
            byte conf1 = this.confs[i].val;
            byte conf2 = other.confs[i].val;

            //If the bits don't match, break the tie with confidence
            if ((myBit != otherBit) && (conf2 > conf1)){
                this.base ^= CondSet.singleBits[i];
                confs[i] = new Conf((byte)(conf2 - conf1));  //difference is new confidence
            }

            //otherwise just average the confidences
            else {
                confs[i] = new Conf((byte)((conf1 + conf2) / 2));
            }
        }
    }//mergeWith

    /**
     * bitString
     * <p>
     * returns a string of '1' and '0' that represents this.base
     */
    public String bitString() {
        StringBuilder sb = new StringBuilder();
        for(int i = 0; i < this.confs.length; ++i) {
            char bit = (getBit(i) == 1) ? '1' : '0';
            sb.append(bit);
        }
        return sb.toString();
    }//bitString

    /**
     * wcBitString
     * <p>
     * returns a string of '1', '0' and '.' that represents this CondSet
     * '1' and '0' are used when confidence is high.  Otherwise '.' is used.
     */
    public String wcBitString() {
        StringBuilder sb = new StringBuilder();
        for(int i = 0; i < this.confs.length; ++i) {
            int bit = getBit(i);
            char bitChar;
            if (confs[i].val > Conf.MOST_SIG) {
                bitChar = (bit == 1) ? '1' : '0';
            } else {
                bitChar = (bit == 1) ? '¹' : '°';  //smaller symbols indicate lower confidence
            }
            sb.append(bitChar);
        }
        return sb.toString();
    }//wcBitString

    /**
     * verboseString
     * <p>
     * creates a string representation of this object that shows all the conf values
     */
    public String verboseString() {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        for(int i = 0; i < confs.length; ++i) {
            if (i > 0) sb.append(", ");
            sb.append(getBit(i));
            sb.append(':');
            sb.append(confs[i].val);
        }
        sb.append("}");
        return sb.toString();
    }//verboseString

    /** equals() override:  'base' must match */
    @Override
    public boolean equals(Object obj) {
        if (! (obj instanceof CondSet other)) return false;
        if (this.confs.length != other.confs.length) return false;
        return (this.base == other.base);
    }

    /** clone() override */
    @Override
    public Object clone() {
        try { super.clone(); }
        catch(CloneNotSupportedException cnse) { /* don't care */ }
        return new CondSet(this);
    }

    /** toString() override */
    @Override
    public String toString() {
        return wcBitString();
    }

    /** getBit retrieves the value of a single bit in the base */
    public int getBit(int i) {
        if (i >= confs.length) return 0; //should not happen!
        if (i < 0) return 0; //should not happen!
        int bit = 0;
        if ( (this.base & CondSet.singleBits[i]) > 0) bit = 1;
        return bit;
    }

    //WARNING:  Think carefully before you write a working setBit method!
    // By design, this.base should not change once set in the ctor.
    private void setBit() { System.err.println("see warning above"); }

    public int size() { return confs.length; }

}//class CondSet
