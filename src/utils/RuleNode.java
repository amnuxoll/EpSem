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
    protected int maxDepth;
    //Potential:
    private int[] indices;
    private double expectation;

    public RuleNode(Move[] potentialMoves, int sense, int maxDepth){
        this.potentialMoves = potentialMoves;
        int alphabetSize = potentialMoves.length;
        this.sense = sense;
        frequency = 0;
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

        ArrayList<RuleNode> moveChildren = children.get(move);
        return getChildBySense(moveChildren, nextSense);
    }

    public RuleNodeGoal getGoalChild(Move move){
        if (maxDepth == 0){
            return null;
        }

        return (RuleNodeGoal) children.get(move).get(0);

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

    private ArrayList<String> toStringArray(){
        ArrayList<String> result = new ArrayList<>();

        if (children == null || children.isEmpty()) {
            return result;
        }

        for (Map.Entry<Move, ArrayList<RuleNode>> entry : children.entrySet()){
            Move move = entry.getKey();
            for (RuleNode ruleNode : entry.getValue()){
                for (String childItem : ruleNode.toStringArray())
                {
                    result.add(String.valueOf(sense) + move + " ->" + childItem);
                }
            }
        }
        result.add(sense + ": " + frequency);
        return result;
    }

}
