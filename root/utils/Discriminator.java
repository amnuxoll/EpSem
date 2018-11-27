package utils;

import framework.Move;
import framework.SensorData;
//import javafx.util.Pair;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

public class Discriminator {

    private String[] sensors;

    private HashMap<Move, Match> matchingMaps = new HashMap<>();

    public void add(SensorData sensor, Move move)
    {
        if (this.sensors == null)
            this.sensors = sensor.getSensorNames().toArray(new String[0]);
        if (this.matchingMaps.containsKey(move))
            this.matchingMaps.get(move).update(sensor);
        else
            this.matchingMaps.put(move, new Match(sensor));
    }

    public boolean match(SensorData sensor1, SensorData sensor2)
    {
        for (String sensorName : this.sensors)
        {
            if (this.matters(sensorName) && !sensor1.getSensor(sensorName).equals(sensor2.getSensor(sensorName)))
                return false;
        }
        return true;
    }

    private boolean matters(String sensorName)
    {
        for (Match match : this.matchingMaps.values())
        {
            if (match.isConsistent(sensorName) == false)
                return false;
        }
        return true;
    }

    @Override
    public String toString()
    {
        String me = "";
        for (Map.Entry entry : this.matchingMaps.entrySet())
        {
            me += "move(" + entry.getKey() + ") matching:: " + entry.getValue() + "\n";
        }
        return me;
    }

    private class Match
    {
        private double tolerance = 0.25;

        private HashMap<String, Consistency> consistencyMapping;

        private SensorData sensorData;

        public Match(SensorData sensorData)
        {
            this.sensorData = sensorData;
            this.consistencyMapping = new HashMap<>();
            for (String sensor : this.sensorData.getSensorNames())
            {
                Consistency consistency = new Consistency();
                consistency.distinctValues.add(this.sensorData.getSensor(sensor));
                this.consistencyMapping.put(sensor, consistency);
            }
        }

        public void update(SensorData toMatch)
        {
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

        public boolean isConsistent(String sensorName)
        {
            return this.consistencyMapping.get(sensorName).calculate() < this.tolerance;
        }

        @Override
        public String toString()
        {
            String me = "";
            for (Map.Entry<String, Consistency> entry : this.consistencyMapping.entrySet())
            {
                me += "sensor(" + entry.getKey() + ") has variance(" + entry.getValue().calculate() + ") ";
            }
            return me;
        }

        private class Consistency
        {
            public HashSet<Object> distinctValues = new HashSet<>();
            public int total = 1;

            public double calculate()
            {
                return (double)this.distinctValues.size() / this.total;
            }
        }
    }
}
