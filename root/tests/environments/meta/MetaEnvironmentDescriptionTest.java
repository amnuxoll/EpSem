package environments.meta;

import framework.*;
import org.junit.jupiter.api.Test;
//import org.junit.jupiter.params.ParameterizedTest;


import static org.junit.jupiter.api.Assertions.*;

public class MetaEnvironmentDescriptionTest {
    /**
     * contructor test
     * make sure the contructor throws the correct exceptions
     */
    @Test
    public void constructor() {
        //test exceptions
        assertThrows(IllegalArgumentException.class,
                () -> new MetaEnvironmentDescription(null,MetaConfiguration.DEFAULT));
        assertThrows(IllegalArgumentException.class,
                () -> new MetaEnvironmentDescription(new TestEnvironmentDescriptionProvider(),null));
    }

    /**
     * make sure the moves on the meta description match the moves on the description
     * which the description provider should provide
     */
    @Test
    public void getMoves() {
        TestEnvironmentDescription testEnvironmentDescription= new TestEnvironmentDescription();

        TestEnvironmentDescriptionProvider provider= new TestEnvironmentDescriptionProvider();
        MetaEnvironmentDescription description= new MetaEnvironmentDescription(
                provider,MetaConfiguration.DEFAULT
        );

        //move arrays match
        assertArrayEquals(description.getMoves(),testEnvironmentDescription.getMoves());
    }

    /**
     * check that the meta description
     * 1) correctly transitions on a transition method call
     * 2) keeps track of the transition
     */
    @Test
    public void transition() {
        TestEnvironmentDescriptionProvider provider= new TestEnvironmentDescriptionProvider();
        MetaEnvironmentDescription description= new MetaEnvironmentDescription(
                provider,MetaConfiguration.DEFAULT
        );

        //check that we get the correct result
        assertEquals(42,description.transition(0,new Move("a")));
        //test that the counter has increased
        assertEquals(1,description.getTransitionCounter());
    }

    /**
     * make sure the transition method throws correct exceptions
     */
    @Test
    public void transitionsExceptions() {
        TestEnvironmentDescriptionProvider provider= new TestEnvironmentDescriptionProvider();
        MetaEnvironmentDescription description= new MetaEnvironmentDescription(
                provider,MetaConfiguration.DEFAULT
        );

        //test illegal args
        assertThrows(IllegalArgumentException.class, () -> description.transition(0,null));
        assertThrows(IllegalArgumentException.class, () -> description.transition(-1,new Move("a")));
        assertThrows(IllegalArgumentException.class, () -> description.transition(3,new Move("a")));
    }

    /**
     * test that the meta description has the correct goal state
     * (matches the provider's description's goal state)
     */
    @Test
    public void isGoalStateCorrect() {
        TestEnvironmentDescriptionProvider provider= new TestEnvironmentDescriptionProvider();
        MetaEnvironmentDescription description= new MetaEnvironmentDescription(
                provider,MetaConfiguration.DEFAULT
        );

        assertTrue(description.isGoalState(13));
        assertFalse(description.isGoalState(10));
    }

    /**
     * check that the transition counter gets reset when the goal state is hit
     */
    @Test
    public void isGoalStateTransitionCounter() {
        TestEnvironmentDescriptionProvider provider= new TestEnvironmentDescriptionProvider();
        MetaEnvironmentDescription description= new MetaEnvironmentDescription(
                provider,MetaConfiguration.DEFAULT
        );

        //get the right number of transition
        for (int i=0;i<3;i++) {
            description.transition(1,new Move("a"));
        }

        assertEquals(3,description.getTransitionCounter());
        assertFalse(description.isGoalState(12));
        assertEquals(3,description.getTransitionCounter());
        assertTrue(description.isGoalState(13));
        assertEquals(0,description.getTransitionCounter());
    }

    /**
     * test that, only under the correct conditions, the meta description makes
     * a new environment
     */
    @Test
    public void isGoalStateNewEnvironment() {
        TestEnvironmentDescriptionProvider provider= new TestEnvironmentDescriptionProvider();
        MetaEnvironmentDescription description= new MetaEnvironmentDescription(
                provider,MetaConfiguration.DEFAULT
        );

        assertEquals(1,provider.numGenerated);

        for (int i=0;i<MetaConfiguration.DEFAULT.getStepThreshold();i++) {
            description.transition(2,new Move("a"));
        }
        assertTrue(description.isGoalState(13));

        //should not have generated another description yet, because we only hit one goal
        assertEquals(1,provider.numGenerated);

        //now move it to the goal in less than DEFAULT.stepThreshold moves, DEFAULT.successQueueMaxSize times
        //so that provider should generate another EvironmentDescription
        for (int i=0;i<MetaConfiguration.DEFAULT.getSuccessQueueMaxSize()-1;i++) {
            for (int j=0;j<MetaConfiguration.DEFAULT.getStepThreshold()-2;j++) {
                description.transition(2,new Move("a"));
            }
            assertTrue(description.isGoalState(13));
        }

        //now we should have generated another description
        assertEquals(2,provider.numGenerated);

        //we also should empty the successQueue
        assertEquals(0,description.getSuccessQueue().size());
    }

    /**
     * verify that the average moves to goal is correct
     */
    @Test
    public void isGoalStateCorrectAverage() {
        TestEnvironmentDescriptionProvider provider= new TestEnvironmentDescriptionProvider();
        MetaEnvironmentDescription description= new MetaEnvironmentDescription(
                provider,MetaConfiguration.DEFAULT
        );

        //if hit less than DEFAULT.maxQueueSize goals, our average should be
        //the mean of the all the tries
        int average= 0;
        for (int goals=0;goals<MetaConfiguration.DEFAULT.getSuccessQueueMaxSize()-1;goals++) {
            int transitionsToGoal= (int)(Math.random()*MetaConfiguration.DEFAULT.getStepThreshold());
            for (int transitions=0;transitions<transitionsToGoal;transitions++) {
                description.transition(2,new Move("a"));
            }
            assertTrue(description.isGoalState(13));
            average+= transitionsToGoal;
        }
        average/= MetaConfiguration.DEFAULT.getSuccessQueueMaxSize()-1;

        assertEquals(1,provider.numGenerated);
        assertEquals(average,description.averageEnqueuedSuccesses());

        //now if we hit one more goal, the average should reset
        assertTrue(description.isGoalState(13));
        assertEquals(-1,description.averageEnqueuedSuccesses());
    }

    /**
     * test that the meta description has the correct number of states.
     * it should match the description which the provider provides
     */
    @Test
    public void getNumStates() {
        TestEnvironmentDescriptionProvider provider= new TestEnvironmentDescriptionProvider();
        MetaEnvironmentDescription description= new MetaEnvironmentDescription(
                provider,MetaConfiguration.DEFAULT
        );

        assertEquals(3,description.getNumStates());
    }


    /**
     * test that the meta environment has the same functionality for applying sensors
     * as it's current environment
     */
    @Test
    public void applySensors() {
        TestEnvironmentDescriptionProvider provider= new TestEnvironmentDescriptionProvider();
        MetaEnvironmentDescription description= new MetaEnvironmentDescription(
                provider,MetaConfiguration.DEFAULT
        );

        SensorData data = new SensorData(description.isGoalState(13));
        description.applySensors(2,new Move("a"), 2, data);
        assertTrue(data.isGoal());
        assertTrue(data.hasSensor("sensei"));
        assertEquals(2,data.getSensor("sensei"));
    }

    /**
     * tests whether applySensors throws IllegalArgumentException properly
     */
    @Test
    public void applysSensorsExceptions() {
        TestEnvironmentDescriptionProvider provider= new TestEnvironmentDescriptionProvider();
        MetaEnvironmentDescription description= new MetaEnvironmentDescription(
                provider,MetaConfiguration.DEFAULT
        );

        Move move= new Move("a");
        SensorData data= new SensorData(false);

        assertThrows(IllegalArgumentException.class,
                () -> description.applySensors(2, move, 2, null));
        assertThrows(IllegalArgumentException.class,
                () -> description.applySensors(2, null, 2, data));
        assertThrows(IllegalArgumentException.class,
                () -> description.applySensors(-1 ,move, 2, data));
        assertThrows(IllegalArgumentException.class,
                () -> description.applySensors(4, move, 2, data));
        assertThrows(IllegalArgumentException.class,
                () -> description.applySensors(2, move, -1, data));
        assertThrows(IllegalArgumentException.class,
                () -> description.applySensors(2, move, 4, data));
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

    private class TestEnvironmentDescription implements  IEnvironmentDescription{

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
        public int getNumStates() {
            return 3;
        }

        @Override
        public void applySensors(int lastState, Move move, int currState, SensorData sensorData) {
            sensorData.setSensor("sensei", new Integer(2));
        }
    }
}
