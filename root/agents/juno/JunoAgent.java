package agents.juno;

import agents.marz.ISuffixNodeBaseProvider;
import agents.marz.MaRzAgent;
import framework.Episode;
import framework.OutputStreamContainer;
import framework.Services;
import utils.Sequence;
import agents.juno.WeightTable.ScoredIndex;


import java.io.*;
import java.util.*;

public class JunoAgent extends MaRzAgent {
    private WeightTable weightTable= null;

    //the window of episodes we matched before trying our sequence
    private WeightTable.ScoredIndex lastMatch= null;
    //the index of the current episode just before trying our sequence
    private int lastSequenceIndex= -1;

    //how many episodes we match at a time
    private int NUM_MATCHES= 5;
    private int goals= 0;
    private int marzGoals= 3;

    private JunoConfiguration config;

    private int marzCount= 0;
    private int junoCount= 0;

    //average of all averages seen so far from the weight table
    private double prevAverage= 0;
    //how mature we think our weight table is
    private double tableMaturity= Double.MAX_VALUE;
    private int tableSizeOnMatch= -1;

    /**
     * JunoAgent
     *
     * @param nodeProvider
     */
    public JunoAgent(ISuffixNodeBaseProvider nodeProvider, JunoConfiguration config) {
        super(nodeProvider);
        this.config= config;
        marzCount= 0;
        junoCount= 0;
        this.weightTable= new WeightTable(1);
    }


    @Override
    protected Sequence selectNextSequence(){
        Sequence marzSuggestion= super.selectNextSequence();

        //if we haven't hit a goal yet || if we haven't done enough marz goals || if we haven't done a 'window size'
        // of moves yet, go with marz
        if(this.lastGoalIndex <= 0 || goals < marzGoals || lastGoalIndex >= episodicMemory.size()-weightTable.size()){
            marzCount++;
            this.tableSizeOnMatch= -1;
            lastMatch= null;
            return marzSuggestion;
        }

        ScoredIndex[] bestIndices= weightTable.bestIndices(episodicMemory, NUM_MATCHES, super.lastGoalIndex);
        //best matching index is first in array
        ScoredIndex bestIndexToTry= bestIndexToTry(bestIndices);

        Sequence junoSuggestion= sequenceToGoal(bestIndexToTry.index);

        //we will go with the most confident of the two suggested sequences
        double L_WEIGHT= .05;

        double junoScore= bestIndexToTry.score + junoSuggestion.getLength() * L_WEIGHT;
        double marzScore= super.getActiveNode().getNormalizedWeight() + marzSuggestion.getLength() * L_WEIGHT;
        if(junoScore > marzScore){
            //if were going with juno's suggestion, we gotta tell marz

            super.setActiveNode(super.suffixTree.findBestMatch(junoSuggestion));
            this.lastSequenceIndex= episodicMemory.size()-1;
            this.lastMatch= bestIndexToTry;
            this.tableSizeOnMatch= weightTable.size();

            junoCount++;
            return junoSuggestion;
        }

        lastMatch= null;
        this.tableSizeOnMatch= -1;
        marzCount++;
        return marzSuggestion;
    }

    private ScoredIndex bestIndexToTry(ScoredIndex[] indexes){
        if(indexes == null){
            throw new IllegalArgumentException("Indexes cannot be null");
        }
        if(indexes.length < 1){
            throw new IllegalArgumentException("Indexes should not be empty");
        }

        ScoredIndex best= null;
        double maxScore= -1;

        for(ScoredIndex index : indexes){
            double score= index.score/sequenceToGoal(index.index).getLength();

            if(score > maxScore){
                maxScore= score;
                best= index;
            }
        }

        return best;
    }


    @Override
    protected void markSuccess(){
        goals++;
        super.markSuccess();
        //we pass in the index at which we started the sequence that brought us to the goal *here* vvv
        weightTable.updateOnGoal(episodicMemory, episodicMemory.size()-currentSequence.getCurrentIndex()-1);

        //collect data to determine how mature our wight table is
        double averageEntry= weightTable.averageEntry();
        double average= ((goals-1)*prevAverage + averageEntry)/goals;
        this.tableMaturity= Math.abs(average - prevAverage);

        Services.retrieve(OutputStreamContainer.class).write("tableInfo", weightTable.toString(false));
    }

    @Override
    protected void markFailure(){
        super.markFailure();

        if(lastMatch != null){
            weightTable.updateOnFailure(episodicMemory, lastMatch.index, lastSequenceIndex);
        }

        if(super.getActiveNode() != null){
            weightTable.setSize(super.getActiveNode().getSuffix().getLength() + 1);
        }
    }

    /**
     * indicates whether we should bail early
     * we will do this if we beleive we were wrong
     * about the position we thought we were in when we
     * selected the current sequence
     *
     * @return whether to give up on the current sequence early
     */
    @Override
    protected boolean shouldBail(){
        //if we didn't choose the current sequence,
        //don't interrupt it
        //if we are told to never bail, don't
        if(lastMatch == null || !config.getCanBail()){
            return false;
        }

        //if we dont have a mature weight table, don't bail
        if(tableMaturity > config.getMaturity()){
            return false;
        }

        //if we've taken enough steps in this sequence to start doing comparisons with
        //what we expect
        if(currentSequence.getCurrentIndex() + 1 >= weightTable.size()) {
            try {
                double matchScore = weightTable.calculateMatchScore(episodicMemory,
                        episodicMemory.size() - 1,
                        lastMatch.index + currentSequence.getCurrentIndex());

                //if our match is less than our confidence
                if (matchScore < config.getBailSlider() * lastMatch.score) {
                    return true;
                }
            } catch (WindowContainsGoalException wcg) {
                int x = 4;
            }
        }

        //we haven't had enough time to make a legit comparison
        return false;
    }

    private void printInfo(ScoredIndex indexToTry){
        OutputStreamContainer outputStreamContainer=
                Services.retrieve(OutputStreamContainer.class);

        outputStreamContainer.write("tableInfo", episodesToString(indexToTry.index, weightTable.size()));
        outputStreamContainer.write("tableInfo", episodesToString(episodicMemory.size()-1, weightTable.size()));
        outputStreamContainer.write("tableInfo", weightTable.toString(false));
    }

    /**
     * print episodes ending at index, print count number of them
     * @param index the index of the first episode to convert to string
     * @param count the number of episdoes to include
     */
    private String episodesToString(int index, int count){
        String str= "";

        int i= Math.max(index-count+1, 0);
        for(; i<= index; i++){
            str+= episodicMemory.get(i);
        }
        str+= "\n";

        return str;
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

    /**
     *
     * @return the ratio of juno desicions to all decisions
     */
    public double getJunoRatio(){
        double total = marzCount + junoCount;

        if(total == 0){
            return 0;
        }
        return (double)junoCount/total;
    }

    @Override
    public String getMetaData(){
        return "bailSlider= " + config.getBailSlider();
    }
}
