package tests.agents.phujus;

import agents.marzrules.TestHeuristic;
import agents.phujus.PhuJusAgent;
import agents.phujus.TFRule;
import framework.Action;
import framework.SensorData;
import jdk.jshell.spi.ExecutionControlProvider;
import tests.Assertions;
import tests.EpSemTest;
import tests.EpSemTestClass;

/**
 * This Class tests the TFRule Class
 */
@EpSemTestClass
public class TFRuleTest {

    @EpSemTest
    public static TFRule quickCreateTFRule() throws Exception {
        PhuJusAgent agent = PhuJusAgentTest.quickAgentGen("abcde", "0101101");

        SensorData sd = PhuJusAgentTest.quickExtGen("1011010");
        agent.getNextAction(sd);

        sd = PhuJusAgentTest.quickExtGen("1001011");
        agent.setCurrExternal(sd);

        return new TFRule(agent);

    }

    @EpSemTest
    public void testCstor() throws Exception {
        PhuJusAgent agent = PhuJusAgentTest.quickAgentGen("abcde", "0101101");

        SensorData sd = PhuJusAgentTest.quickExtGen("1011010");
        Action act = agent.getNextAction(sd);

        // The TFRule's ID should be unique
        TFRule tfr1 = new TFRule(agent);
        Assertions.assertTrue(tfr1.getId() > 0);

        TFRule tfr2 = new TFRule(agent);
        Assertions.assertTrue(tfr1.getId() != tfr2.getId());

        // The TFRule's action should match the selected
        char cAct = act.toString().charAt(0);
        char rAct = tfr1.getAction();
        Assertions.assertTrue(cAct == rAct);

        // Will need to check initInternal and initExternal are being properly set
    }

    @EpSemTest
    public void testTFData() throws Exception {
        TFRule.TFData tfd = new TFRule.TFData(3.0, 2.0);

        Assertions.assertEquals(tfd.numMatches, 3.0, 0.01);
        Assertions.assertEquals(tfd.numOn, 2.0, 0.01);
        Assertions.assertEquals(tfd.getTF(), 0.66, 0.01);
    }

    @EpSemTest
    public void testCond() throws Exception {
        // testing Cond Ctor
        TFRule.Cond c1 = new TFRule.Cond("1", true);


        // The data of Cond with an initial value of true should match corresponding TFData
        Assertions.assertEquals(c1.sName, "1");
        Assertions.assertTrue(c1.data.numMatches == 1.0);
        Assertions.assertTrue(c1.data.numOn == 1.0);

        // testing equals
        TFRule.Cond c2 = new TFRule.Cond("1", false);


        // The data of Cond with an initial value of false should match corresponding TFData
        Assertions.assertTrue(c2.data.numMatches == 1.0);
        Assertions.assertTrue(c2.data.numOn == 0.0);

        // should be false because c1 tfData has a differnt numOn than c2
        Assertions.assertTrue(!(c1.equals(c2)));


        TFRule.Cond c3 = new TFRule.Cond("1", true);
        Assertions.assertTrue(c1.equals(c3));

    }

    @EpSemTest
    public void testRhsMatchScore() throws Exception {
        PhuJusAgent agent = PhuJusAgentTest.quickAgentGen("abcde", "0101101");

        SensorData sd = PhuJusAgentTest.quickExtGen("1011010");

        TFRule tfr1 = new TFRule(agent);

        Double rhsms = tfr1.rhsMatchScore(sd);

        // TODO: What's the best approach for testing this?
        // When the agent is first created and takes an action, the sensors will not be on yet.
        // They'll turn on in the next time step.
        // Should we take multiple steps and ensure the sensors are on later?
        // The issue being its hard to predict which sensors will end up being on.
    }
}
