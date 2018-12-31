package agents.nsm;

/**
 * class NBor
 *
 * describes a "neighbor", specifically a sequence that matches the current
 * sequence ending with the current episode (which represents the present
 * moment) presuming that a specific action will be taken next.
 */
public class NBor implements Comparable<NBor> {
    //region Class Variables
    //public int end;  // index of the current episode of the sequence
    //public int begin; // index of the first episode of the sequence
    public int len;  //length of the sequence
    public QEpisode qEpisode;
    //endregion

    //region Constructors
    public NBor(int initEnd, int initLen, QEpisode episode) {
        //this.begin = initEnd - initLen;
        //this.end = initEnd;
        this.len = initLen;
        this.qEpisode = episode;
    }
    //endregion

    //region Comparable<NBor> Members
    /** this allows a collection of NBors to be sorted on length */
    public int compareTo(NBor other) {
        return this.len - other.len;
    }
    //endregion

    //region Public Methods
    public double calculateQValue() {
        return this.qEpisode.qValue;
    }
    //endregion
}//class NBor
