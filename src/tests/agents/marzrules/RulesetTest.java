package tests.agents.marzrules;

import agents.marzrules.Heuristic;
import framework.Action;
import framework.SensorData;
import tests.EpSemTest;
import tests.EpSemTestClass;
import agents.marzrules.RuleNode;
import agents.marzrules.RuleNodeRoot;
import agents.marzrules.Ruleset;

import java.util.ArrayList;

import static tests.Assertions.*;

/**
 * Created by Ryan on 2/9/2019.
 */
@EpSemTestClass
public class RulesetTest {
    @EpSemTest
    public void nullAlphabetTest() {
        assertThrows(IllegalArgumentException.class, () -> new Ruleset(null, 2, new Heuristic(1, 0)));
    }

    @EpSemTest
    public void emptyAlphabetTest() {
        assertThrows(IllegalArgumentException.class, () -> new Ruleset(new Action[] {}, 2, new Heuristic(1, 0)));
    }

    @EpSemTest
    public void assertConstructorNoThrows() {
        assertNotNull(
                new Ruleset(new Action[] {new Action("a")}, 1, new Heuristic(1, 0))
        );
    }

    @EpSemTest
    public void testRulesetUpdates() {
        Ruleset ruleset = new Ruleset(new Action[] {new Action("a")},2, new Heuristic(1, 0));

        RuleNodeRoot root = (RuleNodeRoot)ruleset.getCurrent().get(0);
        assertNotNull(root);

        ruleset.update(new Action("a"), new SensorData(false));

        ArrayList<RuleNode> current = ruleset.getCurrent();
        assertTrue(!current.isEmpty());
        assertTrue(ruleset.getCurrent().contains(root));
    }
}
