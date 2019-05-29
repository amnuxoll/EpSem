package tests.environments.fsm;

import environments.fsm.FSMTransitionTable;
import framework.Action;

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
        HashMap<Action, Integer> transitionSet1 = new HashMap<>();
        transitionSet1.put(new Action("a"), 1);
        transitionSet1.put(new Action("b"), 1);
        transitionSet1.put(new Action("c"), 1);
        HashMap<Action, Integer> transitionSet2 = new HashMap<>();
        transitionSet2.put(new Action("a"), 1);
        transitionSet2.put(new Action("b"), 1);
        HashMap<Action, Integer>[] transitionTable = new HashMap[] {
                transitionSet1,
                transitionSet2
        };
        assertThrows(IllegalArgumentException.class, () -> new FSMTransitionTable(transitionTable));
    }

    @EpSemTest
    @SuppressWarnings("unchecked")
    public void constructorValidatesTransitionTableNotAllTransitionsContainSameMoves() {
        HashMap<Action, Integer> transitionSet1 = new HashMap<>();
        transitionSet1.put(new Action("a"), 1);
        transitionSet1.put(new Action("b"), 1);
        transitionSet1.put(new Action("c"), 1);
        HashMap<Action, Integer> transitionSet2 = new HashMap<>();
        transitionSet2.put(new Action("a"), 1);
        transitionSet2.put(new Action("b"), 1);
        transitionSet2.put(new Action("d"), 1);
        HashMap<Action, Integer>[] transitionTable = new HashMap[] {
                transitionSet1,
                transitionSet2
        };
        assertThrows(IllegalArgumentException.class, () -> new FSMTransitionTable(transitionTable));
    }
    //endregion
}
