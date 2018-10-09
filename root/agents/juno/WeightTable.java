package agents.juno;

import framework.Episode;
import framework.EpisodeWeights;
import framework.Move;
import utils.Sequence;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.PriorityQueue;
import java.util.Queue;

public class WeightTable {
    //table of episode weights
    //indexed where 0 is most recent episode
    protected ArrayList<EpisodeWeights> table;

    /**
     * makes a weight table with 'windowSize' rows
     * @param windowSize number of rows to store weights for
     */
    public WeightTable(int windowSize){
        if(windowSize < 1){
            throw new IllegalArgumentException("Window size must be positive");
        }
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
     * @param numMatches the number of best matches to return
     * @return the indexes of the best (at most) 'numMatches' matches to the most recent window of episodes,
     *          which weren't goals
     */
    public ScoredIndex[] bestIndices(ArrayList<Episode> episodes, int numMatches){
        if(episodes == null){
            throw new IllegalArgumentException("episodes cannot be null");
        }
        if(numMatches < 0){
            throw new IllegalArgumentException("numMatches must be non-negative");
        }

        int lastGoalIndex= lastGoalIndex(episodes, episodes.size()-1);

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

        ScoredIndex[] indexArray= new ScoredIndex[numMatches];
        int i= 0; //i is index of last attempted add to the array
        for(;i<indexArray.length;i++){
            if(bestIndexes.peek() == null){
                break;
            }
            ScoredIndex scoredIndex= bestIndexes.poll();
            int idx = scoredIndex.index;
            if(episodes.get(idx).getSensorData().isGoal()) {
                i--;
                continue;
            }
            indexArray[i]= scoredIndex;
        }

        return Arrays.copyOf(indexArray,i);
    }

    /**
     * updates the weight table after an attempted sequence that didn't hit the goal
     * takes as parameters the indexes of the episode sequences that matched
     * when we determined what sequence to try
     *
     * @param episodes the episodic memory
     * @param index1 the index of the first window in the match
     * @param index2 the index of the second window in the match
     */
    public void updateOnFailure(ArrayList<Episode> episodes, int index1, int index2){
        if(index1 < table.size()-1 || index2 < table.size()-1){
            throw new IllegalArgumentException("Index is invalid. You are probably not passing correct indicies");
        }

        for(int i=0; i<table.size();i++){
            //pass *negative* .5 because we want to reward mismatches
            table.get(i).updateWeights(episodes.get(index1-i), episodes.get(index2-i), -.5);
        }
    }

    /**
     * compares goalSequence and pre-goalSequences and updates the table based on matching values and/or mismatching values
     * @param episodes the entire episodic memory
     * @param goalSequenceIndex the index at which we reached the goal
     */
    public void updateOnGoal(ArrayList<Episode> episodes, int goalSequenceIndex){
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

        for(int i = startIndex; i>=table.size(); i--) {

            if(i-table.size() <= previousGoalIndex) {
                i = previousGoalIndex -1;
                nextGoalIndex = previousGoalIndex;
                previousGoalIndex = lastGoalIndex(episodes, i);

                if(i < table.size()-1){
                    return;
                }
            }

            Sequence goalSequence2= new Sequence(episodes, i+1,nextGoalIndex+1);
            double attemptSimilarity = getAttemptSimlarity(goalSequence, goalSequence2);
            double actualSimilarity = getActualSimilarity(goalSequence, goalSequence2);

            double adjustValue = attemptSimilarity*actualSimilarity;

            for(int j = 0; j<table.size(); j++) {
                Episode ep1= episodes.get(goalSequenceIndex-j-1);
                Episode ep2= episodes.get(i-j);

                table.get(j).updateWeights(ep1, ep2, adjustValue);
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

    /**
     * finds the index of the goal that happened most previously
     * to the given index
     *
     * @param list the episodes to search in
     * @param index the index to start the search
     * @return the index of the most recent goal which occurred
     *          at or before the given index
     */
    private int lastGoalIndex(ArrayList<Episode> list, int index) {
        for(int i = index; i>=0; i--) {
            if(list.get(i).getSensorData().isGoal()) return i; //if the episode at i was a goal, return i
        }
        return -1; //return -1 if we don't find any goals in the list
    }

    public class ScoredIndex implements Comparable<ScoredIndex>{
        public int index;
        public double score;

        public ScoredIndex(int index, double score) {
            this.index = index;
            this.score = score;
        }

        /**
         * compares one scored index to another
         * since priority queue prioritizes the least,
         * this comparable works unconventionally
         *
         * @param si the scoredindex to compare to
         * @return -1 if this score is greater than si,
         *          0 if the scores are equal
         *          1 if this score is less than si
         */
        @Override
        public int compareTo(ScoredIndex si) {
            if(this.score == si.score) return 0;

            return this.score < si.score ? 1 : -1;
        }
    }
}