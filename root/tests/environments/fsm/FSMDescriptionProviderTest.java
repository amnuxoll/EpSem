package environments.fsm;

import org.junit.jupiter.api.Test;
import utils.Randomizer;

import java.util.EnumSet;

import static org.junit.jupiter.api.Assertions.*;

public class FSMDescriptionProviderTest {

    // constructor Tests
    @Test
    public void constructorAlphabetSizeLessThanOneThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> new FSMDescriptionProvider(new FSMTransitionTableBuilder(0, 1, new Randomizer()), FSMDescription.Sensor.ALL_SENSORS));
    }

    @Test
    public void constructorNumberOfStatesLessThanOneThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> new FSMDescriptionProvider(new FSMTransitionTableBuilder(1, 0, new Randomizer()), FSMDescription.Sensor.ALL_SENSORS));
    }

    @Test
    public void constructorNullEnumSetThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> new FSMDescriptionProvider(new FSMTransitionTableBuilder(1, 1, new Randomizer()), null));
    }

    // getEnvironmentDescription Tests
    @Test
    public void getEnvironmentDescriptionHeedsConfiguration1() {
        FSMDescriptionProvider descriptionProvider = new FSMDescriptionProvider(new FSMTransitionTableBuilder(1, 1, new Randomizer()), FSMDescription.Sensor.ALL_SENSORS);
        FSMDescription description = (FSMDescription)descriptionProvider.getEnvironmentDescription();
        assertEquals(1, description.getMoves().length);
        assertEquals(FSMDescription.Sensor.ALL_SENSORS, description.getSensorsToInclude());
    }

    @Test
    public void getEnvironmentDescriptionHeedsConfiguration2() {
        FSMDescriptionProvider descriptionProvider = new FSMDescriptionProvider(new FSMTransitionTableBuilder(13, 42, new Randomizer()), EnumSet.of(FSMDescription.Sensor.EVEN_ODD));
        FSMDescription description = (FSMDescription)descriptionProvider.getEnvironmentDescription();
        assertEquals(13, description.getMoves().length);
        assertEquals(EnumSet.of(FSMDescription.Sensor.EVEN_ODD), description.getSensorsToInclude());
    }
}
