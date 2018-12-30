package framework;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 *
 * @author Zachary Paul Faltersack
 * @version 0.95
 */
public class TransitionResultTest {
    //region Constructor Tests
    @Test
    public void constructorNullSensorDataThrowsException()
    {
        assertThrows(IllegalArgumentException.class, () -> new TransitionResult(13, null));
    }
    //endregion

    //region getState Tests
    @Test
    public void getState()
    {
        TransitionResult result = new TransitionResult(13, new SensorData(true));
        assertEquals(13, result.getState());
    }
    //endregion

    //region getSensorData Tests
    @Test
    public void getSensorData()
    {
        SensorData sensorData = new SensorData(true);
        TransitionResult result = new TransitionResult(13, sensorData);
        assertSame(sensorData, result.getSensorData());
    }
    //endregion
}
