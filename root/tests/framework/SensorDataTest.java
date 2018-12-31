package tests.framework;

import framework.SensorData;

import java.util.Set;

import tests.EpSemTest;
import tests.EpSemTestClass;
import static tests.Assertions.*;

/**
 *
 * @author Zachary Paul Faltersack
 * @version 0.95
 */
@EpSemTestClass
public class SensorDataTest {
    //region setSensor & getSensor Tests
    @EpSemTest
    public void testSetSensorGetSensor() {
        SensorData sensorData = new SensorData(false);
        sensorData.setSensor("sensor", 5);
        assertEquals(5, sensorData.getSensor("sensor"));
    }

    @EpSemTest
    public void testSetSensorOverwritesPreviousValue() {
        SensorData sensorData = new SensorData(false);
        sensorData.setSensor("sensor", 5);
        sensorData.setSensor("sensor", "otherValue");
        assertEquals("otherValue", sensorData.getSensor("sensor"));
    }

    @EpSemTest
    public void testSetSensorNullSensorNameThrowsException() {
        SensorData sensorData = new SensorData(false);
        assertThrows(IllegalArgumentException.class, () -> sensorData.setSensor(null, 5));
    }

    @EpSemTest
    public void testSetSensorEmptySensorNameThrowsException() {
        SensorData sensorData = new SensorData(false);
        assertThrows(IllegalArgumentException.class, () -> sensorData.setSensor("", 5));
    }

    @EpSemTest
    public void testGetSensorNullSensorNameThrowsException() {
        SensorData sensorData = new SensorData(false);
        assertThrows(IllegalArgumentException.class, () -> sensorData.getSensor(null));
    }

    @EpSemTest
    public void testGetSensorEmptySensorNameThrowsException() {
        SensorData sensorData = new SensorData(false);
        assertThrows(IllegalArgumentException.class, () -> sensorData.getSensor(""));
    }
    //endregion

    //region hasSensor Argument Tests
    @EpSemTest
    public void testHasSensorTrue() {
        SensorData sensorData = new SensorData(false);
        sensorData.setSensor("sensor", 5);
        assertTrue(sensorData.hasSensor("sensor"));
    }

    @EpSemTest
    public void testHasSensorFalse() {
        SensorData sensorData = new SensorData(false);
        assertFalse(sensorData.hasSensor("sensor"));
    }

    @EpSemTest
    public void testHasSensorNullSensorNameThrowsException() {
        SensorData sensorData = new SensorData(false);
        assertThrows(IllegalArgumentException.class, () -> sensorData.hasSensor(null));
    }

    @EpSemTest
    public void testHasSensorEmptySensorNameThrowsException() {
        SensorData sensorData = new SensorData(false);
        assertThrows(IllegalArgumentException.class, () -> sensorData.hasSensor(""));
    }
    //endregion

    //region isGoal Tests
    @EpSemTest
    public void testIsGoalFalse() {
        SensorData sensorData = new SensorData(false);
        assertFalse(sensorData.isGoal());
    }

    @EpSemTest
    public void testIsGoalTrue() {
        SensorData sensorData = new SensorData(true);
        assertTrue(sensorData.isGoal());
    }
    //endregion

    //region getSensorNames Tests
    @EpSemTest
    public void getSensorNamesEmptySensorSet()
    {
        SensorData sensorData = new SensorData(true);
        Set<String> sensorNames = sensorData.getSensorNames();
        assertEquals(1, sensorNames.size());
        assertTrue(sensorNames.contains("GOAL"));
    }

    @EpSemTest
    public void getSensorNonEmptySensorSet()
    {
        SensorData sensorData = new SensorData(true);
        sensorData.setSensor("additional", 13);
        Set<String> sensorNames = sensorData.getSensorNames();
        assertEquals(2, sensorNames.size());
        assertTrue(sensorNames.contains("GOAL"));
        assertTrue(sensorNames.contains("additional"));
    }
    //endregion

    //region equals Tests
    @EpSemTest
    public void testEqualsEmptySensorDataNotGoalAreEqual() {
        SensorData sensorData1 = new SensorData(false);
        SensorData sensorData2 = new SensorData(false);
        assertEquals(sensorData1, sensorData2);
    }

    @EpSemTest
    public void testEqualsEmptySensorDataGoalAreEqual() {
        SensorData sensorData1 = new SensorData(true);
        SensorData sensorData2 = new SensorData(true);
        assertEquals(sensorData1, sensorData2);
    }

    @EpSemTest
    public void testEqualsEmptySensorDataGoalAreNotEqual() {
        SensorData sensorData1 = new SensorData(true);
        SensorData sensorData2 = new SensorData(false);
        assertNotEquals(sensorData1, sensorData2);
    }

    @EpSemTest
    public void testEqualsAllSensorsAreAccountedForAreEqual() {
        SensorData sensorData1 = new SensorData(true);
        sensorData1.setSensor("sensor1", 4);
        sensorData1.setSensor("sensor2", "sensor");
        SensorData sensorData2 = new SensorData(true);
        sensorData2.setSensor("sensor1", 4);
        sensorData2.setSensor("sensor2", "sensor");
        assertEquals(sensorData1, sensorData2);
    }

    @EpSemTest
    public void testEqualsNullValuesAreEqual() {
        SensorData sensorData1 = new SensorData(true);
        sensorData1.setSensor("sensor1", null);
        SensorData sensorData2 = new SensorData(true);
        sensorData2.setSensor("sensor1", null);
        assertEquals(sensorData1, sensorData2);
    }

    @EpSemTest
    public void testEqualsAllSensorsAreAccountedForNotAllEqual() {
        SensorData sensorData1 = new SensorData(true);
        sensorData1.setSensor("sensor1", 4);
        sensorData1.setSensor("sensor2", "sensor");
        SensorData sensorData2 = new SensorData(true);
        sensorData2.setSensor("sensor1", 4);
        sensorData2.setSensor("sensor2", "sensorvalue");
        assertNotEquals(sensorData1, sensorData2);
    }

    @EpSemTest
    public void testEqualsDifferentSensorCountsNotEqual() {
        SensorData sensorData1 = new SensorData(true);
        sensorData1.setSensor("sensor1", 4);
        sensorData1.setSensor("sensor2", "sensor");
        SensorData sensorData2 = new SensorData(true);
        sensorData2.setSensor("sensor1", 4);
        assertNotEquals(sensorData1, sensorData2);
    }
    //endregion

    //region hashCode Tests
    @EpSemTest
    public void testHashCodeAllSensorsAreAccountedFor() {
        SensorData sensorData1 = new SensorData(true);
        sensorData1.setSensor("sensor1", 4);
        sensorData1.setSensor("sensor2", "sensor");
        SensorData sensorData2 = new SensorData(true);
        sensorData2.setSensor("sensor1", 4);
        sensorData2.setSensor("sensor2", "sensor");
        assertEquals(sensorData1.hashCode(), sensorData2.hashCode());
    }
    //endregion

    //region toString Tests
    @EpSemTest
    public void testToShortStringSingleSensorOverrideExcludesLabels() {
        SensorData sensorData = new SensorData(true);
        assertEquals("[true]", sensorData.toString());
    }

    @EpSemTest
    public void testToShortStringMultipleSensorsCustomSortOverrideExcludesLabels() {
        SensorData sensorData = new SensorData(false);
        sensorData.setSensor("b", "value");
        sensorData.setSensor("a", 5);
        sensorData.setSensor("c", 10.0);
        assertEquals("[false;5;value;10.0]", sensorData.toString());
    }

    @EpSemTest
    public void testToShortStringSingleSensorExplicitExcludeLabels() {
        SensorData sensorData = new SensorData(true);
        assertEquals("[true]", sensorData.toString(false));
    }

    @EpSemTest
    public void testToShortStringMultipleSensorsCustomSortExplicitExcludeLabels() {
        SensorData sensorData = new SensorData(false);
        sensorData.setSensor("b", "value");
        sensorData.setSensor("a", 5);
        sensorData.setSensor("c", 10.0);
        assertEquals("[false;5;value;10.0]", sensorData.toString(false));
    }

    @EpSemTest
    public void testToStringSingleSensorIncludeLabels() {
        SensorData sensorData = new SensorData(true);
        assertEquals("[GOAL:true]", sensorData.toString(true));
    }

    @EpSemTest
    public void testToStringMultipleSensorsCustomSortIncludeLabels() {
        SensorData sensorData = new SensorData(false);
        sensorData.setSensor("b", "value");
        sensorData.setSensor("a", 5);
        sensorData.setSensor("c", 10.0);
        assertEquals("[GOAL:false;a:5;b:value;c:10.0]", sensorData.toString(true));
    }
    //endregion
}
