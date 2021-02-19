package tests;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;

/**
 *
 * @author Zachary Paul Faltersack
 * @version 0.95
 */
public class EpSemTestRunner {
    //region Main
    public static void main(String[] args) {

        TestClassCollection testClassCollection;
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();

        if (args == null || args.length == 0) {
            File srcDir = new File(System.getProperty("user.dir"), "src");
            testClassCollection = new TestClassCollection(classLoader, new File(srcDir, "tests"));
        }
        else {
            if (args[0].equals("-new"))
                testClassCollection = new TestClassCollection(classLoader, Arrays.copyOfRange(args, 1, args.length));
            else
                testClassCollection = new TestClassCollection(classLoader, new File(args[0]));
        }

        ArrayList<Results> results = new ArrayList<>();
        for (UnitTestClass testClass : testClassCollection.getUnitTestClasses()) {
	        try {
                results.add(testClass.executeTestClass());
	        } catch (Exception ex) {
                ex.printStackTrace();
	        }
        }
        int successCount = 0, failureCount = 0;
        for (Results result : results) {
            result.print(false);
            successCount += result.getSuccessCount();
            failureCount += result.getFailureCount();
        }
        System.out.println();
        System.out.println("Total tests executed: " + (successCount + failureCount));
        System.out.println("Success Count: " + successCount);
        System.out.println("Failure Count: " + failureCount);
    }
    //endregion
}
