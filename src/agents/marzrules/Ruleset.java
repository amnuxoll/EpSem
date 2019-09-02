package agents.marzrules;

import framework.Action;
import framework.SensorData;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;
import java.util.ArrayDeque;

/**
 * Created by Ryan on 2/7/2019.
 */
public class Ruleset {

    private RuleNodeRoot root;
    private RuleNode driver;
    private ArrayList<RuleNode> current;
    //private ArrayList<Double> goalProbabilities;
    private Action[] alphabet;
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

    //for popper-bot
    public Ruleset(Action[] alphabet, int maxDepth){
        if (alphabet == null) throw new IllegalArgumentException();
        if (alphabet.length == 0) throw new IllegalArgumentException();

        root = new RuleNodeRoot(alphabet, maxDepth);
        current = new ArrayList<>();
        current.add(root);
        driver = root;
        this.alphabet = alphabet;
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
            //double h = heuristic.getHeuristic(root.getIncreasedGoalProbability(), node.getCurrentDepth());
            Optional<Double> expectation = node.getExpectation(current, true, heuristic);
            //if (expectation.isPresent()) System.out.print("" + expectation.get() +",");
            //else System.out.print(",");
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

    public ArrayList<RuleNode> getCurrent(){
        return current;
    }

    public int getExplores() { return explores; }

    /*public ArrayList<Double> getGoalProbabilities() {
        return goalProbabilities;
    }*/

    public void update(Action action, SensorData sensorData){
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
                ruleNode.incrementMoveFrequency(action); // TODO: Prevent null action
                RuleNode node = ruleNode.getGoalChild(action);
                if (node != null) node.occurs();
            }
            current.clear();
            current.add(root);
            //System.out.print("G");
            root.reachedGoal();
            driver = root;
        }

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

        //setGoalProbabilities();
    }

    public double evaluateMoves(Action[] actions) {
        ArrayList<Action> movesList = new ArrayList<>(Arrays.asList(actions));
        return root.getGoalProbability(movesList, 0);
    }

    /*private void setGoalProbabilities() {
        goalProbabilities = new ArrayList<>();
        for (RuleNode node : current) {
            ArrayList<Action> actions = new ArrayList<>(Arrays.asList(node.potentialActions));
            goalProbabilities.add(node.getGoalProbability(actions, 0));
        }
    }*/


    //region PopperBotStuff
    public Action falsify() {
        Action toTake = null;//TODO: make random?
        int bestDepth = 500; //arbitrary; right now just max depth limit
        for (int i = 0; i < current.size(); i++) {
            if ( current.get(i) instanceof RuleNodeRoot ) { // Note : I don't think we need to look at 0-deep rules, might be wrong
                continue;
            }
            if (i >= bestDepth){ // because current has one rule of each depth, i also effectively measures depth of parent RuleNode
                break;
            }

            for (Action action : alphabet) {
                RuleNode superstition = getNextTestable(current.get(i), action);
                //TODO: this assumes depth means something more intuitive than what I think is reflected in our implementation

                if (superstition.getCurrentDepth() < bestDepth){ //TODO: superstition.depth + 1 to enforce choice of simplest rules?
                    toTake = action;
                }
            }
        }
        return toTake;
    }
    //breadth first search for nearest testable child
    //this function is called twice (in a 2-alphabet) per node in current, which I'd like do more elegantly but alas alack

    public RuleNode getNextTestable(RuleNode parent, Action action) {
        ArrayDeque<RuleNode> queue = new ArrayDeque<>();
        RuleNode p = parent;
        ArrayList<RuleNode> moveChildren = p.children.get(action);
        queue.addAll(moveChildren); //initial queue is just the specific branch of children that the caller of this function was looking at
        while(queue.size() > 0) {
            p = queue.remove();

            if (current.contains(p)){ //if p is in current, continue b/c this is redundant
                continue;
            }

            for (Action m : alphabet){ //to get each set of child nodes
                moveChildren = p.children.get(m);
                if (moveChildren.size() == 1){ //i.e. this node has never been expanded, only instantiated with goal child
                    return p;
                } else {
                    queue.addAll(moveChildren);
                }
            }
        }
    return null;
    }
    //endregion

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
