package framework;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 *
 * @author Zachary Paul Faltersack
 * @version 0.95
 */
public class EpisodeTest {

    //region Constructor Tests
    @Test
    public void testConstructorNullMoveThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> new Episode(null));
    }
    //endregion

    //region getMove Tests
    @Test
    public void testGetMove() {
        Move move = new Move("move");
        Episode ep = new Episode(move);
        assertSame(move, ep.getMove());
    }
    //endregion

    //region getSensorData Tests
    @Test
    public void testGetSensorDataNotSet() {
        Episode ep = new Episode(new Move("move"));
        SensorData sensorData = ep.getSensorData();
        assertNull(sensorData);
    }

    @Test
    public void testGetSensorData() {
        Episode ep = new Episode(new Move("move"));
        ep.setSensorData(new SensorData(true));
        SensorData sensorData = ep.getSensorData();
        assertTrue(sensorData.isGoal());
    }
    //endregion

    //region setSensorData Tests
    @Test
    public void setSensorDataNullSensorDataThrowsException() {
        Episode ep = new Episode(new Move("a"));
        assertThrows(IllegalArgumentException.class, () -> ep.setSensorData(null));
    }

    @Test
    public void setSensorData() {
        Episode ep = new Episode(new Move("a"));
        ep.setSensorData(new SensorData(true));
        assertTrue(ep.getSensorData().isGoal());
    }
    //endregion

    //region hitGoal Tests
    @Test
    public void hitGoal()
    {
        Episode episode = new Episode(new Move("move"));
        episode.setSensorData(new SensorData(true));;
        assertTrue(episode.hitGoal());
        episode.setSensorData(new SensorData(false));;
        assertFalse(episode.hitGoal());
    }
    //endregion

    //region equals Tests
    @Test
    public void testEqualsAreEqualNullSensorData() {
        Episode episode1 = new Episode(new Move("move"));
        Episode episode2 = new Episode(new Move("move"));
        assertEquals(episode1, episode2);
    }

    @Test
    public void testEqualsAreEqualNotNullSensorData() {
        Episode episode1 = new Episode(new Move("move"));
        episode1.setSensorData(new SensorData(true));
        Episode episode2 = new Episode(new Move("move"));
        episode2.setSensorData(new SensorData(true));
        assertEquals(episode1, episode2);
    }

    @Test
    public void testEqualsAreNotEqualDifferentSensorData() {
        Episode episode1 = new Episode(new Move("move"));
        episode1.setSensorData(new SensorData(false));
        Episode episode2 = new Episode(new Move("move"));
        episode2.setSensorData(new SensorData(true));
        assertNotEquals(episode1, episode2);
    }

    @Test
    public void testEqualsAreNotEqualDifferentMove() {
        Episode episode1 = new Episode(new Move("move1"));
        Episode episode2 = new Episode(new Move("move2"));
        assertNotEquals(episode1, episode2);
    }

    @Test
    public void testEqualsAreNotEqualLeftHasNullSensorData() {
        Episode episode1 = new Episode(new Move("move"));
        Episode episode2 = new Episode(new Move("move"));
        episode2.setSensorData(new SensorData(true));
        assertNotEquals(episode1, episode2);
    }

    @Test
    public void testEqualsAreNotEqualRightHasNullSensorData() {
        Episode episode1 = new Episode(new Move("move"));
        episode1.setSensorData(new SensorData(true));
        Episode episode2 = new Episode(new Move("move"));
        assertNotEquals(episode1, episode2);
    }
    //endregion

    //region hashCode Tests
    @Test
    public void testHashCodesAreEqualNullSensorData() {
        Episode episode1 = new Episode(new Move("move"));
        Episode episode2 = new Episode(new Move("move"));
        assertEquals(episode1.hashCode(), episode2.hashCode());
    }

    @Test
    public void testHashcodesAreEqualNotNullSensorData() {
        Episode episode1 = new Episode(new Move("move"));
        episode1.setSensorData(new SensorData(true));
        Episode episode2 = new Episode(new Move("move"));
        episode2.setSensorData(new SensorData(true));
        assertEquals(episode1.hashCode(), episode2.hashCode());
    }
    //endregion

    //region toString Tests
    @Test
    public void toStringNullSensorData()
    {
        Episode episode = new Episode(new Move("move"));
        assertEquals("move", episode.toString());
    }

    @Test
    public void toStringWithSensorData()
    {
        Episode episode = new Episode(new Move("move"));
        episode.setSensorData(new SensorData(true));
        assertEquals("move[true]", episode.toString());
    }
    //endregion
}
