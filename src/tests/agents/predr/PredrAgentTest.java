package tests.agents.predr;

import framework.*;
import tests.*;
import utils.*;
import java.util.Arrays;
import java.util.ArrayList;
import agents.predr.PredrAgent;

@EpSemTestClass
public class PredrAgentTest {


    /** A quick test to see if PredrAgent will generate the correct
        sequence of actions given the alphabet: [a,b]; */
    @EpSemTest
    public void testGetNextMove() {
        //create a PredrAgent with the right alphabet
        Action[] actions = new Action[2];
        actions[0] = new Action("a");
        actions[1] = new Action("b");
        PredrAgent testme = new PredrAgent();
        testme.initialize(actions, null);

        //Extract the first 10 actions
        SensorData anyOldSensorDataWillDo = new SensorData(false);
        anyOldSensorDataWillDo.setSensor("foo", 1);
        anyOldSensorDataWillDo.setSensor("bar", 2);
        anyOldSensorDataWillDo.setSensor("baz", 3);
        Action[] results = new Action[10];
        for(int i = 0; i < 10; ++i) {
            try {
                results[i] = testme.getNextAction(anyOldSensorDataWillDo);
            } catch(Exception e) {
                throw new AssertionFailedException("Error:  could not call getNextAction on PredrAgent object" + ExceptionUtils.getStacktrace(e));
            }
        }

        //Verify that it's a match
        String[] correct = {"a", "b", "a", "a", "a", "b", "b", "a", "b", "b" };
        for(int i = 0; i < 10; ++i) {
            Assertions.assertTrue(results[i].getName() == correct[i],
                                  "\nThe actions in results: " + Arrays.toString(results) +
                                  "\n      does not equal: " + Arrays.toString(correct));
        }
        
    }//testGetNextMove

    /** needed: a test ensures that the proper rules are created.  currently
     * can't write it because there is no accessor that lets us see the rules  */

    /** helper method for tests to create a canned episode sequence */
    private Episode quickEpMaker(int foo, int bar, int baz, int qux, boolean goal, String action) {
        SensorData sd = new SensorData(goal);
        sd.setSensor("foo", foo);
        sd.setSensor("bar", bar);
        sd.setSensor("baz", baz);
        sd.setSensor("qux", qux);
        return new Episode(sd, new Action(action));
        
    }//quickEpMaker
    
    

    @EpSemTest
    public void testFindRuleBasedSequence() {
        //create a canned starting sequence of episodes
        ArrayList<Episode> initEpmem = new ArrayList<Episode>();
        initEpmem.add(quickEpMaker(1,0,0,0,false,"a"));
        initEpmem.add(quickEpMaker(0,1,1,0,false,"a"));
        initEpmem.add(quickEpMaker(1,0,0,0,false,"c"));
        initEpmem.add(quickEpMaker(0,0,0,1,false,"c"));
        initEpmem.add(quickEpMaker(0,1,1,0,true,"b"));
        initEpmem.add(quickEpMaker(1,1,0,1,false,"b"));

        //DEBUG
        for(Episode ep : initEpmem) {
            System.err.println("INITEP: " + ep);
        }
        
        
        //create a PredrAgent with the right epmem
        Action[] actions = new Action[3];
        actions[0] = new Action("a");
        actions[1] = new Action("b");
        actions[2] = new Action("c");
        PredrAgent testme = new PredrAgent();
        testme.initialize(actions, null);
        testme.initWithEpmem(initEpmem);
        testme.setNSTNum(100); //any large num will do
        
        //Extract the next action
        SensorData sd = new SensorData(false);
        sd.setSensor("foo", 1);
        sd.setSensor("bar", 0);
        sd.setSensor("baz", 0);
        sd.setSensor("qux", 0);
        Action nextAct = null;
        try {
            nextAct = testme.getNextAction(sd);
        } catch(Exception e) {
            throw new AssertionFailedException("Error:  could not call getNextAction on PredrAgent object" + ExceptionUtils.getStacktrace(e));
        }
        

        //Verify success
        System.err.println("NST result: " + testme.getNST());
        Assertions.assertEquals("c", nextAct.getName());

        actions = new Action[2];
        actions[0] = new Action("c");
        actions[1] = new Action("c");
        Sequence correctSeq = new Sequence(actions);
        Sequence nst = testme.getNST();
        Assertions.assertEquals(correctSeq, nst);
    
    }//testFindRuleBasedSequence
    
}//class PredrAgentTest
