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

import java.util.HashSet;

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
        PhuJusAgent agent = PhuJusAgentTest.quickAgentGen("a", "010");

        SensorData sd = PhuJusAgentTest.quickExtGen("101");
        SensorData sd2 = PhuJusAgentTest.quickExtGen("010");
        SensorData sd3 = PhuJusAgentTest.quickExtGen("110");
        agent.getNextAction(sd);
        TFRule tfr1 = new TFRule(agent);

        Double rhsms = tfr1.rhsMatchScore(sd2);

        Assertions.assertEquals(rhsms, 0,0.01);

        rhsms = tfr1.rhsMatchScore(sd);
        Assertions.assertEquals(rhsms, 0.5, 0.01);

        //tfr1.updateTFVals();

        rhsms = tfr1.rhsMatchScore(sd3);
        Assertions.assertEquals(rhsms, 0.166, 0.01);

        agent.getNextAction(sd3);
        rhsms = tfr1.rhsMatchScore(sd2);
        Assertions.assertEquals(rhsms, 0.417, 0.01);


        // TODO: What's the best approach for testing this?
        // When the agent is first created and takes an action, the sensors will not be on yet.
        // They'll turn on in the next time step.
        // Should we take multiple steps and ensure the sensors are on later?
        // The issue being its hard to predict which sensors will end up being on.
    }

    @EpSemTest
    public void testInitInternal() throws Exception {
        PhuJusAgent agent = PhuJusAgentTest.quickAgentGen("a", "100011");

        SensorData sd = PhuJusAgentTest.quickExtGen("101011");
        agent.getNextAction(sd);
        agent.getNextAction(sd);

        TFRule tfr1 = new TFRule(agent);

        HashSet<TFRule.Cond> lhsInt = tfr1.getLhsInternal();

        for(TFRule.Cond cond : lhsInt){
            Assertions.assertTrue(cond.sName.length() > 0);
            Assertions.assertTrue(cond.data.getTF() >= 0);
        }

        Assertions.assertTrue(!lhsInt.isEmpty());
        TFRule.Cond[] carr = lhsInt.toArray(new TFRule.Cond[0]);
        Assertions.assertTrue(carr[0].data.numMatches == 1);
        Assertions.assertTrue(carr[0].data.numOn == 0);
        Assertions.assertTrue(carr.length == 1);

    }

    @EpSemTest
    public void testInitExternal() throws Exception {
        PhuJusAgent agent = PhuJusAgentTest.quickAgentGen("abcde", "100011");

        SensorData sd = PhuJusAgentTest.quickExtGen("101011");
        agent.getNextAction(sd);
        SensorData sd2 = PhuJusAgentTest.quickExtGen("101100");
        agent.getNextAction(sd2);

        TFRule tfr1 = new TFRule(agent);

        HashSet<TFRule.Cond> rhsExt = tfr1.getRHSExternalRaw();

        //a quick precursor check to make sure there is no error initializing
        for(TFRule.Cond cond : rhsExt){
            Assertions.assertTrue(cond.sName.length() > 0);
            Assertions.assertTrue(cond.data.getTF() >= 0);
        }

        //we should have 6 external sensors in our rhsexternal with these values
        Assertions.assertTrue(rhsExt.remove(new TFRule.Cond("sen0",true)));
        Assertions.assertTrue(rhsExt.remove(new TFRule.Cond("sen1",false)));
        Assertions.assertTrue(rhsExt.remove(new TFRule.Cond("sen2",true)));
        Assertions.assertTrue(rhsExt.remove(new TFRule.Cond("sen3",true)));
        Assertions.assertTrue(rhsExt.remove(new TFRule.Cond("sen4",false)));
        Assertions.assertTrue(rhsExt.remove(new TFRule.Cond("GOAL",false)));
        Assertions.assertTrue(rhsExt.isEmpty());

    }

    @EpSemTest
    public void testUpdateTFVals() throws Exception {

        //creating our agent
        PhuJusAgent agent = PhuJusAgentTest.quickAgentGen("a", "100011");

        SensorData sd = PhuJusAgentTest.quickExtGen("101011");
        SensorData sd2 = PhuJusAgentTest.quickExtGen("101001");
        agent.getNextAction(sd);
        agent.getNextAction(sd);
        //the tfrule that we will be calling updatetfvals for
        TFRule tfr1 = new TFRule(agent);
        agent.getNextAction(sd);

        tfr1.updateTFVals();

        //the rule was created and updated once, it should have matched so num on should increase
        TFRule.Cond[] carr = tfr1.getLhsInternal().toArray(new TFRule.Cond[0]);
        Assertions.assertTrue(carr[0].data.numMatches == 2);
        Assertions.assertTrue(carr[0].data.numOn == 1);
        Assertions.assertTrue(carr.length == 1);

        tfr1.updateTFVals();
        //the rule should have matched again so num on and matches should increase again
        carr = tfr1.getLhsInternal().toArray(new TFRule.Cond[0]);
        Assertions.assertTrue(carr[0].data.numMatches == 3);
        Assertions.assertTrue(carr[0].data.numOn == 2);
        Assertions.assertTrue(carr.length == 1);

        agent.getNextAction(sd2);
        tfr1.updateTFVals();
        //the rule should have matched again but this time it was not correct so num on should be the same
        carr = tfr1.getLhsInternal().toArray(new TFRule.Cond[0]);
        Assertions.assertTrue(carr[0].data.numMatches == 4);
        Assertions.assertTrue(carr[0].data.numOn == 2);
        Assertions.assertTrue(carr.length == 1);


    }

}
