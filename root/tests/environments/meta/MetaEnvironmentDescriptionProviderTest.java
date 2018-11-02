package environments.meta;

import framework.*;
import org.junit.jupiter.api.Test;
import utils.Sequence;

import static org.junit.Assert.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class MetaEnvironmentDescriptionProviderTest {
    @Test
    public void constuctor(){
        assertThrows(IllegalArgumentException.class,
                () -> new MetaEnvironmentDescriptionProvider(new TestEnvironmentDescriptionProvider(),null));
        assertThrows(IllegalArgumentException.class,
                () -> new MetaEnvironmentDescriptionProvider(null,MetaConfiguration.DEFAULT));
    }

    @Test
    public void getEnvironmentDescription(){
        MetaEnvironmentDescriptionProvider provider=
                new MetaEnvironmentDescriptionProvider(
                        new TestEnvironmentDescriptionProvider(),MetaConfiguration.DEFAULT);

        IEnvironmentDescription description= provider.getEnvironmentDescription();

        //check that the provedier successfully provided by checking the description's goal state
        assertTrue(description.isGoalState(13));
        assertFalse(description.isGoalState(12));
    }

    /**
     *  Mock Classes
     *
     */

    private class TestEnvironmentDescriptionProvider implements IEnvironmentDescriptionProvider {
        public int numGenerated= 0;

        @Override
        public IEnvironmentDescription getEnvironmentDescription() {
            numGenerated++;
            return new TestEnvironmentDescription();
        }

    }

    private class TestEnvironmentDescription implements  IEnvironmentDescription {

        @Override
        public Move[] getMoves() {
            Move[] moves= {
                    new Move("a"),
                    new Move("b")
            };
            return moves;
        }

        @Override
        public int transition(int currentState, Move move) {
            return 42;
        }

        @Override
        public boolean isGoalState(int state) {
            return state == 13;
        }

        @Override
        public int getNumGoalStates() {
            return 1;
        }

        @Override
        public int getNumStates() {
            return 3;
        }

        @Override
        public void applySensors(int lastState, Move move, int currState, SensorData sensorData) {
            sensorData.setSensor("sensei", new Integer(2));
        }

        @Override
        public void addEnvironmentListener(IEnvironmentListener listener) {

        }

        @Override
        public boolean validateSequence(int state, Sequence sequence) {
            return false;
        }
    }
}
