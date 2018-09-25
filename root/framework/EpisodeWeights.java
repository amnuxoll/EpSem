package framework;

import framework.Episode;

import java.util.HashMap;

/**
 * episode weights contains weights for each component of an episode
 */
public class EpisodeWeights {
    private double actionWeight;
    private HashMap<String, Double> sensorWeights;

    /**
     * initialize moveWeight to 0
     */
    public EpisodeWeights(){
        actionWeight= 0.0;
        sensorWeights= new HashMap<>();
    }

    /**
     * updates the episode weights based on how well these episodes match
     * @param ep1
     * @param ep2
     */
    public void updateWeights(Episode ep1, Episode ep2, double adjustValue){
        if(ep1.getMove().equals(ep2.getMove())) {
            actionWeight *= adjustValue;
        }
        else {
            actionWeight *= -adjustValue;
        }

        for(String sensor : sensorWeights.keySet()) {
            if (episodeSensorsMatch(ep1, ep2, sensor)) {
                sensorWeights.put(sensor, sensorWeights.get(sensor)*adjustValue);
            }
            else sensorWeights.put(sensor, sensorWeights.get(sensor)* -adjustValue);
        }
    }

    private boolean episodeSensorsMatch(Episode ep1, Episode ep2, String sensor) {
        if(ep1.getSensorData().hasSensor(sensor) && ep2.getSensorData().hasSensor(sensor))
            return ep1.getSensorData().getSensor(sensor).equals(ep2.getSensorData().getSensor(sensor));

        return !ep1.getSensorData().hasSensor(sensor) && !ep2.getSensorData().hasSensor(sensor);
    }

    /**
     *  c
     * @param ep1
     * @param ep2
     * @return
     */
    public double matchScore(Episode ep1, Episode ep2){
        return ep1.matchScore(ep2, this);
    }

    public double getActionWeight() {
        return actionWeight;
    }

    public double getSensorWeight(String sensorName) {
        if(sensorWeights.containsKey(sensorName)){
            return sensorWeights.get(sensorName);
        }

        sensorWeights.put(sensorName,0.0);
        return 0.0;
    }
}
