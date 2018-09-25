package agents.juno;

import agents.marz.ISuffixNodeBaseProvider;
import agents.marz.MaRzAgent;
import agents.marz.nodes.SuffixNode;
import framework.Episode;
import framework.EpisodeWeights;
import framework.Move;
import framework.SensorData;
import utils.Sequence;


import java.util.ArrayList;

public class JunoAgent extends MaRzAgent {
    WeightTable weightTable= null;

    //how many episodes we match at a time
    private int WINDOW_SIZE= 5;
    private int NUM_MATCHES= 5;

    /**
     * JunoAgent
     *
     * @param nodeProvider
     */
    public JunoAgent(ISuffixNodeBaseProvider nodeProvider) {
        super(nodeProvider);

        this.weightTable= new WeightTable(WINDOW_SIZE);
    }


    @Override
    protected Sequence selectNextSequence(){
        Sequence marzSuggestion= super.selectNextSequence();

        if(this.lastGoalIndex <= 0){
            return marzSuggestion;
        }

        int[] bestIndices= weightTable.bestIndices(episodicMemory,lastGoalIndex,5);

        Sequence junoSuggestion= shortestSequenceToGoal(bestIndices);

        //we will go with the shorter of the two suggested sequences
        if(junoSuggestion.getLength() < marzSuggestion.getLength()){
            //if were going with juno's suggestion, we gotta tell marz
            super.setActiveNode(junoSuggestion);
            return junoSuggestion;
        }

        return marzSuggestion;
    }


    @Override
    protected void markSuccess(){
        weightTable.updateTable(episodicMemory, currentSequence.getCurrentIndex());
    }



    /**
     * finds the shortest sequence to goal given an array of starting indexes
     *
     * @param startIndexes places to start looking
     * @return the shortest sequence to goal
     *          or null if no goals occurred after any start index
     */
    private Sequence shortestSequenceToGoal(int[] startIndexes){
        Sequence minSequence= null;

        for(int i=0;i<startIndexes.length;i++){
            Sequence currSequence= sequenceToGoal(startIndexes[i]);

            if(currSequence != null &&
                    (minSequence == null || currSequence.getLength() < minSequence.getLength()))
            {
                minSequence= currSequence;
            }
        }

        return minSequence;
    }

    /**
     * creates a sequence of moves from the start index to the next goal
     *
     * @param startIndex index at which to start
     * @return the sequence of moves that brought the agent from the episode at
     *          start index to the goal,
     *          null if no goal as since been hit
     */
    private Sequence sequenceToGoal(int startIndex){
        int index= startIndex;
        while(((Episode)episodicMemory.get(index)).getSensorData().isGoal()){
            index++;

            if(index == episodicMemory.size()-1){
                return null;
            }
        }

        return new Sequence(episodicMemory,startIndex,index);
    }
}
