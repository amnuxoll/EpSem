package tests.environments.meta;

import environments.meta.MetaConfiguration;
import environments.meta.MetaEnvironmentDescription;
import framework.*;

import tests.EpSemTest;
import tests.EpSemTestClass;
import static tests.Assertions.*;

@EpSemTestClass
public class MetaEnvironmentDescriptionTest {
    //region Constructor Tests
    @EpSemTest
    public void constructor() {
        //test exceptions
        assertThrows(IllegalArgumentException.class,
                () -> new MetaEnvironmentDescription(null,MetaConfiguration.DEFAULT));
        assertThrows(IllegalArgumentException.class,
                () -> new MetaEnvironmentDescription(new TestEnvironmentDescriptionProvider(),null));
    }
    //endregion

    //region getActions Tests
    @EpSemTest
    public void getMoves() {
        TestEnvironmentDescriptionProvider provider = new TestEnvironmentDescriptionProvider();
        MetaEnvironmentDescription description= new MetaEnvironmentDescription(provider, MetaConfiguration.DEFAULT);

        //move arrays match
        Action[] expected = {
                new Action("a"),
                new Action("b")
        };
        assertArrayEquals(expected, description.getActions());
    }
    //endregion

    //region transition Tests
    @EpSemTest
    public void transition() {
        TestEnvironmentDescriptionProvider provider = new TestEnvironmentDescriptionProvider();
        MetaEnvironmentDescription description = new MetaEnvironmentDescription(provider, MetaConfiguration.DEFAULT);

        TransitionResult result = description.transition(0, new Action("a"));
        assertEquals(10, result.getState());
        assertTrue(result.getSensorData().isGoal());
        assertEquals(1, provider.numGenerated);
    }

    @EpSemTest
    public void transitionResetsEnvironmentOnGoalCount() {
        TestEnvironmentDescriptionProvider provider = new TestEnvironmentDescriptionProvider();
        MetaEnvironmentDescription description = new MetaEnvironmentDescription(provider, new MetaConfiguration(2));

        int expectedResetcount = 3;
        for (int i = 0; i < 2 * expectedResetcount; i++) {
            TransitionResult result = description.transition(0, new Action("a"));
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
        public Action[] getActions() {
            Action[] actions = {
                    new Action("a"),
                    new Action("b")
            };
            return actions;
        }

        @Override
        public TransitionResult transition(int currentState, Action action) {
            return new TransitionResult(10, new SensorData(true));
        }

        @Override
        public int getRandomState() {
            return 13;
        }

    }
    //endregion
}
