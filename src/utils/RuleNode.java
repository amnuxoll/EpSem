package utils;

import framework.Move;

import java.util.*;

/**
 * Created by Ryan on 1/28/2019.
 */
public class RuleNode {
    //private static RuleNodeRoot root;

    protected int sense;
    protected HashMap<Move, ArrayList<RuleNode>> children;
    protected int frequency;
    protected final Move[] potentialMoves;
    private int[] moveFrequencies;
    // maxDepth is decremented as you move down the tree; at the depth limit this value will be 0.
    // can be understood as "how much deeper does the tree go beyond this node".
    protected int maxDepth;
    //TODO: protected int depth; //common-sense notion of depth. Technically redundant with maxDepth as of right now, but makes some stuff easier
    //Potential:
    //private int[] indices;
    protected double expectation = -1;
    private Move bestMove = null;
    protected boolean explore = false;

    protected boolean visited = true;

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
                ArrayList<RuleNode> list = new ArrayList<>(Collections.singletonList(new RuleNodeGoal(potentialMoves)));
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

    public int incrementMoveFrequency(Move move){
        int index = Arrays.asList(potentialMoves).indexOf(move);
        if (index == -1) {
            return -1;
        }

        return moveFrequencies[index]++;
    }

    public boolean inAlphabet(Move move){
        return Arrays.asList(potentialMoves).contains(move);
    }

    // The "goal probability" of a sequence of moves is the estimated likelihood that a goal will be reached by the end
    // of the sequence.
    // Parameters: a sequence of moves, and an index in that sequence (for recursive calls)

    // E.g. if moves = aaba, and getGoalProbability(moves, 0) returns .55, our model has predicted that
    // taking those moves has a 55% chance of *having reached the goal* by the time the last 'a' move is taken (or sooner).

    // likewise, getGoalProbability(moves, 3) would return the estimated probability of reaching the goal just by taking that
    // last 'a' move.
    public double getGoalProbability(ArrayList<Move> moves, int moveIdx) {
        if (moves == null){
            throw new IllegalArgumentException("Moves cannot be null");
        }

        // CASE 1:
        // if you have no children, i.e. if you have reached the bottom of the tree
        if (maxDepth == 0) {
            return 0;
        }
        // CASE 2:
        // you've reached the end of the sequence whose goal probability we are calculating
        if (moves.size() <= moveIdx) {
            return 0;
        } else if (moveIdx < 0) {
            throw new IllegalArgumentException("moveIdx cannot be negative");
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
        // return value.
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

    /**
     * Gets the expected number of moves to goal from this node, assuming the best possible move is always made.
     * Fails if it cannot be calculated.
     * Fails if these conditions are met:
     *      Node is at depth limit or otherwise does not have children
     *      Node is in current (this prevents loops)
     *      A child in every possible move fails
     *
     * This function is cached. When unvisit is called, it will begin returning cached results. When occurs is called,
     * it will stop caching the results until it is unvisited. The idea is that it only recalculates if it or a child is
     * in current, and caches if it has not been visited since the last goal.
     * TODO: Is cache correct for a failed EV result (ex depth limit)?
     *
     * @param current The nodes in current. Needed so that EV calculation fails if it hits another current node.
     * @param top Whether this node is in current. Should be true unless called recursively. Causes code to ignore current check.
     * @param h The heuristic to value unexplored moves
     * @return The expected value (if it is defined)
     *
     * Side effects:
     *      If it returns an empty optional, explore may be changed to an arbitrary value
     *      Otherwise, expectation stores the value contained in the optional (used for caching result)
     *                 bestMove stores the move that led to the minimum expected value
     *                 explore will be true if bestMove is an explore move (never been done before)
     */
    public Optional<Double> getExpectation(ArrayList<RuleNode> current, boolean top, double h){

        //Cached value
        //TODO: Fix so that it will uncache if h changes
        if (!visited && expectation != -1.0){
            return Optional.of(expectation);
        }

        //Error check
        if (current == null){
            throw new IllegalArgumentException("current cannot be null");
        }

        //BASE CASE: Is goal (method override)

        //BASE CASE: Not top and in current
        if (!top && current.contains(this)){
            bestMove = null;
            expectation = -1;
            return Optional.empty();
        }

        //BASE CASE: At depth limit
        if (maxDepth == 0){
            bestMove = null;
            expectation = -1;
            return Optional.empty();
        }


        //BASE CASE: No children

        //RECURSIVE CASE: Add 1 to expectation of best child
        return getEVRecursive(current, h);
    }

    protected Optional<Double> getEVRecursive(ArrayList<RuleNode> current, double h){
        Optional<Double> best = Optional.empty();
        Move bestMove = null;
        boolean madeMove = false;
        for (int i = 0; i < potentialMoves.length; i++){
            if(moveFrequencies[i] == 0){
                if (!best.isPresent() || h < best.get()){
                    bestMove = potentialMoves[i];
                    best = Optional.of(h);
                    explore = true;
                }
            } else {
                madeMove = true;
                ArrayList<RuleNode> childArray = children.get(potentialMoves[i]);
                final int frequency = moveFrequencies[i];
                Optional<Double> moveEV = getMoveEV(childArray, frequency, current, h);
                if (moveEV.isPresent()){
                    Double ev = moveEV.get();
                    //System.out.print("" + potentialMoves[i].toString() + ev + ",");
                    if (!best.isPresent() || ev < best.get()){
                        bestMove = potentialMoves[i];
                        best = Optional.of(ev);
                        explore = false;
                    }
                }
            }
            //System.out.println(":::");
        }
        if(!madeMove){
            this.bestMove = null;
            expectation = -1;
            return Optional.empty();
        } else if (best.isPresent()){
            expectation = best.get();
            this.bestMove = bestMove;
        } /*else {
            expectation = -1;
            this.bestMove = null;
        }*/ // Makes lack of side effects consistent
        return best;
    }

    protected Optional<Double> getMoveEV(ArrayList<RuleNode> childArray, int moveFrequency, ArrayList<RuleNode> current, double h){
        Optional<Double> moveEV = childArray.stream()
                .map(node -> node.getExpectation(current, false, h) //Gets expected value
                        .map(val -> val*node.frequency)) //Multiply by node frequency
                .reduce(Optional.of(0.0), (sum, ev) -> sum.flatMap(x -> ev.map(y -> x+y))) //Add EVs up
                .map(val -> val / moveFrequency + 1); //Divide by overall node frequency
        return moveEV;
    }

    protected void unvisit(){
        visited = false;
    }

    public Move getBestMove(){
        return bestMove;
    }

    public int getFrequency() { return frequency; }

    public boolean getExplore() { return explore; }

    public double getCachedExpectation() {
        return expectation;
    }

    public void occurs(){
        frequency++;
        visited = true;
    }

    /**
     * Gets the child that corresponds to the given sense in the given list, or creates it if it does not exist
     * @param children The list of children to look through
     * @param nextSense The sense that the child should have
     * @return The corresponding child
     *
     * Side effect: Can create the child if a child with the given sense is not found
     *              Will increase child frequency
     */
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

    /**
     * Gets the child that corresponds to the given move and sense value
     * @param move The move of the child
     * @param nextSense The sense of the child
     * @return null if at max depth, otherwise the corresponding child
     *
     * Side effects: Will create the child if it does not exist
     *               Will increment move frequency of this node and frequency of child
     */
    public RuleNode getNextChild(Move move, int nextSense){
        if (maxDepth == 0){
            return null;
        }

        if (move == null){
            throw new IllegalArgumentException("Move cannot be null");
        }

        if (!inAlphabet(move)){
            throw new IllegalArgumentException("Move does not exist");
        }

        ArrayList<RuleNode> moveChildren = children.get(move);

        return getChildBySense(moveChildren, nextSense);
    }

    /**
     * Gets the goal child for the node
     *
     * @param move The move that led to the goal
     * @return The goal child
     *
     * Side Effect: Increments goal child frequency
     */
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
