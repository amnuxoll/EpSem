package agents.nsm;

/**
 * class NBor
 *
 * describes a "neighbor", specifically a sequence that matches the current
 * sequence ending with the last episode (which represents the present
 * moment) presuming that a specific action will be taken next.
 */
public class NBor implements Comparable<NBor> {
    public int end;  // index of the last episode of the sequence
    public int begin; // index of the first episode of the sequence
    public int len;  //length of the sequence

    public NBor(int initEnd, int initLen) {
        this.begin = initEnd - initLen;
        this.end = initEnd;
        this.len = initLen;
    }

    /** this allows a collection of NBors to be sorted on length */
    public int compareTo(NBor other) {
        return this.len - other.len;
    }
}//class NBor
