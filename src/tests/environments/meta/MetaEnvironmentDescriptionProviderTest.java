package tests.environments.meta;

import environments.meta.MetaConfiguration;
import environments.meta.MetaEnvironmentDescription;
import environments.meta.MetaEnvironmentDescriptionProvider;
import framework.*;

import tests.EpSemTest;
import tests.EpSemTestClass;
import static tests.Assertions.*;

@EpSemTestClass
public class MetaEnvironmentDescriptionProviderTest {
    //region Constructor Tests
    @EpSemTest
    public void constuctor(){
        assertThrows(IllegalArgumentException.class,
                () -> new MetaEnvironmentDescriptionProvider(new TestEnvironmentDescriptionProvider(),null));
        assertThrows(IllegalArgumentException.class,
                () -> new MetaEnvironmentDescriptionProvider(null, MetaConfiguration.DEFAULT));
    }
    //endregion

    //region getEnvironmentDescription Tests
    @EpSemTest
    public void getEnvironmentDescription(){
        MetaEnvironmentDescriptionProvider provider =
                new MetaEnvironmentDescriptionProvider(new TestEnvironmentDescriptionProvider(), MetaConfiguration.DEFAULT);
        IEnvironmentDescription description= provider.getEnvironmentDescription();
        assertTrue(description instanceof MetaEnvironmentDescription);

    }
    //endregion

    //region getAlias Tests
    @EpSemTest
    public void getAlias() {
        MetaEnvironmentDescriptionProvider provider =
                new MetaEnvironmentDescriptionProvider(new TestEnvironmentDescriptionProvider(), MetaConfiguration.DEFAULT);
        assertEquals("MetaEnvironmentDescription{alias}", provider.getAlias());
    }
    //endregion

    //region "mock" classes
    private class TestEnvironmentDescriptionProvider implements IEnvironmentDescriptionProvider {
        @Override
        public IEnvironmentDescription getEnvironmentDescription() {
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
            Move[] moves = {
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
    //endregion
}