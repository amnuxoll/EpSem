package agents.juno;

import agents.marz.ISuffixNodeBaseProvider;
import agents.marz.MaRzAgent;
import framework.Episode;
import utils.Sequence;


import java.util.*;

public class JunoAgent extends MaRzAgent {
    private WeightTable weightTable= null;

    //the window of episodes we matched before trying our sequence
    private WeightTable.ScoredIndex lastMatch= null;
    //the index of the current episode just before trying our sequence
    private int lastSequenceIndex= -1;

    //how many episodes we match at a time
    private int WINDOW_SIZE= 5;
    private int NUM_MATCHES= 5;
    private int goals= 0;
    private int marzGoals= 5;

    public static int marzCount= 0;
    public static int junoCount= 0;

    /**
     * JunoAgent
     *
     * @param nodeProvider
     */
    public JunoAgent(ISuffixNodeBaseProvider nodeProvider) {
        super(nodeProvider);
        marzCount= 0;
        junoCount= 0;
        this.weightTable= new WeightTable(WINDOW_SIZE);
    }


    @Override
    protected Sequence selectNextSequence(){
        Sequence marzSuggestion= super.selectNextSequence();

        if(this.lastGoalIndex <= 0 || goals < marzGoals){
            marzCount++;
            return marzSuggestion;
        }

        WeightTable.ScoredIndex[] bestIndices= weightTable.bestIndices(episodicMemory,NUM_MATCHES);
        //best matching index is first in array
        WeightTable.ScoredIndex bestMatch= bestIndices[0];

        Sequence junoSuggestion= sequenceToGoal(bestMatch.index);

        //we will go with the most confident of the two suggested sequences
        if(bestMatch.score > super.getActiveNode().getNormalizedWeight()){
            //if were going with juno's suggestion, we gotta tell marz
            super.setActiveNode(junoSuggestion);
            this.lastSequenceIndex= episodicMemory.size()-1;
            this.lastMatch= bestMatch;

            junoCount++;
            return junoSuggestion;
        }

        lastMatch= null;
        marzCount++;
        return marzSuggestion;
    }


    @Override
    protected void markSuccess(){
        goals++;
        super.markSuccess();
        weightTable.updateOnGoal(episodicMemory, episodicMemory.size()-currentSequence.getCurrentIndex()-1);
    }

    @Override
    protected void markFailure(){
        super.markFailure();

        if(lastMatch != null){
            weightTable.updateOnFailure(episodicMemory, lastMatch.index, lastSequenceIndex);
        }
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
        while(!((Episode)episodicMemory.get(index)).getSensorData().isGoal()){
            if(index == episodicMemory.size()-1){
                return null;
            }

            index++;
        }

        return new Sequence(episodicMemory,startIndex,index+1);
    }
}
