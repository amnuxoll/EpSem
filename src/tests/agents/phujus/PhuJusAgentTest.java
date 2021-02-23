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
    public void testGenSuccessors(){
        Assertions.assertTrue(true);

    }
}
