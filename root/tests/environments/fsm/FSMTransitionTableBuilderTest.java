package environments.fsm;

import framework.Move;
import org.junit.jupiter.api.Test;
import utils.Random;

import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 *
 * @author Zachary Paul Faltersack
 * @version 0.95
 */
public class FSMTransitionTableBuilderTest {
    //region Constructor Tests
    @Test
    public void constructorAlphabetSizeLessThanOneThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> new FSMTransitionTableBuilder(0, 1, Random.getTrue()));
    }

    @Test
    public void constructorNumStatesLessThanOneThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> new FSMTransitionTableBuilder(1, 0, Random.getTrue()));
    }
    //endregion

    //region getTransitionTable Tests
    @Test
    public void buildTransitionTableSingleTransitionSingleState() {
        FSMTransitionTableBuilder builder = new FSMTransitionTableBuilder(1, 1, Random.getTrue());
        FSMTransitionTable transitionTable = builder.getTransitionTable();
        assertEquals(1, transitionTable.getTransitions().length);
        HashMap<Move, Integer> goalTransitions = transitionTable.getTransitions()[0];
        this.validateGoalTransitions(1, 0, goalTransitions);
    }

    @Test
    public void buildTransitionTableMultipleTransitionSingleState() {
        FSMTransitionTableBuilder builder = new FSMTransitionTableBuilder(13, 1, Random.getTrue());
        FSMTransitionTable transitionTable = builder.getTransitionTable();
        assertEquals(1, transitionTable.getTransitions().length);
        HashMap<Move, Integer> goalTransitions = transitionTable.getTransitions()[0];
        this.validateGoalTransitions(13, 0, goalTransitions);
    }
    //endregion

    //region Helper Methods
    private void validateGoalTransitions(int expectedMoveCount, int goalState, HashMap<Move, Integer> transitions) {
        assertEquals(expectedMoveCount, transitions.size());
        for (int state : transitions.values()) {
            assertEquals(goalState, state);
        }
    }
    //endregion
}
