package agents.juno;

import framework.Episode;
import framework.EpisodeWeights;
import utils.Sequence;

import java.util.ArrayList;
import java.util.PriorityQueue;
import java.util.Queue;

public class WeightTable {
    //table of episode weights
    //indexed where 0 is most recent episode
    private ArrayList<EpisodeWeights> table;

    /**
     * makes a weight table with 'windowSize' rows
     * @param windowSize number of rows to store weights for
     */
    public WeightTable(int windowSize){
        this.table= new ArrayList<>(windowSize);
        for(int i=0; i<windowSize; i++){
            table.add(new EpisodeWeights());
        }
    }

    /**
     * finds the indexes of the windows that best match the most
     * recent window
     *
     * @param episodes the list of episodes to look for matches in
     * @param lastGoalIndex the index of the last goal, where to start the search
     * @param numMatches the number of best matches to return
     * @return the indexes of the best (at most) 'numMatches' matches to the most recent window of episodes
     */
    public int[] bestIndices(ArrayList<Episode> episodes, int lastGoalIndex, int numMatches){
        PriorityQueue<ScoredIndex> bestIndexes= new PriorityQueue<>(numMatches);

        //shift which sub-sequence we are looking at
        //down the episodic memory,
        //curr index is the index of the most recent episode in the sub-sequence
        for(int currIndex= lastGoalIndex-1; currIndex >= table.size()-1; currIndex-- ){
            double sequenceScore= 0;

            //compare each episode in the subsequence
            //to the most recent momories
            for(int i=0; i < table.size(); i++){
                //compare the memories we just had
                //with corresponding episode relative to currIndex
                Episode recentEpisode= episodes.get(episodes.size()-1-i);
                Episode windowEpisode= episodes.get(currIndex-i);

                //add this episode's match score to the sequence's match score
                sequenceScore+= table.get(i).matchScore(recentEpisode,windowEpisode);
            }

            //add this score to priority queue
            bestIndexes.add(new ScoredIndex(currIndex,sequenceScore));
        }

        //maybe there werent 'numMatches' sequeunces to check, so we have less match
        numMatches= Math.max(bestIndexes.size(),numMatches);

        int[] indexArray= new int[numMatches];
        for(int i=0;i<indexArray.length;i++){
            indexArray[i]= bestIndexes.poll().index;
        }

        return indexArray;
    }

    /**
     * compares goalSequence and pre-goalSequences and updates the table based on matching values and/or mismatching values
     * @param episodes the entire episodic memory
     * @param goalSequenceIndex the index at which we reached the goal
     */
    public void updateTable(ArrayList<Episode> episodes, int goalSequenceIndex){
        Sequence goalSequence = new Sequence(episodes, episodes.size()-goalSequenceIndex, episodes.size());
        if(lastGoalIndex(episodes, episodes.size()-1) > episodes.size() - (goalSequenceIndex+table.size())){
            return; //don't do anything if the last goalSequence was too short to matter
        }

        Sequence preGoalSequence =
                new Sequence(episodes, episodes.size() - (goalSequenceIndex+table.size()),
                        episodes.size()-goalSequenceIndex);
        
    }

    private int lastGoalIndex(ArrayList<Episode> list, int index) {
        for(int i = index; i>=0; i--) {
            if(list.get(i).getSensorData().isGoal()) return i; //if the episode at i was a goal, return i
        }
        return -1; //return -1 if we don't find any goals in the list
    }

    private class ScoredIndex implements Comparable<ScoredIndex>{
        public int index;
        public double score;

        public ScoredIndex(int index, double score) {
            this.index = index;
            this.score = score;
        }

        /**
         * compares one scored index to another
         * since priority queue prioritizes the least,
         * this comparable works conventionally
         *
         * @param si the scoredindex to compare to
         * @return 1 if this score is greater than si, -1 otherwise
         */
        @Override
        public int compareTo(ScoredIndex si) {
            if(this.score == si.score) return 0;

            return this.score > si.score ? 1 : -1;
        }
    }
}
