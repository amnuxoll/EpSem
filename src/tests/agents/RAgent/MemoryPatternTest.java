package tests.agents.RAgent;

import agents.RAgent.MemoryPattern;
import framework.Action;
import framework.SensorData;
import tests.EpSemTest;
import tests.EpSemTestClass;
import static tests.Assertions.*;

@EpSemTestClass
public class MemoryPatternTest {

    @EpSemTest
    public void EqualsTest() {
        SensorData pre1 = new SensorData(false);
        pre1.setSensor("sensor1", true);

        SensorData post1 = new SensorData(false);
        post1.setSensor("sensor1", false);

        SensorData pre2 = new SensorData(false);
        pre2.setSensor("sensor1", true);

        SensorData post2 = new SensorData(false);
        post2.setSensor("sensor1", false);

        Action a1 = new Action("a");
        Action a2 = new Action("a");

        MemoryPattern mp1 = new MemoryPattern(pre1, a1, post1);
        MemoryPattern mp2 = new MemoryPattern(pre2, a2, post2);

        assertTrue(mp1.equals(mp2));
    }
}
