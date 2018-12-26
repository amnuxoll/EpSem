package agents.juno;

import environments.fsm.FSMDescription;
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

        for(String s : ep1.getSensorData().getSensorNames()){
            if(!sensorWeights.containsKey(s)){
                sensorWeights.put(s, 0.0);
            }
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
     *  compute an normalized match score between two episodes
     *
     * @param ep1
     * @param ep2
     * @return a normalized match in range [0,1]
     */
    public double matchScore(Episode ep1, Episode ep2){
        double score= 0;
        if(ep2.getMove().equals(ep1.getMove())) {
            score+= this.getActionWeight();
        }
        //find match of each sensor
        for(String sensorName : ep1.getSensorData().getSensorNames()){
            //if sensor values match
            if(ep1.getSensorData().getSensor(sensorName).equals(ep2.getSensorData().getSensor(sensorName))){
                score+= this.getSensorWeight(sensorName);
            }
        }
        double sum= this.sumEntries();
        return sum == 0 ? 0 : score/sum;
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

    /**
     * calculates the average entry in the list
     * @return the average entry
     */
    public double averageEntry(){
        //return average (+1 for action weight)
        return sumEntries()/size();
    }

    public int size(){
        return sensorWeights.size()+1;
    }

    public double sumEntries(){
        double sum= 0;
        for(Double weight : sensorWeights.values()){
            sum+= weight;
        }

        sum+= actionWeight;

        return sum;
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
