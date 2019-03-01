package utils;

import framework.Move;
import framework.SensorData;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

/**
 * Created by Ryan on 2/7/2019.
 */
public class Ruleset {

    private RuleNodeRoot root;
    private ArrayList<RuleNode> current;
    private ArrayList<Double> goalProbabilities;
    private Move[] alphabet;

    public Ruleset(Move[] alphabet, int maxDepth){
        if (alphabet == null) throw new IllegalArgumentException();
        if (alphabet.length == 0) throw new IllegalArgumentException();

        root = new RuleNodeRoot(alphabet, maxDepth);
        current = new ArrayList<>();
        current.add(root);
        this.alphabet = alphabet;
    }

    public ArrayList<RuleNode> getCurrent(){
        return current;
    }

    public ArrayList<Double> getGoalProbabilities() {
        return goalProbabilities;
    }

    public void update(Move move, SensorData sensorData){
        boolean isGoal = sensorData.isGoal();
        int sense = 0;

        for (String sensor : sensorData.getSensorNames()){
            if (sensor.equals(SensorData.goalSensor)) {
                continue;
            }

            int value = (boolean) sensorData.getSensor(sensor) ? 1 : 0;
            sense *= 2;
            sense += value;
        }

        if (isGoal){
            for (RuleNode ruleNode : current){
                ruleNode.getGoalChild(move);
            }
            current.clear();
            current.add(root);
        }

        for (int i = 0; i < current.size(); i++){
            RuleNode child = current.get(i).getNextChild(move, sense);
            current.set(i, child);
        }

        current.removeAll(Collections.singleton(null));
        current.add(root);
        root.occurs();

        setGoalProbabilities();
    }

    public double evaluateMoves(Move[] moves) {
        ArrayList<Move> movesList = new ArrayList<>(Arrays.asList(moves));
        return root.getGoalProbability(movesList, 0);
    }

    private void setGoalProbabilities() {
        goalProbabilities = new ArrayList<>();
        for (RuleNode node : current) {
            ArrayList<Move> moves = new ArrayList<>(Arrays.asList(node.potentialMoves));
            goalProbabilities.add(node.getGoalProbability(moves, 0));
        }
    }

    @Override
    public String toString(){
        ArrayList<Move> moves = new ArrayList<>(
                Arrays.asList(new Move("a"), new Move("b"), new Move("a"), new Move("b"), new Move("a"))
        );
        return "Ruleset:\n" + root.toString() + "\n" + root.getGoalProbability(moves, 0);
    }
}
