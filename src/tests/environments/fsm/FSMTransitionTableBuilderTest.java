package tests.environments.fsm;

import environments.fsm.FSMTransitionTable;
import environments.fsm.FSMTransitionTableBuilder;
import framework.Move;
import utils.Random;

import java.util.HashMap;

import tests.EpSemTest;
import tests.EpSemTestClass;
import static tests.Assertions.*;

/**
 *
 * @author Zachary Paul Faltersack
 * @version 0.95
 */
@EpSemTestClass
public class FSMTransitionTableBuilderTest {
    //region Constructor Tests
    @EpSemTest
    public void constructorAlphabetSizeLessThanOneThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> new FSMTransitionTableBuilder(0, 1, Random.getTrue()));
    }

    @EpSemTest
    public void constructorNumStatesLessThanOneThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> new FSMTransitionTableBuilder(1, 0, Random.getTrue()));
    }
    //endregion

    //region getTransitionTable Tests
    @EpSemTest
    public void buildTransitionTableSingleTransitionSingleState() {
        FSMTransitionTableBuilder builder = new FSMTransitionTableBuilder(1, 1, Random.getTrue());
        FSMTransitionTable transitionTable = builder.getTransitionTable();
        assertEquals(1, transitionTable.getTransitions().length);
        HashMap<Move, Integer> goalTransitions = transitionTable.getTransitions()[0];
        this.validateGoalTransitions(1, 0, goalTransitions);
    }

    @EpSemTest
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
