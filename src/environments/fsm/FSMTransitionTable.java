package environments.fsm;

import framework.Action;
import framework.Sequence;

import java.util.*;

/**
 *
 * @author Zachary Paul Faltersack
 * @version 0.95
 */
public class FSMTransitionTable {
    //region Class Variables
    private HashMap<Action, Integer>[] transitions;
    private HashMap<Integer, ArrayList<Action>> shortestSequences;
    private Sequence universalSequence;
    private Action[] actions;
    private String shortestBlindPath = null;
    //endregion

    //region Constructors
    public FSMTransitionTable(HashMap<Action, Integer>[] transitions) {
        if (transitions == null)
            throw new IllegalArgumentException("transitions cannot be null.");
        this.transitions = transitions;
        Set<Action> actionSet = this.transitions[0].keySet();
        this.actions = actionSet.toArray(new Action[0]);
        for (HashMap<Action, Integer> aTransitionTable : this.transitions) {
            actionSet = aTransitionTable.keySet();
            if (this.actions.length != actionSet.size())
                throw new IllegalArgumentException("transitionTable is not valid for FSM. All transitions must exist for each state.");
            for (Action action : this.actions) {
                if (!actionSet.contains(action))
                    throw new IllegalArgumentException("transitionTable is not valid for FSM. All transition actions must exist for each state.");
            }
        }
    }
    //endregion

    /**
     * class PathNode
     *
     * used to contain info for the shortest path search.  Each node contains a
     * path and the current position the agent would be in having followed that
     * path from each state as well as 'h' and 'g' values to support A*
     * searching
     */
    public class PathNode implements Comparable<PathNode> {
        public ArrayList<Action> path = new ArrayList<>();
        public final int NUM_STATES = getNumberOfStates();
        public final int GOAL_STATE = NUM_STATES - 1;
        public int[] currStates = new int[NUM_STATES];
        public int g = 0;
        public int h;

        /** default ctor */
        public PathNode() {
            for(int i = 0; i < NUM_STATES; ++i) {
                currStates[i] = i;
            }
            updateH();
        }

        /** copy ctor */
        public PathNode(PathNode parent) {
            path = new ArrayList<>(parent.path);
            for(int i = 0; i < NUM_STATES; ++i) {
                currStates[i] = parent.currStates[i];
            }
            g = parent.g;
            h = parent.h;
        }

        /** calculate the 'h' (heuristic) value for A* search.  In this case
         * it's the length of the longest remaining shortest path */
        public void updateH() {
            this.h = 0;
            for(int i = 0; i < NUM_STATES; ++i) {
                if (currStates[i] != GOAL_STATE) {
                    this.h += shortestSequences.get(i).size();
                }
            }
        }//updateH

        /** the 'f' value for A* search */
        public int getF() { return h + g; }

        /** this method is for the Comparable interface so that we can use this
         * node in a sorted collection */
        public int compareTo(PathNode other) {
            int result = (this.getF() - other.getF());

            //tie breaker with g-value
            if (result == 0) {
                result = (this.g - other.g);
            }

            //tie breaker with lowest numbered curr state (arbitrary)
            if (result == 0) {
                for (int i = 0; i < NUM_STATES; ++i) {
                    result = this.currStates[i] - other.currStates[i];
                    if (result != 0) break;
                }
            }


            return result;
        }

        /** appends a new action to the path and adjusts the states accordingly */
        public void advance(Action act) {
            for(int i = 0; i < NUM_STATES; ++i) {
                if (currStates[i] != GOAL_STATE) {
                    currStates[i] = transitions[currStates[i]].get(act);
                }
            }
            path.add(act);
            this.g++;
            updateH();
        }//advance

        /** @return true if the agent would reach the goal from all states with
         * this node's path
         */
        public boolean allGoal() {
            for(int i = 0; i < NUM_STATES; ++i) {
                if (currStates[i] != GOAL_STATE) return false;
            }

            return true;
        }//allGoal

    }//PathNode

    //region Public Methods
    public HashMap<Action, Integer>[] getTransitions() {
        return this.transitions;
    }

    /**
     * Calculates the shortest path to the goal if the agent has a perfect model
     * of the environment but does not know what state it has started in.  This
     * method uses A* search to reduce resource usage.
     *
     * CAVEAT: This method is solving an NP-hard probelm and can take a really
     * long time to execute on larger FSMs.
     *
     */
    public Sequence getUniversalSequence() {
        if (this.universalSequence != null) return this.universalSequence;
        getShortestSequences();

        //An ordered set containing this initial node
        PathNode pn = new PathNode();
        TreeSet<PathNode> queue = new TreeSet<PathNode>();
        queue.add(pn);

        //Main search loop
        while(! queue.isEmpty()) {
            PathNode parent = queue.first();
            queue.remove(parent);
            for(Action act : this.actions) {

                //Create a child node with this action
                PathNode node = new PathNode(parent);
                node.advance(act);

                //Did we find the shortest path?
                if (node.allGoal()) {
                    Action[] seqArray = node.path.toArray(new Action[0]);
                    this.universalSequence = new Sequence(seqArray);
                    return this.universalSequence;
                }

                //Use this node as parent for future searching
                queue.add(node);
            }//for
        }//while

        return null; //should not be reached
    }//shortestBlindPathToGoal



    public HashMap<Integer, ArrayList<Action>> getShortestSequences() {
        if (this.shortestSequences == null)
        {
            int numStates = transitions.length;
            this.shortestSequences = new HashMap<>();
            //Goal node is presumed to be the highest numbered state.
            //  So put a zero-step path (empty ArrayList) for its sequence
            this.shortestSequences.put(numStates - 1, new ArrayList<>());
            boolean tryAgain = true;
            int currLen = 0;  //at each iteration we are looking for states that are this far from the goal
            while(tryAgain) {
                tryAgain = false;  //assume we're done until we find out otherwise

                for(int state = 0; state < numStates - 1; ++state) {
                    if (shortestSequences.get(state) == null) {
                        tryAgain = true;  //if there are any unset sequences, we need loop again
                        for(Action act : this.actions) {
                            Integer destState = this.transitions[state].get(act);
                            ArrayList<Action> destSS = this.shortestSequences.get(destState);
                            if ((destSS != null) && (destSS.size() <= currLen)) {
                                ArrayList<Action> newSS = new ArrayList<>(destSS);
                                newSS.add(0, act);
                                this.shortestSequences.put(state, newSS);
                                break;
                            }
                        }//for each action
                    }//if SS isn't set
                }//for each state
                currLen++;  //next time, look for SS that are +1 longer than this time
            }//while(tryAgain)
        }
        return this.shortestSequences;
    }

    public int getNumberOfStates()
    {
        return this.transitions.length;
    }

    public boolean isGoalState(int state) {
        return state == (this.transitions.length - 1);
    }

    /** this doesn't necessarily calculate the shortest universal sequence but it will be at least close */
    public Sequence old_getUniversalSequence() {
        getShortestSequences();
        if (this.universalSequence == null)
        {
            ArrayList<Integer> states = new ArrayList<>(this.shortestSequences.keySet());
            states.sort(Comparator.comparingInt(o -> this.shortestSequences.get(o).size()));
            ArrayList<Action> universalSequence = new ArrayList<>();
            for (Integer i : states) {
                int newState = i;
                for(Action m : universalSequence){
                    newState = this.transitions[newState].get(m);
                    if(this.isGoalState(newState)) {
                        break;
                    }
                }
                universalSequence.addAll(shortestSequences.get(newState));
            }
            this.universalSequence = new Sequence(universalSequence.toArray(new Action[0]));
        }
        return this.universalSequence;
    }
    //endregion

    @Override
    public String toString(){
        StringBuilder builder = new StringBuilder();
        for (Action action : actions){
            builder.append(",");
            builder.append(action.toString());
        }

        for (int i = 0; i < transitions.length; i++){
            builder.append("\n");
            builder.append(i);
            for (Action action : actions){
                int result = transitions[i].get(action);
                builder.append(",");
                builder.append(result);
            }
        }
        return builder.toString();

    }
}
