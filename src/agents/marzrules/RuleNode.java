package agents.marzrules;

import framework.Action;

import java.util.*;

/**
 * Created by Ryan on 1/28/2019.
 */
public class RuleNode {
    //private static RuleNodeRoot root;

    protected int sense;
    protected HashMap<Action, ArrayList<RuleNode>> children;
    protected int frequency;
    protected final Action[] potentialActions;

    //For each possible action, this array tracks how frequently it has been selected
    private int[] moveFrequencies;
    // maxDepth is decremented as you move down the tree; at the depth limit this value will be 0.
    // can be understood as "how much deeper does the tree go beyond this node".
    protected int currentDepth;
    protected int maxDepth;
    //TODO: protected int depth; //common-sense notion of depth. Technically redundant with maxDepth as of right now, but makes some stuff easier
    //Potential:
    //private int[] indices;
    protected double expectation = -1;
    private Action bestAction = null;
    protected boolean explore = false;

    protected boolean visited = true;

    // constructor
    public RuleNode(Action[] potentialActions, int sense, int maxDepth, int currentDepth){
        if (currentDepth < 0) {
            throw new IllegalArgumentException("Current depth cannot be negative!");
        }
        this.currentDepth = currentDepth;

        if (potentialActions == null){
            throw new IllegalArgumentException("Cannot have null move set");
        } else if (potentialActions.length == 0){
            throw new IllegalArgumentException("Cannot have empty move set");
        }

        if (maxDepth < 0){
            throw new IllegalArgumentException("Cannot have negative max depth");
        }

        this.potentialActions = potentialActions;
        int alphabetSize = potentialActions.length;
        this.sense = sense;
        // Note that frequency is seemingly recorded in two places--calculating accurate probabilities for prospective
        // actions needs us to keep track of the frequencies that this node was visited and how often each move was taken
        // from the node.
        frequency = 0;
        moveFrequencies = new int[alphabetSize];
        this.maxDepth = maxDepth;

        if (maxDepth > 0) {//if maxDepth is positive, we are not yet at depth limit
            children = new HashMap<>(alphabetSize);
            for (Action action : potentialActions) {
                //initialize the list for a given possible action and make the goal node--which is a special case--the first
                // element. This means that the first element is *always* the goal child for every possible action, which is handy.
                ArrayList<RuleNode> list = new ArrayList<>(Collections.singletonList(
                        new RuleNodeGoal(potentialActions, currentDepth + 1))
                );
                children.put(action, list);
            }
        } else { //at the depth limit, don't build more nodes.
            children = new HashMap<>(alphabetSize);
            for (Action action: potentialActions){
                children.put(action, new ArrayList<>());
            }
        }
    }

    protected int getMoveFrequency(Action action){
        int index = Arrays.asList(potentialActions).indexOf(action);
        if (index == -1) {
            return -1;
        }
        //indexing of moveFrequencies is constructed to be consistent with ordering of the potential actions the agent can
        //take, thus assumes that at any two points in memory the same actions were available to the agent as were in the beginning
        return moveFrequencies[index];
    }

    /**
     * incrementMoveFrequency
     *
     * adds +1 to the frequency of a given action
     * @return the new frequency
     */
    public int incrementMoveFrequency(Action action){
        int index = Arrays.asList(potentialActions).indexOf(action);
        if (index == -1) {
            throw new IllegalArgumentException("Action not valid");
        }

        return ++moveFrequencies[index];
    }

    public boolean inAlphabet(Action action){
        return Arrays.asList(potentialActions).contains(action);
    }

    // The "goal probability" of a sequence of actions is the estimated likelihood that a goal will be reached by the end
    // of the sequence.
    // Parameters: a sequence of actions, and an index in that sequence (for recursive calls)

    // E.g. if actions = aaba, and getGoalProbability(actions, 0) returns .55, our model has predicted that
    // taking those actions has a 55% chance of *having reached the goal* by the time the last 'a' move is taken (or sooner).

    // likewise, getGoalProbability(actions, 3) would return the estimated probability of reaching the goal just by taking that
    // last 'a' move.
    public double getGoalProbability(ArrayList<Action> actions, int moveIdx) {
        if (actions == null){
            throw new IllegalArgumentException("Moves cannot be null");
        }

        // CASE 1:
        // if you have no children, i.e. if you have reached the bottom of the tree
        if (maxDepth == 0) {
            return 0;
        }
        // CASE 2:
        // you've reached the end of the sequence whose goal probability we are calculating
        if (actions.size() <= moveIdx) {
            return 0;
        } else if (moveIdx < 0) {
            throw new IllegalArgumentException("moveIdx cannot be negative");
        }

        // CASE 3:
        // we haven't had the occasion in memory to create a rule corresponding to the proposed action,
        // in this case we assume that this couldn't have reached the goal; at the very least we have no reason to think
        // this action *would* reach the goal.
        Action action = actions.get(moveIdx);
        int frequency = getMoveFrequency(action);
        if (frequency == -1) { //we didn't find the action
            throw new IllegalArgumentException("Action not in alphabet!");
        } else if (frequency == 0) { // we haven't seen anything this action yet, return prob = 0
            return 0;
        }

        // RECURSIVE CASE:
        // for each child node associated with the given action, call this function on the child with the same sequence but
        // looking at the next action in the sequence, and increment the probability for this call by the frequency * that
        // return value.
        // Note that the only way for the child.getGoalProbability call to return a nonzero value is if we have reached
        // the goal before on that action, or if its children reach the goal later in the sequence. This functionality comes
        // from overloading the method in RuleNodeGoal to return 1.

        // TL;DR: how likely has the goal been reached by the time we've made the particular moveIdx-th action in the actions sequence
        double probability = 0;
        ArrayList<RuleNode> children = this.children.get(action);
        for (RuleNode child : children) {
            double childGoalProbability = child.getGoalProbability(actions, moveIdx+1);
            probability += child.frequency * childGoalProbability;
        }
        return probability / frequency;
    }

    /**
     * Gets the expected number of actions to goal from this node, assuming the best possible move is always made.
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
     * @param heuristic The heuristic to value unexplored actions
     * @return The expected value (if it is defined)
     *
     * Side effects:
     *      If it returns an empty optional, explore may be changed to an arbitrary value
     *      Otherwise, expectation stores the value contained in the optional (used for caching result)
     *                 bestAction stores the move that led to the minimum expected value
     *                 explore will be true if bestAction is an explore move (never been done before)
     */
    public Optional<Double> getExpectation(ArrayList<RuleNode> current, boolean top, Heuristic heuristic){

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
            bestAction = null;
            expectation = -1;
            return Optional.empty();
        }

        //BASE CASE: At depth limit
        if (maxDepth == 0){
            bestAction = null;
            expectation = -1;
            return Optional.empty();
        }


        //BASE CASE: No children

        //RECURSIVE CASE: Add 1 to expectation of best child
        return getEVRecursive(current, heuristic);
    }

    protected Optional<Double> getEVRecursive(ArrayList<RuleNode> current, Heuristic heuristic){
        Optional<Double> best = Optional.empty();
        int bestMoveFrequency = 0;
        double h = heuristic.getHeuristic(currentDepth);
        Action bestAction = null;
        boolean madeMove = false;
        for (int i = 0; i < potentialActions.length; i++){
            if(moveFrequencies[i] == 0){
                if (!best.isPresent() || h < best.get()){
                    bestAction = potentialActions[i];
                    best = Optional.of(h);
                    explore = true;
                    bestMoveFrequency = 0;
                }
            } else {
                madeMove = true;
                ArrayList<RuleNode> childArray = children.get(potentialActions[i]);
                final int frequency = moveFrequencies[i];
                Optional<Double> moveEV = getMoveEV(childArray, frequency, current, heuristic);
                if (moveEV.isPresent()){
                    double ev = moveEV.get();
                    //System.out.print("" + potentialActions[i].toString() + ev + ",");
                    if (!best.isPresent() || ev < best.get() || (ev == best.get() && bestMoveFrequency > moveFrequencies[i])){
                        bestAction = potentialActions[i];
                        best = Optional.of(ev);
                        explore = false;
                        bestMoveFrequency = moveFrequencies[i];
                    }
                }
            }
            //System.out.println(":::");
        }
        if(!madeMove){
            this.bestAction = null;
            expectation = -1;
            return Optional.empty();
        } else if (best.isPresent()){
            expectation = best.get();
            this.bestAction = bestAction;
        } /*else {
            expectation = -1;
            this.bestAction = null;
        }*/ // Makes lack of side effects consistent
        return best;
    }

    protected Optional<Double> getMoveEV(ArrayList<RuleNode> childArray, int moveFrequency, ArrayList<RuleNode> current, Heuristic heuristic){
        return childArray.stream()
                .map(node -> node.getExpectation(current, false, heuristic) //Gets expected value
                        .map(val -> val*node.frequency)) //Multiply by node frequency
                .reduce(Optional.of(0.0), (sum, ev) -> sum.flatMap(x -> ev.map(y -> x+y))) //Add EVs up
                .map(val -> val / moveFrequency + 1);
    }

    public double getAverageBits(){
        double maxBits = 0;
        for(Action a: potentialActions){
            if(getMoveFrequency(a) == 0)
                continue;
            double bits = 0;
            for(RuleNode child: children.get(a)){
                double childBits = child.getAverageBits();
                int f = child.getFrequency();
                if(f == 0)
                    continue;
                double p = f/(double)getMoveFrequency(a);
                bits += p*(childBits + Math.log(1.0/p));
            }
            maxBits = Math.max(maxBits, bits);
        }
        return maxBits;
    }

    RuleNode maxChild = null; //Variable traced for debugging purposes. Functionally useless

    public double getMaxBits(){
        double maxBits = 0;
        for(Action a: potentialActions){
            if(getMoveFrequency(a) == 0)
                continue;
            for(RuleNode child: children.get(a)){
                double bits = child.getMaxBits();
                int f = child.getFrequency();
                if(f == 0)
                    continue;
                double p = f/(double)getMoveFrequency(a);
                bits += (Math.log(1.0/p));
                if (bits > maxBits){
                    maxBits = bits;
                    maxChild = child;
                }
                //maxBits = Math.max(maxBits, bits);
            }
        }
        return maxBits;
    }

    protected void unvisit(){
        if (visited) {
            visited = false;

            if (children == null || children.isEmpty()) {
                return;
            }

            for (ArrayList<RuleNode> childArray : children.values()) {
                for (RuleNode child : childArray) {
                    child.unvisit();
                }
            }
        }
    }

    public Action getBestAction(){
        return bestAction;
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
     * Gets the child that corresponds to the given sense in the given list,
     * or creates it if it does not exist.
     *
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
        if (maxDepth == 0){
            return null;
            //throw new IllegalStateException("At depth limit - cannot create children");
        }
        RuleNode child = new RuleNode(potentialActions, nextSense, maxDepth - 1, currentDepth + 1);
        children.add(child);
        return child;
    }

    /**
     * Gets the child that corresponds to the given action and sense value
     * @param action The action of the child
     * @param nextSense The sense of the child
     * @return null if at max depth, otherwise the corresponding child
     *
     * Side effects: Will create the child if it does not exist
     *               Will increment action frequency of this node and frequency of child
     */
    public RuleNode getNextChild(Action action, int nextSense){
        if (maxDepth == 0){
            return null;
        }

        if (action == null){
            throw new IllegalArgumentException("Action cannot be null");
        }

        if (!inAlphabet(action)){
            throw new IllegalArgumentException("Action does not exist");
        }

        ArrayList<RuleNode> moveChildren = children.get(action);

        return getChildBySense(moveChildren, nextSense);
    }

    /**
     * Gets the goal child for a given action if it exists
     *
     * @param action The action that led to the goal
     * @return The goal child
     */
    public RuleNodeGoal getGoalChild(Action action){
        if (maxDepth == 0){
            return null;
        }

        if (action == null){
            throw new IllegalArgumentException("Action cannot be null");
        }

        ArrayList<RuleNode> moveChildren = children.get(action);
        if (moveChildren == null){
            throw new IllegalArgumentException("Action not in alphabet");
        }

        return (RuleNodeGoal) moveChildren.get(0);
    }//getGoalChild

    public int getSense(){
        return sense;
    }

    @Override
    public String toString(){
        //return this.toString(true); //Commented out so debugger doesn't have an Out Of Memory Error
        return "(" + currentDepth + "deep) -> " + sense + ": " + frequency;
    }

    public String toString(boolean includeNewline) {
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
            for (int i = 0; i < potentialActions.length; i++) {
                result.add(sense + potentialActions[i].toString() + ": " + moveFrequencies[i]);
            }
        }

        if (children == null || children.isEmpty()) {
            return result;
        }

        for (Map.Entry<Action, ArrayList<RuleNode>> entry : children.entrySet()){
            Action action = entry.getKey();
            for (RuleNode ruleNode : entry.getValue()){
                for (String childItem : ruleNode.toStringArray())
                {
                    result.add(String.valueOf(sense) + action + " -> " + childItem);
                }
            }
        }
        return result;
    }

    public int getCurrentDepth() {
        return currentDepth;
    }
    public ArrayList<RuleNode> getChild(Action action) { return this.children.get(action); }
}
