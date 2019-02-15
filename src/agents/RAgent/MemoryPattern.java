package agents.RAgent;

import framework.Move;
import framework.SensorData;

/**
 * MemoryPattern Class
 *
 * stores a SensorData object, a move, and the resulting SensorData
 *
 * @author Patrick Maloney
 * @version 0.01
 */

public class MemoryPattern {
    private SensorData currentSensors;
    private Move action;
    private SensorData resultSensors;
    int timesSeen;

    public MemoryPattern(SensorData current, Move action, SensorData result){
        this.currentSensors = current;
        this.action = action;
        this.resultSensors = result;
        this.timesSeen=1;
    }

    public boolean equals(MemoryPattern toCheck){
        if(toCheck == this) return true;
        if(!this.currentSensors.equals(toCheck.currentSensors))
            return false;
        if(!this.action.equals(toCheck.action))
            return false;
        return this.resultSensors.equals(toCheck.resultSensors);
    }

    public String toString(){
        return currentSensors.toString(true)+action.toString()+resultSensors.toString(true);
    }
}
