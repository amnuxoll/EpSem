package agents.RAgent;

import framework.*;
import utils.EpisodicMemory;

import java.util.ArrayList;

/**
 * RAgent Class
 *
 * an agent that makes random moves in an FSM to measure the most commonly occurring patterns
 * in the agent's episodic memory
 *
 * @author Patrick Maloney
 * @version 0.95
 */
public class RAgent implements IAgent {

    private Move[] moves;
    private int movesMade;
    private int numMovesToMake;
    protected EpisodicMemory<Episode> episodicMemory = new EpisodicMemory<>();
    private int numGoalsFound = 0;
    ArrayList<MemoryPattern> patternsSeen;
    //private SensorData previousSensors;

    public RAgent(int numMoves){
        //this.previousSensors = null;
        this.movesMade = 0;
        this.numMovesToMake = numMoves;
    }

    @Override
    public void initialize(Move[] alphabet, IIntrospector introspector){
        this.patternsSeen = new ArrayList<>();
        this.moves = alphabet;
    }

    @Override
    public Move getNextMove(SensorData sensorData){
        movesMade++;
        Move nextMove = moves[(int)(Math.random() * moves.length)];

        if (episodicMemory.any()) {
            episodicMemory.current().setSensorData(sensorData);


            if(episodicMemory.length() > 1){ //if we have enough memories to start making patterns
                MemoryPattern currentPattern = new MemoryPattern(
                        episodicMemory.get(episodicMemory.length() -2).getSensorData(), //the previous episode's sensorData
                        episodicMemory.get(episodicMemory.length() -1).getMove(), //the previous episodes move
                        sensorData); //the sensordata that we saw as a result of that move

                if(!containsPattern(patternsSeen, currentPattern)){ //if we haven't seen this pattern yet, add it to the list
                    this.patternsSeen.add(currentPattern);
                } else {
                    patternsSeen.get(getIndexOf(currentPattern)).timesSeen++;
                }
            }
        }

        if(movesMade %1000 == 0) {
            System.out.println("made "+movesMade+" moves.");
        }
        if(sensorData != null && sensorData.isGoal()) {
            numGoalsFound++;
            //System.out.println("I found a goal!");
        }
        episodicMemory.add(new Episode(nextMove));
        return nextMove;
    }

    private boolean containsPattern(ArrayList<MemoryPattern> patternsSeen, MemoryPattern pattern){
        for(MemoryPattern m : patternsSeen){
            if(m.equals(pattern)) return true;
        }
        return false;
    }

    private int getIndexOf(MemoryPattern currentPattern){
        for(int i = 0; i < patternsSeen.size(); i++){
            if(patternsSeen.get(i).equals(currentPattern))return i;
        }
        return -1;
    }

    @Override
    public String[] getStatisticTypes() {
        return new String[] {
                "patternFrequency"
        };
    }

    @Override
    public ArrayList<Datum> getAgentFinishedData(){

        ArrayList<Datum> data = new ArrayList<>();
        for(MemoryPattern mp : patternsSeen){
            data.add(new Datum("patternFrequency", mp.toString()+" : "+mp.timesSeen));
        }
        return data;
    }

    @Override
    public void onTestRunComplete() {
        System.out.println("iteration complete");
    }
}
