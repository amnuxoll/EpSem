package tests.utils;

import com.sun.javafx.css.Rule;
import framework.Move;
import tests.EpSemTest;
import tests.EpSemTestClass;
import utils.Random;
import utils.RuleNode;
import utils.RuleNodeGoal;

import java.util.ArrayList;
import java.util.Arrays;

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

    //region getGoalProbability

    @EpSemTest
    public void testBadMoves(){
        Move[] moves = new Move[] {new Move("a"), new Move("b")};
        RuleNode node = new RuleNode(moves, 0, 2);
        assertThrows(IllegalArgumentException.class, () -> node.getGoalProbability(null, 0));
        assertThrows(IllegalArgumentException.class, () -> node.getGoalProbability(new ArrayList<Move>(Arrays.asList(new Move("c"))), 0));
        assertEquals(0.0, node.getGoalProbability(new ArrayList<>(), 0));
    }

    @EpSemTest
    public void testBadIndex(){
        Move[] moves = new Move[] {new Move("a"), new Move("b")};
        RuleNode node = new RuleNode(moves, 0, 2);
        ArrayList<Move> sequence = new ArrayList<>(Arrays.asList(moves[0], moves[1], moves[1]));
        assertEquals(0.0, node.getGoalProbability(sequence, 10)); //Index too high returns 0
        assertThrows(IllegalArgumentException.class, () -> node.getGoalProbability(sequence, -1)); //Index too low throws
    }

    @EpSemTest
    public void testBaseCases(){
        //Setup
        Move[] moves = new Move[] {new Move("a"), new Move("b")};
        ArrayList<Move> sequence = new ArrayList<>(Arrays.asList(moves[0], moves[1]));

        //Max depth returns 0
        RuleNode leafNode = new RuleNode(moves, 0, 0);
        assertEquals(0.0, leafNode.getGoalProbability(sequence, 0));

        //Return if move has never been tried
        RuleNode node = new RuleNode(moves, 0, 2);
        node.getGoalChild(moves[1]);
        assertEquals(0.0, node.getGoalProbability(sequence, 0));

        //Return 0 when end of sequence reached
        RuleNode nextNode = node.getNextChild(moves[0], 0);
        nextNode.getNextChild(moves[1], 0);
        assertEquals(0.0, node.getGoalProbability(sequence, sequence.size()));

        //Return 1 if goal node
        RuleNode goalNode = new RuleNodeGoal(moves);
        assertEquals(1.0, goalNode.getGoalProbability(sequence, 0));
    }

    @EpSemTest
    public void testRecursiveCase(){
        Move[] moves = new Move[] {new Move("a"), new Move("b")};
        ArrayList<Move> aSequence = new ArrayList<>(Arrays.asList(moves[0]));
        ArrayList<Move> bSequence = new ArrayList<>(Arrays.asList(moves[1]));
        ArrayList<Move> twoSequence = new ArrayList<>(Arrays.asList(moves[0], moves[1]));
        RuleNode node = new RuleNode(moves, 0, 2);
        RuleNode aChild = node.getNextChild(moves[0], 0);
        node.getGoalChild(moves[0]); //Goal child with an "a" move
        node.getGoalChild(moves[1]);

        //Test 1 deep recursion
        for (int i = 0; i < 8; i++){
            node.getNextChild(moves[0], 0);
        }
        for (int i = 0; i < 19; i++){
            node.getNextChild(moves[1], 0);
        }
        assertEquals(0.1, node.getGoalProbability(aSequence, 0), 0);
        assertEquals(0.05, node.getGoalProbability(bSequence, 0), 0);

        //Test 2 deep recursion
        aChild.getGoalChild(moves[1]);
        for (int i = 0; i < 4; i++){
            aChild.getNextChild(moves[1], 0);
        }
        assertEquals(0.1 + 0.9*0.2, node.getGoalProbability(twoSequence, 0), 0.0001);
    }

    //endregion

}
