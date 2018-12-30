package environments.meta;

import org.junit.jupiter.api.Test;


import static org.junit.jupiter.api.Assertions.*;

public class MetaConfigurationTest {
    //region Constructor Tests
    @Test
    public void constructorResetGoalCountLessThan1ThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> new MetaConfiguration(0));
    }
    //endregion

    //region getResetGoalCount Tests
    @Test
    public void getResetGoalCount() {
        MetaConfiguration configuration = new MetaConfiguration(13);
        assertEquals(13, configuration.getResetGoalCount());
    }
    //endregion
}
