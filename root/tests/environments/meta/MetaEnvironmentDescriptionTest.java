package environments.meta;

import framework.*;
import org.junit.jupiter.api.Test;
import framework.Sequence;
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
        fail("come back to this");
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
            return null;
        }

        @Override
        public int getRandomState() {
            return 0;
        }

    }
}
