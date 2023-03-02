package agents.ndxr;

import java.util.BitSet;

/**
 * class WCBitSet
 * <p>
 * extends java.util.BitSet to allow a bit to have a "wildcard" value that
 * matches either 1 or 0.  New wcget() method replaces the original get()
 * method so it can returns 0, 1 or -1. The -1 value is used to denate a wildcard.
 * <p>
 * I've not overriden all the methods that should be affected by wildcarding.
 * Instead, I've only overridden those that are used by the agent.  If you
 * wish to use a new method, you may have to implement it here.
 */
public class WCBitSet extends BitSet implements Cloneable {
    //indicates which bits are wild.  This is left null if none are wild.
    private BitSet wilds = null;

    /** ctor to init with an existing BitSet */
    public WCBitSet(BitSet bs) {
        super();
        for(int i = 0; i <= bs.length(); ++i) {
            if (bs.get(i)) this.set(i);
        }
    }

    /** This method should not be used anymore */
    @Override public boolean get(int index) {
        System.err.println("Do not call get() on a WCBitSet!");
        return false;
    }

    /** return -1 if wc, otherwise 0=false and 1=true */
    public int wcget(int index) {
        if ((this.wilds != null) && (this.wilds.get(index))) {
            return -1;
        }
        else {
            return super.get(index) ? 1 : 0;
        }
    }

    /** replaces a bit with a wildcard */
    public void wcset(int index) {
        if (wilds == null) wilds = new BitSet();
        wilds.set(index);
    }

    /**
     * mergeWith
     *
     * merges another WCBitSet with this one.  Where their bits disagree,
     * the value is set to a wildcard.
     */
    public void mergeWith(WCBitSet other) {
        for(int i = 0; i <= other.length(); ++i) {
            int thisVal = this.wcget(i);
            if (thisVal == -1) continue;
            int otherVal = other.wcget(i);
            if (thisVal != otherVal) this.wcset(i);
        }
    }

    /** clone override */
    @Override
    public Object clone() {
        BitSet bits = (BitSet)super.clone();
        WCBitSet retVal = new WCBitSet(bits);
        if (this.wilds != null) {
            retVal.wilds = (BitSet)this.wilds.clone();
        }
        return retVal;
    }


}//class WCBitSet
