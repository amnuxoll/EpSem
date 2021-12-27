package tests.agents.phujus;

import agents.phujus.BaseRule;
import agents.phujus.EpRule;
import agents.phujus.PhuJusAgent;
import framework.Action;
import framework.SensorData;
import tests.Assertions;
import tests.EpSemTest;
import tests.EpSemTestClass;

import java.util.Arrays;
import java.util.HashSet;

/**
 * This tests both the Rule and BaseRule classes
 */
@EpSemTestClass
public class BaseRuleTest {

    public static BaseRule quickCreateBaseRule() throws Exception {
        PhuJusAgent agent = PhuJusAgentTest.quickAgentGen("abcde", "0101011");
        //agent must have taken at least one step for a rule to be created
        SensorData sd = PhuJusAgentTest.quickExtGen("0101011");
        agent.getNextAction(sd);

        //normally rules are created during getNextAction but here we'll create one after
        //it has run.  To simulate the "between times" we set the current external sensors
        //to something new
        sd = PhuJusAgentTest.quickExtGen("1010100");
        agent.setCurrExternal(sd);

        return new BaseRule(agent);
    }//quickCreateBaseRule

    @EpSemTest
    public void testCtor() throws Exception {
        PhuJusAgent agent = PhuJusAgentTest.quickAgentGen("abcde", "0101011");
        //agent must have taken at least one step for a rule to be created
        //Note:  can't use quickCreateBaseRule because we need ref to agent and action
        SensorData sd = PhuJusAgentTest.quickExtGen("1010100");
        Action act = agent.getNextAction(sd);

        //Test that the rule has been assigned an id
        //  note:  can't test that id == 1 since nextRuleId variable is static
        //         and these tests can run in any order
        BaseRule br1 = new BaseRule(agent);
        Assertions.assertTrue(br1.getId() > 0);

        //A new rule should have a new id
        BaseRule br2 = new BaseRule(agent);
        Assertions.assertTrue(br2.getId() != br1.getId());

        //The rule's action should match the one selected by the agent
        char cAct = act.toString().charAt(0);
        char rAct = br1.getAction();
        Assertions.assertTrue(cAct == rAct);
    }//testCtor

    @EpSemTest
    public void testToString() throws Exception {
        BaseRule br = quickCreateBaseRule();
        String brstr = br.toString();
        Assertions.assertTrue(brstr != null);
        Assertions.assertTrue(brstr.length() > 0);

        //rule should be shown
        String compStr = br.getAction() + " -> 1010100";
        int rulePos = brstr.indexOf(compStr);
        Assertions.assertTrue(rulePos != -1);

        //rule should have an id
        int hashPos = brstr.indexOf("#");
        int idPos = brstr.indexOf("" + br.getId());
        Assertions.assertTrue(hashPos != -1);
        Assertions.assertTrue(idPos != -1);
        Assertions.assertTrue(hashPos < idPos);
        Assertions.assertTrue(idPos < rulePos);

        //rule should have activation
        compStr = "acc=1.0";
        int accPos = brstr.indexOf(compStr);
        Assertions.assertTrue(accPos != -1);
        Assertions.assertTrue(rulePos < accPos);
    }//testToString

    @EpSemTest
    public void testConfidence() throws Exception {
        BaseRule br = quickCreateBaseRule();
        //Initial rule should be fully confident
        Assertions.assertTrue(br.getAccuracy() == 1.0);

        //Confidence can't be increased above 1.0
        br.increaseConfidence();
        Assertions.assertTrue(br.getAccuracy() == 1.0);

        //Confidence can be decreased
        br.decreaseConfidence();
        double conf = br.getAccuracy();
        Assertions.assertTrue(conf < 1.0);

        //Confidence can be further decreased
        br.decreaseConfidence();
        double conf2 = br.getAccuracy();
        Assertions.assertTrue(conf > conf2);

        //Confidence can be increased again
        br.increaseConfidence();
        double conf3 = br.getAccuracy();
        Assertions.assertTrue(conf3 > conf2);

        //Note:  not testing particular values or behaviors since confidence may be adjusted at some point
    }//testConfidence

    @EpSemTest
    public void testActivation() throws Exception {
        BaseRule br = quickCreateBaseRule();
        //Activation can be applied
        Assertions.assertTrue(br.addActivation(2, 1.0));

        //Activation can be retrieved
        Assertions.assertEquals(br.calculateActivation(2), 1.0, 0.01);

        //Same or lesser activation can't be applied to same timestep
        Assertions.assertFalse(br.addActivation(2, 1.0));
        Assertions.assertFalse(br.addActivation(2, 0.5));

        //Activation for a timestep can be increased
        Assertions.assertTrue(br.addActivation(2, 1.5));
        Assertions.assertEquals(br.calculateActivation(2), 1.5, 0.01);

        //Prior activation increases current
        Assertions.assertTrue(br.addActivation(1, 1.0));
        Assertions.assertTrue(br.calculateActivation(2) > 1.5);
    }//testActivation

    @EpSemTest
    public void testExtCond() throws Exception {

        //ctor
        BaseRule.ExtCond ec = new BaseRule.ExtCond("sen1", true);
        Assertions.assertEquals(ec.sName, "sen1");
        Assertions.assertTrue(ec.val);

        //equals method
        BaseRule.ExtCond same = new BaseRule.ExtCond("sen1", true);
        BaseRule.ExtCond diff1 = new BaseRule.ExtCond("sen1", false);
        BaseRule.ExtCond diff2 = new BaseRule.ExtCond("sen2", true);
        Assertions.assertEquals(ec, same);
        Assertions.assertNotEquals(ec, diff1);
        Assertions.assertNotEquals(ec, diff2);

        //toString method
        String ecstr = ec.toString();
        Assertions.assertTrue(ecstr.indexOf("sen1") != -1);
        Assertions.assertTrue(ecstr.indexOf("true") != -1);

        //compareTo method
        BaseRule.ExtCond[] arr = new BaseRule.ExtCond[2];
        arr[0] = diff2;
        arr[1] = ec;
        Arrays.sort(arr);
        Assertions.assertEquals(arr[0], ec);

        //hashCode method
        HashSet<BaseRule.ExtCond> hs = new HashSet<>();
        hs.add(ec);
        Assertions.assertEquals(hs.size(), 1);
        hs.add(ec);
        Assertions.assertEquals(hs.size(), 1);
        hs.add(same);
        Assertions.assertEquals(hs.size(), 1);
    }//testExtCond

    @EpSemTest
    public void testRHSMatchScore() throws Exception {
        BaseRule br = quickCreateBaseRule();

        //Test for match
        SensorData sdMatch = PhuJusAgentTest.quickExtGen("1010100");
        double score = br.rhsMatchScore(sdMatch);
        Assertions.assertEquals(score, 1.0, 0.01);

        //Test for mismatch
        SensorData sdMisMatch = PhuJusAgentTest.quickExtGen("1010101");
        score = br.rhsMatchScore(sdMisMatch);
        Assertions.assertEquals(score, 0.8571, 0.01);  // 1/7 incorrect

        //Test for complete mismatch
        sdMisMatch = PhuJusAgentTest.quickExtGen("0101011");
        score = br.rhsMatchScore(sdMisMatch);
        Assertions.assertEquals(score, 0.0, 0.01);  // 1/7 incorrect
    }//testRHSMatchScore

    @EpSemTest
    public void testLHSMatchScore() throws Exception {
        BaseRule br = quickCreateBaseRule();

        //Test for match
        char act = br.getAction();
        double score = br.lhsMatchScore(act, null, null);
        Assertions.assertEquals(score, 1.0, 0.01);

        //Test for mismatch
        char wrongAct = 'a';
        if (wrongAct == act) {
            wrongAct = 'b';
        }
        score = br.lhsMatchScore(wrongAct, null, null);
        Assertions.assertEquals(score, 0.0, 0.01);
    }//testLHSMatchScore

    @EpSemTest
    public void testSpawn() throws Exception {
        BaseRule br = quickCreateBaseRule();
        EpRule er = br.spawn();

        //should be different rules
        Assertions.assertNotEquals(br, er);
        Assertions.assertTrue(br.getId() != er.getId());

        //Test expected features of the EpRule
        Assertions.assertTrue(er.getTimeDepth() == 0);
        String erstr = er.toString();
        int extLHSPos = erstr.indexOf("0101011");
        Assertions.assertTrue(extLHSPos != -1);

    }//testSpawn

    @EpSemTest
    public void testCompareLHS() throws Exception {
        BaseRule br1 = quickCreateBaseRule();
        EpRule er1 = br1.spawn();

        //Test for match
        double score = br1.compareLHS(er1, 0);
        Assertions.assertEquals(score, 1.0, 0.01);

        //Test for mismatch
        BaseRule br2 = quickCreateBaseRule();
        while(br2.getAction() == br1.getAction()) {
            br2 = quickCreateBaseRule();
        }
        EpRule er2 = br2.spawn();
        score = br1.compareLHS(er2, 0);
        Assertions.assertEquals(score, 0.0, 0.01);

    }//testCompareLHS

    @EpSemTest
    public void testIsAncestor() throws Exception {
        BaseRule br1 = quickCreateBaseRule();
        EpRule er1 = br1.spawn();

        //Test for ancestor
        Assertions.assertTrue(er1.isAncestor(br1));

        //Can't be an ancestor of yourself
        Assertions.assertFalse(er1.isAncestor(er1));

        //Test not an ancestor
        BaseRule br2 = quickCreateBaseRule();
        Assertions.assertFalse(er1.isAncestor(br2));
    }


}
