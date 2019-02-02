package utils;

import framework.Episode;
import framework.Move;
import framework.SensorData;

import java.util.*;

/**
 * Created by Ryan on 1/28/2019.
 */
public class Rule {
    private SensorData sensorData;
    private HashMap<Move, ArrayList<Rule>> children;
    private int frequency;
    private Move[] potentialMoves;
    //Potential:
    private int[] indicies;
    private double expectation;

    public Rule(Move[] potentialMoves, SensorData sensors){
        int alphabetSize = potentialMoves.length;
        children = new HashMap<>(alphabetSize);
        for (Move move : potentialMoves){
            children.put(move, new ArrayList<>());
        }
        sensorData = sensors;
        frequency = 0;
    }

    public void occurs(){
        frequency++;
    }

    public void sequenceOccurs(List<Episode> previous, SensorData current){
        occurs();

        //Base case
        if (previous.size() == 1){
            Move move = previous.get(0).getMove();
            Rule child = getChild(move, current);
            if (child != null){
                child.occurs();
            } else {
                Rule newChild = new Rule(potentialMoves, current);
                newChild.occurs();
                children.get(move).add(newChild);
            }
            return;
        }

        //Recursive case
        Move move = previous.get(0).getMove();
        SensorData nextSensor = previous.get(1).getSensorData();
        Rule child = getChild(move, nextSensor);
        if (child == null){
            child = new Rule(potentialMoves, nextSensor);
            children.get(move).add(child);
        }
        child.sequenceOccurs(previous.subList(1, previous.size()), current);

    }

    private Rule getChild(Move move, SensorData sensorData){
        ArrayList<Rule> moveChildren = children.get(move);
        for (Rule rule : moveChildren){
            if (rule.sensorData == sensorData){
                return rule;
            }
        }
        return null;
    }

    public SensorData getSensorData(){
        return sensorData;
    }

    @Override
    public String toString(){
        return String.join("\n", toStringArray());
    }

    private ArrayList<String> toStringArray(){
        ArrayList<String> result = new ArrayList<>();

        for (Map.Entry<Move, ArrayList<Rule>> entry : children.entrySet()){
            Move move = entry.getKey();
            for (Rule rule : entry.getValue()){
                for (String childItem : rule.toStringArray())
                {
                    result.add(this.sensorData.toString(false) + move + " ->" + childItem);
                }
            }
        }
        result.add(sensorData.toString(false) + ": " + frequency);
        return result;
    }

}
