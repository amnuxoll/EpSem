package utils;

import framework.Action;
import framework.SensorData;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

/**
 * The {@link Discriminator} will track the variance of sensor values received after a given {@link Action} is
 * executed. The INVARIANT here is that the entity using this class has some reasonable understanding that the
 * {@link SensorData} that are tracked are all received when entering the same state.
 *
 * Thoughts after the fact:
 * On the one hand it kind of seems reasonable to use variance to identify the sensors that are unique or consistent
 * in a given state. However, once you know where you are in an environment it seems reasonable to think that salient
 * information could exist in the non-standard or different inputs. This could imply important changes to your
 * environment that may cause you to want to modify your default behavior. For example, you walk into your living room
 * and there's a person standing there you don't recognize. Your living room itself is consistent but you certainly
 * would react differently than if that person isn't there.
 *
 * This class helps to identify the consistent data in a given state by obfuscating the data that is unexpected.
 *
 *
 * @author Zachary Paul Faltersack
 * @version 0.95
 */
public class Discriminator {

    //region Class Variables

    private String[] sensors;

    private HashMap<Action, Match> matchingMaps = new HashMap<>();

    //endregion

    //region Public Methods

    /**
     * Update the variance of sensor data using the given sensor against the given action.
     *
     * @param sensor the {@link SensorData} received after executing the given Action and entering a known state.
     * @param action the {@link Action} that was just executed.
     */
    public void add(SensorData sensor, Action action) {
        if (this.sensors == null)
            this.sensors = sensor.getSensorNames().toArray(new String[0]);
        if (this.matchingMaps.containsKey(action))
            this.matchingMaps.get(action).update(sensor);
        else
            this.matchingMaps.put(action, new Match(sensor));
    }

    /**
     * Using sensor variance, determine if two {@link SensorData} are equivalent.
     *
     * @param sensor1 the first {@link SensorData} to compare.
     * @param sensor2 the second {@link SensorData} to compare.
     * @return true if the sensors seem equivalent based on sensor variance; otherwise false.
     */
    public boolean match(SensorData sensor1, SensorData sensor2) {
        for (String sensorName : this.sensors)
        {
            if (this.matters(sensorName) && !sensor1.getSensor(sensorName).equals(sensor2.getSensor(sensorName)))
                return false;
        }
        return true;
    }

    //endregion

    //region Private Methods

    /**
     * Determine if any {@link Match} has identified the given sensor as being highly variant, and therefore
     * unreliable when comparing {@link SensorData}.
     *
     * @param sensorName the name of the sensor to judge.
     * @return true if all {@link Match} indicate low variance.
     */
    private boolean matters(String sensorName) {
        for (Match match : this.matchingMaps.values())
        {
            if (match.isConsistent(sensorName) == false)
                return false;
        }
        return true;
    }

    //endregion

    //region Object Overrides

    @Override
    public String toString() {
        String me = "";
        for (Map.Entry entry : this.matchingMaps.entrySet())
        {
            me += "move(" + entry.getKey() + ") matching:: " + entry.getValue() + "\n";
        }
        return me;
    }

    //endregion

    //region Nested Classes

    /**
     * Tracks the variance of {@link SensorData}.
     */
    private class Match {

        //region Class Variables

        private double tolerance = 0.25;

        private HashMap<String, Consistency> consistencyMapping;

        private SensorData sensorData;

        //endregion

        //region Constructors

        /**
         * Creates an instance of a {@link Match} with a given {@link SensorData} as a template.
         *
         * @param sensorData the template {@link SensorData}.
         */
        public Match(SensorData sensorData) {
            this.sensorData = sensorData;
            this.consistencyMapping = new HashMap<>();
            for (String sensor : this.sensorData.getSensorNames())
            {
                Consistency consistency = new Consistency();
                consistency.distinctValues.add(this.sensorData.getSensor(sensor));
                this.consistencyMapping.put(sensor, consistency);
            }
        }

        //endregion

        //region Public Methods

        /**
         * Updates the {@link Consistency} that is tracking each sensor in the given {@link SensorData}.
         *
         * @param toMatch the {@link SensorData} to update from.
         */
        public void update(SensorData toMatch) {
            for (String sensor : toMatch.getSensorNames())
            {
                if (this.consistencyMapping.containsKey(sensor))
                {
                    Consistency consistency = this.consistencyMapping.get(sensor);
                    consistency.total++;
                    consistency.distinctValues.add(toMatch.getSensor(sensor));
                }
                else {
                    Consistency consistency = new Consistency();
                    consistency.distinctValues.add(toMatch.getSensor(sensor));
                    this.consistencyMapping.put(sensor, consistency);
                }
            }
        }

        /**
         * Determines whether or not the given sensor can be considered consistent based off of a constant tolerance.
         *
         * The hardcoded tolerance here is a weakness. Longer-term we would want to try and find a way to determine
         * what could be considered a "good" tolerance organically.
         *
         * @param sensorName the name of the sensor to evaluate.
         * @return true if the sensor is consistent; otherwise false.
         */
        public boolean isConsistent(String sensorName) {
            return this.consistencyMapping.get(sensorName).calculate() < this.tolerance;
        }

        //endregion

        //region Object Overrides

        @Override
        public String toString() {
            String me = "";
            for (Map.Entry<String, Consistency> entry : this.consistencyMapping.entrySet())
            {
                me += "sensor(" + entry.getKey() + ") has variance(" + entry.getValue().calculate() + ") ";
            }
            return me;
        }

        //endregion

        //region Nested Classes

        /**
         * Tracks the total "hits" and the distinct values across the hits in order to determine the variance of the
         * sensor values.
         */
        private class Consistency {

            //region Class Variables

            /** The distinct values seen for a given sensor. */
            public HashSet<Object> distinctValues = new HashSet<>();

            /** The total times a sensor has been seen. */
            public int total = 1;

            //endregion

            //region Public Methods

            /**
             * Calculates the variance of the sensor values.
             *
             * @return the number of distinct values divided by the total number of times the sensor has been seen.
             */
            public double calculate() {
                return (double)this.distinctValues.size() / this.total;
            }

            //endregion
        }

        //endregion
    }

    //endregion
}
