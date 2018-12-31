package tests.environments.meta;

import environments.meta.MetaConfiguration;

import tests.EpSemTest;
import tests.EpSemTestClass;
import static tests.Assertions.*;

@EpSemTestClass
public class MetaConfigurationTest {
    //region Constructor Tests
    @EpSemTest
    public void constructorResetGoalCountLessThan1ThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> new MetaConfiguration(0));
    }
    //endregion

    //region getResetGoalCount Tests
    @EpSemTest
    public void getResetGoalCount() {
        MetaConfiguration configuration = new MetaConfiguration(13);
        assertEquals(13, configuration.getResetGoalCount());
    }
    //endregion
}
