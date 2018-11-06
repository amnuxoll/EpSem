package framework;

import environments.fsm.FSMDescription;
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

    @Test
    public void testEpisodeMatchScore() {
        Move move = new Move("a");
        Episode episode1 = new Episode(move);
        Episode episode2 = new Episode(move);
        SensorData data1 = new SensorData(false);
        SensorData data2 = new SensorData(false);
        data1.setSensor("sensor", 1);
        data2.setSensor("sensor", 1);
        episode1.setSensorData(data1);
        episode2.setSensorData(data2);

        EpisodeWeights weights = new EpisodeWeights();


        double score = episode1.matchScore(episode2, weights);
        assertEquals(0, score);
        weights.updateWeights(episode1, episode2, 0.5);
        assertEquals(0.5, weights.getActionWeight());

        for(String s : episode1.getSensorData().getSensorNames()) {
            assertTrue(weights.episodeSensorsMatch(episode1, episode2, s));
            assertTrue(weights.getSensorWeight(s) == 0.5);
            assertTrue(episode1.getSensorData().getSensor(s).equals(episode2.getSensorData().getSensor(s)));
        }


        score = episode1.matchScore(episode2, weights);

        assertEquals(1.0, score);
    }
}
