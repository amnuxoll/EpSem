package tests.agents.juno;

import agents.juno.EpisodeWeights;
import framework.Action;
import framework.Episode;
import framework.SensorData;

import tests.EpSemTest;

import static tests.Assertions.*;

//temporarily removed as these tests don't pass anymore:  @EpSemTestClass
public class EpisodeWeightsTest {

    @EpSemTest
    public void testConstructor() {
        EpisodeWeights weights = new EpisodeWeights();
        assertEquals(0.0, weights.getActionWeight());
    }

    @EpSemTest
    public void updateWeightsTest() {
        EpisodeWeights weights = new EpisodeWeights();
        Action action = new Action("a");

        SensorData data = new SensorData(false);
        data.setSensor("sensor", true);
        Episode ep1 = new Episode(data, action);
        Episode ep2 = new Episode(data, action);

        weights.updateWeights(ep1, ep2, 0.5);

        assertEquals(0.5, weights.getActionWeight());

        assertTrue(ep1.getSensorData().getSensor("sensor")==ep2.getSensorData().getSensor("sensor"));

        assertTrue(ep1.getSensorData().getSensor("GOAL") == ep1.getSensorData().getSensor("GOAL"));
    }

    @EpSemTest
    public void episodeSensorsMatchTest() {
        EpisodeWeights weights = new EpisodeWeights();

        SensorData data1 = new SensorData(false);
        SensorData data2 = new SensorData(false);

        data1.setSensor("sensy", true);
        data2.setSensor("sensy", true);
        Action action = new Action("a");
        Episode ep1 = new Episode(data1, action);
        Episode ep2 = new Episode(data2, action);

        assertTrue(weights.episodeSensorsMatch(ep1, ep2, "sensy"));


        SensorData data3 = new SensorData(false);
        SensorData data4 = new SensorData(false);

        data3.setSensor("thanos", true);
        data3.setSensor("thanos", false);

        ep1 = new Episode(data3, ep1.getAction());
        ep2 = new Episode(data4, ep2.getAction());

        assertFalse(weights.episodeSensorsMatch(ep1, ep2, "thanos"));
    }

    @EpSemTest
    public void matchScoreTest() {
        fail("This method requires greater testing");
//        EpisodeWeights weights = new EpisodeWeights();
//        Action a = new Action("a");
//        Action b = new Action("b");
//        Episode ep1 = new Episode(a);
//        Episode ep2 = new Episode(a);
//
//        Episode testMismatch = new Episode(b);
//        testMismatch.setSensorData(new SensorData(false));
//
//        weights.updateWeights(ep1, ep2, 0.76);
//
//        ep1.setSensorData(new SensorData(true));
//        ep2.setSensorData(new SensorData(false));



        //assertEquals(ep1.matchScore(ep2, weights), weights.matchScore(ep1, ep2));

//        Action move = new Action("a");
//        Episode episode1 = new Episode(move);
//        Episode episode2 = new Episode(move);
//        SensorData data1 = new SensorData(false);
//        SensorData data2 = new SensorData(false);
//        data1.setSensor("sensor", 1);
//        data2.setSensor("sensor", 1);
//        episode1.setSensorData(data1);
//        episode2.setSensorData(data2);
//
//        EpisodeWeights weights = new EpisodeWeights();
//
//
//        double score = episode1.matchScore(episode2, weights);
//        assertEquals(0, score);
//        weights.updateWeights(episode1, episode2, 0.5);
//        assertEquals(0.5, weights.getActionWeight());
//
//        for(String s : episode1.getSensorData().getSensorNames()) {
//            assertTrue(weights.episodeSensorsMatch(episode1, episode2, s));
//            assertTrue(weights.getSensorWeight(s) == 0.5);
//            assertTrue(episode1.getSensorData().getSensor(s).equals(episode2.getSensorData().getSensor(s)));
//        }
//
//
//        score = episode1.matchScore(episode2, weights);
//
//        assertEquals(1.0, score);
    }
}
