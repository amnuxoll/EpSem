package agents.nsm;

import framework.Action;
import utils.EpisodicMemory;

/**
 *
 * @author Zachary Paul Faltersack
 * @version 0.95
 */
public class QEpisodicMemory extends EpisodicMemory<QEpisode> {
    //region Public Methods
    public NHood buildNeighborhoodForMove(Action action)
    {
        NHood nHood = new NHood(action);
        //find the kNN
        for(int i = 0; i <= this.currentIndex(); ++i) {
            int matchLen = this.matchedMemoryStringLength(action, i);
            if ((matchLen > 0) && ((nHood.shortest <= matchLen) || (nHood.nbors.size() < nHood.K_NEAREST))) {
                nHood.addNBor(new NBor(i, matchLen, this.episodicMemory.get(i)));
            }
        }//for
        return nHood;
    }
    //endregion

    //region Private Methods
    /**
     * matchedMemoryStringLength
     *
     * Starts from a given index and the end of the Agent's episodic memory and
     * actions backwards, comparing each episode to the present episode and it
     * prededessors until the corresponding episdoes no longer match.
     *
     * @param endOfStringIndex The index from which to start the backwards search
     * @return the number of consecutive matching characters
     */
    private int matchedMemoryStringLength(Action nHoodAction, int endOfStringIndex) {
        if (!nHoodAction.equals(this.episodicMemory.get(endOfStringIndex).getAction()))
            return 0;
        int length = 0;
        endOfStringIndex--;
        int indexOfMatchingAction = this.currentIndex();
        for (int i = endOfStringIndex; i >= 0; i--) {
            //We want to compare the command from the prev episode and the
            //sensors from the "right now" episode to the sequence at the
            //index indicated by 'i'
            if (this.episodicMemory.get(indexOfMatchingAction).equals(this.episodicMemory.get(i))) {
                length++;
                indexOfMatchingAction--;
            }
            else {
                return length;
            }
        }//for

        return length;
    }//matchedMemoryStringLength
    //endregion
}
