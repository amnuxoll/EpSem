package agents.ndxr;

import framework.SensorData;

import java.util.Arrays;
import java.util.BitSet;

/**
 * class CondSet
 * <p>
 * represents a sequence of 1+ conditions of a rule that can be used to
 * calculate a match score with a given sensor array. (Could be LHS or RHS.)
 */
public class CondSet implements Cloneable {
    /**
     * MOST_SIG is used for updates.  Only the most significant bit is set.
     *
     */
    public static final byte MOST_SIG = (byte)0b01000000;  //i.e., 64

    /** number of bits per condition  (Remember, in Java there is no unsigned
     * byte so the leftmost bit can't be used.)
     */
    public static final double NUM_BITS = 7.0;

    /** max value of a condition */
    public static final byte MAX_COND = (byte)0b01111111; //i.e., 127

    //Each condition is represented by a value.  The closer this value is
    //to zero the more confident the agent is that the sensor must be zero
    //to match it.
    //NOTE:  Right now I'm using a byte.  A longer value may be a better
    //       choice in the future (:AMN: March 2023)
    private final byte[] conds;


    /**
     * ctor
     *
     * @param sensors values to init the conditions with
     */
    public CondSet(SensorData sensors) {
        conds = new byte[sensors.size()];
        BitSet bits = sensors.toBitSet();
        for(int i = 0; i < sensors.size(); ++i) {
            conds[i] = (bits.get(i)) ? Byte.MAX_VALUE : 0;
        }
    }//ctor

    /** copy ctor */
    public CondSet(CondSet orig) {
        this.conds = Arrays.copyOf(orig.conds, orig.conds.length);
    }

    /**
     * updateOne
     * <p>
     * updates the value of a particular condition with a given value
     *
     * @return the new value
     */
    public byte updateOne(int i, boolean val) {
        //check for valid index
        if ((i < 0) || (i  >= this.conds.length)) {
            return 0;
        }

        //new value replaces the most significant bit
        conds[i] >>= 1;
        if (val) {
            conds[i] &= MOST_SIG;
        }

        return conds[i];
    }//update one condition

    /**
     * update
     * <p>
     * updates all the conditions with a given SensorData set
     */
    public void update(SensorData sensors) {
        BitSet bits = sensors.toBitSet();
        for(int i = 0; i < sensors.size(); ++i) {
            updateOne(i, bits.get(i));
        }
    }//update all conditions

    /**
     * matchScore
     * <p>
     * calculates how closely this CondSet matches a given SensorData set
     *
     * @return a match score in the range [0.0..1.0]
     *
     * TODO:  Use TF-IDF for this method?  For now it's just a weighted cardinality count
     */
    public double matchScore(SensorData sensors) {
        BitSet bits = sensors.toBitSet();
        double sum = 0.0;
        for(int i = 0; i < sensors.size(); ++i) {
            if (bits.get(i)) {
                sum += conds[i];
            } else {
                sum += Byte.MAX_VALUE - conds[i];
            }
        }
        double score = sum / (NUM_BITS * Byte.MAX_VALUE);
        return score;
    }//matchScore

    /**
     * matchScore
     * <p>
     * calculates how closely this CondSet matches another CondSet
     * NOTE:  the other set must be same size
     *
     * @return a match score in the range [0.0..1.0]
     *
     * TODO:  Use TF-IDF for this method?  For now it's just an avg diff.  Could also use a squared diff??
     */
    public double matchScore(CondSet other) {
        double sum = 0.0;
        for(int i = 0; i < this.size(); ++i) {
            sum += Math.abs(this.conds[i] - other.conds[i]);
        }
        double score = 1.0 - (sum / (this.size() * MAX_COND));
        return score;
    }//matchScore

    /**
     * mergeWith
     * <p>
     * destructively merges this CondSet with a given one.
     * Each condition is set to the average of the two.
     */
    public void mergeWith(CondSet other) {
        for(int i = 0; i < this.conds.length; ++i) {
            int n1 = this.conds[i];
            int n2 = other.conds[i];
            int avg = (n1 + n2) / 2;
            conds[i] = (byte)avg;
        }
    }//mergeWith

    /**
     * bitString
     * <p>
     * returns a string of '1' and '0' that is closest to this CondSet
     */
    public String bitString() {
        StringBuilder sb = new StringBuilder();
        for(int i = 0; i < this.conds.length; ++i) {
            char bit = (conds[i] > MOST_SIG) ? '1' : '0';
            sb.append(bit);
        }
        return sb.toString();
    }//bitString

    /** equals() override */
    @Override
    public boolean equals(Object obj) {
        if (! (obj instanceof CondSet)) return false;
        CondSet other = (CondSet) obj;
        double score = this.matchScore(other);
        return (score == 1.0);  //TODO: too strict?  Could start strict and relax based on experience?
    }

    /** clone() override */
    @Override
    public Object clone() {
        return new CondSet(this);
    }

    /** getBit retrieves the value of a single condition at a given index rounded to 0 or 1 */
    public int getBit(int i) {
        if (i >= conds.length) return 0; //should not happen!
        if (i < 0) return 0; //should not happen!
        return conds[i] > MOST_SIG ? 1 : 0;
    }


    public int size() { return conds.length; }
}//class CondSet
