package tests.environments.fsm;

import environments.fsm.FSMTransitionTable;
import framework.Move;

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
public class FSMTransitionTableTest {
    //region Constructor Tests
    @EpSemTest
    public void constructorNullTransitionsThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> new FSMTransitionTable(null));
    }

    @EpSemTest
    @SuppressWarnings("unchecked")
    public void constructorValidatesTransitionTableNotAllTransitionsContainSameMoveCount() {
        HashMap<Move, Integer> transitionSet1 = new HashMap<>();
        transitionSet1.put(new Move("a"), 1);
        transitionSet1.put(new Move("b"), 1);
        transitionSet1.put(new Move("c"), 1);
        HashMap<Move, Integer> transitionSet2 = new HashMap<>();
        transitionSet2.put(new Move("a"), 1);
        transitionSet2.put(new Move("b"), 1);
        HashMap<Move, Integer>[] transitionTable = new HashMap[] {
                transitionSet1,
                transitionSet2
        };
        assertThrows(IllegalArgumentException.class, () -> new FSMTransitionTable(transitionTable));
    }

    @EpSemTest
    @SuppressWarnings("unchecked")
    public void constructorValidatesTransitionTableNotAllTransitionsContainSameMoves() {
        HashMap<Move, Integer> transitionSet1 = new HashMap<>();
        transitionSet1.put(new Move("a"), 1);
        transitionSet1.put(new Move("b"), 1);
        transitionSet1.put(new Move("c"), 1);
        HashMap<Move, Integer> transitionSet2 = new HashMap<>();
        transitionSet2.put(new Move("a"), 1);
        transitionSet2.put(new Move("b"), 1);
        transitionSet2.put(new Move("d"), 1);
        HashMap<Move, Integer>[] transitionTable = new HashMap[] {
                transitionSet1,
                transitionSet2
        };
        assertThrows(IllegalArgumentException.class, () -> new FSMTransitionTable(transitionTable));
    }
    //endregion
}
