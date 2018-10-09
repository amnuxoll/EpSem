package framework;

import framework.Episode;

import java.util.HashMap;

/**
 * episode weights contains weights for each component of an episode
 */
public class EpisodeWeights {
    protected double actionWeight;
    protected HashMap<String, Double> sensorWeights;

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
            actionWeight= addAndCap(actionWeight, adjustValue, 0, 1);
        }
        else {
            actionWeight= addAndCap(actionWeight, -adjustValue, 0, 1);
        }

        for(String sensor : sensorWeights.keySet()) {
            if (episodeSensorsMatch(ep1, ep2, sensor)) {
                sensorWeights.put(sensor, addAndCap(getSensorWeight(sensor),adjustValue, 0, 1));
            }
            else sensorWeights.put(sensor, addAndCap(getSensorWeight(sensor),-adjustValue, 0, 1));
        }
    }

    /**
     * adds two numbers and keeps them within specified range
     *
     * @param x op1
     * @param y op2
     * @param min min value to keep range in
     * @param max max value to keep range in
     */
    private double addAndCap(double x, double y, double min, double max){
        x+= y;

        if(x > max) return max;
        if(x < min) return min;
        return x;
    }

    public boolean episodeSensorsMatch(Episode ep1, Episode ep2, String sensor) {
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
        //episode match over highest possible score, which is 1*(sensor size + action weight)
        return ep1.matchScore(ep2, this)/(sensorWeights.size()+1);
    }

    public double getActionWeight() {
        return actionWeight;
    }

    public double getSensorWeight(String sensorName) {
        if(sensorWeights.containsKey(sensorName)){
            return sensorWeights.get(sensorName);
        }

        addSensorWeight(sensorName);
        return 0.0;
    }

    private void addSensorWeight(String sensorName){
        sensorWeights.put(sensorName,0.0);
    }

    @Override
    public String toString(){
        String str= Double.toString(actionWeight);
        for(Double w : sensorWeights.values()){
            str+= (", "+w.toString());
        }

        return str;
    }
}