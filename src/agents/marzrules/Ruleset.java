package agents.marzrules;

import framework.Action;
import framework.SensorData;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;

/**
 * class Ruleset
 *
 * Stores all the agent's rules in a tree.  The depth of the rule adds
 * additional conditions from past sensors+actions.
 *
 * Example Depth 1 rule:  010a
 * Example Depth 2 rule:  110b -> 010a
 * Example Depth 3 rule:  000a -> 110b -> 010a
 * etc..
 *
 * @author Ryan Regier (created on 2/7/2019)
 */
public class Ruleset {

    private final RuleNodeRoot root;  //root node of the tree
    private RuleNode driver;

    /** current is the set of all rules in that set that match the agent's
     * experiences since last finding a goal.
     */
    protected ArrayList<RuleNode> current;
    protected Action[] alphabet;
    private int explores;
    private Heuristic heuristic;

    public Ruleset(Action[] alphabet, int maxDepth, Heuristic heuristic){
        if (alphabet == null) throw new IllegalArgumentException();
        if (alphabet.length == 0) throw new IllegalArgumentException();

        root = new RuleNodeRoot(alphabet, maxDepth);
        current = new ArrayList<>();
        current.add(root);
        this.alphabet = alphabet;
        this.heuristic = heuristic;
    }

    public Action getBestMove(){

        if(driver != null && driver.getBestAction() != null){
            if (driver.getExplore()) explores++;
            return driver.getBestAction();
        }

        double bestEV = -1;
        boolean explore = false;
        Action bestAction = alphabet[0];
        for (RuleNode node : current){
            heuristic.setGoalProbability(root.getIncreasedGoalProbability());
            Optional<Double> expectation = node.getExpectation(current, true, heuristic);
            if (expectation.isPresent() && (bestEV == -1 || expectation.get() <= bestEV)){
                if (node.getBestAction() != null) {
                    bestEV = expectation.get();
                    bestAction = node.getBestAction();
                    explore = node.getExplore();
                    driver = node;
                }
            }
        }
        //System.out.println("---");
        if (explore) explores++;
        //System.out.print(bestAction);
        return bestAction;
    }

    public ArrayList<RuleNode> getCurrent() {
        return current;
    }

    public int getExplores() { return explores; }

    public double getAvgBitStateEstimate(){
       return Math.exp(root.getAverageBits());
    }

    public double getMaxBitStateEstimate(){
        double bits = root.getMaxBits();
        return Math.exp(bits);
    }

    /**
     * sdToBits
     *
     * converts a given SensorData to an int with one bit per sensor.
     * The goal sensor is ignored.
     */
    public static int sdToBits(SensorData sensorData) {
        int result = 0;
        for (String sensor : sensorData.getSensorNames()){
            if (sensor.equals(SensorData.goalSensor)) {
                continue;
            }

            int value = (boolean) sensorData.getSensor(sensor) ? 1 : 0;
            result *= 2;
            result += value;
        }

        return result;
    }//sdToBits

    /**
     * update
     *
     * updates the ruleset to reflect a new agent experience
     *
     * @param action  agent's most recent action
     * @param sensorData the new sensorData the agent experienced as a result
     */
    public void update(Action action, SensorData sensorData){
        boolean isGoal = sensorData.isGoal();

        if (isGoal){
            root.occurs();  //TODO: remove?  This line is already at the bottom of this method
            for (RuleNode ruleNode : current){
                ruleNode.incrementMoveFrequency(action); // TODO: Prevent null action
                RuleNode node = ruleNode.getGoalChild(action);
                if (node != null) node.occurs();
            }
            current.clear();
            current.add(root);
            heuristic.setGoalProbability(root.getIncreasedGoalProbability());
            root.getExpectation(current, true, heuristic); //Update caches before saving
            root.reachedGoal(); // Cache EVs
            driver = root;
        }

        int sense = sdToBits(sensorData);
        for (int i = 0; i < current.size(); i++){
            RuleNode node = current.get(i);
            node.incrementMoveFrequency(action);
            RuleNode child = node.getNextChild(action, sense);
            if (child != null) {
                child.occurs();
            }
            current.set(i, child);
            if (driver == node){
                driver = child;
            }
        }

        current.removeAll(Collections.singleton(null));
        current.add(root);
        root.occurs();
    }


    @Override
    public String toString(){
        ArrayList<Action> actions = new ArrayList<>(
                Arrays.asList(new Action("a"), new Action("b"), new Action("a"), new Action("b"), new Action("a"))
        );
        return "Ruleset:\n" + root.toString() + "\n" + root.getGoalProbability(actions, 0);
    }

    public RuleNodeRoot getRoot() {
        return root;
    }
}
