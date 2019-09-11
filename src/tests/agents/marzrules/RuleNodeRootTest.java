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

    //region getMaxBits
    @EpSemTest
    public void testNoChildren(){
        Action[] actions = new Action[] {new Action("a"), new Action("b")};
        RuleNodeRoot node = new RuleNodeRoot(actions,2);
        node.occurs();
        assertEquals(node.getMaxBits(), 0.0);
    }

    @EpSemTest
    public void testOneAction(){
        Action[] actions = new Action[] {new Action("a"), new Action("b")};
        RuleNodeRoot node = new RuleNodeRoot(actions, 2);
        RuleNode child0 = node.getNextChild(actions[0], 0);
        for(int i = 0; i < 3; i++){
            child0.occurs();
            node.occurs();
        }
        assertEquals(0.0, node.getMaxBits());
        RuleNode child1 = node.getNextChild(actions[0], 1);
        child1.occurs();
        node.occurs();
        assertEquals(node.getMaxBits(), Math.log(4));
        for(int i = 0; i < 10; i++){
            child1.occurs();
            node.occurs();
        }
        assertEquals(Math.log(14/3.0), node.getMaxBits());
    }

    @EpSemTest
    public void testTwoAction(){
        Action[] actions = new Action[] {new Action("a"), new Action("b")};
        RuleNodeRoot node = new RuleNodeRoot(actions, 3);
        RuleNode childa0 = node.getNextChild(actions[0], 0);
        RuleNode childb0 = node.getNextChild(actions[1], 0);
        RuleNode childa1 = node.getNextChild(actions[0], 1);
        RuleNode childb1 = node.getNextChild(actions[1], 1);
        for(int i = 0; i < 5; i++) {
            childa0.occurs();
            node.occurs();
        }for(int i = 0; i < 6; i++){
            childa1.occurs();
            node.occurs();
        }for(int i = 0; i < 10; i++){
            childb0.occurs();
            node.occurs();
        }for(int i = 0; i < 4; i++){
            childb1.occurs();
            node.occurs();
        }
        assertEquals(Math.log(5/2.0), node.getMaxBits());
    }

    @EpSemTest
    public void testGrandchild(){
        Action[] actions = new Action[] {new Action("a"), new Action("b")};
        RuleNodeRoot node = new RuleNodeRoot(actions,3);
        RuleNode child0 = node.getNextChild(actions[0], 0);
        RuleNode child1 = node.getNextChild(actions[0], 1);
        RuleNode grandchild00 = child0.getNextChild(actions[0], 0);
        RuleNode grandchild01 = child0.getNextChild(actions[0], 1);
        RuleNode grandchild10 = child1.getNextChild(actions[0], 0);
        RuleNode grandchild11 = child1.getNextChild(actions[0], 1);
        for(int i = 0; i < 4; i++){
            child0.occurs();
            node.occurs();
        }
        for(int i = 0; i < 5; i++){
            child1.occurs();
            node.occurs();
        }
        for(int i = 0; i < 2; i++){
            grandchild00.occurs();
            child0.incrementMoveFrequency(actions[0]);
        }
        for(int i = 0; i < 2; i++){
            grandchild01.occurs();
            child0.incrementMoveFrequency(actions[0]);
        }
        grandchild10.occurs();
        child1.incrementMoveFrequency(actions[0]);
        for(int i = 0; i < 4; i++){
            grandchild11.occurs();
            child1.incrementMoveFrequency(actions[0]);
        }
        assertEquals(Math.log(5) + Math.log(9/5.0), node.getMaxBits());
    }
    //endregion

    //region getAverageBits
    @EpSemTest
    public void testNoChildrenAverage(){
        Action[] actions = new Action[] {new Action("a"), new Action("b")};
        RuleNodeRoot node = new RuleNodeRoot(actions,2);
        node.occurs();
        assertEquals(node.getAverageBits(), 0.0);
    }

    @EpSemTest
    public void testOneActionAverage(){
        Action[] actions = new Action[] {new Action("a"), new Action("b")};
        RuleNodeRoot node = new RuleNodeRoot(actions, 2);
        RuleNode child0 = node.getNextChild(actions[0], 0);
        for(int i = 0; i < 3; i++){
            child0.occurs();
            node.occurs();
        }
        assertEquals(0.0, node.getAverageBits());
        RuleNode child1 = node.getNextChild(actions[0], 1);
        child1.occurs();
        node.occurs();
        assertEquals(node.getAverageBits(), (3/4.0)*Math.log(4/3.0) + (1/4.0)*Math.log(4));
        for(int i = 0; i < 10; i++){
            child1.occurs();
            node.occurs();
        }
        assertEquals((3/14.0)*Math.log(14/3.0) + (11/14.0)*Math.log(14/11.0), node.getAverageBits());
    }

    @EpSemTest
    public void testTwoActionAverage(){
        Action[] actions = new Action[] {new Action("a"), new Action("b")};
        RuleNodeRoot node = new RuleNodeRoot(actions, 3);
        RuleNode childa0 = node.getNextChild(actions[0], 0);
        RuleNode childb0 = node.getNextChild(actions[1], 0);
        RuleNode childa1 = node.getNextChild(actions[0], 1);
        RuleNode childb1 = node.getNextChild(actions[1], 1);
        for(int i = 0; i < 5; i++) {
            childa0.occurs();
            node.occurs();
        }for(int i = 0; i < 6; i++){
            childa1.occurs();
            node.occurs();
        }for(int i = 0; i < 10; i++){
            childb0.occurs();
            node.occurs();
        }for(int i = 0; i < 4; i++){
            childb1.occurs();
            node.occurs();
        }
        assertEquals((3/5.0)*Math.log(5/3.0) + (2/5.0)*Math.log(5/2.0), node.getAverageBits());
    }

    @EpSemTest
    public void testGrandchildAverage(){
        Action[] actions = new Action[] {new Action("a"), new Action("b")};
        RuleNodeRoot node = new RuleNodeRoot(actions, 2);
        RuleNode child0 = node.getNextChild(actions[0], 0);
        RuleNode child1 = node.getNextChild(actions[0], 1);
        RuleNode grandchild00 = child0.getNextChild(actions[0], 0);
        RuleNode grandchild01 = child0.getNextChild(actions[0], 1);
        RuleNode grandchild10 = child1.getNextChild(actions[0], 0);
        RuleNode grandchild11 = child1.getNextChild(actions[0], 1);
        for(int i = 0; i < 4; i++){
            child0.occurs();
            node.occurs();
        }
        for(int i = 0; i < 5; i++){
            child1.occurs();
            node.occurs();
        }
        for(int i = 0; i < 2; i++){
            grandchild00.occurs();
            child0.incrementMoveFrequency(actions[0]);
        }
        for(int i = 0; i < 2; i++){
            grandchild01.occurs();
            child0.incrementMoveFrequency(actions[0]);
        }
        grandchild10.occurs();
        child1.incrementMoveFrequency(actions[0]);
        for(int i = 0; i < 4; i++){
            grandchild11.occurs();
            child1.incrementMoveFrequency(actions[0]);
        }
        assertEquals(((4/9.0)*(Math.log(2)+Math.log(9/4.0))) + ((5/9.0)*((1/5.0)*Math.log(5)+(4/5.0)*Math.log(5/4.0) + Math.log(9/5.0))), node.getAverageBits());
    }
    //endregion
}
