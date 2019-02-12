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
    private int[] indicies;
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
        this.maxDepth = maxDepth;

        if (maxDepth > 0) {
            children = new HashMap<>(alphabetSize);
            for (Move move : potentialMoves) {
                ArrayList<RuleNode> list = new ArrayList<>();
                list.add(new RuleNodeGoal(potentialMoves));
                children.put(move, list);
            }
        } else {
            children = null;
        }
    }

    protected void occurs(){
        frequency++;
    }

    public RuleNode getNextChild(Move move, int nextSense){
        if (maxDepth == 0){
            return null;
        }

        if (move == null){
            throw new IllegalArgumentException("Move cannot be null");
        }

        ArrayList<RuleNode> moveChildren = children.get(move);
        if (moveChildren == null){
            throw new IllegalArgumentException("Move does not exist");
        }
        for (RuleNode ruleNode : moveChildren){
            if (ruleNode.sense == nextSense){
                return ruleNode;
            }
        }

        RuleNode child = new RuleNode(potentialMoves, nextSense, maxDepth - 1);
        moveChildren.add(child);
        return child;
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
        return String.join("\n", toStringArray());
    }

    private ArrayList<String> toStringArray(){
        ArrayList<String> result = new ArrayList<>();

        for (Map.Entry<Move, ArrayList<RuleNode>> entry : children.entrySet()){
            Move move = entry.getKey();
            for (RuleNode ruleNode : entry.getValue()){
                for (String childItem : ruleNode.toStringArray())
                {
                    result.add(String.valueOf(sense) + move + " ->" + childItem);
                }
            }
        }
        result.add(String.valueOf(sense) + ": " + frequency);
        return result;
    }

}
