package tests.agents.RAgent;

import framework.SensorData;
import tests.EpSemTest;
import tests.EpSemTestClass;
import static tests.Assertions.*;

public class MemoryPatternTest {

    @EpSemTest
    public void EqualsTest() {
        SensorData sd1 = new SensorData(false);
        sd1.setSensor("sensor1", true);

        SensorData sd2 = new SensorData(false);
        sd2.setSensor("sensor1", true);

        assertTrue(sd1.equals(sd2));
    }
}
