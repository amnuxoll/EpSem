package environments.fsm;

import framework.Move;
import framework.Sequence;
import utils.Randomizer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Set;

/**
 *
 * @author Zachary Paul Faltersack
 * @version 0.95
 */
public class FSMTransitionTable {
    //region Class Variables
    private HashMap<Move, Integer>[] transitions;
    private HashMap<Integer, ArrayList<Move>> shortestSequences;
    private Sequence universalSequence;
    private Move[] moves;
    //endregion

    //region Constructors
    public FSMTransitionTable(HashMap<Move, Integer>[] transitions) {
        if (transitions == null)
            throw new IllegalArgumentException("transitions cannot be null.");
        this.transitions = transitions;
        Set<Move> moveSet = this.transitions[0].keySet();
        this.moves = moveSet.toArray(new Move[0]);
        for (HashMap<Move, Integer> aTransitionTable : this.transitions) {
            moveSet = aTransitionTable.keySet();
            if (this.moves.length != moveSet.size())
                throw new IllegalArgumentException("transitionTable is not valid for FSM. All transitions must exist for each state.");
            for (Move move : this.moves) {
                if (!moveSet.contains(move))
                    throw new IllegalArgumentException("transitionTable is not valid for FSM. All transition moves must exist for each state.");
            }
        }
    }
    //endregion

    //region Public Methods
    public HashMap<Move, Integer>[] getTransitions() {
        return this.transitions;
    }

    public HashMap<Integer, ArrayList<Move>> getShortestSequences() {
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
            ArrayList<Move> universalSequence = new ArrayList<>();
            for (Integer i : states) {
                int newState = i;
                for(Move m : universalSequence){
                    newState = this.transitions[newState].get(m);
                    if(this.isGoalState(newState)) {
                        break;
                    }
                }
                universalSequence.addAll(shortestSequences.get(newState));
            }
            this.universalSequence = new Sequence(universalSequence.toArray(new Move[0]));
        }
        return this.universalSequence;
    }
    //endregion

    //region Protected Methods
    protected Tweak[] tweakTable(int numSwaps, Randomizer randomizer) {
        ArrayList<Tweak> tweaks = new ArrayList<>();
        for(int i = 0;i<numSwaps; i++) {
            int stateToSwitch = randomizer.getRandomNumber(this.transitions.length); //pick which state whose moves will be swapped
            //pick two moves from a state's possible moves to exchange
            //don't allow the selected moves to be the same one
            int selectedMove1, selectedMove2;
            do {
                selectedMove1 = randomizer.getRandomNumber(this.moves.length);
                selectedMove2 = randomizer.getRandomNumber(this.moves.length);
            } while(selectedMove1 == selectedMove2 && this.moves.length != 1);

            //save value to temp and put new values in swapped places
            Integer temp = this.transitions[stateToSwitch].get(this.moves[selectedMove1]);
            this.transitions[stateToSwitch].put(this.moves[selectedMove1], this.transitions[stateToSwitch].get(this.moves[selectedMove2]));
            this.transitions[stateToSwitch].put(this.moves[selectedMove2], temp);

            Tweak tweak = new Tweak();
            tweak.state = stateToSwitch;
            tweak.move1 = selectedMove1;
            tweak.move2 = selectedMove2;
            tweaks.add(tweak);
        }
        return tweaks.toArray(new Tweak[0]);
    }
    //endregion

    //region Nested Classes
    public class Tweak {
        //region Class Variables
        public int state;
        public int move1;
        public int move2;
        //endregion
    }
    //endregion
}
