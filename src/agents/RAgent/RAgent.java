package agents.RAgent;

import framework.*;
import utils.EpisodicMemory;

import javax.naming.Name;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;

/**
 * RAgent Class
 *
 * an agent that makes random actions in an FSM to measure the most commonly occurring patterns
 * in the agent's episodic memory
 *
 * @author Patrick Maloney
 * @version 0.95
 */
public class RAgent implements IAgent {

    private Action[] actions;
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
    public void initialize(Action[] alphabet, IIntrospector introspector){
        this.patternsSeen = new ArrayList<>();
        this.actions = alphabet;
    }

    @Override
    public Action getNextAction(SensorData sensorData){
        movesMade++;
        Action nextAction = actions[(int)(Math.random() * actions.length)];

        if (episodicMemory.any()) {
            if(episodicMemory.length() > 1){ //if we have enough memories to start making patterns
                MemoryPattern currentPattern = new MemoryPattern(
                        episodicMemory.get(episodicMemory.length() -2).getSensorData(), //the previous episode's sensorData
                        episodicMemory.get(episodicMemory.length() -1).getAction(), //the previous episodes move
                        sensorData); //the sensordata that we saw as a result of that move

                if(!containsPattern(patternsSeen, currentPattern)){ //if we haven't seen this pattern yet, add it to the list
                    this.patternsSeen.add(currentPattern);
                } else {
                    patternsSeen.get(getIndexOf(currentPattern)).timesSeen++;
                }
            }
        }

        if(movesMade %1000 == 0) {
            System.out.println("made "+movesMade+" actions.");
        }
        if(sensorData != null && sensorData.isGoal()) {
            numGoalsFound++;
            //System.out.println("I found a goal!");
        }
        episodicMemory.add(new Episode(sensorData, nextAction));
        return nextAction;
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
        HashSet<String> statistics = new HashSet<>();
        statistics.addAll(Arrays.asList(IAgent.super.getStatisticTypes()));
        statistics.add("patternFrequency");
        return statistics.toArray(new String[0]);
    }

    @Override
    public void onTestRunComplete() {
        NamedOutput namedOutput = NamedOutput.getInstance();
        for(MemoryPattern mp : patternsSeen){
            namedOutput.writeLine("patternFrequency", mp.toString()+" : " + mp.timesSeen);
        }
        System.out.println("iteration complete");
    }
}
