package utils;

import framework.Move;
import framework.SensorData;

import javax.swing.text.html.Option;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;

/**
 * Created by Ryan on 2/7/2019.
 */
public class Ruleset {

    private RuleNodeRoot root;
    private ArrayList<RuleNode> current;
    private ArrayList<Double> goalProbabilities;
    private Move[] alphabet;
    private int explores;

    public Ruleset(Move[] alphabet, int maxDepth){
        if (alphabet == null) throw new IllegalArgumentException();
        if (alphabet.length == 0) throw new IllegalArgumentException();

        root = new RuleNodeRoot(alphabet, maxDepth);
        current = new ArrayList<>();
        current.add(root);
        this.alphabet = alphabet;
    }

    public double get_heuristic(){
        double p = root.getIncreasedGoalProbability();
        double h = 1/p /*- 1*/; //TODO: Subtract 1?
        return h;
        //return 4;
    }

    public Move getBestMove(){
        double h = get_heuristic();
        double bestEV = -1;
        boolean explore = false;
        Move bestMove = alphabet[0];
        for (RuleNode node : current){
            Optional<Double> expectation = node.getExpectation(current, true, h);
            if (expectation.isPresent() && (bestEV == -1 || expectation.get() < bestEV)){
                bestEV = expectation.get();
                bestMove = node.getBestMove();
                explore = node.getExplore();
            }
        }
        if (explore) explores++;
        return bestMove;
    }

    public ArrayList<RuleNode> getCurrent(){
        return current;
    }

    public int getExplores() { return explores; }

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
                ruleNode.incrementMoveFrequency(move);
                RuleNode node = ruleNode.getGoalChild(move);
                //TODO: Fix NPE here
                node.occurs();
            }
            current.clear();
            current.add(root);
        }

        for (int i = 0; i < current.size(); i++){
            RuleNode node = current.get(i);
            node.incrementMoveFrequency(move);
            RuleNode child = node.getNextChild(move, sense);
            if (child != null) {
                child.occurs();
            }
            current.set(i, child);
        }

        current.removeAll(Collections.singleton(null));
        current.add(root);
        root.occurs();

        //setGoalProbabilities();
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
