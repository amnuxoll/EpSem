package tests.agents.marzrules;

import framework.Action;
import tests.EpSemTest;
import tests.EpSemTestClass;
import agents.marzrules.RuleNode;
import agents.marzrules.RuleNodeRoot;
import static tests.Assertions.*;

/**
 * Created by Ryan on 2/9/2019.
 */
@EpSemTestClass
public class RuleNodeRootTest {

    public static RuleNodeRoot setup(){
        Action[] actions = { new Action("a"), new Action("b"), new Action("c") };
        return new RuleNodeRoot(actions, 4);
    }

    //region Constructor tests
    @EpSemTest
    public void testZeroMaxDepth(){
        assertThrows(IllegalArgumentException.class, () -> new RuleNodeRoot(new Action[] {new Action("a") }, 0));
        assertThrows(IllegalArgumentException.class, () -> new RuleNodeRoot(new Action[] {new Action("a") }, -1));
    }

    @EpSemTest
    public void testNoThrow(){
        assertNotNull(new RuleNodeRoot(new Action[] {new Action("a")}, 1));
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
        Action action = new Action("a");
        RuleNode child1 = root.getNextChild(action, 0);
        RuleNode child2 = root.getNextChild(action, 0);
        assertEquals(child1, child2);
    }

    @EpSemTest
    public void testSameSenseDifferentMove(){
        RuleNodeRoot root = setup();
        RuleNode child1 = root.getNextChild(new Action("a"), 0);
        RuleNode child2 = root.getNextChild(new Action("b"), 0);
        assertEquals(child1, child2);
    }

    @EpSemTest
    public void testDifferentSense(){
        RuleNodeRoot root = setup();
        Action action = new Action("a");
        RuleNode child1 = root.getNextChild(action, 0);
        RuleNode child2 = root.getNextChild(action, 1);
        assertNotEquals(child1, child2);
    }
    //endregion
}
