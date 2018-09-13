package environments.meta;

import org.junit.jupiter.api.Test;


import static org.junit.jupiter.api.Assertions.*;

public class MetaConfigurationTest {

    /**
     * test the constructor throws IllegalArgumentException for illegal arguments
     */
    @Test
    public void constructor() {
        assertThrows(IllegalArgumentException.class,
                () -> new MetaConfiguration(0, 1));
        assertThrows(IllegalArgumentException.class,
                () -> new MetaConfiguration(1, 0));
    }
}
