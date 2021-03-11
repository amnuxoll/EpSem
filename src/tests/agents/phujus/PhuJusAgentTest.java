package tests.agents.phujus;
import agents.phujus.PhuJusAgent;
import agents.phujus.Rule;
import agents.phujus.TreeNode;
import framework.Action;
import framework.SensorData;
import tests.Assertions;
import tests.EpSemTest;
import tests.EpSemTestClass;

import static environments.fsm.FSMEnvironment.Sensor.IS_EVEN;

@EpSemTestClass
public class PhuJusAgentTest {
    @EpSemTest
    public void testFirstRuleMatch(){
        //test that it matches

        char action = 'a';
        SensorData currExternal = new SensorData(true);
        currExternal.setSensor("testSensor1", true);
        currExternal.setSensor("testSensor2", true);
        currExternal.setSensor("testSensor3", false);
        int[][] currInternal = new int[1][1];
        currInternal[0][0] = 1;

        Rule rule = new Rule(action, currExternal, currInternal);
        Assertions.assertTrue(rule.matches(action,currExternal,currInternal[0]));
    }

    @EpSemTest
    public void testSecondRuleMatch(){

        //test for matches with two separate declarations
        //one rule
        char action = 'a';
        SensorData currExternal = new SensorData(true);
        currExternal.setSensor("testSensor1", true);
        currExternal.setSensor("testSensor2", true);
        currExternal.setSensor("testSensor3", false);
        int[][] currInternal = new int[1][1];
        currInternal[0][0] = 1;

        //separate rule
        SensorData sepCurrExternal = new SensorData(true);
        sepCurrExternal.setSensor("testSensor1", true);
        sepCurrExternal.setSensor("testSensor2", true);
        sepCurrExternal.setSensor("testSensor3", false);
        int[][] sepCurrInternal = new int[1][1];
        sepCurrInternal[0][0] = 1;

        Rule rule = new Rule(action, currExternal, currInternal);
        Assertions.assertTrue(rule.matches(action,sepCurrExternal,sepCurrInternal[0]));
    }

    @EpSemTest
    public void testSepRuleMatchSensOff(){

        //test for two separate declarations, sensors are off
        //one rule
        char action = 'a';
        SensorData currExternal = new SensorData(true);
        currExternal.setSensor("testSensor1", true);
        currExternal.setSensor("testSensor2", true);
        currExternal.setSensor("testSensor3", false);
        int[][] currInternal = new int[1][1];
        currInternal[0][0] = 1;

        //separate rule
        SensorData sepCurrExternal = new SensorData(false);
        sepCurrExternal.setSensor("testSensor1", false);
        sepCurrExternal.setSensor("testSensor2", false);
        sepCurrExternal.setSensor("testSensor3", false);
        int[][] sepCurrInternal = new int[1][1];
        sepCurrInternal[0][0] = 1;

        Rule rule = new Rule(action, currExternal, currInternal);
        Assertions.assertFalse(rule.matches(action,sepCurrExternal,sepCurrInternal[0]));
    }

    @EpSemTest
    public void testSepRuleMatchIntOff(){

        //test for two separate declarations, sensors are off
        //one rule
        char action = 'a';
        SensorData currExternal = new SensorData(true);
        currExternal.setSensor("testSensor1", true);
        currExternal.setSensor("testSensor2", true);
        currExternal.setSensor("testSensor3", false);
        int[][] currInternal = new int[1][1];
        currInternal[0][0] = 1;

        //separate rule
        SensorData sepCurrExternal = new SensorData(false);
        sepCurrExternal.setSensor("testSensor1", false);
        sepCurrExternal.setSensor("testSensor2", false);
        sepCurrExternal.setSensor("testSensor3", false);
        int[][] sepCurrInternal = new int[1][1];
        sepCurrInternal[0][0] = 2;

        Rule rule = new Rule(action, currExternal, currInternal);
        Assertions.assertFalse(rule.matches(action,sepCurrExternal,sepCurrInternal[0]));
    }

    @EpSemTest
    public void testGenSuccessors1(){
        //Initialize agent with a list of actions
        Action[] actions = new Action[2];
        actions[0] = new Action("a");
        actions[1] = new Action("b");
        PhuJusAgent agent = new PhuJusAgent();
        agent.initialize(actions, null);

        //Add rule to the agent
        SensorData currExternal = new SensorData(false);
        currExternal.setSensor("testSensor1", true);
        currExternal.setSensor("testSensor2", true);
        currExternal.setSensor("testSensor3", false);

        // create simple internal sensors for the rule class
        int[][] currInternal = new int[1][4];
        currInternal[0][0] = 1;

        // add rule to the agent rule's list
        Rule rule = new Rule(actions[0].getName().charAt(0), currExternal, currInternal, "testSensor1");
        agent.addRule(rule);
        agent.setCurrExternal(currExternal);
        agent.setCurrInternal(currInternal[0]);

        //Initialize tree and generate children
        TreeNode root = new TreeNode(agent, agent.getRules(), agent.getNow(),
                agent.getCurrInternal(), agent.getCurrExternal());
        root.genSuccessors(1);

        root.printTree();
        System.out.println();
        Assertions.assertTrue(true);
    }

    @EpSemTest
    public void testGenSuccessors2(){
        //Initialize agent with a list of actions
        Action[] actions = new Action[2];
        actions[0] = new Action("a");
        actions[1] = new Action("b");
        PhuJusAgent agent = new PhuJusAgent();
        agent.initialize(actions, null);

        //Add rule to the agent
        SensorData currExternal = new SensorData(false);
        currExternal.setSensor("testSensor1", true);
        currExternal.setSensor("testSensor2", true);
        currExternal.setSensor("testSensor3", false);

        // create simple internal sensors for the rule class
        int[][] currInternal = new int[1][4];
        currInternal[0][0] = 1;

        // add rule to the agent rule's list
        Rule rule = new Rule(actions[0].getName().charAt(0), currExternal, currInternal, "testSensor1");
        agent.addRule(rule);
        agent.setCurrExternal(currExternal);
        agent.setCurrInternal(currInternal[0]);

        //Initialize tree and generate children
        TreeNode root = new TreeNode(agent, agent.getRules(), agent.getNow(),
                agent.getCurrInternal(), agent.getCurrExternal());
        root.genSuccessors(2);

        root.printTree();
        Assertions.assertTrue(true);
    }

    @EpSemTest
    public void findBestGoalPathTest(){
        //Initialize agent with a list of actions
        Action[] actions = new Action[2];
        actions[0] = new Action("a");
        actions[1] = new Action("b");
        PhuJusAgent agent = new PhuJusAgent();
        agent.initialize(actions, null);

        //Add rule to the agent
        SensorData currExternal = new SensorData(false);
        currExternal.setSensor("testSensor1", true);
        currExternal.setSensor("testSensor2", true);
        currExternal.setSensor("testSensor3", false);

        // create simple internal sensors for the rule class
        int[][] currInternal = new int[1][4];
        currInternal[0][0] = 1;

        // add rule to the agent rule's list | predict GOAL Sensor is TRUE if action "a" is taken
        Rule rule = new Rule(actions[0].getName().charAt(0), currExternal, currInternal, "GOAL");
        rule.setRhsValue(1);

        agent.addRule(rule);
        agent.setCurrExternal(currExternal);
        agent.setCurrInternal(currInternal[0]);

        //Initialize tree and generate children
        TreeNode root = new TreeNode(agent, agent.getRules(), agent.getNow(),
                agent.getCurrInternal(), agent.getCurrExternal());
        root.genSuccessors(1);

        root.printTree();
        System.out.println();
        Assertions.assertTrue(root.findBestGoalPath(root).equals("a"));
    }
}
