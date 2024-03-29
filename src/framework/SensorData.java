package framework;

import java.util.*;
import java.util.Map.Entry;

/**
 * Wraps the sensors received from actions in an environment.
 *
 * @author Zachary Paul Faltersack
 * @version 0.95
 */
public class SensorData {

    //region Static Variables

    /** the goal sensor name. */
    public static final String goalSensor = "GOAL";

    //endregion

    //region Class Variables

    private HashMap<String, Object> data;

    //endregion

    //region Constructors

    /**
     * Creates an instance of a {@link SensorData} configured with the goal sensor either on or off.
     *
     * @param isGoal indicates whether or not to set the goal sensor on.
     */
    public SensorData(boolean isGoal) {
        this.data = new HashMap<>();
        this.data.put(SensorData.goalSensor, isGoal);
    }

    /**
     * copy constructor
     *
     * @param other the {@link SensorData} to copy.
     */
    @SuppressWarnings("unchecked")
    public SensorData(SensorData other) {
        this.data = (HashMap<String, Object>)(other.data.clone());
    }

    /**
     * createEmpty
     *
     * creates an empty SensorData.
     *
     * There is no ctor to do this because it's an iffy thing to do.  So
     * you have to explicitly call this method to prove you really
     * want an empty one.
     */
    public static SensorData createEmpty() {
        SensorData result = new SensorData(false);
        result.data.clear();
        return result;
    }//createEmpty

    //endregion

    //region Public Methods

    /**
     * @return the number of entries
     */
    public int size() {
        return this.data.size();
    }

    /**
     * Assigns a sensor value to the given sensor name.
     *
     * @param sensorName the name of the sensor to set.
     * @param sensorValue the value of the sensor to set.
     * @throws IllegalArgumentException
     */
    public void setSensor(String sensorName, Object sensorValue) throws IllegalArgumentException {
        if (sensorName == null)
            throw new IllegalArgumentException("sensorName cannot be null");
        if (sensorValue == null)
            throw new IllegalArgumentException("sensorValue cannot be null");
        if (sensorName.isEmpty())
            throw new IllegalArgumentException("sensorName cannot be empty");
        this.data.put(sensorName, sensorValue);
    }

    /**
     * Removes the given sensor.
     *
     * @param sensorName the name of the sensor to remove.
     * @throws IllegalArgumentException
     */
    public void removeSensor(String sensorName) throws IllegalArgumentException {
        if (sensorName == null)
            throw new IllegalArgumentException("sensorName cannot be null");
        if (sensorName.isEmpty())
            throw new IllegalArgumentException("sensorName cannot be empty");
        this.data.remove(sensorName);
    }

    /**
     * Gets the value of the sensor with the given name.
     *
     * @param sensorName the name of the sensor to retrieve.
     * @return the value of the sensor.
     * @throws IllegalArgumentException
     */
    public Object getSensor(String sensorName) throws IllegalArgumentException {
        if (sensorName == null)
            throw new IllegalArgumentException("sensorName cannot be null");
        if (sensorName.isEmpty())
            throw new IllegalArgumentException("sensorName cannot be empty");
        return this.data.get(sensorName);
    }

    /**
     * Determines whether or not the indicated sensor is set on this {@link SensorData}.
     *
     * @param sensorName the name of the sensor to search for.
     * @return true if the sensor is set; otherwise false.
     * @throws IllegalArgumentException
     */
    public boolean hasSensor(String sensorName) throws IllegalArgumentException {
        if (sensorName == null)
            throw new IllegalArgumentException("sensorName cannot be null");
        if (sensorName.isEmpty())
            throw new IllegalArgumentException("sensorName cannot be empty");
        return this.data.containsKey(sensorName);
    }

    /**
     * Determines if the goal was hit.
     *
     * @return true if the goal sensor was triggered; otherwise false.
     */
    public boolean isGoal() {
        return ((boolean)this.data.get(SensorData.goalSensor));
    }

    /**
     * getEntriesSorted
     *
     * @return a list of the entries in this.data sorted by sensor labels
     */
    private ArrayList<Entry<String, Object>> getEntriesSorted() {
        ArrayList<Entry<String, Object>> entries = new ArrayList<>(this.data.entrySet());
        entries.sort((o1, o2) -> {
            String leftKey = o1.getKey();
            String rightKey = o2.getKey();
            if (leftKey.equals(rightKey))
                return 0;
            if (leftKey.equals(SensorData.goalSensor))  //always at the end
                return 1;
            if (rightKey.equals(SensorData.goalSensor))
                return -1;
            return leftKey.compareTo(rightKey);
        });
        return entries;
    }

    /**
     * toBitSet
     *
     * Converts the SensorData into a java.util.BitSet
     *
     * Caveat:  If your SensorData is not a list of Boolean values this will not be helpful
     */
    public BitSet toBitSet() {
        ArrayList<Entry<String, Object>> entries = getEntriesSorted();
        BitSet result = new BitSet(entries.size());
        int bitIndex = 0;
        for (Entry entry : entries) {
            Object obj = entry.getValue();
            if (obj instanceof Boolean) {
                boolean val = (Boolean) obj;
                if (val) result.set(bitIndex);
            }
            bitIndex++;
        }

        return result;
    }//toBitSet




    /**
     * toStringShort
     *
     * Converts the SensorData into a string with boolean values represented as 0 or 1
     *
     * Caveat:  If your SensorData is not a list of Boolean values this will not be helpful
     */
    public String toStringShort() {
        ArrayList<Entry<String, Object>> entries = getEntriesSorted();
        StringBuilder result = new StringBuilder();
        for (Entry entry : entries) {
            Object obj = entry.getValue();
            if (obj instanceof Boolean) {
                Boolean val = (Boolean) obj;
                result.append(val ? 1 : 0);
            }
        }

        return result.toString();
    }//toStringShort



    /**
     * Performs a pretty-print of the sensor set and its values by formatting the output in
     * sorted key-value pairs.
     *
     * @param includeSensorLabels if true, the sensor names will be included in the string.
     * @return the string representation of this {@link SensorData}.
     */
    public String toString(boolean includeSensorLabels) {
        ArrayList<Entry<String, Object>> entries = getEntriesSorted();

        StringBuilder result = new StringBuilder("[");
        for (Entry entry : entries) {
            if (includeSensorLabels) {
                result.append(entry.getKey());
                result.append(":");
            }
            //If the value is a boolean represent as 0 or 1
            Object obj = entry.getValue();
            if (obj instanceof Boolean) {
                Boolean val = (Boolean) obj;
                result.append(val ? 1 : 0);
            } else {
                result.append(obj);
            }

            result.append(";");
        }
        // remove the unnecessary final semi-colon (if present)
        if (result.length() > 1) {
            result.setLength(result.length() - 1);
        }
        
        result.append("]");
        return result.toString();
    }

    @Override
    public String toString() {
        return this.toString(true);
    }


    /**
     * Gets all the sensors that are configured in this {@link SensorData}.
     *
     * @return the set of all sensor names.
     */
    public Set<String> getSensorNames(){
        return data.keySet();
    }

    /** @return true if there are no sensor values */
    public boolean isEmpty() {
        return data.size() == 0;
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


    //endregion
}
