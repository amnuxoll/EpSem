package tests;

import java.util.ArrayList;

/**
 *
 * @author Zachary Paul Faltersack
 * @version 0.95
 */
public class Results {
    //region Class Variables
    private ArrayList<Result> successes = new ArrayList<>();
    private ArrayList<Result> failures = new ArrayList<>();
    private String testClassName;
    //endregion

    //region Constructors
    public Results(String testClassName) {
        this.testClassName = testClassName;
    }
    //endregion

    //region Public Methods
    public void addResult(String testName) {
        this.successes.add(new Result(testName));
    }

    public void addResult(String testName, Exception exception) {
        this.failures.add(new Result(testName, exception));
    }

    public int getSuccessCount() {
        return this.successes.size();
    }

    public int getFailureCount() {
        return this.failures.size();
    }

    public void print(boolean includeSuccesses) {
        if (includeSuccesses == false && this.failures.size() == 0)
            return;
        System.out.println(this.testClassName + " :: " + (this.successes.size() + this.failures.size())+ " tests");
        System.out.println("\tSuccess Count: " + this.successes.size());
        System.out.println("\tFailure Count: " + this.failures.size());
        if (includeSuccesses) {
            for (Result result : this.successes) {
                System.out.println("\t\t" + result);
            }
        }
        for (Result result : this.failures) {
            System.out.println("\t\t" + result);
        }
        System.out.println();
    }
    //endregion
}
