package agents.ndxr;

/**
 * class Conf
 * <p>
 * An instance of this class tracks the agent's confidence in something.
 */
public class Conf {
    /** number of bits per condition used to measure confidence.  I have this
     * set to 7 right now because a byte is being used for the confidence value.
     * (Remember, in Java there is no unsigned byte so the leftmost bit can't
     * be used.)
     */
    public static final int NUM_CONF_BITS = 7;

    /** the highest confidence that the agent can have */
    public static final byte MAX = (byte)0b01111111; //i.e., 127

    /** for convenice, identify the most significant bit in a confidence value */
    public static final byte MOST_SIG = (byte)(byte)0b01000000;

    /** the raw confidence value */
    public byte val = MAX;

    /** base ctor assumed max value */
    public Conf() { /* no add'l actions needed */  }

    /** ctor with a given init val */
    public Conf(byte initVal) { this.val = initVal;  }

    /** copy ctor */
    public Conf(Conf orig) {
        this.val = orig.val;
    }


    /**
     * adjusts the confidence up or down
     */
    public void adj(boolean increase) {
        //new value replaces the most significant bit
        val >>= 1;
        if (increase) {
            val |= MOST_SIG;
        }
    }//adj

    /**
     * returns the confidence value as a double in the range [0.0 .. 1.0]
     */
    public double dval() { return (double) val / (double) MAX; }

}//class Conf
