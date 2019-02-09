package utils;

import environments.fsm.FSMDescription;
import framework.Move;
import framework.SensorData;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;

/**
 * Created by Ryan on 2/7/2019.
 */
public class Ruleset {

    private RuleNodeRoot root;
    private ArrayList<RuleNode> current;
    private EnumSet<FSMDescription.Sensor> sensors;

    public Ruleset(Move[] alphabet, int maxDepth, EnumSet<FSMDescription.Sensor> sensors){
        root = new RuleNodeRoot(alphabet, maxDepth);
        current = new ArrayList<>();
        current.add(root);
        this.sensors = sensors;
    }

    public ArrayList<RuleNode> getCurrent(){
        return current;
    }

    public void update(Move move, SensorData sensorData){
        boolean isGoal = sensorData.isGoal();
        int sense = 0; //TODO: make real

        for (FSMDescription.Sensor sensor : sensors){
            int value = (boolean) sensorData.getSensor(sensor.toString()) ? 1 : 0;
            sense += value;
            sense *= 2;
        }

        if (isGoal){
            for (RuleNode ruleNode : current){
                RuleNodeGoal goalChild = ruleNode.getGoalChild(move);
                if (goalChild != null){
                    goalChild.occurs();
                }
            }
            current.clear();
            current.add(root);
        }

        for (int i = 0; i < current.size(); i++){
            RuleNode child = current.get(i).getNextChild(move, sense);
            if (child != null){
                child.occurs();
            }
            current.set(i, child);
        }

        current.removeAll(Collections.singleton(null));
        current.add(root);
        root.occurs();
    }
}
