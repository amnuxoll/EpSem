package agents.juno;

import agents.marz.MaRzAgent;
import framework.*;
import agents.juno.WeightTable.ScoredIndex;
import utils.EpisodeUtils;

import java.util.ArrayList;
import java.util.Arrays;

public class JunoAgent extends MaRzAgent {
    //region Class Variables
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
    //endregion

    //region Constructors
    /**
     * JunoAgent
     *
     */
    public JunoAgent(JunoConfiguration config) {
        this.config= config;
        marzCount= 0;
        junoCount= 0;
        this.weightTable= new WeightTable(1);
    }
    //endregion

    //region MaRzAgent Overrides
    @Override
    public String[] getStatisticTypes()
    {
        String[] items = super.getStatisticTypes();
        ArrayList<String> itemsList = new ArrayList<>(Arrays.asList(items));
        itemsList.add("junoRatios");
        return itemsList.toArray(new String[0]);
    }

    @Override
    public ArrayList<Datum> getGoalData()
    {
        ArrayList<Datum> data = super.getGoalData();
        data.add(new Datum("junoRatios", this.getJunoRatio()));
        return data;
    }

    @Override
    protected Sequence selectNextSequence(){
        Sequence marzSuggestion= super.selectNextSequence();

        //if we haven't hit a goal yet || if we haven't done enough marz goals || if we haven't done a 'window size'
        // of moves yet, go with marz
        if(this.lastGoalIndex <= 0 || goals < marzGoals || lastGoalIndex >= episodicMemory.length() - weightTable.size()){
            marzCount++;
            this.tableSizeOnMatch= -1;
            lastMatch= null;
            return marzSuggestion;
        }

        ScoredIndex[] bestIndices= weightTable.bestIndices(episodicMemory, NUM_MATCHES, super.lastGoalIndex);
        if(bestIndices.length == 0) {
            marzCount++;
            return marzSuggestion;
        }
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
            this.lastSequenceIndex= episodicMemory.currentIndex();
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

    @Override
    protected void markSuccess(){
        goals++;
        super.markSuccess();
        //we pass in the index at which we started the sequence that brought us to the goal *here* vvv
        weightTable.updateOnGoal(episodicMemory, episodicMemory.currentIndex() - currentSequence.getCurrentIndex());

        //collect data to determine how mature our wight table is
        double averageEntry= weightTable.averageEntry();
        double average= ((goals-1)*prevAverage + averageEntry)/goals;
        this.tableMaturity= Math.abs(average - prevAverage);

        NamedOutput.getInstance().writeLine("tableInfo", weightTable.toString(false));
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
                        episodicMemory.currentIndex(),
                        lastMatch.index + currentSequence.getCurrentIndex()+1);

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

    @Override
    public void onGoalFound()
    {
        super.onGoalFound();
    }

    @Override
    public void onTestRunComplete()
    {
        super.onTestRunComplete();
    }
    //endregion

    //region Private Methods
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

    private void printInfo(ScoredIndex indexToTry){
        NamedOutput namedOutput = NamedOutput.getInstance();
        namedOutput.writeLine("tableInfo", episodesToString(indexToTry.index, weightTable.size()));
        namedOutput.writeLine("tableInfo", episodesToString(episodicMemory.currentIndex(), weightTable.size()));
        namedOutput.writeLine("tableInfo", weightTable.toString(false));
    }

    /**
     * print episodes ending at index, print length number of them
     * @param index the index of the first episode to convert to string
     * @param count the number of episdoes to include
     */
    private String episodesToString(int index, int count){
        String str= "";
        int i = Math.max(index-count+1, 0);
        for (; i<= index; i++) {
            str += this.episodicMemory.get(i);
        }
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
        while(!episodicMemory.get(index).hitGoal()){
            if(index == episodicMemory.currentIndex()){
                return null;
            }
            index++;
        }
        Move[] moves = EpisodeUtils.selectMoves(episodicMemory.subset(startIndex + 1, index + 1));
        return new Sequence(moves);
    }

    /**
     *
     * @return the ratio of juno desicions to all decisions
     */
    private double getJunoRatio(){
        double total = marzCount + junoCount;

        if(total == 0){
            return 0;
        }
        return (double)junoCount/total;
    }
    //endregion

    //region Object Overrides
    @Override
    public String toString(){
        return "bailSlider= " + config.getBailSlider();
    }
    //endregion
}
