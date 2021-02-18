package tests.agents.phujus;
import agents.phujus.Rule;
import framework.SensorData;
import tests.Assertions;
import tests.EpSemTest;
import tests.EpSemTestClass;

import static environments.fsm.FSMEnvironment.Sensor.IS_EVEN;

@EpSemTestClass
public class PhuJusAgentTest {
    @EpSemTest
    public void testRuleMatch(){

        //One rule
        char actionOne = 'a';
        SensorData testExternal = new SensorData(false);
        testExternal.setSensor("IS_EVEN", true);
        int[][] testInternal = new int[1][1];
        testInternal[0][0] = 0;


        //Second rule
        char action = 'a';
        int[] testExt = new int[2];
        testExt[0] = 0;
        testExt[1] = 1;
        int[][] testInt = new int[1][1];
        testInt[0][0] = 0;
        Rule rule = new Rule(action, testExt, testInt);

        //Check if first rule equals second rule
        Assertions.assertTrue(rule.matches(actionOne, testExternal, testInternal[1]));

    }

    @EpSemTest
    public void testGenSuccessors(){

    }
}
