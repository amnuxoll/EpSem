package environments.fsm;

import framework.Action;
import java.util.HashMap;

public class FSMTransitionTableTest {
    private static Action[] actions;

    private static FSMTransitionTable createTable(int[][] destinations) {
        actions = new Action[3];
        for(int i = 0; i < actions.length; ++i) {
            char next = (char)('a' + i);
            actions[i] = new Action(next  + "");
        }
       
        @SuppressWarnings("unchecked")
        HashMap<Action, Integer>[] transitions =
                new HashMap[destinations.length];

        for (int state = 0; state < destinations.length; state++) {
            transitions[state] = new HashMap<>();
            transitions[state].put(actions[0], destinations[state][0]);
        }

        return new FSMTransitionTable(transitions);
    }

    private static void testIsFSMValid(String testName, FSMTransitionTable table, boolean expected) {

        boolean actual = table.isFSMValid();

        if (actual == expected) {
            System.out.println("[PASS] " + testName);
        } else {
            System.out.println("[FAIL] " + testName
                    + " | Expected: " + expected
                    + " | Actual: " + actual);
        }
    }

    public void testRandomTransitionChange() {
        // Create two actions
        Action actionA = new Action("a");
        Action actionB = new Action("b");

        // Create a three-state transition table
        @SuppressWarnings("unchecked")
        HashMap<Action, Integer>[] transitions = new HashMap[3];

        // State 0
        transitions[0] = new HashMap<>();
        transitions[0].put(actionA, 2);
        transitions[0].put(actionB, 2);

        // State 1
        transitions[1] = new HashMap<>();
        transitions[1].put(actionA, 2);
        transitions[1].put(actionB, 2);

        // Goal state (state 2)
        transitions[2] = new HashMap<>();
        transitions[2].put(actionA, 2);
        transitions[2].put(actionB, 2);

        FSMTransitionTable table = new FSMTransitionTable(transitions);

        // Verify the initial FSM is valid
        if (!table.isFSMValid()) {
            throw new AssertionError("Initial FSM should be valid.");
        }

        // Save the original transitions
        HashMap<Action, Integer>[] originalTransitions =
                new HashMap[transitions.length];

        for (int i = 0; i < transitions.length; i++) {
            originalTransitions[i] = new HashMap<>(transitions[i]);
        }

        // Execute the method being tested
        table.randomTransitionChange();

        // Verify that the FSM remains valid
        if (!table.isFSMValid()) {
            throw new AssertionError(
                    "FSM should remain valid after randomTransitionChange().");
        }

        // Verify that at least one transition changed
        boolean transitionChanged = false;

        for (int state = 0; state < transitions.length - 1; state++) {
            for (Action action : originalTransitions[state].keySet()) {
                int oldDestination = originalTransitions[state].get(action);
                int newDestination = table.getTransitions()[state].get(action);

                if (oldDestination != newDestination) {
                    transitionChanged = true;
                }
            }
        }

        if (!transitionChanged) {
            throw new AssertionError(
                    "randomTransitionChange() should modify a transition.");
        }

        // Verify that the goal state was not modified
        for (Action action : originalTransitions[2].keySet()) {
            if (!table.getTransitions()[2].get(action).equals(2)) {
                throw new AssertionError(
                        "Goal state transitions should remain unchanged.");
            }
        }
    }

    public static void main(String[] args) {

        /* Testing isFSMValid method */

        // Every state has a direct path to the goal
        testIsFSMValid("Direct paths to goal",
                createTable(new int[][]{
                        {2}, {2}, {2}
                }), true);

        // State 0 must pass through State 1 to reach the goal
        testIsFSMValid("Indirect path to goal",
                createTable(new int[][]{
                        {1}, {2}, {2}
                }), true);

        // State 1 loops forever and cannot reach the goal
        testIsFSMValid("Unreachable state",
                createTable(new int[][]{
                        {2}, {1}, {2}
                }), false);

        // States 0 and 1 form a cycle without reaching the goal
        testIsFSMValid("Cycle without goal",
                createTable(new int[][]{
                        {1}, {0}, {2}
                }), false);

        /* Testing randomTransitionChange method */
        FSMTransitionTableTest test = new FSMTransitionTableTest();
        test.testRandomTransitionChange();
        System.out.println("randomTransitionChange() test passed.");
    }
}