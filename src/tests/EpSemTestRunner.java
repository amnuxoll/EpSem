package tests;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * The main() method in this class runs all the unit tests
 *
 * You can run a subset of the unit tests by specifying a file or folder on
 * the command line as a full path from the project root. Examples:
 *    java EpSemTestRunner ./src/tests/agents/myagent
 *    java EpSemTestRunner ./src/tests/agents/myagent/MyAgentTest.java
 *
 * You can use the -stoponfail flag to ask the runner to stop as soon as
 * any test fails.  This must be the first flag (before -new).
 *
 * You can use the -new flag to 1 more specific test classes to run
 * Example:
 *    java EpSemTestRunner -new tests.agents.myagent.MyAgentTest tests.agents.myagent.AgentData
 *
 * @author Zachary Paul Faltersack
 * @version 0.95
 */
public class EpSemTestRunner {
    //region Main
    public static void main(String[] args) {

        TestClassCollection testClassCollection;
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        boolean stoponfail = false;

        //Load the test classes based upon command line arguments
        if (args == null || args.length == 0) {
            //load all the tests I can find (default)
            File srcDir = new File(System.getProperty("user.dir"), "src");
            testClassCollection = new TestClassCollection(classLoader, new File(srcDir, "tests"));
        }
        else {
            int currIndex = 0;
            if (args[currIndex].equals("-stoponfail")) {
                currIndex++;
                stoponfail = true;
            }

            if (args[currIndex].equals("-new"))
                //load specific test classes
                testClassCollection = new TestClassCollection(classLoader, Arrays.copyOfRange(args, 1, args.length));
            else
                //load tests in a given file or folder
                testClassCollection = new TestClassCollection(classLoader, new File(args[currIndex]));
        }

        //Run all the tests
        ArrayList<Results> results = new ArrayList<>();
        for (UnitTestClass testClass : testClassCollection.getUnitTestClasses()) {
	        try {
                results.add(testClass.executeTestClass(stoponfail));
	        } catch (Exception ex) {
                ex.printStackTrace();
            }
        }

        //Print final results summary
        int successCount = 0, failureCount = 0;
        for (Results result : results) {
            result.print(false);
            successCount += result.getSuccessCount();
            failureCount += result.getFailureCount();
        }
        System.out.println();
        System.out.println("---------------------------------------------------------------------");
        System.out.println("---------------------------------------------------------------------");
        System.out.println("---------------------------------------------------------------------");
        System.out.println("Total tests executed: " + (successCount + failureCount));
        System.out.println("Success Count: " + successCount);
        System.out.println("Failure Count: " + failureCount);
    }
    //endregion
}
