package tests.environments.fsm;

import environments.fsm.FSMEnvironment;
import environments.fsm.FSMEnvironmentProvider;
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
public class FSMEnvironmentProviderTest {
    //region constructor Tests
    @EpSemTest
    public void constructorNullSeederThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> new FSMEnvironmentProvider(null, new FSMTransitionTableBuilder(1, 1, Random.getTrue()), FSMEnvironment.Sensor.ALL_SENSORS));
    }

    @EpSemTest
    public void constructorAlphabetSizeLessThanOneThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> new FSMEnvironmentProvider(Random.getTrue(), new FSMTransitionTableBuilder(0, 1, Random.getTrue()), FSMEnvironment.Sensor.ALL_SENSORS));
    }

    @EpSemTest
    public void constructorNumberOfStatesLessThanOneThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> new FSMEnvironmentProvider(Random.getTrue(), new FSMTransitionTableBuilder(1, 0, Random.getTrue()), FSMEnvironment.Sensor.ALL_SENSORS));
    }

    @EpSemTest
    public void constructorNullEnumSetThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> new FSMEnvironmentProvider(Random.getTrue(), new FSMTransitionTableBuilder(1, 1, Random.getTrue()), null));
    }
    //endregion

    //region getEnvironmentDescription Tests
    @EpSemTest
    public void getEnvironmentDescriptionHeedsConfiguration1() {
        FSMEnvironmentProvider descriptionProvider = new FSMEnvironmentProvider(Random.getTrue(), new FSMTransitionTableBuilder(1, 2, Random.getTrue()), FSMEnvironment.Sensor.ALL_SENSORS);
        FSMEnvironment description = (FSMEnvironment)descriptionProvider.getEnvironment();
        assertEquals(1, description.getActions().length);
    }

    @EpSemTest
    public void getEnvironmentDescriptionHeedsConfiguration2() {
        FSMEnvironmentProvider descriptionProvider = new FSMEnvironmentProvider(Random.getTrue(), new FSMTransitionTableBuilder(13, 42, Random.getTrue()), EnumSet.of(FSMEnvironment.Sensor.IS_EVEN));
        FSMEnvironment description = (FSMEnvironment)descriptionProvider.getEnvironment();
        assertEquals(13, description.getActions().length);
    }
    //endregion

    //region getAlias Tests
    @EpSemTest
    public void getAlias() {
        FSMEnvironmentProvider descriptionProvider = new FSMEnvironmentProvider(Random.getTrue(), new FSMTransitionTableBuilder(13, 42, Random.getTrue()), EnumSet.of(FSMEnvironment.Sensor.IS_EVEN));
        assertEquals("FSMEnvironment[Alpha_13_States_42]", descriptionProvider.getAlias());
    }
    //endregion
}
