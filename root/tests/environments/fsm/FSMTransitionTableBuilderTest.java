package environments.fsm;

import framework.IRandomizer;
import framework.Move;
import framework.Randomizer;
import framework.Services;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class FSMTransitionTableBuilderTest {

    // test setup/teardown
    @BeforeEach
    public void initialize()
    {
        Services.register(IRandomizer.class, new Randomizer());
    }
    // buildTransitionTable Tests
    @Test
    public void constructorAlphabetSizeLessThanOneThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> new FSMTransitionTableBuilder(0, 1));
    }

    @Test
    public void constructorNumStatesLessThanOneThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> new FSMTransitionTableBuilder(1, 0));
    }

    // getTransitionTable Tests
    @Test
    public void buildTransitionTableSingleTransitionSingleState() {
        FSMTransitionTableBuilder builder = new FSMTransitionTableBuilder(1, 1);
        HashMap[] transitionTable = builder.getTransitionTable();
        assertEquals(1, transitionTable.length);
        HashMap<Move, Integer> goalTransitions = transitionTable[0];
        this.validateGoalTransitions(1, 0, goalTransitions);
    }

    @Test
    public void buildTransitionTableMultipleTransitionSingleState() {
        FSMTransitionTableBuilder builder = new FSMTransitionTableBuilder(13, 1);
        HashMap[] transitionTable = builder.getTransitionTable();
        assertEquals(1, transitionTable.length);
        HashMap<Move, Integer> goalTransitions = transitionTable[0];
        this.validateGoalTransitions(13, 0, goalTransitions);
    }

    private void validateGoalTransitions(int expectedMoveCount, int goalState, HashMap<Move, Integer> transitions) {
        assertEquals(expectedMoveCount, transitions.size());
        for (int state : transitions.values()) {
            assertEquals(goalState, state);
        }
    }
}
