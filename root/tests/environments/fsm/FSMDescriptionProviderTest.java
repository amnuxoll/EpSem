package environments.fsm;

import framework.IRandomizer;
import framework.Randomizer;
import framework.Services;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;

import static org.junit.jupiter.api.Assertions.*;

public class FSMDescriptionProviderTest {

    // test setup
    @BeforeEach
    public void initialize()
    {
        Services.register(IRandomizer.class, new Randomizer());
    }

    // constructor Tests
    @Test
    public void constructorAlphabetSizeLessThanOneThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> new FSMDescriptionProvider(0, 1, FSMDescription.Sensor.ALL_SENSORS));
    }

    @Test
    public void constructorNumberOfStatesLessThanOneThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> new FSMDescriptionProvider(1, 0, FSMDescription.Sensor.ALL_SENSORS));
    }

    @Test
    public void constructorNullEnumSetThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> new FSMDescriptionProvider(1, 1, null));
    }

    // getEnvironmentDescription Tests
    @Test
    public void getEnvironmentDescriptionHeedsConfiguration1() {
        FSMDescriptionProvider descriptionProvider = new FSMDescriptionProvider(1, 1, FSMDescription.Sensor.ALL_SENSORS);
        FSMDescription description = (FSMDescription)descriptionProvider.getEnvironmentDescription();
        assertEquals(1, description.getMoves().length);
        assertEquals(1, description.getNumStates());
        assertEquals(FSMDescription.Sensor.ALL_SENSORS, description.getSensorsToInclude());
    }

    @Test
    public void getEnvironmentDescriptionHeedsConfiguration2() {
        FSMDescriptionProvider descriptionProvider = new FSMDescriptionProvider(13, 42, EnumSet.of(FSMDescription.Sensor.EVEN_ODD));
        FSMDescription description = (FSMDescription)descriptionProvider.getEnvironmentDescription();
        assertEquals(13, description.getMoves().length);
        assertEquals(42, description.getNumStates());
        assertEquals(EnumSet.of(FSMDescription.Sensor.EVEN_ODD), description.getSensorsToInclude());
    }
}
