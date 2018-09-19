package agents.juno;

import framework.Episode;
import framework.EpisodeWeights;

import java.util.ArrayList;
import java.util.PriorityQueue;

public class WeightTable {
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
     * @return the indexes of the best 'numMatches' matches to the most recent window of episodes
     */
    public int[] bestIndices(ArrayList<Episode> episodes, int lastGoalIndex, int numMatches){
        PriorityQueue<WeightedIndex> bestIndexes= new PriorityQueue<>(numMatches);

        //TODO do this method
        return null;

    }

    private class WeightedIndex implements Comparable<WeightedIndex>{
        public int index;
        public double weight;

        public WeightedIndex(int index, double weight) {
            this.index = index;
            this.weight = weight;
        }

        @Override
        public int compareTo(WeightedIndex wi) {
            if(this.weight == wi.weight) return 0;

            return this.weight > wi.weight ? 1 : -1;
        }
    }
}
