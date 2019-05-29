package tests.environments.meta;

import environments.meta.MetaConfiguration;
import environments.meta.MetaEnvironment;
import framework.*;

import tests.EpSemTest;
import tests.EpSemTestClass;
import static tests.Assertions.*;

@EpSemTestClass
public class MetaEnvironmentTest {
    //region Constructor Tests
    @EpSemTest
    public void constructor() {
        //test exceptions
        assertThrows(IllegalArgumentException.class,
                () -> new MetaEnvironment(null,MetaConfiguration.DEFAULT));
        assertThrows(IllegalArgumentException.class,
                () -> new MetaEnvironment(new TestEnvironmentDescriptionProvider(),null));
    }
    //endregion

    //region getActions Tests
    @EpSemTest
    public void getMoves() {
        TestEnvironmentDescriptionProvider provider = new TestEnvironmentDescriptionProvider();
        MetaEnvironment description= new MetaEnvironment(provider, MetaConfiguration.DEFAULT);

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
        MetaEnvironment description = new MetaEnvironment(provider, MetaConfiguration.DEFAULT);

        SensorData sensorData = description.applyAction(new Action("a"));
        assertTrue(sensorData.isGoal());
        assertEquals(1, provider.numGenerated);
    }

    @EpSemTest
    public void transitionResetsEnvironmentOnGoalCount() {
        TestEnvironmentDescriptionProvider provider = new TestEnvironmentDescriptionProvider();
        MetaEnvironment description = new MetaEnvironment(provider, new MetaConfiguration(2));

        int expectedResetcount = 3;
        for (int i = 0; i < 2 * expectedResetcount; i++) {
            SensorData sensorData = description.applyAction(new Action("a"));
            assertTrue(sensorData.isGoal());
        }
        // Add one because of initial environment
        assertEquals(1 + expectedResetcount, provider.numGenerated);
    }
    //endregion

    //region "mock" classes
    private class TestEnvironmentDescriptionProvider implements IEnvironmentProvider {
        public int numGenerated = 0;

        @Override
        public IEnvironment getEnvironment() {
            numGenerated++;
            return new TestEnvironment();
        }

        @Override
        public String getAlias() {
            return "alias";
        }

    }

    private class TestEnvironment implements IEnvironment {

        @Override
        public Action[] getActions() {
            Action[] actions = {
                    new Action("a"),
                    new Action("b")
            };
            return actions;
        }

        @Override
        public SensorData applyAction(Action action) {
            return new SensorData(true);
        }

        @Override
        public SensorData getNewStart() {
            return null;
        }

        @Override
        public Boolean validateSequence(Sequence sequence) {
            return null;
        }
    }
    //endregion
}
