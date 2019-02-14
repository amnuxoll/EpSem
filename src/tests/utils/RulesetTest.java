package tests.utils;

import framework.Move;
import framework.SensorData;
import tests.EpSemTest;
import tests.EpSemTestClass;
import utils.RuleNode;
import utils.RuleNodeRoot;
import utils.Ruleset;

import java.util.ArrayList;

import static tests.Assertions.*;

/**
 * Created by Ryan on 2/9/2019.
 */
@EpSemTestClass
public class RulesetTest {
    @EpSemTest
    public void nullAlphabetTest() {
        assertThrows(IllegalArgumentException.class, () -> new Ruleset(null, 2));
    }

    @EpSemTest
    public void emptyAlphabetTest() {
        assertThrows(IllegalArgumentException.class, () -> new Ruleset(new Move[] {}, 2));
    }

    @EpSemTest
    public void assertConstructorNoThrows() {
        assertNotNull(
                new Ruleset(new Move[] {new Move("a")}, 1)
        );
    }

    @EpSemTest
    public void testRulesetUpdates() {
        Ruleset ruleset = new Ruleset(new Move[] {new Move("a")},2);

        RuleNodeRoot root = (RuleNodeRoot)ruleset.getCurrent().get(0);
        assertNotNull(root);

        ruleset.update(new Move("a"), new SensorData(false));

        ArrayList<RuleNode> current = ruleset.getCurrent();
        assertTrue(!current.isEmpty());
        assertTrue(ruleset.getCurrent().contains(root));
    }
}
