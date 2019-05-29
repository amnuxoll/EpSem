//package tests.environments.fsm;
//
//import environments.fsm.FSMEnvironment;
//import environments.fsm.FSMTransitionTable;
//import framework.Action;
//
//import java.util.HashMap;
//import java.util.Random;
//
//import tests.EpSemTest;
//import tests.EpSemTestClass;
//import static tests.Assertions.*;
//
///**
// *
// * @author Zachary Paul Faltersack
// * @version 0.95
// */
//@EpSemTestClass
//public class FSMDescriptionTest {
//    //region constructor Tests
//    @EpSemTest
//    public void constructorNullTransitionTableThrowsException() {
//        assertThrows(IllegalArgumentException.class, () -> new FSMEnvironment(null));
//    }
//
//    @EpSemTest
//    public void constructorNullEnumSetThrowsException() {
//        assertThrows(IllegalArgumentException.class, () -> new FSMEnvironment(this.getFsmTransitionTable(), null));
//    }
//    //endregion
//
//    //region getActions Tests
//    @EpSemTest
//    @SuppressWarnings("unchecked")
//    public void getMovesFSMDescriptionInfersMoveSets() {
//        HashMap<Action, Integer> transitionSet1 = new HashMap<>();
//        transitionSet1.put(new Action("a"), 1);
//        transitionSet1.put(new Action("b"), 1);
//        transitionSet1.put(new Action("c"), 1);
//        HashMap<Action, Integer> transitionSet2 = new HashMap<>();
//        transitionSet2.put(new Action("a"), 1);
//        transitionSet2.put(new Action("b"), 1);
//        transitionSet2.put(new Action("c"), 1);
//        HashMap<Action, Integer>[] transitionTable = new HashMap[] {
//                        transitionSet1,
//                        transitionSet2
//                };
//        FSMEnvironment description = new FSMEnvironment(new FSMTransitionTable(transitionTable));
//        Action[] expectedActions = new Action[] {
//                        new Action("a"),
//                        new Action("b"),
//                        new Action("c"),
//                };
//        assertArrayEquals(expectedActions, description.getActions());
//    }
//    //endregion
//
//    //region transition Tests
//    @EpSemTest
//    @SuppressWarnings("unchecked")
//    public void transitionStateLessThanZeroThrowsException() {
//        HashMap<Action, Integer> transitionSet1 = new HashMap<>();
//        transitionSet1.put(new Action("a"), 1);
//        transitionSet1.put(new Action("b"), 1);
//        transitionSet1.put(new Action("c"), 1);
//        HashMap<Action, Integer>[] transitionTable = new HashMap[] {
//                        transitionSet1
//                };
//        FSMEnvironment description = new FSMEnvironment(new FSMTransitionTable(transitionTable));
//        assertThrows(IllegalArgumentException.class, () -> description.applyAction(new Action("a")));
//    }
//
//    @EpSemTest
//    @SuppressWarnings("unchecked")
//    public void transitionStateTooLargeThrowsException() {
//        HashMap<Action, Integer> transitionSet1 = new HashMap<>();
//        transitionSet1.put(new Action("a"), 0);
//        transitionSet1.put(new Action("b"), 0);
//        transitionSet1.put(new Action("c"), 0);
//        HashMap<Action, Integer>[] transitionTable = new HashMap[] {
//                        transitionSet1
//                };
//        FSMEnvironment description = new FSMEnvironment(new FSMTransitionTable(transitionTable));
//        assertThrows(IllegalArgumentException.class, () -> description.transition(1, new Action("a")));
//    }
//
//    @EpSemTest
//    @SuppressWarnings("unchecked")
//    public void transitionNullMoveThrowsException() {
//        HashMap<Action, Integer> transitionSet1 = new HashMap<>();
//        transitionSet1.put(new Action("a"), 0);
//        transitionSet1.put(new Action("b"), 0);
//        transitionSet1.put(new Action("c"), 0);
//        HashMap<Action, Integer>[] transitionTable = new HashMap[] {
//                        transitionSet1
//                };
//        FSMEnvironment description = new FSMEnvironment(new FSMTransitionTable(transitionTable));
//        assertThrows(IllegalArgumentException.class, () -> description.transition(0, null));
//    }
//
//    @EpSemTest
//    @SuppressWarnings("unchecked")
//    public void transitionInvalidMoveThrowsException() {
//        HashMap<Action, Integer> transitionSet1 = new HashMap<>();
//        transitionSet1.put(new Action("a"), 0);
//        transitionSet1.put(new Action("b"), 0);
//        transitionSet1.put(new Action("c"), 0);
//        HashMap<Action, Integer>[] transitionTable = new HashMap[] {
//                        transitionSet1
//                };
//        FSMEnvironment description = new FSMEnvironment(new FSMTransitionTable(transitionTable));
//        assertThrows(IllegalArgumentException.class, () -> description.transition(0, new Action("d")));
//    }
//
//    @EpSemTest
//    @SuppressWarnings("unchecked")
//    public void transitionReturnsNewState() {
//        HashMap<Action, Integer> transitionSet1 = new HashMap<>();
//        transitionSet1.put(new Action("a"), 1);
//        transitionSet1.put(new Action("b"), 2);
//        transitionSet1.put(new Action("c"), 3);
//        HashMap<Action, Integer>[] transitionTable = new HashMap[] {
//                        transitionSet1
//                };
//        FSMEnvironment description = new FSMEnvironment(new FSMTransitionTable(transitionTable));
//        assertEquals(2, description.transition(0, new Action("b")).getState());
//    }
//    //endregion
//
//    //region getRandomState Tests
//    @EpSemTest
//    public void getRandomState() {
//        FSMEnvironment description = new FSMEnvironment(this.getFsmTransitionTable());
//        int randomState = description.getRandomState();
//        assertTrue(randomState >= 0);
//        assertTrue(randomState < 2);
//    }
//    //endregion
//
//    //region tweakTable Tests
//    @EpSemTest
//    public void tweakTableNumSwapsLessThan0ThrowsException() {
//        FSMEnvironment description = new FSMEnvironment(this.getFsmTransitionTable());
//        assertThrows(IllegalArgumentException.class, () -> description.tweakTable(-1, new Random()));
//    }
//
//    @EpSemTest
//    public void tweakTableNullRandomThrowsException() {
//        FSMEnvironment description = new FSMEnvironment(this.getFsmTransitionTable());
//        assertThrows(IllegalArgumentException.class, () -> description.tweakTable(1, null));
//    }
//    //endregion
//
//    //region Helper Methods
//    @SuppressWarnings("unchecked")
//    private FSMTransitionTable getFsmTransitionTable() {
//        HashMap<Action, Integer> transitionSet1 = new HashMap<>();
//        transitionSet1.put(new Action("a"), 1);
//        transitionSet1.put(new Action("b"), 1);
//        transitionSet1.put(new Action("c"), 1);
//        HashMap<Action, Integer> transitionSet2 = new HashMap<>();
//        transitionSet2.put(new Action("a"), 1);
//        transitionSet2.put(new Action("b"), 1);
//        transitionSet2.put(new Action("c"), 1);
//        HashMap<Action, Integer> transitionSet3 = new HashMap<>();
//        transitionSet3.put(new Action("a"), 1);
//        transitionSet3.put(new Action("b"), 1);
//        transitionSet3.put(new Action("c"), 1);
//        HashMap<Action, Integer>[] transitionTable = new HashMap[] {
//                transitionSet1,
//                transitionSet2,
//                transitionSet3
//        };
//        return new FSMTransitionTable(transitionTable);
//    }
//    //endregion
//}
