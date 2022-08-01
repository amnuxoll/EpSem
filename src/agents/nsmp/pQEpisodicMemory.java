package agents.nsmp;

import agents.nsm.NBor;
import agents.nsm.NHood;
import agents.nsm.QEpisodicMemory;
import framework.Action;
import framework.Episode;

public class pQEpisodicMemory extends QEpisodicMemory {

    public static final boolean PARTIAL_MATCHING = true;

    @Override
    public NHood buildNeighborhoodForMove(Action action)
    {
        NHood nHood = new NHood(action);
        //find the kNN
        for(int i = 0; i <= this.currentIndex(); ++i) {
            double matchLen = this.pMatchedMemoryStringLength(action, i);
            if ((matchLen > 0) && ((nHood.shortest <= matchLen) || (nHood.nbors.size() < nHood.K_NEAREST))) {
                nHood.addNBor(new NBor(i, (int) Math.round(matchLen), this.episodicMemory.get(i)));
            }
        }//for
        return nHood;
    }

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
    public double pMatchedMemoryStringLength(Action nHoodAction, int endOfStringIndex) {
        if (!nHoodAction.equals(this.episodicMemory.get(endOfStringIndex).getAction()))
            return 0;
        double length = 0;
        endOfStringIndex--;
        int indexOfMatchingAction = this.currentIndex();
        for (int i = endOfStringIndex; i >= 0; i--) {
            //We want to compare the command from the prev episode and the
            //sensors from the "right now" episode to the sequence at the
            //index indicated by 'i'
            if (!PARTIAL_MATCHING) {
                if (this.episodicMemory.get(indexOfMatchingAction).equals(this.episodicMemory.get(i))) {
                    length++;
                    indexOfMatchingAction--;
                } else {
                    return length;
                }
            } else {
                double score = partialEquals(this.episodicMemory.get(indexOfMatchingAction), this.episodicMemory.get(i));
                if (score > 0.0d) {
                    length += score;
                    indexOfMatchingAction--;
                }
                else {
                    return length;
                }
            }
        }//for

        return length;
    }//matchedMemoryStringLength

    /**
     * partialEquals
     * <p>
     * Returns a value from 0 to 1.0 of how much this episode matches the given episode. If the actions don't match,
     * it's a zero.
     * @param e the other episode being compared
     * @return
     */
    public double partialEquals(Episode q, Episode e) {

        double score = 0.0d;

        if (q.getSensorData() == null || e.getSensorData() == null) {
            return score;
        }

        if (!q.getAction().equals(e.getAction())) {
            return score;
        }

        int numMatches = 0;

        for (String sensorName : q.getSensorData().getSensorNames()) {

            if (sensorName.equals("GOAL")) {
                continue;
            }

            if (q.getSensorData().getSensor(sensorName).equals(e.getSensorData().getSensor(sensorName))) {
                numMatches++;
            }
        }

        return ((double) numMatches  / ((double) q.getSensorData().size() - 1.0));
    }//partialEquals
}
