package framework;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import framework.Episode;

public class EpisodeTest {

    // constructor Tests
    @Test
    public void testConstructorNullMoveThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> new Episode(null));
    }

    // getMove Tests
    @Test
    public void testGetMove() {
        Episode ep = new Episode(new Move("move"));
        Move move = ep.getMove();
        assertEquals("move", move.getName());
    }

    // getSensorData Tests
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

    // setSensorData Tests
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

    // equals Tests
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

    // hashCode Tests

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
}
