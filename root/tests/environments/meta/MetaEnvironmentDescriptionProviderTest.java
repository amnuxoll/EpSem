package environments.meta;

import framework.*;
import org.junit.jupiter.api.Test;
import framework.Sequence;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;
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
