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
import java.util.Vector;


@SuppressWarnings("unused")

@EpSemTestClass
public class PhuJusAgentTest {

    /**
     * quickAgentGen
     *
     * is a helper method that quickly creates an agent
     *
     * @param actStr  A string containing the valid action chars for this agent
     */
    public PhuJusAgent quickAgentGen(String actStr) {
        Action[] actions = new Action[actStr.length()];
        for(int i = 0; i < actions.length; ++i) {
            actions[i] = new Action("" + actStr.charAt(i));
        }
        PhuJusAgent agent = new PhuJusAgent();
        agent.initialize(actions, null);
        return agent;
    }//quickAgentGen


    /**
     * quickIntGen
     *
     * is a helper method to quickly create a set of internal sensors given a signature
     *
     * @param lhsIntStr a string of binary digits representing sensor values.  A dot
     *                  can be used as a wildcard.  Example:  "..0.1"
     */
    public HashMap<Integer, Boolean> quickIntGen(String lhsIntStr) {
        HashMap<Integer, Boolean> lhsInt = new HashMap<>();
        for(int i = 0; i < lhsIntStr.length(); ++i) {
            if (lhsIntStr.charAt(i) != '.') {
                lhsInt.put(i, lhsIntStr.charAt(i) == '1');
            }
        }
        return lhsInt;
    }//quickIntGen

    /**
     * quickIntGen
     *
     *  is a helper method to quickly create a set of external sensors given a signature
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
        for(int i = 0; i < lhsExtStr.length() - 1; ++i) {
            if (lhsExtStr.charAt(i) != '.') {
                lhsExt.setSensor("sen" + i, lhsExtStr.charAt(i) == '1');
            }
        }

        return lhsExt;
    }//quickExtGen

    /**
     * quickRuleGen
     *
     * is a helper method to quickly create a rule that matches a given
     * signature.  Example call:
     *    Rule r = quickRuleGen("0010", "000", 'b', ".1.");
     *
     * Note:  the rule is given an initial activation level
     *
     * CAVEAT:  there is no error checking so do this right :)
     *
     * @param agent      the agent that will use this rule
     * @param lhsIntStr  a sequence of binary digits indicating
     *                   the values of the internal sensors
     * @param lhsExtStr  a sequence of binary digits indicating
     *                   the values of the external sensors.
     * @param action     the rule's LHS action
     * @param rhsExtStr  a sequence of dots with one binary digit indicating
     *                   which binary digit is predicted.
     *
     * @return  a Rule object matching the specification or null on failure
     */
    public Rule quickRuleGen(PhuJusAgent agent, String lhsIntStr, String lhsExtStr, char action, String rhsExtStr) {
        //init internal sensors
        HashMap<Integer, Boolean> lhsInt = quickIntGen(lhsIntStr);

        //init external sensors
        SensorData lhsExt = quickExtGen(lhsExtStr);

        //calculate the correct sensor name for the RHS.
        String rhsSensorName = SensorData.goalSensor;
        boolean rhsSensorValue = true;
        if (rhsExtStr.endsWith(".")) {  //indifferent about goal sensor
            if (rhsExtStr.contains("0")) {
                rhsSensorName = "sen" + rhsExtStr.indexOf("0");
                rhsSensorValue = false;
            } else {
                rhsSensorName = "sen" + rhsExtStr.indexOf("1");
            }
        } else if (rhsExtStr.endsWith("0")) {
            rhsSensorValue = false;
        }

        Rule rule =  new Rule(agent, action, lhsExt, lhsInt, rhsSensorName, rhsSensorValue);
        rule.addActivation(-1, PhuJusAgent.INITIAL_ACTIVATION);  //kludge: using timestamp -1 to guarantee it's in the past
        return rule;
    }//quickRuleGen


    /**
     * Test that a rule can match current sensors
     */
    @EpSemTest
    public void testFirstRuleMatch(){
        PhuJusAgent agent = quickAgentGen("ab");
        Rule rule = quickRuleGen( agent, "0", "1100", 'a', "1...");
        SensorData currExternal = quickExtGen("1100");
        HashMap<Integer, Boolean> currInternal = quickIntGen("0");
        Assertions.assertTrue(rule.matches('a',currExternal,currInternal));
    }


    /**
     * test that the agent can build a tree from a set of rules
     */
     @EpSemTest
     public void testGenSuccessors(){
         PhuJusAgent agent = quickAgentGen("ab");

         //Add rule to the agent
         Rule rule = quickRuleGen(agent,"0", "1100", 'b', "1...");
         agent.addRule(rule);
         agent.setCurrExternal(rule.getLHSExternal());  //ensures an initial match

         //Initialize tree and generate children
         TreeNode root = new TreeNode(agent, agent.getRules(), agent.getNow(),
                 agent.getCurrInternal(), agent.getCurrExternal(), "z");
         root.genSuccessors(2);

         root.printTree();
         Assertions.assertTrue(root.toString().startsWith("z->0"));
         Assertions.assertTrue(root.toString().endsWith("0|1100"));
     }

    /**
     * tests that the agent can find a goal path at depth 1 in a tree
     */
    @EpSemTest
     public void testFindBestGoalPathDepth1(){
         PhuJusAgent agent = quickAgentGen("ab");

         //add rule to the agent rule's list | predict GOAL Sensor is TRUE if action "a" is taken
         Rule rule = quickRuleGen(agent, "0", "1100", 'a', "...1");
         agent.addRule(rule);
         agent.setCurrExternal(rule.getLHSExternal());

         //Initialize tree and generate children
         TreeNode root = new TreeNode(agent, agent.getRules(), agent.getNow(),
                 agent.getCurrInternal(), agent.getCurrExternal(), "");
         root.genSuccessors(1);

         //DEBUG output
         System.out.println("Rules:");
         agent.printRules();
         System.out.println("Tree:");
         root.printTree();
         String expected = "b";
         System.out.println();
         System.out.println("Received Goal Path: " + root.findBestGoalPath());
         System.out.println("Expected Goal Path: " + expected);
     }


    /**
     * tests that the agent can find a goal path at depth 2 in a tree
     */
     @EpSemTest
     public void testFindBestGoalPathDepth2(){
         PhuJusAgent agent = quickAgentGen("ab");

         // Create rule 1: predict sen0 is TRUE if action "a" is taken
         Rule ruleOne = quickRuleGen(agent, "..", "..0", 'a', "1..");

         // Create rule 2: predict GOAL Sensor is TRUE if action "b" is taken after rule 1 has fired
         Rule ruleTwo = quickRuleGen(agent, "1.", "1..", 'b', "..1");

         //install the rules and setup the agent so that the rules will fire as intended
         agent.addRule(ruleOne);
         agent.addRule(ruleTwo);
         agent.setCurrExternal(ruleOne.getLHSExternal());
         ruleOne.setRHSInternal(0);  //ensure ruleOne triggers the correct internal sensor so rule 2 will fire
         ruleTwo.setRHSInternal(1);  //ensure ruleTwo has a different RHS internal than ruleOne


         //Initialize tree and generate children
         TreeNode root = new TreeNode(agent, agent.getRules(), agent.getNow(),
                 agent.getCurrInternal(), agent.getCurrExternal(), "");
         root.genSuccessors(2);

         //DEBUG output
         System.out.println("Rules:");
         agent.printRules();
         System.out.println("Tree:");
         root.printTree();
         String expected = "ab";
         System.out.println();
         System.out.println("Received Goal Path: " + root.findBestGoalPath());
         System.out.println("Expected Goal Path: " + expected);

         Assertions.assertTrue(root.findBestGoalPath().equals(expected));

     }//findBestGoalPathTestDepth2

    /**
     * tests that the agent can find a goal path at depth 2 in a tree
     */
     @EpSemTest
     public void testFndBestGoalPathDepth3(){
         PhuJusAgent agent = quickAgentGen("ab");

         Rule ruleOne = quickRuleGen(agent, "000","0000", 'a', "1...");
         Rule ruleTwo = quickRuleGen(agent, "1..", "1...", 'b', ".1..");
         Rule ruleThree = quickRuleGen(agent, ".1.", ".1..", 'a', "...1");

         agent.addRule(ruleOne);
         agent.addRule(ruleTwo);
         agent.addRule(ruleThree);
         ruleOne.setRHSInternal(0);
         ruleTwo.setRHSInternal(1);
         ruleThree.setRHSInternal(2);

         agent.setCurrExternal(ruleOne.getLHSExternal());

         //Initialize tree and generate children
         TreeNode root = new TreeNode(agent, agent.getRules(), agent.getNow(),
                 agent.getCurrInternal(), agent.getCurrExternal(), "");
         root.genSuccessors(3);

         //DEBUG output
         System.out.println("Rules:");
         agent.printRules();
         System.out.println("Tree:");
         root.printTree();
         String expected = "aba";
         System.out.println();
         System.out.println("Received Goal Path: " + root.findBestGoalPath());
         System.out.println("Expected Goal Path: " + expected);

         Assertions.assertTrue(root.findBestGoalPath().equals(expected));
     }

    /**
     * tests that the agent can generate random rules based on its current sensors
     */
     @EpSemTest
     public void testGenerateRules(){
         PhuJusAgent agent = quickAgentGen("ab");
         SensorData currExternal = quickExtGen("0000");
         HashMap<Integer, Boolean> currInternal = quickIntGen("1");
         agent.setPrevExternal(currExternal);
         agent.setCurrExternal(currExternal);
         agent.setCurrInternal(currInternal);

         //Generate rules until an agent's rule inventory is full
         while(agent.getRules().size() < PhuJusAgent.MAXNUMRULES) {
             agent.generateRule(PhuJusAgent.INITIAL_ACTIVATION);
         }

         //Check to see if rule inventory has been filled up properly
         Vector<Rule> rules = agent.getRules();
         Assertions.assertTrue(rules.size() == PhuJusAgent.MAXNUMRULES);
     }

}
