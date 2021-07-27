package tests.agents.phujus;
import agents.phujus.EpRule;
import agents.phujus.PhuJusAgent;
import agents.phujus.Rule;
import agents.phujus.TreeNode;
import framework.Action;
import framework.SensorData;
import tests.Assertions;
import tests.EpSemTest;
import tests.EpSemTestClass;

import java.util.HashMap;
import java.util.Vector;


@SuppressWarnings("unused")

@EpSemTestClass
public class PhuJusAgentTest {

    /**
     * quickAgentGen
     * <p>
     * is a helper method that quickly creates an agent
     *
     * @param actStr A string containing the valid action chars for this agent
     */
    public PhuJusAgent quickAgentGen(String actStr) {
        Action[] actions = new Action[actStr.length()];
        for (int i = 0; i < actions.length; ++i) {
            actions[i] = new Action("" + actStr.charAt(i));
        }
        PhuJusAgent agent = new PhuJusAgent();
        agent.initialize(actions, null);
        return agent;
    }//quickAgentGen


    /**
     * quickIntGen
     * <p>
     * is a helper method to quickly create a set of internal sensors given a signature
     *
     * @param lhsIntStr a string of binary digits representing sensor values.  A dot
     *                  can be used as a wildcard.  Example:  "..0.1"
     */
    public HashMap<Integer, Boolean> quickIntGen(String lhsIntStr) {
        HashMap<Integer, Boolean> lhsInt = new HashMap<>();
        for (int i = 0; i < lhsIntStr.length(); ++i) {
            if (lhsIntStr.charAt(i) != '.') {
                lhsInt.put(i, lhsIntStr.charAt(i) == '1');
            }
        }
        return lhsInt;
    }//quickIntGen

    /**
     * quickIntGen
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
    public SensorData quickExtGen(String lhsExtStr) {
        SensorData lhsExt = SensorData.createEmpty();

        //If a GOAL value is set, init lhsExt with that value
        char lastChar = lhsExtStr.charAt(lhsExtStr.length() - 1);
        if (lastChar != '.') {
            lhsExt = new SensorData(lastChar == 1);
        }

        //Create the other sensors as needed
        for (int i = 0; i < lhsExtStr.length() - 1; ++i) {
            if (lhsExtStr.charAt(i) != '.') {
                lhsExt.setSensor("sen" + i, lhsExtStr.charAt(i) == '1');
            }
        }

        return lhsExt;
    }//quickExtGen

    /**
     * quickRuleGen
     * <p>
     * is a helper method to quickly create rules that match a given
     * sequence of external sensor values and actions.  The resulting
     * rules are added to a given agent (side effect).
     * Rule r = quickRuleGen(agent, {"100", "010", "011"});
     * <p>
     * Note:  This method does not support internal sensors since there is no
     * easy way to control those.
     * CAVEAT:  there is no error checking so do this right :)
     *
     * @param agent the agent that will use this rule
     * @param exts  an array of strings of binary digits that represent sensor values.
     * @return the actions that were assigned to each rule (as a String)
     */
    public String quickRuleGen(PhuJusAgent agent, String[] exts) {

        //Extract the actions allowed for this agent
        Action[] acts = agent.getActionList();
        String actions = "";
        for (int i = 0; i < acts.length; ++i) {
            actions += acts[i].getName().charAt(0);
        }

        //a temporary agent is used to create the rules so as not to disturb
        // the state of the given agent
        PhuJusAgent tempAgent = quickAgentGen(actions);
        actions = "";
        for (int i = 0; i < exts.length; ++i) {
            SensorData sd = quickExtGen(exts[i]);
            try {
                actions += tempAgent.getNextAction(sd);
            } catch (Exception e) {
            }
        }

        for (EpRule r : tempAgent.getRules()) {
            agent.addRule(r);
        }

        return actions;
    }//quickRuleGen


    /**
     * Test that a rule can match current sensors
     */
    @EpSemTest
    public void testFirstRuleMatch() throws Exception {
        PhuJusAgent agent = quickAgentGen("ab");
        String[] extList = {"001"};
        String action = quickRuleGen(agent, extList);
        Action chosenAction = chosenAction = agent.getNextAction(quickExtGen("001"));

        Assertions.assertEquals(chosenAction.getName(), action);

    }

//    /**
//     * test that the agent can build a tree from a set of rules
//     */
//     @EpSemTest
//     public void testGenSuccessors(){
//         PhuJusAgent agent = quickAgentGen("ab");
//
//         //Add rule to the agent
//         Rule rule = quickRuleGen(agent,"0", "1100", 'b', "1...");
//         agent.addRule(rule);
//         agent.setCurrExternal(rule.getLHSExternal());  //ensures an initial match
//
//         //Initialize tree and generate children
//         TreeNode root = new TreeNode(agent, agent.getRules(), agent.getNow(),
//                 agent.getCurrInternal(), agent.getCurrExternal(), "z");
//         root.genSuccessors();
//
//         root.printTree();
//         Assertions.assertTrue(root.toString().startsWith("z->0"));
//         Assertions.assertTrue(root.toString().endsWith("0|1100"));
//     }
//
//     /** test the EpRule.compareTo() method */
//     @EpSemTest
//     public void testEpRuleCompare() {
//         //Setup an agent that has taken two steps
//         PhuJusAgent agent = quickAgentGen("ab");
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
//         Assertions.assertTrue(er1.compareTo(er2) == 1.0);
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
//         Assertions.assertTrue(er3.compareTo(er2) < 1.0);
//         Assertions.assertTrue(er3.compareTo(er2) >= 0.0);
//         Assertions.assertTrue(er1.compareTo(er2) == 1.0);
//
//     }//testEpRuleCompare
//
//    /** test the EpRule.matchScore() method */
//    @EpSemTest
//    public void testEpRuleMatchScore() {
//        //Setup an agent that has taken two steps
//        PhuJusAgent agent = quickAgentGen("ab");
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
//        Assertions.assertTrue(er1.matchScore() == 1.0);
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
//        Assertions.assertTrue(er1.matchScore() < 1.0);
//        Assertions.assertTrue(er1.matchScore() >= 0.0);
//
//
//    }//testEpRuleMatchScore
//
//
//    /**
//     * tests that the agent can generate random rules based on its current sensors
//     */
//    @EpSemTest
//    public void testGenerateRules(){
//        PhuJusAgent agent = quickAgentGen("ab");
//        SensorData currExternal = quickExtGen("0000");
//        HashMap<Integer, Boolean> currInternal = quickIntGen("1");
//        agent.setPrevExternal(currExternal);
//        agent.setCurrExternal(currExternal);
//        agent.setCurrInternal(currInternal);
//
//        //Generate rules until an agent's rule inventory is full
//        while(agent.getRules().size() < PhuJusAgent.MAXNUMRULES) {
//            Rule candidate = agent.generateRule(PhuJusAgent.INITIAL_ACTIVATION);
//            if(candidate != null) {
//                agent.addRule(candidate);
//            }
//        }
//
//        //Check to see if rule inventory has been filled up properly
//        Vector<Rule> rules = agent.getRules();
//        Assertions.assertTrue(rules.size() == PhuJusAgent.MAXNUMRULES);
//    }
//
//    /**
//     * tests that the agent can find a goal path at depth 1 in a tree
//     */
//    @EpSemTest
//     public void testFindBestGoalPathDepth1(){
//         PhuJusAgent agent = quickAgentGen("ab");
//
//         //add rule to the agent rule's list | predict GOAL Sensor is TRUE if action "a" is taken
//         Rule rule = quickRuleGen(agent, "0", "1100", 'a', "...1");
//         agent.addRule(rule);
//         agent.setCurrExternal(rule.getLHSExternal());
//
//         //Initialize tree and generate children
//         TreeNode root = new TreeNode(agent, agent.getRules(), agent.getNow(),
//                 agent.getCurrInternal(), agent.getCurrExternal(), "");
//         root.genSuccessors();
//
//         //DEBUG output
//         System.out.println("Rules:");
//         agent.printRules();
//         System.out.println("Tree:");
//         root.printTree();
//         String expected = "b";
//         System.out.println();
//         System.out.println("Received Goal Path: " + root.findBestGoalPath());
//         System.out.println("Expected Goal Path: " + expected);
//     }
//
//
//    /**
//     * tests that the agent can find a goal path at depth 2 in a tree
//     */
//     @EpSemTest
//     public void testFindBestGoalPathDepth2(){
//         PhuJusAgent agent = quickAgentGen("ab");
//
//         // Create rule 1: predict sen0 is TRUE if action "a" is taken
//         Rule ruleOne = quickRuleGen(agent, "..", "..0", 'a', "1..");
//
//         // Create rule 2: predict GOAL Sensor is TRUE if action "b" is taken after rule 1 has fired
//         Rule ruleTwo = quickRuleGen(agent, "1.", "1..", 'b', "..1");
//
//         //install the rules and setup the agent so that the rules will fire as intended
//         agent.addRule(ruleOne);
//         agent.addRule(ruleTwo);
//         agent.setCurrExternal(ruleOne.getLHSExternal());
//         ruleOne.setRHSInternal(0);  //ensure ruleOne triggers the correct internal sensor so rule 2 will fire
//         ruleTwo.setRHSInternal(1);  //ensure ruleTwo has a different RHS internal than ruleOne
//
//
//         //Initialize tree and generate children
//         TreeNode root = new TreeNode(agent, agent.getRules(), agent.getNow(),
//                 agent.getCurrInternal(), agent.getCurrExternal(), "");
//         root.genSuccessors();
//
//         //DEBUG output
//         System.out.println("Rules:");
//         agent.printRules();
//         System.out.println("Tree:");
//         root.printTree();
//         String expected = "ab";
//         System.out.println();
//         System.out.println("Received Goal Path: " + root.findBestGoalPath());
//         System.out.println("Expected Goal Path: " + expected);
//
//         Assertions.assertTrue(root.findBestGoalPath().equals(expected));
//
//     }//findBestGoalPathTestDepth2
//
//    /**
//     * tests that the agent can find a goal path at depth 2 in a tree
//     */
//     @EpSemTest
//     public void testFndBestGoalPathDepth3(){
//         PhuJusAgent agent = quickAgentGen("ab");
//
//         Rule ruleOne = quickRuleGen(agent, "000","0000", 'a', "1...");
//         Rule ruleTwo = quickRuleGen(agent, "1..", "1...", 'b', ".1..");
//         Rule ruleThree = quickRuleGen(agent, ".1.", ".1..", 'a', "...1");
//
//         agent.addRule(ruleOne);
//         agent.addRule(ruleTwo);
//         agent.addRule(ruleThree);
//         ruleOne.setRHSInternal(0);
//         ruleTwo.setRHSInternal(1);
//         ruleThree.setRHSInternal(2);
//
//         agent.setCurrExternal(ruleOne.getLHSExternal());
//
//         //Initialize tree and generate children
//         TreeNode root = new TreeNode(agent, agent.getRules(), agent.getNow(),
//                 agent.getCurrInternal(), agent.getCurrExternal(), "");
//         root.genSuccessors();
//
//         //DEBUG output
//         System.out.println("Rules:");
//         agent.printRules();
//         System.out.println("Tree:");
//         root.printTree();
//         String expected = "aba";
//         System.out.println();
//         System.out.println("Received Goal Path: " + root.findBestGoalPath());
//         System.out.println("Expected Goal Path: " + expected);
//
//         Assertions.assertTrue(root.findBestGoalPath().equals(expected));
//     }
}