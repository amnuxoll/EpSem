package tests.utils;

import framework.Move;
import tests.EpSemTest;
import tests.EpSemTestClass;
import utils.RuleNode;
import utils.RuleNodeRoot;
import static tests.Assertions.*;

/**
 * Created by Ryan on 2/9/2019.
 */
@EpSemTestClass
public class RuleNodeRootTest {

    public static RuleNodeRoot setup(){
        Move[] moves = { new Move("a"), new Move("b"), new Move("c") };
        return new RuleNodeRoot(moves, 4);
    }

    //region Constructor tests
    @EpSemTest
    public void testZeroMaxDepth(){
        assertThrows(IllegalArgumentException.class, () -> new RuleNodeRoot(new Move[] {new Move("a") }, 0));
        assertThrows(IllegalArgumentException.class, () -> new RuleNodeRoot(new Move[] {new Move("a") }, -1));
    }

    @EpSemTest
    public void testNoThrow(){
        assertNotNull(new RuleNodeRoot(new Move[] {new Move("a")}, 1));
    }
    //endregion

    //region getNextChildTest
    @EpSemTest
    public void testNullMove(){
        RuleNodeRoot root = setup();
        RuleNode child = root.getNextChild(null, 0);
        assertNotNull(child);
    }

    @EpSemTest
    public void testSameSenseSameMove(){
        RuleNodeRoot root = setup();
        Move move = new Move("a");
        RuleNode child1 = root.getNextChild(move, 0);
        RuleNode child2 = root.getNextChild(move, 0);
        assertEquals(child1, child2);
    }

    @EpSemTest
    public void testSameSenseDifferentMove(){
        RuleNodeRoot root = setup();
        RuleNode child1 = root.getNextChild(new Move("a"), 0);
        RuleNode child2 = root.getNextChild(new Move("b"), 0);
        assertEquals(child1, child2);
    }

    @EpSemTest
    public void testDifferentSense(){
        RuleNodeRoot root = setup();
        Move move = new Move("a");
        RuleNode child1 = root.getNextChild(move, 0);
        RuleNode child2 = root.getNextChild(move, 1);
        assertNotEquals(child1, child2);
    }
    //endregion
}
