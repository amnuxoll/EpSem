package utils;

import framework.Move;

import java.util.*;

/**
 * Created by Ryan on 1/28/2019.
 */
public class RuleNode {

    private static RuleNodeRoot root;

    protected int sense;
    private HashMap<Move, ArrayList<RuleNode>> children;
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

    protected void occurs(){
        frequency++;
    }

    protected RuleNode getChildBySense(ArrayList<RuleNode> children, int nextSense) {
        for (RuleNode ruleNode : children){
            if (ruleNode.sense == nextSense){
                return ruleNode;
            }
        }

        RuleNode child = new RuleNode(potentialMoves, nextSense, maxDepth - 1);
        children.add(child);
        return child;
    }

    public RuleNode getNextChild(Move move, int nextSense){
        if (maxDepth == 0){
            return null;
        }

        if (move == null){
            throw new IllegalArgumentException("Move cannot be null");
        }

        int index = -1;
        for (int i = 0; i < potentialMoves.length; i++){
            if (potentialMoves[i].equals(move)){
                index = i;
                break;
            }
        }

        if (index == -1){
            throw new IllegalArgumentException("Move does not exist");
        }

        moveFrequencies[index]++;

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

        return (RuleNodeGoal) moveChildren.get(0);

    }

    public int getSense(){
        return sense;
    }

    @Override
    public String toString(){
        ArrayList<String> stringArrayList = toStringArray();
        if (stringArrayList.isEmpty()) {
            return "";
        }

        return String.join("\n", stringArrayList);
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
