package environments.meta;

import framework.*;
import org.junit.jupiter.api.Test;
import framework.Sequence;
//import org.junit.jupiter.params.ParameterizedTest;


import static org.junit.jupiter.api.Assertions.*;

public class MetaEnvironmentDescriptionTest {
    //region Constructor Tests
    @Test
    public void constructor() {
        //test exceptions
        assertThrows(IllegalArgumentException.class,
                () -> new MetaEnvironmentDescription(null,MetaConfiguration.DEFAULT));
        assertThrows(IllegalArgumentException.class,
                () -> new MetaEnvironmentDescription(new TestEnvironmentDescriptionProvider(),null));
    }
    //endregion

    //region getMoves Tests
    @Test
    public void getMoves() {
        TestEnvironmentDescriptionProvider provider = new TestEnvironmentDescriptionProvider();
        MetaEnvironmentDescription description= new MetaEnvironmentDescription(provider, MetaConfiguration.DEFAULT);

        //move arrays match
        Move[] expected = {
                new Move("a"),
                new Move("b")
        };
        assertArrayEquals(expected, description.getMoves());
    }
    //endregion

    //region transition Tests
    @Test
    public void transition() {
        TestEnvironmentDescriptionProvider provider = new TestEnvironmentDescriptionProvider();
        MetaEnvironmentDescription description = new MetaEnvironmentDescription(provider, MetaConfiguration.DEFAULT);

        TransitionResult result = description.transition(0, new Move("a"));
        assertEquals(10, result.getState());
        assertTrue(result.getSensorData().isGoal());
        assertEquals(1, provider.numGenerated);
    }

    @Test
    public void transitionResetsEnvironmentOnGoalCount() {
        TestEnvironmentDescriptionProvider provider = new TestEnvironmentDescriptionProvider();
        MetaEnvironmentDescription description = new MetaEnvironmentDescription(provider, new MetaConfiguration(2));

        int expectedResetcount = 3;
        for (int i = 0; i < 2 * expectedResetcount; i++) {
            TransitionResult result = description.transition(0, new Move("a"));
            assertEquals(10, result.getState());
            assertTrue(result.getSensorData().isGoal());
        }
        // Add one because of initial environment
        assertEquals(1 + expectedResetcount, provider.numGenerated);
    }
    //endregion

    //region "mock" classes
    private class TestEnvironmentDescriptionProvider implements IEnvironmentDescriptionProvider {
        public int numGenerated = 0;

        @Override
        public IEnvironmentDescription getEnvironmentDescription() {
            numGenerated++;
            return new TestEnvironmentDescription();
        }

        @Override
        public String getAlias() {
            return "alias";
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
        public TransitionResult transition(int currentState, Move move) {
            return new TransitionResult(10, new SensorData(true));
        }

        @Override
        public int getRandomState() {
            return 13;
        }

    }
    //endregion
}
