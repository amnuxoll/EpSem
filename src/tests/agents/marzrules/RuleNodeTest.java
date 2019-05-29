package tests.agents.marzrules;

import agents.marzrules.Heuristic;
import agents.marzrules.RuleNode;
import agents.marzrules.RuleNodeGoal;
import agents.marzrules.TestHeuristic;
import framework.Action;
import tests.EpSemTest;
import tests.EpSemTestClass;

import java.util.*;

import static tests.Assertions.*;


/**
 * Created by Ryan on 2/9/2019.
 */
@EpSemTestClass
public class RuleNodeTest {

    //region Constructor tests

    @EpSemTest
    public void testEmptyAlphabet(){
        assertThrows(IllegalArgumentException.class, () -> new RuleNode(null, 0, 1, 0));
        assertThrows(IllegalArgumentException.class, () -> new RuleNode(new Action[] {}, 0, 1, 0));
    }

    @EpSemTest
    public void testNegativeDepth(){
        assertThrows(IllegalArgumentException.class, () -> new RuleNode(new Action[] {new Action("a")}, 0, -1, 0));
    }

    @EpSemTest
    public void testNoThrow(){
        assertNotNull(new RuleNode(new Action[] {new Action("a")}, 0, 1, 0));
    }

    //endregion

    //region getNextChild tests

    @EpSemTest
    public void testMaxDepth(){
        Action action = new Action("a");
        assertNull(new RuleNode(new Action[] {action}, 0, 0, 0).getNextChild(action, 1));
    }

    @EpSemTest
    public void testNullMove(){
        RuleNode node = new RuleNode(new Action[] {new Action("a")}, 0, 2, 0);
        assertThrows(IllegalArgumentException.class, () -> node.getNextChild(null, 1));
    }

    @EpSemTest
    public void testSameChild(){
        Action[] actions = new Action[] {new Action("a"), new Action("b")};
        RuleNode node = new RuleNode(actions, 0, 2, 0);
        RuleNode child1 = node.getNextChild(actions[0], 0);
        assertNotNull(child1);
        assertNotEquals(node, child1);
        assertFalse(child1 instanceof RuleNodeGoal);
        RuleNode child2 = node.getNextChild(actions[0], 0);
        assertNotNull(child2);
        assertNotEquals(node, child2);
        assertFalse(child2 instanceof RuleNodeGoal);
        assertEquals(child1, child2);
    }

    @EpSemTest
    public void testDifferentMove(){
        Action[] actions = new Action[] {new Action("a"), new Action("b")};
        RuleNode node = new RuleNode(actions, 0, 2, 0);
        RuleNode child1 = node.getNextChild(actions[0], 0);
        RuleNode child2 = node.getNextChild(actions[1], 0);
        assertNotNull(child1);
        assertNotNull(child2);
        assertNotEquals(child1, child2);
    }

    @EpSemTest
    public void testDifferentSense(){
        Action[] actions = new Action[] {new Action("a"), new Action("b")};
        RuleNode node = new RuleNode(actions, 0, 2, 0);
        RuleNode child1 = node.getNextChild(actions[0], 0);
        RuleNode child2 = node.getNextChild(actions[0], 1);
        assertNotNull(child1);
        assertNotNull(child2);
        assertNotEquals(child1, child2);
    }

    @EpSemTest
    public void testIllegalMove(){
        Action[] actions = new Action[] {new Action("a"), new Action("b")};
        RuleNode node = new RuleNode(actions, 0, 2, 0);
        assertThrows(IllegalArgumentException.class, () -> node.getNextChild(new Action("c"), 0));
    }

    //endregion

    //region getGoalChild

    @EpSemTest
    public void testMaxDepthGoal(){
        Action[] actions = new Action[] {new Action("a"), new Action("b")};
        RuleNode node = new RuleNode(actions, 0, 0, 0);
        assertNull(node.getGoalChild(actions[0]));
    }

    @EpSemTest
    public void testBadMoveGoal(){
        Action[] actions = new Action[] {new Action("a"), new Action("b")};
        RuleNode node = new RuleNode(actions, 0, 2, 0);
        assertThrows(IllegalArgumentException.class, () -> node.getGoalChild(null));
        assertThrows(IllegalArgumentException.class, () -> node.getGoalChild(new Action("c")));
    }

    @EpSemTest
    public void testDifferentMoveGoal(){
        Action[] actions = new Action[] {new Action("a"), new Action("b")};
        RuleNode node = new RuleNode(actions, 0, 2, 0);
        RuleNodeGoal goalChild1 = node.getGoalChild(actions[0]);
        RuleNodeGoal goalChild2 = node.getGoalChild(actions[1]);
        assertNotNull(goalChild1);
        assertNotNull(goalChild2);
        assertNotEquals(goalChild1, goalChild2);
    }

    @EpSemTest
    public void testSameMoveGoal(){
        Action[] actions = new Action[] {new Action("a"), new Action("b")};
        RuleNode node = new RuleNode(actions, 0, 2, 0);
        RuleNodeGoal goalChild1 = node.getGoalChild(actions[0]);
        assertNotNull(goalChild1);
        assertNotEquals(node, goalChild1);
        RuleNodeGoal goalChild2 = node.getGoalChild(actions[0]);
        assertNotNull(goalChild2);
        assertNotEquals(node, goalChild2);
        assertEquals(goalChild1, goalChild2);
    }

    //endregion

    //region getGoalProbability

    @EpSemTest
    public void testBadMoves(){
        Action[] actions = new Action[] {new Action("a"), new Action("b")};
        RuleNode node = new RuleNode(actions, 0, 2, 0);
        assertThrows(IllegalArgumentException.class, () -> node.getGoalProbability(null, 0));
        assertThrows(IllegalArgumentException.class, () -> node.getGoalProbability(new ArrayList<Action>(Arrays.asList(new Action("c"))), 0));
        assertEquals(0.0, node.getGoalProbability(new ArrayList<>(), 0));
    }

    @EpSemTest
    public void testBadIndex(){
        Action[] actions = new Action[] {new Action("a"), new Action("b")};
        RuleNode node = new RuleNode(actions, 0, 2, 0);
        ArrayList<Action> sequence = new ArrayList<>(Arrays.asList(actions[0], actions[1], actions[1]));
        assertEquals(0.0, node.getGoalProbability(sequence, 10)); //Index too high returns 0
        assertThrows(IllegalArgumentException.class, () -> node.getGoalProbability(sequence, -1)); //Index too low throws
    }

    @EpSemTest
    public void testBaseCases(){
        //Setup
        Action[] actions = new Action[] {new Action("a"), new Action("b")};
        ArrayList<Action> sequence = new ArrayList<>(Arrays.asList(actions[0], actions[1]));

        //Max depth returns 0
        RuleNode leafNode = new RuleNode(actions, 0, 1, 0);
        assertEquals(0.0, leafNode.getGoalProbability(sequence, 0));

        //Return if move has never been tried
        RuleNode node = new RuleNode(actions, 0, 3, 0);
        node.getGoalChild(actions[1]);
        assertEquals(0.0, node.getGoalProbability(sequence, 0));

        //Return 0 when end of sequence reached
        RuleNode nextNode = node.getNextChild(actions[0], 0);
        nextNode.getNextChild(actions[1], 0);
        assertEquals(0.0, node.getGoalProbability(sequence, sequence.size()));

        //Return 1 if goal node
        RuleNode goalNode = new RuleNodeGoal(actions, 0);
        assertEquals(1.0, goalNode.getGoalProbability(sequence, 0));
    }

    @EpSemTest
    public void testRecursiveCase(){
        Action[] actions = new Action[] {new Action("a"), new Action("b")};
        ArrayList<Action> aSequence = new ArrayList<>(Collections.singletonList(actions[0]));
        ArrayList<Action> bSequence = new ArrayList<>(Collections.singletonList(actions[1]));
        ArrayList<Action> twoSequence = new ArrayList<>(Arrays.asList(actions[0], actions[1]));
        RuleNode node = new RuleNode(actions, 0, 3, 0);
        RuleNode aChild = node.getNextChild(actions[0], 0);
        RuleNode bChild = node.getNextChild(actions[1], 0);
        node.getGoalChild(actions[0]).occurs(); //Goal child with an "a" move
        node.incrementMoveFrequency(actions[0]);
        node.getGoalChild(actions[1]).occurs();
        node.incrementMoveFrequency(actions[1]);

        //Test 1 deep recursion
        for (int i = 0; i < 9; i++){
            aChild.occurs();
            node.incrementMoveFrequency(actions[0]);
        }
        for (int i = 0; i < 19; i++){
            bChild.occurs();
            node.incrementMoveFrequency(actions[1]);
        }
        assertEquals(0.1, node.getGoalProbability(aSequence, 0), 0);
        assertEquals(0.05, node.getGoalProbability(bSequence, 0), 0);

        //Test 2 deep recursion
        aChild.getGoalChild(actions[1]).occurs();
        RuleNode grandchild = aChild.getNextChild(actions[1], 0);
        aChild.incrementMoveFrequency(actions[1]);
        for (int i = 0; i < 4; i++){
            aChild.incrementMoveFrequency(actions[1]);
            grandchild.occurs();
        }
        assertEquals(0.1 + 0.9*0.2, node.getGoalProbability(twoSequence, 0), 0.0001);
    }

    //TODO: Test root only increases by frequency 1

    //endregion

    //region expected value

    @EpSemTest
    public void testExpectedBaseCases(){
        Heuristic h = new TestHeuristic(1);
        Action[] actions = new Action[] {new Action("a"), new Action("b")};
        RuleNode node = new RuleNode(actions, 0, 2, 0);
        node.occurs();
        node.occurs();
        node.incrementMoveFrequency(actions[0]);
        node.incrementMoveFrequency(actions[0]);
        RuleNode depthLimitNode = node.getNextChild(actions[0], 0);
        depthLimitNode.occurs();
        RuleNodeGoal goalNode = node.getGoalChild(actions[0]);
        goalNode.occurs();
        ArrayList<RuleNode> current = new ArrayList<>();
        current.add(node);

        //Current not null
        assertThrows(IllegalArgumentException.class, () -> node.getExpectation(null, false, h));

        //Not in current and not top
        assertEquals(Optional.empty(), node.getExpectation(current, false, h));

        //At max depth
        assertEquals(Optional.empty(), depthLimitNode.getExpectation(current, true, h));

        //Is goal
        assertEquals(Optional.of(0.0), goalNode.getExpectation(current, true, h));

        //Not a base case
        assertNotEquals(Optional.empty(), node.getExpectation(current, true, h));
    }

    /**
     * Tests that the expected value is correct when in must call itself recursively
     *
     * TODO: Test these things with root node
     */
    @EpSemTest
    public void testExpectedRecursiveCase(){
        Action[] actions = new Action[] {new Action("a"), new Action("b")};

        //Root node, makes a twice
        RuleNode node = new RuleNode(actions, 0, 5, 0);
        node.occurs();
        node.occurs();
        node.incrementMoveFrequency(actions[0]);
        node.incrementMoveFrequency(actions[0]);

        //Child a node, visited once, made a b
        RuleNode childNode = node.getNextChild(actions[0], 0);
        childNode.occurs();
        childNode.incrementMoveFrequency(actions[1]);

        //Child a goal node, visited once
        RuleNodeGoal goalGrandChild = childNode.getGoalChild(actions[1]);
        goalGrandChild.occurs();

        //Grandchild b goal node, child of childNode, visited once
        RuleNodeGoal goalNode = node.getGoalChild(actions[0]);
        goalNode.occurs();

        //Current
        ArrayList<RuleNode> current = new ArrayList<>();
        current.add(node);

        //Returns 1.5, since it beats the heuristic
        assertEquals(Optional.of(1.5), node.getExpectation(current, true, new TestHeuristic(2)));

        //Since the heuristic is better, returns that instead
        assertEquals(Optional.of(1.0), node.getExpectation(current, true, new TestHeuristic(1)));
    }

    @EpSemTest
    public void testExpectedSideEffectsRecursiveCase(){
        Action[] actions = new Action[] {new Action("a"), new Action("b")};
        RuleNode node = new RuleNode(actions, 0, 5, 0);
        node.occurs();
        node.occurs();
        node.incrementMoveFrequency(actions[0]);
        node.incrementMoveFrequency(actions[0]);
        RuleNode childNode = node.getNextChild(actions[0], 0);
        childNode.occurs();
        childNode.incrementMoveFrequency(actions[1]);
        RuleNodeGoal goalGrandChild = childNode.getGoalChild(actions[1]);
        goalGrandChild.occurs();
        RuleNodeGoal goalNode = node.getGoalChild(actions[0]);
        goalNode.occurs();
        ArrayList<RuleNode> current = new ArrayList<>();
        current.add(node);

        //When heuristic is greater than best move, explore is false, exploit move and EV is cached
        assertEquals(Optional.of(1.5), node.getExpectation(current, true, new TestHeuristic(2)));
        assertEquals(false, node.getExplore());
        assertEquals(actions[0], node.getBestAction());
        assertEquals(1.5, node.getCachedExpectation());

        //When heuristic is less, explore is true, explore move and EV cached
        assertEquals(Optional.of(1.0), node.getExpectation(current, true, new TestHeuristic(1)));
        assertEquals(true, node.getExplore());
        assertEquals(actions[1], node.getBestAction());
        assertEquals(1.0, node.getCachedExpectation());
    }

    @EpSemTest
    public void testExpectedSideEffectsBaseCase(){
        Action[] actions = new Action[] {new Action("a"), new Action("b")};
        RuleNode node = new RuleNode(actions, 0, 2, 0);
        node.occurs();
        node.occurs();
        node.incrementMoveFrequency(actions[0]);
        node.incrementMoveFrequency(actions[0]);
        RuleNode depthLimitNode = node.getNextChild(actions[0], 0);
        depthLimitNode.occurs();
        RuleNodeGoal goalNode = node.getGoalChild(actions[0]);
        goalNode.occurs();
        ArrayList<RuleNode> current = new ArrayList<>();
        current.add(node);

        //Expected side effects: explore arbitrary, best move null, and expectation -1 on fail

        //Current not null throws, so no other tested side effects

        //Not in current and not top
        assertEquals(Optional.empty(), node.getExpectation(current, false, new TestHeuristic(1)));
        assertNull(node.getBestAction());
        assertEquals(-1.0, node.getCachedExpectation());

        //At max depth
        assertEquals(Optional.empty(), depthLimitNode.getExpectation(current, true, new TestHeuristic(1)));
        assertNull(depthLimitNode.getBestAction());
        assertEquals(-1.0, depthLimitNode.getCachedExpectation());

        //Is goal. best move null, explore false, expectation 0
        assertEquals(Optional.of(0.0), goalNode.getExpectation(current, true, new TestHeuristic(1)));
        assertNull(goalNode.getBestAction());
        assertEquals(0.0, goalNode.getCachedExpectation());
        assertEquals(false, goalNode.getExplore());
    }

    //endregion

}
