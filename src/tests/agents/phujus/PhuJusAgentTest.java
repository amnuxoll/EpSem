package tests.agents.phujus;
import agents.phujus.PhuJusAgent;
import agents.phujus.Rule;
import agents.phujus.TreeNode;
import framework.Action;
import framework.SensorData;
import tests.Assertions;
import tests.EpSemTest;
import tests.EpSemTestClass;

import java.util.HashMap;

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
        HashMap<Integer, Boolean> currInternal = new HashMap<>();
        currInternal.put(0, true);

        Rule rule = new Rule(action, currExternal, currInternal, "testSensor1", true);
        Assertions.assertTrue(rule.matches(action,currExternal,currInternal));
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
//        int[][] currInternal = new int[1][4];
//        currInternal[0][0] = 1;
        HashMap<Integer, Boolean> currInternal = new HashMap<>();
        currInternal.put(0, true);

        // add rule to the agent rule's list
        Rule rule = new Rule(actions[0].getName().charAt(0), currExternal, currInternal, "testSensor1", true);
        agent.addRule(rule);
        agent.setCurrExternal(currExternal);
        agent.setCurrInternal(currInternal);

        //Initialize tree and generate children
        TreeNode root = new TreeNode(agent, agent.getRules(), agent.getNow(),
                agent.getCurrInternal(), agent.getCurrExternal(), 'z', "");
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
        HashMap<Integer, Boolean> currInternal = new HashMap<>();
        currInternal.put(0, true);

        // add rule to the agent rule's list
        Rule rule = new Rule(actions[1].getName().charAt(0), currExternal, currInternal, "testSensor1", true);
        agent.addRule(rule);
        agent.setCurrExternal(currExternal);
        agent.setCurrInternal(currInternal);

        //Initialize tree and generate children
        TreeNode root = new TreeNode(agent, agent.getRules(), agent.getNow(),
                agent.getCurrInternal(), agent.getCurrExternal(), 'z', "");
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
        HashMap<Integer, Boolean> currInternal = new HashMap<>();
        currInternal.put(0, true);

        // add rule to the agent rule's list | predict GOAL Sensor is TRUE if action "a" is taken
        Rule rule = new Rule(actions[1].getName().charAt(0), currExternal, currInternal, "GOAL", true);

        agent.addRule(rule);
        agent.setCurrExternal(currExternal);
        agent.setCurrInternal(currInternal);

        //Initialize tree and generate children
        TreeNode root = new TreeNode(agent, agent.getRules(), agent.getNow(),
                agent.getCurrInternal(), agent.getCurrExternal(), '\0', "");
        root.genSuccessors(1);

        root.printTree();
        System.out.println();
        System.out.println("The Goal Path Is: " + root.dfFindBestGoalPath(root));
        Assertions.assertTrue(root.dfFindBestGoalPath(root).equals("\0b"));
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
        HashMap<Integer, Boolean> currInternal = new HashMap<>();
        currInternal.put(0, false);
        currInternal.put(1, false);

        // add rule to the agent rule's list | predict testSensor1 is TRUE if action "a" is taken
        Rule ruleOne = new Rule(actions[0].getName().charAt(0), currExternal, currInternal, "testSensor1", true);
        ruleOne.setRHSInternal(0);

        // add rule to the agent rule's list | predict GOAL Sensor is TRUE if action "b" is taken
        currInternal.put(0, true);
        Rule ruleTwo = new Rule(actions[1].getName().charAt(0), secCurrExternal, currInternal, SensorData.goalSensor, true);
        ruleTwo.setRHSInternal(1);

        currInternal.put(1, false);
        agent.addRule(ruleOne);
        agent.addRule(ruleTwo);
        agent.setCurrExternal(currExternal);
        agent.setCurrInternal(currInternal);

        //Initialize tree and generate children
        TreeNode root = new TreeNode(agent, agent.getRules(), agent.getNow(),
                agent.getCurrInternal(), agent.getCurrExternal(), '\0', "");
        root.genSuccessors(2);

        root.printTree();
        System.out.println();
        System.out.println("The Goal Path: " + root.dfFindBestGoalPath(root));
        Assertions.assertTrue(root.dfFindBestGoalPath(root).equals("\0ab"));
    }

    @EpSemTest
    public void findBestGoalPathTestDepth3(){
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
        HashMap<Integer, Boolean> currInternal = new HashMap<>();
        currInternal.put(0, false);
        currInternal.put(1, false);
        currInternal.put(2, false);



        //Second Rule
        SensorData secCurrExternal = new SensorData(false);
        secCurrExternal.setSensor("testSensor1", true);
        secCurrExternal.setSensor("testSensor2", false);
        secCurrExternal.setSensor("testSensor3", false);
        HashMap<Integer, Boolean> secCurrInternal = new HashMap<>();
        secCurrInternal.put(0, true);
        secCurrInternal.put(1, false);
        secCurrInternal.put(2, false);


        //Third Rule
        SensorData thrCurrExternal = new SensorData(false);
        thrCurrExternal.setSensor("testSensor1", true);
        thrCurrExternal.setSensor("testSensor2", true);
        thrCurrExternal.setSensor("testSensor3", false);
        HashMap<Integer, Boolean> thrCurrInternal = new HashMap<>();
        thrCurrInternal.put(0, false);
        thrCurrInternal.put(1, true);
        thrCurrInternal.put(2, false);



        // add rule to the agent rule's list | predict testSensor1 is TRUE if action "a" is taken
        Rule ruleOne = new Rule(actions[0].getName().charAt(0), currExternal, currInternal, "testSensor1", true);
        ruleOne.setRHSInternal(0);

        // add rule to the agent rule's list | predict testSensor2 is TRUE if action "b" is taken
        Rule ruleTwo = new Rule(actions[1].getName().charAt(0), secCurrExternal, secCurrInternal, "testSensor2", true);
        ruleTwo.setRHSInternal(1);

        // add rule to the agent's rule's list | predict GOAL sensor is TRUE if action "a" is taken
        Rule ruleThree = new Rule(actions[0].getName().charAt(0), thrCurrExternal, thrCurrInternal, SensorData.goalSensor, true);
        ruleThree.setRHSInternal(2);

//        Rule ruleOne = new Rule(actions[0].getName().charAt(0), th)

        agent.addRule(ruleOne);
        agent.addRule(ruleTwo);
        agent.addRule(ruleThree);


        agent.setCurrExternal(currExternal);
        agent.setCurrInternal(currInternal);

        //Initialize tree and generate children
        TreeNode root = new TreeNode(agent, agent.getRules(), agent.getNow(),
                agent.getCurrInternal(), agent.getCurrExternal(), '\0', "");
        root.genSuccessors(3);

        root.printTree();
        System.out.println();
        System.out.println("The Goal Path: " + root.dfFindBestGoalPath(root));
        Assertions.assertTrue(root.dfFindBestGoalPath(root).equals("\0aba"));
    }

    @EpSemTest
    public void generateRulesTest(){

        //Initialize current External Sensors
        SensorData currExternal = new SensorData(false);
        currExternal.setSensor("testSensor1", false);
        currExternal.setSensor("testSensor2", false);
        currExternal.setSensor("testSensor3", false);

        //simple current internal sensors
        HashMap<Integer, Boolean> currInternal = new HashMap<>();
        currInternal.put(0, true);

        //Initialize agent with a list of actions
        Action[] actions = new Action[2];
        actions[0] = new Action("a");
        actions[1] = new Action("b");
        PhuJusAgent agent = new PhuJusAgent();
        agent.setCurrExternal(currExternal);
        agent.setCurrInternal(currInternal);
        agent.initialize(actions, null);

        //Generate rules until an agent's rule inventory is full
        agent.generateRules();

        //Check to see if rule inventory has been filled up properly
        Assertions.assertTrue(agent.ruleLimitReached());
    }

    @EpSemTest
    public void generateRulesTest2(){
        //Generate rules based on a finite state machine

        //Initialize current External Sensors
        SensorData currExternal = new SensorData(true);
        currExternal.setSensor("testSensor1", true);
        currExternal.setSensor("testSensor2", true);
        currExternal.setSensor("testSensor3", true);

        SensorData prevExternal = new SensorData(false);
        prevExternal.setSensor("testSensor1", false);
        prevExternal.setSensor("testSensor2", false);
        prevExternal.setSensor("testSensor3", false);

        //simple current internal sensors
        HashMap<Integer, Boolean> currInternal = new HashMap<>();
        currInternal.put(0, false);
        currInternal.put(1, false);
        currInternal.put(2, false);
        currInternal.put(3, false);

        //Initialize agent with a list of actions
        Action[] actions = new Action[2];
        actions[0] = new Action("a");
        actions[1] = new Action("b");
        PhuJusAgent agent = new PhuJusAgent();
        agent.setCurrExternal(currExternal);
        agent.setPrevInternal(currInternal);
        agent.setPrevExternal(prevExternal);
        agent.initialize(actions, null);

        //Generate rules until an agent's rule inventory is full
        agent.generateRules();

        //Check to see if rule inventory has been filled up properly
        Assertions.assertTrue(agent.ruleLimitReached());
    }
}
