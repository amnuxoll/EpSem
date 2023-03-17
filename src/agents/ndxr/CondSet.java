package agents.ndxr;

import framework.SensorData;

import java.util.Arrays;
import java.util.BitSet;

/**
 * class CondSet
 * <p>
 * represents a sequence of conditions of a rule that can be used to
 * calculate a match score with a given sensor array. (Could be LHS or RHS.)
 * <p>
 * Each condition is either a '1' or '0' depending upon what exerience (episode)
 * was used to create it.  Each condition also has a confidence that is
 * adjusted based upon subsequent experiences.  A condition with zero
 * confidence is effectively a wildcard bit (matches either '0' or '1').
 *
 */
public class CondSet implements Cloneable {
    /** number of bits per condition used to measure confidence.  I have this
     * set to 7 right now because a byte is being used for each confidence value.
     * (Remember, in Java there is no unsigned byte so the leftmost bit can't
     * be used.)
     */
    public static final int NUM_CONF_BITS = 7;

    /**
     * a convenient array used to extract individual bits from this.base
     */
    public static final byte[] singleBits = {(byte)0b00000001, (byte)0b00000010, (byte)0b00000100, (byte)0b00001000,
                                             (byte)0b00010000, (byte)0b00100000, (byte)0b01000000 };

    /** the highest confidence that the agent can have */
    public static final byte MAX_CONF = (byte)0b01111111; //i.e., 127

    /** for convenice, identify the most significant bit in a confidnce value */
    public static final byte MOST_SIG = (byte)(byte)0b01000000;

    //The initial value of each bit is represented in this value which never
    // changes once set.  If an agent ever has more than 7 sensors, this will
    // have to become a different type to accommodate
    private byte base = 0;

    //The agent's confidence in each bit is represented by a corresponding
    //value in this array.
    //NOTE:  Right now I'm using a byte.  A longer value may be a better
    //       choice in the future (:AMN:, March 2023)
    private final byte[] confs;


    /**
     * ctor
     *
     * @param sensors values to init the conditions with
     */
    public CondSet(SensorData sensors) {
        confs = new byte[sensors.size()];
        BitSet bits = sensors.toBitSet();
        for(int i = 0; i < sensors.size(); ++i) {
            //initialize base
            if (bits.get(i)) {
                base |= CondSet.singleBits[i];
            }

            //Conds start at maximum
            confs[i] = MAX_CONF;
        }
    }//ctor

    /** copy ctor */
    public CondSet(CondSet orig) {
        this.base = orig.base;
        this.confs = Arrays.copyOf(orig.confs, orig.confs.length);
    }

    /**
     * adjustOne
     * <p>
     * adjusts the confidence in a particular condition to make it stronger or weaker
     *
     * @return the confidence value
     */
    public byte adjustOne(int i, boolean stronger) {
        //check for valid index
        if ((i < 0) || (i  >= this.confs.length)) return 0;

        //new value replaces the most significant bit
        confs[i] >>= 1;
        if (stronger) {
            confs[i] |= MOST_SIG;
        }

        return confs[i];
    }//adjustOne

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
            adjustOne(i, sensorVal == myVal);
        }
    }//update

    /**
     * matchScore
     * <p>
     * calculates how closely this CondSet matches a given SensorData set
     * NOTE:  sensors.size() should equal this.confs.length
     *
     * @return a match score in the range [0.0..1.0]
     *
     * TODO:  Use TF-IDF for this method?  For now, it's just a weighted cardinality count
     */
    public double matchScore(SensorData sensors) {
        BitSet bits = sensors.toBitSet();
        double sum = 0.0;
        for(int i = 0; i < sensors.size(); ++i) {
            boolean sensorVal = bits.get(i);
            boolean myVal = (this.getBit(i) == 1);
            if (sensorVal == myVal) {
                sum += confs[i];
            }
        }
        double score = sum / ((double)(NUM_CONF_BITS * MAX_CONF));
        return score;
    }//matchScore

    /**
     * matchScore
     * <p>
     * calculates how closely this CondSet matches another CondSet
     * NOTE:  the other set should be same size
     *
     * @return a match score in the range [0.0..1.0]
     *
     * TODO:  Use TF-IDF for this method?  For now, it's just a weighted cardinality count
     */
    public double matchScore(CondSet other) {
        double sum = 0.0;
        for(int i = 0; i < this.size(); ++i) {
            int myBit = this.getBit(i);
            int otherBit = other.getBit(i);
            if (myBit == otherBit) {
                sum += this.confs[i];
                sum += other.confs[i];
            }
        }
        double score = sum / (2.0 * this.size() * MAX_CONF);
        return score;
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
            byte conf1 = this.confs[i];
            byte conf2 = other.confs[i];

            //If the bits don't match, break the tie with confidence
            if ((myBit != otherBit) && (conf2 > conf1)){
                this.base ^= CondSet.singleBits[i];
                confs[i] = (byte)(conf2 - conf1);  //difference is new confidence
            }

            //otherwise just average the confidences
            else {
                confs[i] = (byte)((conf1 + conf2) / 2);
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
            char bit = (getBit(i) == 1) ? '1' : '0';
            if (confs[i] <= MOST_SIG) bit = '.';
            sb.append(bit);
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
            sb.append(confs[i]);
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
