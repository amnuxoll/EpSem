package tests.framework;

import framework.Episode;
import framework.Move;
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
    public void testConstructorNullMoveThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> new Episode(null, null));
    }
    //endregion

    //region getMove Tests
    @EpSemTest
    public void testGetMove() {
        Move move = new Move("move");
        Episode ep = new Episode(new SensorData(true), move);
        assertSame(move, ep.getMove());
    }
    //endregion

    //region getSensorData Tests
    @EpSemTest
    public void testGetSensorData() {
        Episode ep = new Episode(new SensorData(true), new Move("move"));
        SensorData sensorData = ep.getSensorData();
        assertTrue(sensorData.isGoal());
    }
    //endregion

    //region hitGoal Tests
    @EpSemTest
    public void hitGoal()
    {
        Episode episode = new Episode(new SensorData(true), new Move("move"));
        assertTrue(episode.hitGoal());
        episode = new Episode(new SensorData(false), new Move("move"));
        assertFalse(episode.hitGoal());
    }
    //endregion

    //region equals Tests
    @EpSemTest
    public void testEqualsAreEqualNotNullSensorData() {
        Episode episode1 = new Episode(new SensorData(true), new Move("move"));
        Episode episode2 = new Episode(new SensorData(true), new Move("move"));
        assertEquals(episode1, episode2);
    }

    @EpSemTest
    public void testEqualsAreNotEqualDifferentSensorData() {
        Episode episode1 = new Episode(new SensorData(false), new Move("move"));
        Episode episode2 = new Episode(new SensorData(true), new Move("move"));
        assertNotEquals(episode1, episode2);
    }

    @EpSemTest
    public void testEqualsAreNotEqualDifferentMove() {
        Episode episode1 = new Episode(new SensorData(true), new Move("move1"));
        Episode episode2 = new Episode(new SensorData(true), new Move("move2"));
        assertNotEquals(episode1, episode2);
    }
    //endregion

    //region hashCode Tests
    @EpSemTest
    public void testHashcodesAreEqualNotNullSensorData() {
        Episode episode1 = new Episode(new SensorData(true), new Move("move"));
        Episode episode2 = new Episode(new SensorData(true), new Move("move"));
        assertEquals(episode1.hashCode(), episode2.hashCode());
    }
    //endregion

    //region toString Tests
    @EpSemTest
    public void toStringWithSensorData()
    {
        Episode episode = new Episode(new SensorData(true), new Move("move"));
        assertEquals("[true]move", episode.toString());
    }
    //endregion
}
