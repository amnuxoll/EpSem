package tests.framework;

import framework.SensorData;
import framework.TransitionResult;

import tests.EpSemTest;
import tests.EpSemTestClass;
import static tests.Assertions.*;

/**
 *
 * @author Zachary Paul Faltersack
 * @version 0.95
 */
@EpSemTestClass
public class TransitionResultTest {
    //region Constructor Tests
    @EpSemTest
    public void constructorNullSensorDataThrowsException()
    {
        assertThrows(IllegalArgumentException.class, () -> new TransitionResult(13, null));
    }
    //endregion

    //region getState Tests
    @EpSemTest
    public void getState()
    {
        TransitionResult result = new TransitionResult(13, new SensorData(true));
        assertEquals(13, result.getState());
    }
    //endregion

    //region getSensorData Tests
    @EpSemTest
    public void getSensorData()
    {
        SensorData sensorData = new SensorData(true);
        TransitionResult result = new TransitionResult(13, sensorData);
        assertSame(sensorData, result.getSensorData());
    }
    //endregion
}
