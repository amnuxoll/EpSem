package tests.agents.predr;

import framework.*;
import tests.*;
import agents.predr.Rule;

@EpSemTestClass
public class RuleTest {

    /** this is a simple test.  More unit test would be nice. */
    @EpSemTest
    public void testMergeWith() {
        //create two rules to merge together
        SensorData lhs1 = new SensorData(false);
        lhs1.setSensor("foo", 1);
        lhs1.setSensor("bar", 2);
        lhs1.setSensor("baz", 3);
        Action action = new Action("a");
        SensorData rhs = new SensorData(true);
        SensorData lhs2 = new SensorData(false);
        lhs2.setSensor("foo", 3);
        lhs2.setSensor("bar", 2);
        lhs2.setSensor("baz", 1);
        Episode ep1 = new Episode(lhs1, action);
        Episode ep2 = new Episode(lhs2, action);
        Rule rule1 = new Rule(ep1, rhs, SensorData.goalSensor, 0);
        Rule rule2 = new Rule(ep2, rhs, SensorData.goalSensor, 0);

        //Merge them
        Rule result = rule1.mergeWith(rule2);

        //Verify they merged correctly
        Assertions.assertNotNull(result);
        Assertions.assertTrue(result.getLHS().get(0).getSensorData().getSensorNames().size() == 2);  //bar and GOAL
        Assertions.assertTrue(result.getLHS().get(0).getSensorData().hasSensor("bar"));
        Assertions.assertTrue(result.getLHS().get(0).getAction().equals(action));
        Assertions.assertTrue(result.getRHS().hasSensor(SensorData.goalSensor));
        
    }//testGetNextMove

    /** TODO: a test for merging rules with multiple sensordata on the LHS.
     * Right now we aren't creating such rules but we will someday  */
    
}//class PredrAgentTest
