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
    private static final String goalSensor = "GOAL";
    //endregion

    //region Class Variables
    private HashMap<String, Object> data;
    //endregion

    //region Constructors
    public SensorData(boolean isGoal) {
        this.data = new HashMap<>();
        this.data.put(SensorData.goalSensor, isGoal);
    }
    //endregion

    //region Public Methods
    public void setSensor(String sensorName, Object sensorValue) throws IllegalArgumentException {
        if (sensorName == null)
            throw new IllegalArgumentException("sensorName cannot be null");
        if (sensorName.isEmpty())
            throw new IllegalArgumentException("sensorName cannot be empty");
        this.data.put(sensorName, sensorValue);
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
        Collections.sort(entries, new Comparator<Entry<String, Object>>() {
            @Override
            public int compare(Entry<String, Object> o1, Entry<String, Object> o2) {
                String leftKey = o1.getKey();
                String rightKey = o2.getKey();
                if (leftKey.equals(rightKey))
                    return 0;
                if (leftKey.equals(SensorData.goalSensor))
                    return -1;
                if (rightKey.equals(SensorData.goalSensor))
                    return 1;
                return leftKey.compareTo(rightKey);
            }
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
            if (this.data.get(key) != sensorData.data.get(key))
                return false;
        }
        return true;
    }

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
