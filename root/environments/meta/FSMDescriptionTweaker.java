package environments.meta;

import environments.fsm.FSMDescription;
import environments.fsm.FSMTransitionTableBuilder;
import framework.*;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Set;

/**
 * Class FSMDescriptionTweaker gives new FSM environments and 'tweaks' them by randomly swapping two moves in the transition table
 *
 * @author Patrick Maloney
 * @author Harry Thoma
 * @version September 2018
 */
public class FSMDescriptionTweaker implements IEnvironmentDescriptionProvider {

    private HashMap<Move, Integer>[] table;
    private FSMTransitionCounterDescription lastDescription = null;

    private int alphaSize;;
    private int numStates;
    private EnumSet<FSMDescription.Sensor> sensorsToInclude;

    public FSMDescriptionTweaker(int alphaSize, int numStates, EnumSet<FSMDescription.Sensor> sensorsToInclude) {

        this.alphaSize= alphaSize;
        this.numStates= numStates;
        this.sensorsToInclude= sensorsToInclude;
    }


    @Override
    public IEnvironmentDescription getEnvironmentDescription() {
        FSMTransitionCounterDescription newDescription;
        if(lastDescription == null) {
            table= new FSMTransitionTableBuilder(alphaSize, numStates).getTransitionTable();
            newDescription = new FSMTransitionCounterDescription(table, sensorsToInclude);

        }
        else {
            tweakTable(1, lastDescription.getSensorTable());
            newDescription =
                    new FSMTransitionCounterDescription(table, sensorsToInclude, lastDescription.getSensorTable());
        }
        lastDescription = newDescription;
        return newDescription;
    }

    /**
     * randomly changes two moves in the transition table
     * @param numChanges
     */
    private void tweakTable(int numChanges, HashMap<Move, Integer>[] sensorTable) {
        IRandomizer randomizer = Services.retrieve(IRandomizer.class);
        for(int i = 0;i<numChanges; i++) {
            int stateToSwitch = randomizer.getRandomNumber(table.length); //pick which state whose moves will be swapped
            Move[] moveArray = lastDescription.getMoves();

            //pick two moves from a state's possible moves to exchange
            //don't allow the selected moves to be the same one
            int selectedMove1, selectedMove2;
            do {
                selectedMove1 = randomizer.getRandomNumber(moveArray.length);
                selectedMove2 = randomizer.getRandomNumber(moveArray.length);
            } while(selectedMove1 == selectedMove2 && moveArray.length != 1);

            //save value to temp and put new values in swapped places
            Integer temp = table[stateToSwitch].get(moveArray[selectedMove1]);
            table[stateToSwitch].put(moveArray[selectedMove1], table[stateToSwitch].get(moveArray[selectedMove2]));
            table[stateToSwitch].put(moveArray[selectedMove2], temp);

            //update the sensor table to reflect the "new" transitions
            sensorTable[stateToSwitch].put(moveArray[selectedMove1], 0);
            sensorTable[stateToSwitch].put(moveArray[selectedMove2], 0);
        }
    }
}
