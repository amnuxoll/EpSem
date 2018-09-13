package agents.nsm;

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
    public final int K_NEAREST = 8;  //max allowed size of neighborhood

    public char command;           // action associated with this neighborhood
    public ArrayList<NBor> nbors;  // neigbhors in the hood
    public int shortest = 0;       //length of shortest neighbor

    public NHood(char initCmd) {
        this.command = initCmd;
        nbors = new ArrayList<NBor>();
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

}//class NHood
