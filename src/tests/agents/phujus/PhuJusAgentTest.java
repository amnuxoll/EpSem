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
                agent.getCurrInternal(), agent.getCurrExternal(), 'z');
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
        Rule rule = new Rule(actions[1].getName().charAt(0), currExternal, currInternal, "testSensor1");
        agent.addRule(rule);
        agent.setCurrExternal(currExternal);
        agent.setCurrInternal(currInternal[0]);

        //Initialize tree and generate children
        TreeNode root = new TreeNode(agent, agent.getRules(), agent.getNow(),
                agent.getCurrInternal(), agent.getCurrExternal(), 'z');
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
        Rule rule = new Rule(actions[1].getName().charAt(0), currExternal, currInternal, "GOAL");
        rule.setRhsValue(1);

        agent.addRule(rule);
        agent.setCurrExternal(currExternal);
        agent.setCurrInternal(currInternal[0]);

        //Initialize tree and generate children
        TreeNode root = new TreeNode(agent, agent.getRules(), agent.getNow(),
                agent.getCurrInternal(), agent.getCurrExternal(), '\0');
        root.genSuccessors(1);

        root.printTree();
        System.out.println();
        System.out.println("The Goal Path Is: " + root.findBestGoalPath(root).length());
        Assertions.assertTrue(root.findBestGoalPath(root).equals("\0b"));
    }

    @EpSemTest
    public void findBestGoalPathTestDepth2(){
        //Initialize agent with a list of actions
        Action[] actions = new Action[2];
        actions[0] = new Action("a");
        actions[1] = new Action("b");
        PhuJusAgent agent = new PhuJusAgent();
        agent.initialize(actions, null);

        //First Rule
        SensorData currExternal = new SensorData(false);
        currExternal.setSensor("testSensor1", false);
        currExternal.setSensor("testSensor2", false);
        currExternal.setSensor("testSensor3", false);

        //Second Rule
        SensorData secCurrExternal = new SensorData(false);
        secCurrExternal.setSensor("testSensor1", true);
        secCurrExternal.setSensor("testSensor2", false);
        secCurrExternal.setSensor("testSensor3", false);

        // create simple internal sensors for the rule class
        int[][] currInternal = new int[1][4];
        currInternal[0][0] = 0;

        // add rule to the agent rule's list | predict testSensor1 is TRUE if action "a" is taken
        Rule ruleOne = new Rule(actions[1].getName().charAt(0), currExternal, currInternal, "testSensor1");
        ruleOne.setRhsValue(1);

        // add rule to the agent rule's list | predict GOAL Sensor is TRUE if action "b" is taken
        Rule ruleTwo = new Rule(actions[0].getName().charAt(0), secCurrExternal, currInternal, SensorData.goalSensor);
        ruleTwo.setRhsValue(1);

        agent.addRule(ruleOne);
        agent.addRule(ruleTwo);
        agent.setCurrExternal(currExternal);
        agent.setCurrInternal(currInternal[0]);

        //Initialize tree and generate children
        TreeNode root = new TreeNode(agent, agent.getRules(), agent.getNow(),
                agent.getCurrInternal(), agent.getCurrExternal(), '\0');
        root.genSuccessors(2);

        root.printTree();
        System.out.println();
        System.out.println("The Goal Path: " + root.findBestGoalPath(root));
        Assertions.assertTrue(root.findBestGoalPath(root).equals("\0ba"));
    }

    @EpSemTest
    public void generateRulesTest(){
        //Initialize agent with a list of actions
        Action[] actions = new Action[2];
        actions[0] = new Action("a");
        actions[1] = new Action("b");
        PhuJusAgent agent = new PhuJusAgent();
        agent.initialize(actions, null);

        //Generate rules until an agent's rule inventory is full
        agent.generateRules();

        //Check to see if rule inventory has been filled up properly
        Assertions.assertTrue(agent.ruleLimitReached());
    }
}
