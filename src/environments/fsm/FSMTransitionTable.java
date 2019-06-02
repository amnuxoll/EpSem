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

    //region Public Methods
    public HashMap<Action, Integer>[] getTransitions() {
        return this.transitions;
    }

    public HashMap<Integer, ArrayList<Action>> getShortestSequences() {
        if (this.shortestSequences == null)
        {
            // TODO for real
            this.shortestSequences = new HashMap<>();
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

    public Sequence getUniversalSequence() {
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
