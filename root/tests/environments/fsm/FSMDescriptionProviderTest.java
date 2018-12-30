package environments.fsm;

import org.junit.jupiter.api.Test;
import utils.Random;

import java.util.EnumSet;

import static org.junit.jupiter.api.Assertions.*;

/**
 *
 * @author Zachary Paul Faltersack
 * @version 0.95
 */
public class FSMDescriptionProviderTest {
    //region constructor Tests
    @Test
    public void constructorAlphabetSizeLessThanOneThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> new FSMDescriptionProvider(new FSMTransitionTableBuilder(0, 1, Random.getTrue()), FSMDescription.Sensor.ALL_SENSORS));
    }

    @Test
    public void constructorNumberOfStatesLessThanOneThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> new FSMDescriptionProvider(new FSMTransitionTableBuilder(1, 0, Random.getTrue()), FSMDescription.Sensor.ALL_SENSORS));
    }

    @Test
    public void constructorNullEnumSetThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> new FSMDescriptionProvider(new FSMTransitionTableBuilder(1, 1, Random.getTrue()), null));
    }
    //endregion

    //region getEnvironmentDescription Tests
    @Test
    public void getEnvironmentDescriptionHeedsConfiguration1() {
        FSMDescriptionProvider descriptionProvider = new FSMDescriptionProvider(new FSMTransitionTableBuilder(1, 1, Random.getTrue()), FSMDescription.Sensor.ALL_SENSORS);
        FSMDescription description = (FSMDescription)descriptionProvider.getEnvironmentDescription();
        assertEquals(1, description.getMoves().length);
    }

    @Test
    public void getEnvironmentDescriptionHeedsConfiguration2() {
        FSMDescriptionProvider descriptionProvider = new FSMDescriptionProvider(new FSMTransitionTableBuilder(13, 42, Random.getTrue()), EnumSet.of(FSMDescription.Sensor.EVEN_ODD));
        FSMDescription description = (FSMDescription)descriptionProvider.getEnvironmentDescription();
        assertEquals(13, description.getMoves().length);
    }
    //endregion

    //region getAlias Tests
    @Test
    public void getAlias()
    {
        FSMDescriptionProvider descriptionProvider = new FSMDescriptionProvider(new FSMTransitionTableBuilder(13, 42, Random.getTrue()), EnumSet.of(FSMDescription.Sensor.EVEN_ODD));
        assertEquals("FSMDescription", descriptionProvider.getAlias());
    }
    //endregion
}
