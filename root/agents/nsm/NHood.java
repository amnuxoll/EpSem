package agents.nsm;

import framework.Move;
import java.util.ArrayList;
import java.util.Collections;

/**
 * class NHood
 *
 * is a public container class for defining a "neighborhood" in episodic
 * memory.  Specifically this a set of k sequences that have the longest
 * match to the current sequene ending with the last episode (which
 * represents the present moment) presuming that a specific action will be
 * taken next.
 */
public class NHood {
    //region Static Variables
    public final int K_NEAREST = 8;  //max allowed size of neighborhood
    //endregion

    //region Class Variables
    public Move command;           // action associated with this neighborhood
    public ArrayList<NBor> nbors;  // neigbhors in the hood
    public int shortest = 0;       //length of shortest neighbor
    //endregion

    //region Constructors
    public NHood(Move initMove) {
        this.command = initMove;
        nbors = new ArrayList<NBor>();
    }
    //endregion

    //region Public Methods
    public Move getMove()
    {
        return this.command;
    }

    /** adds a new neighbor to the neighborhood.
     * CAVEAT:  Caller is responsible for checking the neighbor is long
     * enough to belong. */
    public void addNBor(NBor newGuy) {
        //if the nhood is full, drop the shorest neighbor to make room
        while(nbors.size() >= K_NEAREST) {
            this.nbors.remove(0);
        }

        this.nbors.add(newGuy);

        //update the shortest
        Collections.sort(this.nbors);
        this.shortest = nbors.get(0).len;
    }//addNBor

    public double getQValue() {
        //Don't calculate for empty neighborhoods
        if (this.nbors.size() == 0)
            return 0.0;

        // sum the q-values of each neighbor
        double total = 0.0;
        for(NBor nbor : this.nbors)
        {
            total += nbor.calculateQValue();
//            QEpisode qep = episodicMemory.get(nbor.end);
//            total += qep.qValue;
        }

        // return the average
        return (total / (double)this.nbors.size());
    }
    //endregion
}//class NHood
