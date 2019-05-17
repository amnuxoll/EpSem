package tests.agents.predr;

import framework.*;
import tests.*;
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
        Move[] results = new Move[10];
        for(int i = 0; i < 10; ++i) {
            try {
                results[i] = testme.getNextMove(anyOldSensorDataWillDo);
            } catch(Exception e) {
                throw new AssertionFailedException("Error:  could not call getNextMove on PredrAgent object.");
            }
            
        }

        //Verify that it's a match
        String[] correct = {"a", "b", "a", "a", "a", "b", "b", "a", "b", "b" };
        for(int i = 0; i < 10; ++i) {
            try {
                Assertions.assertTrue(results[i].getName() == correct[i]);
            } catch(AssertionFailedException afe) {
                System.err.println("The moves in results: " + results);
                System.err.println("      does not equal: " + correct);
                throw afe;
            }
        }
        
    }//testGetNextMove
}//class PredrAgentTest
