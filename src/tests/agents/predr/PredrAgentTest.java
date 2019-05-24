package tests.agents.predr;

import framework.*;
import tests.*;
import utils.*;
import java.util.Arrays;
import agents.predr.PredrAgent;

@EpSemTestClass
public class PredrAgentTest {


    /** A quick test to see if PredrAgent will generate the correct
        sequence of moves given the alphabet: [a,b]; */
    @EpSemTest
    public void testGetNextMove() {
        //create a PredrAgent with the right alphabet
        Move[] moves = new Move[2];
        moves[0] = new Move("a");
        moves[1] = new Move("b");
        PredrAgent testme = new PredrAgent();
        testme.initialize(moves, null);

        //Extract the first 10 moves
        SensorData anyOldSensorDataWillDo = new SensorData(false);
        anyOldSensorDataWillDo.setSensor("foo", 1);
        anyOldSensorDataWillDo.setSensor("bar", 2);
        anyOldSensorDataWillDo.setSensor("baz", 3);
        Move[] results = new Move[10];
        for(int i = 0; i < 10; ++i) {
            try {
                results[i] = testme.getNextMove(anyOldSensorDataWillDo);
            } catch(Exception e) {
                throw new AssertionFailedException("Error:  could not call getNextMove on PredrAgent object" + ExceptionUtils.getStacktrace(e));
            }
            
        }

        //Verify that it's a match
        String[] correct = {"a", "b", "a", "a", "a", "b", "b", "a", "b", "b" };
        for(int i = 0; i < 10; ++i) {
            Assertions.assertTrue(results[i].getName() == correct[i],
                                  "\nThe moves in results: " + Arrays.toString(results) +
                                  "\n      does not equal: " + Arrays.toString(correct));
        }
        
    }//testGetNextMove

    /** needed: a test ensures that the proper rules are created.  currently
     * can't write it because there is no accessor that lets us see the rules  */
    
}//class PredrAgentTest
