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
    // maxDepth is decremented as you move down the tree; at the depth limit this value will be 0.
    // can be understood as "how much deeper does the tree go beyond this node".
    protected int maxDepth;
    //Potential:
    private int[] indices;
    private double expectation;

    // constructor
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
        // Note that frequency is seemingly recorded in two places--calculating accurate probabilities for prospective
        // moves needs us to keep track of the frequencies that this node was visited and how often each move was taken
        // from the node.
        frequency = 0;
        moveFrequencies = new int[alphabetSize];
        this.maxDepth = maxDepth;

        if (maxDepth > 0) {//if maxDepth is positive, we are not yet at depth limit
            children = new HashMap<>(alphabetSize);
            for (Move move : potentialMoves) {
                //initialize the list for a given possible move and make the goal node--which is a special case--the first
                // element. This means that the first element is *always* the goal child for every possible move, which is handy.
                ArrayList<RuleNode> list = new ArrayList<>(
                        Collections.singletonList(new RuleNodeGoal(potentialMoves))
                );
                children.put(move, list);
            }
        } else { //at the depth limit, don't build more nodes.
            children = null;
        }
    }

    protected int getMoveFrequency(Move move){
        int index = Arrays.asList(potentialMoves).indexOf(move);
        if (index == -1) {
            return -1;
        }
        //indexing of moveFrequencies is constructed to be consistent with ordering of the potential moves the agent can
        //take, thus assumes that at any two points in memory the same moves were available to the agent as were in the beginning
        return moveFrequencies[index];
    }

    protected int incrementMoveFrequency(Move move){
        int index = Arrays.asList(potentialMoves).indexOf(move);
        if (index == -1) {
            return -1;
        }
        //see getMoveFrequency above
        int frequency = moveFrequencies[index]++;
        return frequency;
    }

    // The "goal probability" of a sequence of moves is the estimated likelihood that a goal will be reached by the end
    // of the sequence.
    // Parameters: a sequence of moves, and an index in that sequence (for recursive calls)

    // E.g. if moves = aaba, and getGoalProbability(moves, 0) returns .55, our model has predicted that
    // taking those moves has a 55% chance of *having reached the goal* by the time the last 'a' move is taken (or sooner).

    // likewise, getGoalProbability(moves, 3) would return the estimated probability of reaching the goal just by taking that
    // last 'a' move.
    public double getGoalProbability(ArrayList<Move> moves, int moveIdx) {
        // CASE 1:
        // if you have no children, i.e. if you have reached the bottom of the tree
        if (maxDepth == 0) {
            return 0;
        }
        // CASE 2:
        // you've reached the end of the sequence whose goal probability we are calculating
        if (moves.size() <= moveIdx) {
            return 0;
        }

        // CASE 3:
        // we haven't had the occasion in memory to create a rule corresponding to the proposed move,
        // in this case we assume that this couldn't have reached the goal; at the very least we have no reason to think
        // this move *would* reach the goal.
        Move move = moves.get(moveIdx);
        int frequency = getMoveFrequency(move);
        if (frequency == -1) { //we didn't find the move
            throw new IllegalArgumentException("Move not in alphabet!");
        } else if (frequency == 0) { // we haven't seen anything this move yet, return prob = 0
            return 0;
        }

        // RECURSIVE CASE:
        // for each child node associated with the given move, call this function on the child with the same sequence but
        // looking at the next move in the sequence, and increment the probability for this call by the frequency * that
        // return value -- i.e. scale the return value of the child by the frequency of visiting that child.
        // Note that the only way for the child.getGoalProbability call to return a nonzero value is if we have reached
        // the goal before on that move, or if its children reach the goal later in the sequence. This functionality comes
        // from overloading the method in RuleNodeGoal to return 1.

        // TL;DR: how likely has the goal been reached by the time we've made the particular moveIdx-th move in the moves sequence
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

        incrementMoveFrequency(move);

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
