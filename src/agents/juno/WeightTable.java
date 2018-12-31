package agents.juno;

import framework.*;
import framework.Sequence;
import utils.EpisodeUtils;
import utils.EpisodicMemory;

import java.util.ArrayList;
import java.util.PriorityQueue;

public class WeightTable {
    //region Class Variables
    //table of episode weights
    //indexed where 0 is most recent episode
    protected ArrayList<EpisodeWeights> table;
    //endregion

    //region Constructors
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
    //endregion

    //region Public Methods
    /**
     * finds the indexes of the windows that best match the most
     * recent window
     *
     * @param episodes the list of episodes to look for matches in
     * @param numMatches the number of best matches to return
     * @param lastGoalIndex the index of the most recent goal -- where to start comparisons
     *
     * @return the indexes of the best (at most) 'numMatches' matches to the most recent window of episodes,
     *          which weren't goals
     */
    public ScoredIndex[] bestIndices(EpisodicMemory<Episode> episodes, int numMatches, int lastGoalIndex){
        if (episodes == null) {
            throw new IllegalArgumentException("episodes cannot be null");
        }
        if (numMatches < 0) {
            throw new IllegalArgumentException("numMatches must be non-negative");
        }
        if (lastGoalIndex >= episodes.length() - table.size()) {
            throw new IllegalArgumentException("There has to be a window size of episodes since current goal");
        }

        PriorityQueue<ScoredIndex> bestIndexes= new PriorityQueue<>(numMatches);

        //shift which sub-sequence we are looking at
        //down the episodic memory,
        //curr index is the index of the most recent episode in the sub-sequence
        for(int currIndex= lastGoalIndex-1; currIndex >= table.size()-1; currIndex-- ){
            try{
                double sequenceScore= calculateMatchScore(episodes, episodes.currentIndex(), currIndex);

                //add this score to priority queue
                //noramlize score to be in range [0,1]
                bestIndexes.add(new ScoredIndex(currIndex,sequenceScore));
            }
            catch(WindowContainsGoalException containsGoal){
                //set curr index to be the location of the goal we collided with
                //next step (bottom of loop) we currIndex--;
                currIndex= containsGoal.getGoalIndex();
            }
            currIndex= currIndex;
        }

        //maybe there werent 'numMatches' sequeunces to check, so we have less match
        numMatches= Math.min(bestIndexes.size(),numMatches);

        ScoredIndex[] indexArray= new ScoredIndex[numMatches];
        int i= 0; //i is index of current attempted add to the array
        for(;i<indexArray.length;i++){
            ScoredIndex scoredIndex= bestIndexes.poll();
            indexArray[i]= scoredIndex;
        }

        return indexArray;
    }

    /**
     * compares windows of episodes using the weight table
     * calculates a normalized match score of these two windows
     * in the range [0,1]
     *
     * @param episodes list of episodes to look in
     * @param index1 the index (of most recent episode) of the first window to compare
     * @param index2 the index of the second window to compare
     * @return a normalized match score between these two windows
     */
    public double calculateMatchScore(EpisodicMemory<Episode> episodes, int index1, int index2){
        if (episodes == null) {
            throw new IllegalArgumentException("episodes cannot be null");
        }
        if (index1 < table.size() - 1 || index2 < table.size() - 1) {
            throw new IllegalArgumentException("indexes must be at least the window size -1");
        }
        if (index1 >= episodes.length() || index2 >= episodes.length()) {
            throw new IllegalArgumentException("indexes must be at less than episodes size");
        }

        double sequenceScore= 0;
        //compare each episode in the subsequence
        //to the most recent memories
        for (int i = 0; i < table.size(); i++) {
            //compare the memories we just had
            //with corresponding episode relative to currIndex
            Episode ep1 = episodes.get(index1 - i);
            Episode ep2 = episodes.get(index2 - i);

            //check if either of these episodes are goals
            if(ep1.hitGoal()){
                throw new WindowContainsGoalException(index1-i);
            }
            if(ep2.hitGoal()){
                throw new WindowContainsGoalException(index2-i);
            }

            //add this episode's match score to the sequence's match score
            sequenceScore += table.get(i).matchScore(ep1, ep2);
        }

        return sequenceScore/table.size();
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
    public void updateOnFailure(EpisodicMemory<Episode> episodes, int index1, int index2){
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
     * @param goalSequenceIndex the index at which we started the sequence that brought us to the goal
     */
    public void updateOnGoal(EpisodicMemory<Episode> episodes, int goalSequenceIndex){
        int preGoalSequenceIndex= goalSequenceIndex - 1;

        int previousGoalIndex = episodes.lastGoalIndex(episodes.currentIndex()-1);

        //do nothing if our pregoal sequence contains this goal
        if(previousGoalIndex > preGoalSequenceIndex - table.size()){
            return;
        }
        //also, if there is no previos goal, do nothing
        if(previousGoalIndex < 0) return;

        Move[] moves = EpisodeUtils.selectMoves(episodes.subset(goalSequenceIndex));
        Sequence goalSequence = new Sequence(moves);

        int nextGoalIndex= previousGoalIndex;
        //nextGoalIndex is the index of the goal after the current window
        int startIndex = nextGoalIndex-1;

        previousGoalIndex = episodes.lastGoalIndex(startIndex);

        //i is index of sequence we're comparing
        for(int i = startIndex; i>=table.size()-1; i--) {

            if(i-table.size() <= previousGoalIndex) {
                i = previousGoalIndex -1;
                nextGoalIndex = previousGoalIndex;
                previousGoalIndex = episodes.lastGoalIndex(i);

                if(i < table.size()-1){
                    return;
                }
            }
            Move[] moves2 = EpisodeUtils.selectMoves(episodes.subset(i + 1,nextGoalIndex + 1));
            Sequence goalSequence2 = new Sequence(moves2);
            double attemptSimilarity = getAttemptSimlarity(goalSequence, goalSequence2);
            double actualSimilarity = getActualSimilarity(goalSequence, goalSequence2);
            double adjustValue = attemptSimilarity*actualSimilarity;

            for(int j = 0; j<table.size(); j++) {
                Episode ep1= episodes.get(preGoalSequenceIndex-j);
                Episode ep2= episodes.get(i-j);

                table.get(j).updateWeights(ep1, ep2, adjustValue);
            }
        }
    }

    public String toString(boolean includeNewlines){
        String str= "";
        for(EpisodeWeights weights : table){
            str+= weights + (includeNewlines ? "\n" : ",");
        }

        return str;
    }

    /**
     * tell this weight table to start tracking another episode
     */
    public void bumpTableSize(){
        table.add(new EpisodeWeights());
    }

    public void setSize(int newSize){
        this.setSize(newSize, false);
    }

    public void setSize(int newSize, boolean allowDecrease){
        if(allowDecrease && newSize < table.size()){
            int pullNumber= table.size()-newSize;
            for(int i=0; i < pullNumber; i++){
                table.remove(table.size()-1);
            }
        }
        else{
            while(table.size() < newSize){
                table.add(new EpisodeWeights());
            }
        }
    }

    public int size(){
        return table.size();
    }

    /**
     * computes the average entry in the table
     * @return the average entry
     */
    public double averageEntry(){
        double sum= 0;
        int numEntries= 0;
        for(EpisodeWeights weights : table){
            sum+= weights.averageEntry();
            numEntries+= weights.size();
        }

        if(numEntries == 0){
            return 0;
        }

        return sum/numEntries;
    }
    //endregion

    //region Private Methods
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

        return (2.0*m)/l - 1;
    }
    //endregion

    //region Object Overrides
    @Override
    public String toString(){
        return toString(true);
    }
    //endregion

    //region Nested Classes
    public class ScoredIndex implements Comparable<ScoredIndex> {
        //region Class Variables
        public int index;
        public double score;
        //endregion

        //region Constructors
        public ScoredIndex(int index, double score) {
            this.index = index;
            this.score = score;
        }
        //endregion

        //region Comparable<ScoredIndex> Members
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
        //endregion
    }
    //endregion
}
