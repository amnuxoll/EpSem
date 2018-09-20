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
    public void updateWeights(Episode ep1, Episode ep2){

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
