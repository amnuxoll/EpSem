package tests.environments.fsm;

import environments.fsm.FSMDescription;
import environments.fsm.FSMDescriptionProvider;
import environments.fsm.FSMTransitionTableBuilder;
import utils.Random;

import java.util.EnumSet;

import tests.EpSemTest;
import tests.EpSemTestClass;
import static tests.Assertions.*;

/**
 *
 * @author Zachary Paul Faltersack
 * @version 0.95
 */
@EpSemTestClass
public class FSMDescriptionProviderTest {
    //region constructor Tests
    @EpSemTest
    public void constructorAlphabetSizeLessThanOneThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> new FSMDescriptionProvider(new FSMTransitionTableBuilder(0, 1, Random.getTrue()), FSMDescription.Sensor.ALL_SENSORS));
    }

    @EpSemTest
    public void constructorNumberOfStatesLessThanOneThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> new FSMDescriptionProvider(new FSMTransitionTableBuilder(1, 0, Random.getTrue()), FSMDescription.Sensor.ALL_SENSORS));
    }

    @EpSemTest
    public void constructorNullEnumSetThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> new FSMDescriptionProvider(new FSMTransitionTableBuilder(1, 1, Random.getTrue()), null));
    }
    //endregion

    //region getEnvironmentDescription Tests
    @EpSemTest
    public void getEnvironmentDescriptionHeedsConfiguration1() {
        FSMDescriptionProvider descriptionProvider = new FSMDescriptionProvider(new FSMTransitionTableBuilder(1, 1, Random.getTrue()), FSMDescription.Sensor.ALL_SENSORS);
        FSMDescription description = (FSMDescription)descriptionProvider.getEnvironmentDescription();
        assertEquals(1, description.getActions().length);
    }

    @EpSemTest
    public void getEnvironmentDescriptionHeedsConfiguration2() {
        FSMDescriptionProvider descriptionProvider = new FSMDescriptionProvider(new FSMTransitionTableBuilder(13, 42, Random.getTrue()), EnumSet.of(FSMDescription.Sensor.EVEN_ODD));
        FSMDescription description = (FSMDescription)descriptionProvider.getEnvironmentDescription();
        assertEquals(13, description.getActions().length);
    }
    //endregion

    //region getAlias Tests
    @EpSemTest
    public void getAlias() {
        FSMDescriptionProvider descriptionProvider = new FSMDescriptionProvider(new FSMTransitionTableBuilder(13, 42, Random.getTrue()), EnumSet.of(FSMDescription.Sensor.EVEN_ODD));
        assertEquals("FSMDescription", descriptionProvider.getAlias());
    }
    //endregion
}
