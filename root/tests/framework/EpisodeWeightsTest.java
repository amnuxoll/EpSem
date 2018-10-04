package framework;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class EpisodeWeightsTest {

    @Test
    public void testConstructor() {
        EpisodeWeights weights = new EpisodeWeights();
        assertEquals(0.0, weights.getActionWeight());
    }

    @Test
    public void updateWeightsTest() {
        EpisodeWeights weights = new EpisodeWeights();
        Move move = new Move("a");
        Episode ep1 = new Episode(move);
        Episode ep2 = new Episode(move);

        SensorData data = new SensorData(false);
        data.setSensor("sensor", true);

        ep1.setSensorData(data);
        ep2.setSensorData(data);

        weights.updateWeights(ep1, ep2, 0.5);

        assertEquals(0.5, weights.getActionWeight());

        assertTrue(ep1.getSensorData().getSensor("sensor")==ep2.getSensorData().getSensor("sensor"));

        assertTrue(ep1.getSensorData().getSensor("GOAL") == ep1.getSensorData().getSensor("GOAL"));
    }

    @Test
    public void episodeSensorsMatchTest() {
        EpisodeWeights weights = new EpisodeWeights();
        Move move = new Move("a");
        Episode ep1 = new Episode(move);
        Episode ep2 = new Episode(move);

        SensorData data1 = new SensorData(false);
        SensorData data2 = new SensorData(false);

        data1.setSensor("sensy", true);
        data2.setSensor("sensy", true);

        ep1.setSensorData(data1);
        ep2.setSensorData(data2);

        assertTrue(weights.episodeSensorsMatch(ep1, ep2, "sensy"));


        SensorData data3 = new SensorData(false);
        SensorData data4 = new SensorData(false);

        data3.setSensor("thanos", true);
        data3.setSensor("thanos", false);

        ep1.setSensorData(data3);
        ep2.setSensorData(data4);

        assertFalse(weights.episodeSensorsMatch(ep1, ep2, "thanos"));
    }

    @Test
    public void matchScoreTest() {
        EpisodeWeights weights = new EpisodeWeights();
        Move move = new Move("a");
        Episode ep1 = new Episode(move);
        Episode ep2 = new Episode(move);

        weights.updateWeights(ep1, ep2, 0.76);

        ep1.setSensorData(new SensorData(true));
        ep2.setSensorData(new SensorData(false));

        assertEquals(ep1.matchScore(ep2, weights)/2, weights.matchScore(ep1, ep2));
    }
}
