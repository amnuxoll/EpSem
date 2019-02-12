package tests.utils;

import framework.Move;
import tests.EpSemTest;
import tests.EpSemTestClass;
import utils.RuleNode;
import utils.RuleNodeGoal;

import static tests.Assertions.*;


/**
 * Created by Ryan on 2/9/2019.
 */
@EpSemTestClass
public class RuleNodeTest {

    //region Constructor tests

    @EpSemTest
    public void testEmptyAlphabet(){
        assertThrows(IllegalArgumentException.class, () -> new RuleNode(null, 0, 1));
        assertThrows(IllegalArgumentException.class, () -> new RuleNode(new Move[] {}, 0, 1));
    }

    @EpSemTest
    public void testNegativeDepth(){
        assertThrows(IllegalArgumentException.class, () -> new RuleNode(new Move[] {new Move("a")}, 0, -1));
    }

    @EpSemTest
    public void testNoThrow(){
        assertNotNull(new RuleNode(new Move[] {new Move("a")}, 0, 1));
    }

    //endregion

    //region getNextChild tests

    @EpSemTest
    public void testMaxDepth(){
        Move move = new Move("a");
        assertNull(new RuleNode(new Move[] {move}, 0, 0).getNextChild(move, 1));
    }

    @EpSemTest
    public void testNullMove(){
        RuleNode node = new RuleNode(new Move[] {new Move("a")}, 0, 2);
        assertThrows(IllegalArgumentException.class, () -> node.getNextChild(null, 1));
    }

    @EpSemTest
    public void testSameChild(){
        Move[] moves = new Move[] {new Move("a"), new Move("b")};
        RuleNode node = new RuleNode(moves, 0, 2);
        RuleNode child1 = node.getNextChild(moves[0], 0);
        assertNotNull(child1);
        assertNotEquals(node, child1);
        assertFalse(child1 instanceof RuleNodeGoal);
        RuleNode child2 = node.getNextChild(moves[0], 0);
        assertNotNull(child2);
        assertNotEquals(node, child2);
        assertFalse(child2 instanceof RuleNodeGoal);
        assertEquals(child1, child2);
    }

    @EpSemTest
    public void testDifferentMove(){
        Move[] moves = new Move[] {new Move("a"), new Move("b")};
        RuleNode node = new RuleNode(moves, 0, 2);
        RuleNode child1 = node.getNextChild(moves[0], 0);
        RuleNode child2 = node.getNextChild(moves[1], 0);
        assertNotNull(child1);
        assertNotNull(child2);
        assertNotEquals(child1, child2);
    }

    @EpSemTest
    public void testDifferentSense(){
        Move[] moves = new Move[] {new Move("a"), new Move("b")};
        RuleNode node = new RuleNode(moves, 0, 2);
        RuleNode child1 = node.getNextChild(moves[0], 0);
        RuleNode child2 = node.getNextChild(moves[0], 1);
        assertNotNull(child1);
        assertNotNull(child2);
        assertNotEquals(child1, child2);
    }

    @EpSemTest
    public void testIllegalMove(){
        Move[] moves = new Move[] {new Move("a"), new Move("b")};
        RuleNode node = new RuleNode(moves, 0, 2);
        assertThrows(IllegalArgumentException.class, () -> node.getNextChild(new Move("c"), 0));
    }

    //endregion

    //region getGoalChild

    @EpSemTest
    public void testMaxDepthGoal(){
        Move[] moves = new Move[] {new Move("a"), new Move("b")};
        RuleNode node = new RuleNode(moves, 0, 0);
        assertNull(node.getGoalChild(moves[0]));
    }

    @EpSemTest
    public void testBadMoveGoal(){
        Move[] moves = new Move[] {new Move("a"), new Move("b")};
        RuleNode node = new RuleNode(moves, 0, 2);
        assertThrows(IllegalArgumentException.class, () -> node.getGoalChild(null));
        assertThrows(IllegalArgumentException.class, () -> node.getGoalChild(new Move("c")));
    }

    @EpSemTest
    public void testDifferentMoveGoal(){
        Move[] moves = new Move[] {new Move("a"), new Move("b")};
        RuleNode node = new RuleNode(moves, 0, 2);
        RuleNodeGoal goalChild1 = node.getGoalChild(moves[0]);
        RuleNodeGoal goalChild2 = node.getGoalChild(moves[1]);
        assertNotNull(goalChild1);
        assertNotNull(goalChild2);
        assertNotEquals(goalChild1, goalChild2);
    }

    @EpSemTest
    public void testSameMoveGoal(){
        Move[] moves = new Move[] {new Move("a"), new Move("b")};
        RuleNode node = new RuleNode(moves, 0, 2);
        RuleNodeGoal goalChild1 = node.getGoalChild(moves[0]);
        assertNotNull(goalChild1);
        assertNotEquals(node, goalChild1);
        RuleNodeGoal goalChild2 = node.getGoalChild(moves[0]);
        assertNotNull(goalChild2);
        assertNotEquals(node, goalChild2);
        assertEquals(goalChild1, goalChild2);
    }

    //endregion

}
