package tests.framework;

import framework.Episode;
import framework.Action;
import framework.SensorData;

import tests.EpSemTest;
import tests.EpSemTestClass;
import static tests.Assertions.*;

/**
 *
 * @author Zachary Paul Faltersack
 * @version 0.95
 */
@EpSemTestClass
public class EpisodeTest {

    //region Constructor Tests

    @EpSemTest
    public void testConstructorNullSensorDataThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> new Episode(null, new Action("action")));
    }

    @EpSemTest
    public void testConstructorNullActionThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> new Episode(new SensorData(true), null));
    }

    //endregion

    //region getAction Tests

    @EpSemTest
    public void testGetMove() {
        Action action = new Action("action");
        Episode ep = new Episode(new SensorData(true), action);
        assertSame(action, ep.getAction());
    }

    //endregion

    //region getSensorData Tests

    @EpSemTest
    public void testGetSensorData() {
        Episode ep = new Episode(new SensorData(true), new Action("move"));
        SensorData sensorData = ep.getSensorData();
        assertTrue(sensorData.isGoal());
    }

    //endregion

    //region hitGoal Tests

    @EpSemTest
    public void hitGoal()
    {
        Episode episode = new Episode(new SensorData(true), new Action("move"));
        assertTrue(episode.hitGoal());
        episode = new Episode(new SensorData(false), new Action("move"));
        assertFalse(episode.hitGoal());
    }

    //endregion

    //region equals Tests

    @EpSemTest
    public void testEqualsAreEqualNotNullSensorData() {
        Episode episode1 = new Episode(new SensorData(true), new Action("move"));
        Episode episode2 = new Episode(new SensorData(true), new Action("move"));
        assertEquals(episode1, episode2);
    }

    @EpSemTest
    public void testEqualsAreNotEqualDifferentSensorData() {
        Episode episode1 = new Episode(new SensorData(false), new Action("move"));
        Episode episode2 = new Episode(new SensorData(true), new Action("move"));
        assertNotEquals(episode1, episode2);
    }

    @EpSemTest
    public void testEqualsAreNotEqualDifferentMove() {
        Episode episode1 = new Episode(new SensorData(true), new Action("move1"));
        Episode episode2 = new Episode(new SensorData(true), new Action("move2"));
        assertNotEquals(episode1, episode2);
    }

    //endregion

    //region hashCode Tests

    @EpSemTest
    public void testHashcodesAreEqualNotNullSensorData() {
        Episode episode1 = new Episode(new SensorData(true), new Action("move"));
        Episode episode2 = new Episode(new SensorData(true), new Action("move"));
        assertEquals(episode1.hashCode(), episode2.hashCode());
    }

    //endregion

    //region toString Tests

    @EpSemTest
    public void toStringWithSensorData()
    {
        Episode episode = new Episode(new SensorData(true), new Action("move"));
        assertEquals("[GOAL:true]move", episode.toString());
    }

    //endregion
}
