package framework;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

public class ServicesTest {

    @BeforeEach
    public void initialize()
    {
        Services.clear();
    }

    // register Tests
    @Test
    public void registerNullClassThrowsException()
    {
        assertThrows(IllegalArgumentException.class, () -> Services.register(null, null));
    }

    @Test
    public void registerNullInstanceThrowsException()
    {
        assertThrows(IllegalArgumentException.class, () -> Services.register(IRandomizer.class, null));
    }

    // retrieve Tests
    @Test
    public void retrieveNullClassThrowsException()
    {
        assertThrows(IllegalArgumentException.class, () -> Services.retrieve(null));
    }

    @Test
    public void retrieveClassNotFoundReturnsNull()
    {
        assertNull(Services.retrieve(IRandomizer.class));
    }

    @Test
    public void retrieveReturnsRegisteredServiceInstance()
    {
        Randomizer expected = new Randomizer();
        Services.register(IRandomizer.class, expected);
        IRandomizer found = Services.retrieve(IRandomizer.class);
        assertSame(expected, found);
    }

    // clear Tests
    @Test
    public void clearEmptiesServices()
    {
        assertNull(Services.retrieve(IRandomizer.class));
        Services.register(IRandomizer.class, new Randomizer());
        assertNotNull(Services.retrieve(IRandomizer.class));
        Services.clear();
        assertNull(Services.retrieve(IRandomizer.class));
    }
}
