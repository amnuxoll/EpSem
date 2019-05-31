package framework;

import java.util.*;
import java.util.Map.Entry;

/**
 *
 * @author Zachary Paul Faltersack
 * @version 0.95
 */
public class SensorData {
    //region Static Variables
    public static final String goalSensor = "GOAL";
    //endregion

    //region Class Variables
    private HashMap<String, Object> data;
    //endregion

    //region Constructors
    public SensorData(boolean isGoal) {
        this.data = new HashMap<>();
        this.data.put(SensorData.goalSensor, isGoal);
    }
    /**
     * copy constructor
     */
    @SuppressWarnings("unchecked")
    public SensorData(SensorData other) {
        this.data = (HashMap<String, Object>)(other.data.clone());
    }
    
    //endregion

    //region Public Methods
    public void setSensor(String sensorName, Object sensorValue) throws IllegalArgumentException {
        if (sensorName == null)
            throw new IllegalArgumentException("sensorName cannot be null");
        if (sensorValue == null)
            throw new IllegalArgumentException("sensorValue cannot be null");
        if (sensorName.isEmpty())
            throw new IllegalArgumentException("sensorName cannot be empty");
        this.data.put(sensorName, sensorValue);
    }

    public void removeSensor(String sensorName) throws IllegalArgumentException {
        if (sensorName == null)
            throw new IllegalArgumentException("sensorName cannot be null");
        if (sensorName.isEmpty())
            throw new IllegalArgumentException("sensorName cannot be empty");
        this.data.remove(sensorName);
    }

    public Object getSensor(String sensorName) throws IllegalArgumentException {
        if (sensorName == null)
            throw new IllegalArgumentException("sensorName cannot be null");
        if (sensorName.isEmpty())
            throw new IllegalArgumentException("sensorName cannot be empty");
        return this.data.get(sensorName);
    }

    public boolean hasSensor(String sensorName) throws IllegalArgumentException {
        if (sensorName == null)
            throw new IllegalArgumentException("sensorName cannot be null");
        if (sensorName.isEmpty())
            throw new IllegalArgumentException("sensorName cannot be empty");
        return this.data.containsKey(sensorName);
    }

    public boolean isGoal() {
        return ((boolean)this.data.get(SensorData.goalSensor));
    }

    public String toString(boolean includeSensorLabels) {
        ArrayList<Entry<String, Object>> entries = new ArrayList<>(this.data.entrySet());
        entries.sort((o1, o2) -> {
            String leftKey = o1.getKey();
            String rightKey = o2.getKey();
            if (leftKey.equals(rightKey))
                return 0;
            if (leftKey.equals(SensorData.goalSensor))
                return -1;
            if (rightKey.equals(SensorData.goalSensor))
                return 1;
            return leftKey.compareTo(rightKey);
        });

        StringBuilder stringBuilder = new StringBuilder("[");
        for (Entry entry : entries) {
            if (includeSensorLabels) {
                stringBuilder.append(entry.getKey());
                stringBuilder.append(":");
            }
            stringBuilder.append(entry.getValue());
            stringBuilder.append(";");
        }
        // safe because there is always at least the goal sensor
        stringBuilder.setLength(stringBuilder.length() - 1);
        stringBuilder.append("]");
        return stringBuilder.toString();
    }

    public Set<String> getSensorNames(){
        return data.keySet();
    }
    //endregion

    //region Object Overrides
    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof SensorData)) {
            return false;
        }
        SensorData sensorData = (SensorData)o;
        if (this.data.size() != sensorData.data.size())
            return false;
        for (String key : this.data.keySet()) {
            if (!sensorData.data.containsKey(key))
                return false;
            if (! this.data.get(key).equals(sensorData.data.get(key)))
                return false;
        }
        return true;
    }

    /**
     * @return true if all the values in a given sensor data match this one
     */
    public boolean contains(SensorData subset) {
        if (subset == null) return false;
        if (subset == this) return true;

        //quick size comparison to save us some time
        if (this.data.size() < subset.data.size())
            return false;

        //check for contains
        for (String key : subset.data.keySet()) {
            if (!this.data.containsKey(key))
                return false;
            if (! this.data.get(key).equals(subset.data.get(key)))
                return false;
        }
        return true;
    }

    /**
     * @return a new SensorData object that contains only the sensor:value pairs
     * that are both this object and another given SensorData object
     */
    public SensorData intersection(SensorData other) {
        if (other == null) return null;
        if (other == this) return this;

        //create the intersection
        SensorData result = new SensorData(false);
        result.removeSensor(SensorData.goalSensor);
        for (String key : other.data.keySet()) {
            if (this.data.containsKey(key) && (this.data.get(key).equals(other.data.get(key)))) {
                result.setSensor(key, this.data.get(key));
            }
        }

        return result;
    }//intersection
    
    @Override
    public int hashCode() {
        int hashcode = 0;
        for(Entry<String, Object> entry : this.data.entrySet()) {
            hashcode += Objects.hash(entry.getKey(), entry.getValue());
        }
        return hashcode;
    }


    @Override
    public String toString() {
        return this.toString(false);
    }
    //endregion
}
