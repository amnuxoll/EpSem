package tests.agents.wfc;

import agents.wfc.WFCPathRule;
import agents.wfc.PathNode;
import agents.wfc.WFCAgent;
import framework.Action;
import framework.SensorData;
import tests.Assertions;
import tests.EpSemTest;
import tests.EpSemTestClass;

import java.util.HashMap;
import java.util.List;
import java.util.Vector;

@EpSemTestClass
public class WFCAgentTest {

    SensorData data_00 = new SensorData(false);
    SensorData data_10 = new SensorData(false);
    SensorData data_01 = new SensorData(true);
    SensorData data_11 = new SensorData(true);

    public void initialize() {
        data_00.setSensor("IS_ODD", false);
        data_10.setSensor("IS_ODD", true);
        data_01.setSensor("IS_ODD", false);
        data_11.setSensor("IS_ODD", true);
    }

    public static WFCAgent quickAgentGen(String actStr) {
        Action[] actions = new Action[actStr.length()];
        for (int i = 0; i < actions.length; ++i) {
            actions[i] = new Action("" + actStr.charAt(i));
        }
        WFCAgent agent = new WFCAgent();
        agent.initialize(actions, null);

        return agent;
    }

    @EpSemTest
    public void testMPathEquals() {
        WFCAgent agent = quickAgentGen("ab");
        initialize();

        Vector<PathNode> testPath = new Vector<>();
        testPath.add(new PathNode(data_00, 'a'));
        testPath.add(new PathNode(data_00, 'b'));
        testPath.add(new PathNode(data_01, 'a'));

        WFCPathRule rule1 = new WFCPathRule(agent, testPath);
        WFCPathRule rule2 = new WFCPathRule(agent, testPath);

        testPath.add(new PathNode(data_11, 'b'));
        WFCPathRule rule3 = new WFCPathRule(agent, testPath);

        Assertions.assertTrue(rule1.equals(rule2));
        Assertions.assertFalse(rule1.equals(rule3));

        Vector<PathNode> testPath2 = new Vector<>();
        testPath2.add(new PathNode(data_10, 'b'));
        testPath2.add(new PathNode(data_10, 'b'));
        testPath2.add(new PathNode(data_10, 'b'));

        WFCPathRule rule4 = new WFCPathRule(agent, testPath2);

        Assertions.assertFalse(rule1.equals(rule4));
    }

    @EpSemTest
    public void testPathNodeEquals() {
        WFCAgent agent = quickAgentGen("ab");
        initialize();

        PathNode node1 = new PathNode(data_00,'*');
        PathNode node2 = new PathNode(data_00,'a');
        PathNode node3 = new PathNode(data_00,'b');

        Assertions.assertTrue(node1.equals(node2));
        Assertions.assertTrue(node1.equals(node3));

        PathNode node4 = new PathNode(data_01,'*');
        PathNode node5 = new PathNode(data_11,'*');
        PathNode node6 = new PathNode(data_10,'*');

        Assertions.assertFalse(node1.equals(node4));
        Assertions.assertFalse(node1.equals(node5));
        Assertions.assertFalse(node1.equals(node6));

        Assertions.assertFalse(node2.equals(node3));
        Assertions.assertFalse(node3.equals(node2));
        Assertions.assertFalse(node3.equals(node4));
        Assertions.assertFalse(node2.equals(node4));
    }

    @EpSemTest
    public void testMatchPattern() {
        WFCAgent agent = quickAgentGen("ab");
        initialize();

        Vector<PathNode> testPath = new Vector<>();
        testPath.add(new PathNode(data_00, 'a'));
        testPath.add(new PathNode(data_00, 'b'));
        testPath.add(new PathNode(data_01, 'a'));
        testPath.add(new PathNode(data_00, 'a'));

        testPath.add(new PathNode(data_00, 'b'));
        testPath.add(new PathNode(data_00, 'a'));

        testPath.add(new PathNode(data_01, 'a'));

        testPath.add(new PathNode(data_00, 'b'));
        testPath.add(new PathNode(data_00, 'a'));

        WFCPathRule rule1 = new WFCPathRule(agent, testPath);
        agent.addRule(rule1);

        Vector<PathNode> pattern = new Vector<>();
        pattern.add(new PathNode(data_00, 'a'));

        // Size one pattern
        Vector<List<PathNode>> matching = agent.matchPattern(pattern);
        Assertions.assertTrue(matching.size() == 4);

        pattern = new Vector<>();
        pattern.add(new PathNode(data_00, 'b'));
        pattern.add(new PathNode(data_00, 'a'));

        // 2 size pattern
        Assertions.assertTrue(agent.matchPattern(pattern).size() == 2);

        pattern = new Vector<>();
        pattern.add(new PathNode(data_00, 'a'));
        pattern.add(new PathNode(data_00, 'b'));
        pattern.add(new PathNode(data_01, '*'));

        // 3 size pattern
        Assertions.assertTrue(agent.matchPattern(pattern).size() == 1);

        pattern = new Vector<>();
        pattern.add(new PathNode(data_00, 'b'));
        pattern.add(new PathNode(data_00, 'b'));

        // 2 size pattern which should NOT match!
        Assertions.assertTrue(agent.matchPattern(pattern).size() == 0);

        // NEW SECTION
        // Checking that it finds patterns with ambiguous actions
        agent = quickAgentGen("ab");;

        testPath = new Vector<>();
        testPath.add(new PathNode(data_00, 'a'));
        testPath.add(new PathNode(data_01, 'b'));

        testPath.add(new PathNode(data_00, 'a'));
        testPath.add(new PathNode(data_01, 'a'));

        rule1 = new WFCPathRule(agent, testPath);
        agent.addRule(rule1);

        pattern = new Vector<>();
        pattern.add(new PathNode(data_00, 'a'));
        pattern.add(new PathNode(data_01, '*'));

        Assertions.assertTrue(agent.matchPattern(pattern).size() == 2);


    }

    @EpSemTest
    public void testMatchPattern2() {
        WFCAgent agent = quickAgentGen("ab");
        initialize();

        Vector<PathNode> testPath = new Vector<>();
        testPath.add(new PathNode(data_00, 'a'));
        // Testing loop:
        testPath.add(new PathNode(data_00, 'a'));
        testPath.add(new PathNode(data_00, 'a'));
        testPath.add(new PathNode(data_00, 'a'));
        testPath.add(new PathNode(data_00, 'a'));
        testPath.add(new PathNode(data_00, 'a'));
        testPath.add(new PathNode(data_00, 'a'));
        testPath.add(new PathNode(data_00, 'a'));
        testPath.add(new PathNode(data_00, 'a'));
        testPath.add(new PathNode(data_00, 'a'));
        testPath.add(new PathNode(data_00, 'b'));
        testPath.add(new PathNode(data_00, 'a'));

        WFCPathRule rule1 = new WFCPathRule(agent, testPath);
        agent.addRule(rule1);

        Vector<PathNode> pattern = new Vector<>();
        pattern.add(new PathNode(data_00, 'a'));
        pattern.add(new PathNode(data_00, '*'));

        // Size one pattern
        Vector<List<PathNode>> matching = agent.matchPattern(pattern);
        System.out.println("Done");
        //Assertions.assertTrue(matching.size() == 4);
    }

    @EpSemTest
    public void testGetActionFromExperiences() {
        WFCAgent agent = quickAgentGen("ab");
        initialize();

        Vector<PathNode> fakePathTravelled = new Vector<>();
        fakePathTravelled.add(new PathNode(data_00, 'a'));
        fakePathTravelled.add(new PathNode(data_00, '*'));

        agent.setRealPathTraversedSoFar(fakePathTravelled);

        Vector<PathNode> pathRule1 = new Vector<>();
        pathRule1.add(new PathNode(data_00, 'a'));
        pathRule1.add(new PathNode(data_00, 'a'));
        pathRule1.add(new PathNode(data_00, 'a'));
        pathRule1.add(new PathNode(data_00, 'a'));
        pathRule1.add(new PathNode(data_00, 'a'));
        pathRule1.add(new PathNode(data_00, 'a'));
        pathRule1.add(new PathNode(data_00, 'b'));
        pathRule1.add(new PathNode(data_00, 'a'));

        WFCPathRule pathRule2 = new WFCPathRule(agent, pathRule1);

        agent.addRule(pathRule2);
        char act = agent.getActionFromExperiences();
        System.out.println("Here");
    }

    @EpSemTest
    public void testGetMaxMinVotes() {
        WFCAgent agent = quickAgentGen("ab");
        initialize();

        HashMap<Action, Integer> votes = new HashMap<>();

        votes.put(new Action("a"), 10);
        votes.put(new Action("b"), 5);

        Assertions.assertTrue(agent.getElectedAction(votes).getName().equals("a"));
        Assertions.assertTrue(agent.getLoserAction(votes).getName().equals("b"));

        votes = new HashMap<>();

        votes.put(new Action("a"), 5);
        votes.put(new Action("b"), 10);

        Assertions.assertTrue(agent.getElectedAction(votes).getName().equals("b"));
        Assertions.assertTrue(agent.getLoserAction(votes).getName().equals("a"));
    }
}
