package tests.agents.phujus;
import agents.phujus.PhuJusAgent;
import agents.phujus.TFRule;
import framework.Action;
import framework.SensorData;
import tests.Assertions;
import tests.EpSemTest;
import tests.EpSemTestClass;


@SuppressWarnings("unused")

@EpSemTestClass
public class PhuJusAgentTest {

    /**
     * quickAgentGen
     * <p>
     * is a helper method that quickly creates an agent
     *
     * @param actStr A string containing the valid action chars for this agent
     * @param initSensors  A binary string describing the agent's initial sensor
     *                     values (see @link {{@link #quickExtGen(String)}}
     */
    public static PhuJusAgent quickAgentGen(String actStr, String initSensors) {
        Action[] actions = new Action[actStr.length()];
        for (int i = 0; i < actions.length; ++i) {
            actions[i] = new Action("" + actStr.charAt(i));
        }
        PhuJusAgent agent = new PhuJusAgent();
        agent.initialize(actions, null);
        agent.setCurrExternal(quickExtGen(initSensors));
        return agent;
    }//quickAgentGen

    /**
     * quickExtGen
     * <p>
     * is a helper method to quickly create a set of external sensors given a signature
     *
     * @param lhsExtStr a sequence of binary digits indicating
     *                  the values of the external sensors.  Default sensor
     *                  names of sen1, sen2, ... etc. will be generated.
     *                  A dot can be used as a wildcard.
     *                  The last digit is assumed to be the goal sensor.
     *                  Example:  "0100"
     */
    public static SensorData quickExtGen(String lhsExtStr) {
        SensorData lhsExt = SensorData.createEmpty();

        //If a GOAL value is set, init lhsExt with that value
        char lastChar = lhsExtStr.charAt(lhsExtStr.length() - 1);
        if (lastChar != '.') {
            lhsExt = new SensorData(lastChar == '1');
        }

        //Create the other sensors as needed
        for (int i = 0; i < lhsExtStr.length() - 1; ++i) {
            if (lhsExtStr.charAt(i) != '.') {
                lhsExt.setSensor("sen" + i, lhsExtStr.charAt(i) == '1');
            }
        }

        return lhsExt;
    }//quickExtGen

    @EpSemTest
    public void testAddRule() throws Exception {
        PhuJusAgent agent = quickAgentGen("abcde", "1101010");
        SensorData sd = PhuJusAgentTest.quickExtGen("1011010");
        agent.getNextAction(sd);

        // Creates and adds a new TFRule
        TFRule tfr = new TFRule(agent, null);
        agent.addRule(tfr);

        // The agent should have the new rule
        Assertions.assertTrue(agent.getRules().contains(tfr));

        // The agent should have the new TF rule
        Assertions.assertTrue(agent.getTfRules().contains(tfr));
    }

    @EpSemTest
    public void testUpdateRuleSet() throws Exception {
        PhuJusAgent agent = quickAgentGen("abcde", "1101010");

        // Creates and adds a new TFRule
        TFRule tfr = new TFRule(agent, null);
        agent.addRule(tfr);


    }
//
//    /** test the EpRule.matchScore() method */
//    @EpSemTest
//    public void testEpRuleMatchScore() {
//        //Setup an agent that has taken two steps
//        PhuJusAgent agent = quickAgentGen("ab", "000");
//        SensorData sd1 = new SensorData(true);
//        sd1.setSensor("sen1", false);
//        SensorData sd2 = new SensorData(false);
//        sd2.setSensor("sen1", true);
//        try {
//            agent.getNextAction(sd1);
//        } catch(Exception e) {
//            //this should not happen
//            Assertions.assertTrue(false);
//        }
//
//        //The first rule should be a perfect match
//        EpRule er1 = new EpRule(agent);
//        Assertions.assertTrue(er1.lhsMatchScore(er1.getAction()) == 1.0);
//
//        //Take another step
//        try {
//            agent.getNextAction(sd2);
//        } catch(Exception e) {
//            //this should not happen
//            Assertions.assertTrue(false);
//        }
//
//        //Now it shouldn't be a perfect match
//        double score = er1.lhsMatchScore(er1.getAction());
//        Assertions.assertTrue(score < 1.0);
//        Assertions.assertTrue(score >= 0.0);
//
//
//    }//testEpRuleMatchScore
//
//
//
//     /** test the EpRule.compareTo() method */
//     @EpSemTest
//     public void testEpRuleCompare() {
//         //Setup an agent that has taken two steps
//         PhuJusAgent agent = quickAgentGen("ab", "000");
//         SensorData sd1 = new SensorData(true);
//         sd1.setSensor("sen1", false);
//         SensorData sd2 = new SensorData(false);
//         sd2.setSensor("sen1", true);
//         try {
//             agent.getNextAction(sd1);
//         } catch(Exception e) {
//             //this should not happen
//             Assertions.assertTrue(false);
//         }
//
//         //Create two EpRules from the agent and they should match
//         EpRule er1 = new EpRule(agent);
//         EpRule er2 = new EpRule(agent);
//         Assertions.assertTrue(er1.compareLHS(er2) == 1.0);
//
//         //Take another step
//         try {
//             agent.getNextAction(sd2);
//         } catch(Exception e) {
//             //this should not happen
//             Assertions.assertTrue(false);
//         }
//
//         //Create a new rule.  It should be different but first two should be unchanged
//         EpRule er3 = new EpRule(agent);
//         Assertions.assertTrue(er3.compareLHS(er2) < 1.0);
//         Assertions.assertTrue(er3.compareLHS(er2) >= 0.0);
//         Assertions.assertTrue(er1.compareLHS(er2) == 1.0);
//
//     }//testEpRuleCompare
//
//    /**
//     * Test that a rule can match current sensors
//     */
//    @EpSemTest
//    public void testFirstRuleMatch() throws Exception {
//        EpRule rule = quickRuleGen("ab", "010", 'a', "001");
//        PhuJusAgent agent = quickAgentGen("ab", "000");
//        agent.addRule(rule);
//        Action chosenAction = agent.getNextAction(quickExtGen("010"));
//
//        Assertions.assertEquals(chosenAction.getName().charAt(0), rule.getAction());
//
//    }
//
//
//    /**
//     * test that the agent can build a tree from a single rule
//     */
//    @EpSemTest
//    public void testCreateTreeDepth1() throws Exception {
//        //Create an agent with a single rule that leads to goal
//        EpRule rule = quickRuleGen("ab", "000", 'b', "101");
//        PhuJusAgent agent = quickAgentGen("ab", "000");
//        agent.addRule(rule);
//
//        //Build a search tree that uses that rule to "find" the goal
//        TreeNode root = new TreeNode(agent);
//        Vector<TreeNode> path = root.findBestGoalPath();
//
//        //Verify the tree has the right path
//        Assertions.assertEquals(path.size(), 1);
//        Assertions.assertEquals(path.get(0).getAction(), 'b');
//
//    }//testCreateTreeDepth1
//
//    /**
//     * test that the agent can build a tree to find a goal that's 2 steps away
//     */
//    @EpSemTest
//    public void testCreateTreeDepth2() throws Exception {
//        //These two rules create a path to the goal
//        EpRule step1 = quickRuleGen("ab", "000", 'a', "100");
//        EpRule step2 = quickRuleGen("ab", "100", 'b', "011");
//
//        //Now create a decoy rule with the same LHS action as step2 so that
//        //step2 won't be the best match for that action until after step1
//        EpRule decoy = quickRuleGen("ab", "000", 'b', "000");
//
//
//        //Create the agent with these rules
//        PhuJusAgent agent = quickAgentGen("ab", "000");
//        agent.addRule(step1);
//        agent.addRule(step2);
//        agent.addRule(decoy);
//
//        //Build a search tree that uses those rules to "find" the goal
//        TreeNode root = new TreeNode(agent);
//        Vector<TreeNode> path = root.findBestGoalPath();
//
//        //Verify the tree has the right path
//        Assertions.assertNotNull(path);
//        Assertions.assertEquals(path.size(), 2);
//        Assertions.assertEquals(path.get(0).getAction(), 'a');
//        Assertions.assertEquals(path.get(1).getAction(), 'b');
//
//    }//testCreateTreeDepth2
//
//    /**
//     * test that the agent can build a tree to find a goal that's 3 steps away
//     */
//    @EpSemTest
//    public void testCreateTreeDepth3() throws Exception {
//        //These three rules create a path to the goal
//        EpRule step1 = quickRuleGen("ab", "000", 'b', "100");
//        EpRule step2 = quickRuleGen("ab", "100", 'a', "010");
//        EpRule step3 = quickRuleGen("ab", "010", 'b', "001");
//
//        //Create a decoy rule to prevent step2 or step3 from firing at the first step
//        EpRule decoy1 = quickRuleGen("ab", "000", 'a', "000");
//
//        //Create a decoy rule to prevent step3 from firing at the second step
//        EpRule decoy2 = quickRuleGen("ab", "100", 'b', "000");
//
//
//        //Create the agent with these rules
//        PhuJusAgent agent = quickAgentGen("ab", "000");
//        agent.addRule(step1);
//        agent.addRule(step2);
//        agent.addRule(step3);
//        agent.addRule(decoy1);
//        agent.addRule(decoy2);
//
//        //Build a search tree that uses those rules to "find" the goal
//        TreeNode root = new TreeNode(agent);
//        Vector<TreeNode> path = root.findBestGoalPath();
//
//        //Verify the tree has the right path
//        Assertions.assertNotNull(path);
//        Assertions.assertEquals(path.size(), 3);
//        Assertions.assertEquals(path.get(0).getAction(), 'b');
//        Assertions.assertEquals(path.get(1).getAction(), 'a');
//        Assertions.assertEquals(path.get(2).getAction(), 'b');
//
//    }//testCreateTreeDepth3



}//class PhuJusAgentTest