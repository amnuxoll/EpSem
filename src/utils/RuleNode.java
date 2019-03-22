package utils;

import framework.Move;

import java.util.*;

/**
 * Created by Ryan on 1/28/2019.
 */
public class RuleNode {
    private static RuleNodeRoot root;

    protected int sense;
    protected HashMap<Move, ArrayList<RuleNode>> children;
    protected int frequency;
    protected final Move[] potentialMoves;
    private int[] moveFrequencies;
    protected int maxDepth;
    //Potential:
    private int[] indices;
    private double expectation;

    public RuleNode(Move[] potentialMoves, int sense, int maxDepth){

        if (potentialMoves == null){
            throw new IllegalArgumentException("Cannot have null move set");
        } else if (potentialMoves.length == 0){
            throw new IllegalArgumentException("Cannot have empty move set");
        }

        if (maxDepth < 0){
            throw new IllegalArgumentException("Cannot have negative max depth");
        }

        this.potentialMoves = potentialMoves;
        int alphabetSize = potentialMoves.length;
        this.sense = sense;
        frequency = 0;
        moveFrequencies = new int[alphabetSize];
        this.maxDepth = maxDepth;

        if (maxDepth > 0) {
            children = new HashMap<>(alphabetSize);
            for (Move move : potentialMoves) {
                ArrayList<RuleNode> list = new ArrayList<>(
                        Collections.singletonList(new RuleNodeGoal(potentialMoves))
                );
                children.put(move, list);
            }
        } else {
            children = null;
        }
    }

    protected int getMoveFrequency(Move move){
        int index = Arrays.asList(potentialMoves).indexOf(move);
        if (index == -1) {
            return -1;
        }

        return moveFrequencies[index];
    }

    protected int incrementMoveFrequency(Move move){
        int index = Arrays.asList(potentialMoves).indexOf(move);
        if (index == -1) {
            return -1;
        }

        int frequency = moveFrequencies[index]++;
        return frequency;
    }

    public double getGoalProbability(ArrayList<Move> moves, int moveIdx) {
        if (maxDepth == 0) {
            return 0;
        }

        if (moves == null){
            throw new IllegalArgumentException("moves cannot be null");
        }

        if (moveIdx >= moves.size()) {
            return 0;
        } else if (moveIdx < 0){
            throw new IllegalArgumentException("moveIdx cannot be negative");
        }

        Move move = moves.get(moveIdx);
        int frequency = getMoveFrequency(move);
        if (frequency == -1) {
            throw new IllegalArgumentException("Move not in alphabet!");
        } else if (frequency == 0) {
            return 0;
        }

        double probability = 0;
        ArrayList<RuleNode> children = this.children.get(move);
        for (RuleNode child : children) {
            double childGoalProbability = child.getGoalProbability(moves, moveIdx+1);
            probability += child.frequency * childGoalProbability;
        }
        return probability / frequency;
    }

    protected void occurs(){
        frequency++;
    }

    protected RuleNode getChildBySense(ArrayList<RuleNode> children, int nextSense) {
        for (RuleNode ruleNode : children){
            if (ruleNode.sense == nextSense){
                ruleNode.occurs();
                return ruleNode;
            }
        }

        RuleNode child = new RuleNode(potentialMoves, nextSense, maxDepth - 1);
        children.add(child);
        child.occurs();
        return child;
    }

    public RuleNode getNextChild(Move move, int nextSense){
        if (maxDepth == 0){
            return null;
        }

        if (move == null){
            throw new IllegalArgumentException("Move cannot be null");
        }

        int frequency = incrementMoveFrequency(move);
        if (frequency == -1){
            throw new IllegalArgumentException("Move does not exist");
        }

        ArrayList<RuleNode> moveChildren = children.get(move);

        return getChildBySense(moveChildren, nextSense);
    }

    public RuleNodeGoal getGoalChild(Move move){
        if (maxDepth == 0){
            return null;
        }

        if (move == null){
            throw new IllegalArgumentException("Move cannot be null");
        }

        ArrayList<RuleNode> moveChildren = children.get(move);
        if (moveChildren == null){
            throw new IllegalArgumentException("Move not in alphabet");
        }

        //TODO: Defer move not in alphabet to this call
        incrementMoveFrequency(move);

        RuleNodeGoal goal = (RuleNodeGoal) moveChildren.get(0);
        goal.occurs();
        return goal;

    }

    public int getSense(){
        return sense;
    }

    @Override
    public String toString(){
        return this.toString(true);
    }

    public String toString(Boolean includeNewline) {
        ArrayList<String> stringArrayList = toStringArray();
        if (stringArrayList.isEmpty()) {
            return "";
        }
        if (includeNewline)
            return String.join("\n", stringArrayList);
        return String.join("", stringArrayList);
    }

    protected ArrayList<String> toStringArray(){
        ArrayList<String> result = new ArrayList<>();

        result.add(sense + ": " + frequency);
        if (maxDepth != 0) {
            for (int i = 0; i < potentialMoves.length; i++) {
                result.add(sense + potentialMoves[i].toString() + ": " + moveFrequencies[i]);
            }
        }

        if (children == null || children.isEmpty()) {
            return result;
        }

        for (Map.Entry<Move, ArrayList<RuleNode>> entry : children.entrySet()){
            Move move = entry.getKey();
            for (RuleNode ruleNode : entry.getValue()){
                for (String childItem : ruleNode.toStringArray())
                {
                    result.add(String.valueOf(sense) + move + " -> " + childItem);
                }
            }
        }
        return result;
    }

}
