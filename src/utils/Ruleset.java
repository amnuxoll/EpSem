package utils;

import framework.Move;
import framework.SensorData;

import javax.swing.text.html.Option;
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


    //region FalsificationStuff
    public Move falsify() {
        Move toTake = null;//TODO: make random?
        int bestDepth = 500; //arbitrary; right now just max depth limit
        for (int i = 0; i < current.size(); i++) {
            if ( current.get(i) instanceof RuleNodeRoot ) { // Note : I don't think we need to look at 0-deep rules, might be wrong
                continue;
            }
            if (i >= bestDepth){ // because current has one rule of each depth, i also effectively measures depth of parent RuleNode
                break;
            }
            for (Move move : alphabet) {
                RuleNode superstition = getNextTestable(current.get(i), move);
                //TODO: this assumes depth means something more intuitive than what I think is reflected in our implementation

                //Commented for compilation purposes
                /*if (superstition.depth < bestDepth){ //TODO: superstition.depth + 1 to enforce choice of simplest rules?
                    toTake = move;
                }*/
            }
        }
        return toTake;
    }
    //breadth first search for nearest testable child
    //this function is called twice (in a 2-alphabet) per node in current, which I'd like do more elegantly but alas alack
    public RuleNode getNextTestable(RuleNode parent, Move move) {
        ArrayDeque<RuleNode> queue = null;
        RuleNode p = parent;
        ArrayList<RuleNode> moveChildren = p.children.get(move);
        queue.addAll(moveChildren); //initial queue is just the specific branch of children that the caller of this function was looking at
        while(queue.size() > 0) {
            p = queue.remove();

            if (current.contains(p)){ //if p is in current, continue b/c this is redundant
                continue;
            }

            for (Move m : alphabet){ //to get each set of child nodes
                moveChildren = p.children.get(m);
                if (moveChildren.size() == 1){ //i.e. this node has never been expanded, only instantiated with goal child
                    return p;
                }
                else{
                    queue.addAll(moveChildren);
                }
            }
        }
    return null;
    }
    //endregion

    @Override
    public String toString(){
        ArrayList<Move> moves = new ArrayList<>(
                Arrays.asList(new Move("a"), new Move("b"), new Move("a"), new Move("b"), new Move("a"))
        );
        return "Ruleset:\n" + root.toString() + "\n" + root.getGoalProbability(moves, 0);
    }
}
