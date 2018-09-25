package agents.juno;

import framework.Episode;
import framework.EpisodeWeights;
import framework.Move;
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
        numMatches= Math.min(bestIndexes.size(),numMatches);

        int[] indexArray= new int[numMatches];
        for(int i=0;i<indexArray.length;i++){
            int idx = bestIndexes.poll().index;
            if(episodes.get(idx).getSensorData().isGoal()) {
                i--;
                continue;
            }
            indexArray[i]= idx;
        }

        return indexArray;
    }

    /**
     * compares goalSequence and pre-goalSequences and updates the table based on matching values and/or mismatching values
     * @param episodes the entire episodic memory
     * @param goalSequenceIndex the index at which we reached the goal
     */
    public void updateTable(ArrayList<Episode> episodes, int goalSequenceIndex){
        int previousGoalIndex = lastGoalIndex(episodes, episodes.size()-2);

        if(previousGoalIndex<0) return; //return if this is the first time we're hitting a goal

        Sequence goalSequence = new Sequence(episodes, goalSequenceIndex, episodes.size());
        if(previousGoalIndex >= goalSequenceIndex-table.size()){
            return; //don't do anything if the last goalSequence was too short to matter
        }

        int nextGoalIndex= lastGoalIndex(episodes, episodes.size()-2);
        //nextGoalIndex is the index of the goal after the current window
        int startIndex = nextGoalIndex-1;

        previousGoalIndex = lastGoalIndex(episodes, startIndex);

        for(int i = startIndex; i>table.size(); i--) {

            if(i-table.size() <= previousGoalIndex) {
                i = previousGoalIndex -1;
                nextGoalIndex = previousGoalIndex;
                previousGoalIndex = lastGoalIndex(episodes, i);
            }

            Sequence goalSequence2= new Sequence(episodes, i+1,nextGoalIndex+1);
            double attemptSimilarity = getAttemptSimlarity(goalSequence, goalSequence2);
            double actualSimilarity = getActualSimilarity(goalSequence, goalSequence2);

            double adjustValue = attemptSimilarity*actualSimilarity;

            for(int j = 0; j<table.size(); j++) {
                table.get(j).updateWeights(episodes.get(goalSequenceIndex-j-1), episodes.get(i-j), adjustValue);
            }
        }


    }

    /**
     * gets the similarity of the 'attempts' after the pre-goal sequence and the window
     * @param goalSequence1
     * @param goalSequence2
     * @return
     */
    private double getAttemptSimlarity(Sequence goalSequence1, Sequence goalSequence2){
        int m= Math.min(goalSequence1.getLength(),goalSequence2.getLength());
        int i = 0;
        Move[] gs1Moves = goalSequence1.getMoves();
        Move[] gs2Moves = goalSequence2.getMoves();

        for(; i<m; i++) {
            if(gs1Moves[i] != gs2Moves[i]) break;
        }

        return (double) i/m;
    }

    /**
     * gets the similarity between what we've done after the pre-goal sequence and the window
     * @param goalSequence1
     * @param goalSequence2
     * @return
     */
    private double getActualSimilarity(Sequence goalSequence1, Sequence goalSequence2) {
        int m= Math.min(goalSequence1.getLength(),goalSequence2.getLength());
        int l= Math.max(goalSequence1.getLength(),goalSequence2.getLength());

        int difference = m-l;

        if(difference ==0) return 1;

        return (2.0*m)/l - 1;
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
