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
    public static final int WILD = -1;

    //indicates which bits are wild.  This is left null if none are wild.
    private BitSet wilds = null;

    /** default ctor */
    public WCBitSet() { super(); }

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
            return WILD;
        }
        else {
            return super.get(index) ? 1 : 0;
        }
    }

    /** replaces a bit with a wildcard */
    public void wcset(int index) {
        if (wilds == null) wilds = new BitSet();
        wilds.set(index);
        this.set(index); //this allow the super bitset to have the right size
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
            if (thisVal == WILD) continue;
            int otherVal = other.wcget(i);
            if (thisVal != otherVal) this.wcset(i);
        }
    }

    @Override
    /** override size to take the wildcards into account */
    public int size() {
        if (this.wilds == null) return super.size();
        return Math.max(super.size(), this.wilds.size());
    }

    @Override
    /** override length to take the wildcards into account */
    public int length() {
        if (this.wilds == null) return super.length();
        return Math.max(super.length(), this.wilds.length());
    }

    /** equals() method that compares this WCBitSet to a regular BitSet */
    public boolean bsEquals(BitSet other) {
        int size = Math.max(other.size(), this.size());
        for(int i = 0; i < size; ++i) {
            boolean othBit = other.get(i);
            int thisBit = wcget(i);
            if ( (thisBit != WILD) && ( (thisBit == 1) != othBit)) {
                return false;
            }
        }
        return true;
    }//bsEquals

    /** equals override lets a wildcard equal any bit */
    @Override
    public boolean equals(Object obj) {
        //Verify matching types
        if (! (obj instanceof WCBitSet)) {
            if (obj instanceof BitSet) return bsEquals((BitSet)obj);
            else return false;
        }

        //compare
        WCBitSet other = (WCBitSet) obj;
        int size = Math.max(other.size(), this.size());
        for(int i = 0; i < size; ++i) {
            int othBit = other.wcget(i);
            int thisBit = wcget(i);
            if ( (thisBit != WILD) && (othBit != WILD) && (thisBit != othBit) ) {
                return false;
            }
        }

        return true;
    }//equals

    /** clone override
     * Note:  don't use WCBitSet's ctor for this as it may call get() on a WCBitSet object instead of wcget() */
    @Override
    public Object clone() {
        WCBitSet copy = new WCBitSet();
        for(int i = 0; i < this.size(); ++i) {
            int bit = this.wcget(i);
            if (bit == WILD) copy.wcset(i);
            else if (bit == 1) copy.set(i);
        }
        return copy;
    }//clone

    /** a variant toString that converts WCBitSet as a string of '1' and '0'
     * and '.' for wildcards.
     * @param len is the length of the specified result string */
    public String bitString(int len) {
        StringBuilder sb = new StringBuilder();
        for(int i = 0; i < len; ++i) {
            int val = wcget(i);
            char cVal = '.';
            if (val == 1) cVal = '1';
            else if (val == 0) cVal = '0';
            sb.append(cVal);
        }
        return sb.toString();
    }

}//class WCBitSet
