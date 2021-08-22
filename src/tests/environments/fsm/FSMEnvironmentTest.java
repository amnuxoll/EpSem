package tests.environments.fsm;

import environments.fsm.FSMEnvironment;
import environments.fsm.FSMTransitionTable;
import framework.Action;

import java.util.HashMap;
import java.util.Random;

import tests.EpSemTest;
import tests.EpSemTestClass;
import static tests.Assertions.*;

/**
 *
 * @author Zachary Paul Faltersack
 * @version 0.95
 */
@EpSemTestClass
public class FSMEnvironmentTest {
    //region constructor Tests
    @EpSemTest
    public void constructorNullTransitionTableThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> new FSMEnvironment(null));
    }

    @EpSemTest
    public void constructorNullEnumSetThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> new FSMEnvironment(this.getFsmTransitionTable(), null));
    }
    //endregion

    //region getActions Tests
    @EpSemTest
    @SuppressWarnings("unchecked")
    public void getMovesFSMDescriptionInfersMoveSets() {
        HashMap<Action, Integer> transitionSet1 = new HashMap<>();
        transitionSet1.put(new Action("a"), 1);
        transitionSet1.put(new Action("b"), 1);
        transitionSet1.put(new Action("c"), 1);
        HashMap<Action, Integer> transitionSet2 = new HashMap<>();
        transitionSet2.put(new Action("a"), 1);
        transitionSet2.put(new Action("b"), 1);
        transitionSet2.put(new Action("c"), 1);
        HashMap<Action, Integer>[] transitionTable = new HashMap[] {
                        transitionSet1,
                        transitionSet2
                };
        FSMEnvironment description = new FSMEnvironment(new FSMTransitionTable(transitionTable));
        Action[] expectedActions = new Action[] {
                        new Action("a"),
                        new Action("b"),
                        new Action("c"),
                };
        assertArrayEquals(expectedActions, description.getActions());
    }
    //endregion

    //region transition Tests

    //TODO -- Add tests
    // test for null action
    // test for unrecognized action
    // test for transition to goal
    // test for non-goal transition
    // test for apply sensors

    //endregion

    //region validateSequence tests

    //TODO -- Add tests
    // test for null sequence
    // test for valid sequence
    // test for invalid sequence

    //endregion

    //region Helper Methods
    @SuppressWarnings("unchecked")
    private FSMTransitionTable getFsmTransitionTable() {
        HashMap<Action, Integer> transitionSet1 = new HashMap<>();
        transitionSet1.put(new Action("a"), 1);
        transitionSet1.put(new Action("b"), 1);
        transitionSet1.put(new Action("c"), 1);
        HashMap<Action, Integer> transitionSet2 = new HashMap<>();
        transitionSet2.put(new Action("a"), 1);
        transitionSet2.put(new Action("b"), 1);
        transitionSet2.put(new Action("c"), 1);
        HashMap<Action, Integer> transitionSet3 = new HashMap<>();
        transitionSet3.put(new Action("a"), 1);
        transitionSet3.put(new Action("b"), 1);
        transitionSet3.put(new Action("c"), 1);
        HashMap<Action, Integer>[] transitionTable = new HashMap[] {
                transitionSet1,
                transitionSet2,
                transitionSet3
        };
        return new FSMTransitionTable(transitionTable);
    }
    //endregion
}
