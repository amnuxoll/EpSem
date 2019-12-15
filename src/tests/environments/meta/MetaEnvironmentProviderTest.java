package tests.environments.meta;

import environments.meta.MetaConfiguration;
import environments.meta.MetaEnvironment;
import environments.meta.MetaEnvironmentProvider;
import framework.*;

import tests.EpSemTest;
import tests.EpSemTestClass;
import static tests.Assertions.*;

@EpSemTestClass
public class MetaEnvironmentProviderTest {
    //region Constructor Tests
    @EpSemTest
    public void constuctor(){
        assertThrows(IllegalArgumentException.class,
                () -> new MetaEnvironmentProvider(new TestEnvironmentDescriptionProvider(),null));
        assertThrows(IllegalArgumentException.class,
                () -> new MetaEnvironmentProvider(null, MetaConfiguration.DEFAULT));
    }
    //endregion

    //region getEnvironmentDescription Tests
    @EpSemTest
    public void getEnvironmentDescription(){
        MetaEnvironmentProvider provider =
                new MetaEnvironmentProvider(new TestEnvironmentDescriptionProvider(), MetaConfiguration.DEFAULT);
        IEnvironment environment = provider.getEnvironment();
        assertTrue(environment instanceof MetaEnvironment);

    }
    //endregion

    //region getAlias Tests
    @EpSemTest
    public void getAlias() {
        MetaEnvironmentProvider provider =
                new MetaEnvironmentProvider(new TestEnvironmentDescriptionProvider(), MetaConfiguration.DEFAULT);
        assertEquals("MetaEnvironment{alias}", provider.getAlias());
    }
    //endregion

    //region "mock" classes
    private class TestEnvironmentDescriptionProvider implements IEnvironmentProvider {
        @Override
        public IEnvironment getEnvironment() {
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
            return null;
        }

        @Override
        public IEnvironment copy() {
            return null;
        }

        @Override
        public boolean validateSequence(Sequence sequence) {
            return false;
        }
    }
    //endregion
}
