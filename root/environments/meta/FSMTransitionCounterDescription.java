package environments.meta;

import environments.fsm.FSMDescription;
import framework.Move;
import framework.SensorData;

import java.util.EnumSet;
import java.util.HashMap;

public class FSMTransitionCounterDescription extends FSMDescription {

    private HashMap<Move, Integer>[] sensorTable;

    /**
     * Create an instance of a {@link FSMDescription}.
     *
     * @param transitionTable The transition table that indicates the structure of a FSM.
     */
    public FSMTransitionCounterDescription(HashMap<Move, Integer>[] transitionTable, HashMap<Move, Integer>[] sensorTable) {
        super(transitionTable);
        this.sensorTable = sensorTable;
    }

    public FSMTransitionCounterDescription(HashMap<Move, Integer>[] transitionTable,
                                           EnumSet<Sensor> sensorsToInclude, HashMap<Move, Integer>[] sensorTable) {
        super(transitionTable, sensorsToInclude);
        this.sensorTable = sensorTable;
    }

    public FSMTransitionCounterDescription(HashMap<Move, Integer>[] transitionTable, EnumSet<Sensor> sensorsToInclude) {
        super(transitionTable, sensorsToInclude);
        makeNewSensorTable();
    }

    public FSMTransitionCounterDescription(HashMap<Move, Integer>[] transitionTable) {
        super(transitionTable);
        makeNewSensorTable();
    }

    @Override
    public int transition(int currentState, Move move) {

        sensorTable[currentState].put(move, sensorTable[currentState].get(move)+1);

        return super.transition(currentState, move);
    }

    @Override
    public void applySensors(int lastState, Move move, int currentState, SensorData sensorData) {
        super.applySensors(lastState, move, currentState, sensorData);
        sensorData.setSensor("Transition age", sensorTable[lastState].get(move) <= 5);
    }

    private void makeNewSensorTable(){
        this.sensorTable = new HashMap[this.getNumStates()];
        for(int i = 0; i < getNumStates(); i++) {
            sensorTable[i] = new HashMap<Move, Integer>(getMoves().length);
            for(Move m : getMoves()) sensorTable[i].put(m, 0);
        }
    }

    public HashMap<Move, Integer>[] getSensorTable() {
        return sensorTable;
    }

}
