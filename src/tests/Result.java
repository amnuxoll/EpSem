package tests;

import utils.ExceptionUtils;

/**
 *
 * @author Zachary Paul Faltersack
 * @version 0.95
 */
public class Result {
    //region Class Variables
    private boolean success;
    private Exception exception;
    private String testName;
    //endregion

    //region Constructors
    public Result(String testName) {
        this.testName = testName;
        this.success = true;
    }

    public Result(String testName, Exception exception) {
        this.testName = testName;
        this.success = false;
        this.exception = exception;
    }
    //endregion

    //region Object Overrides
    @Override
    public String toString() {
        if (this.success)
            return "SUCCESS :: " + this.testName;
        return "FAIL    :: " + this.testName + ": " + this.exception.getMessage() + " { \n" + ExceptionUtils.getStacktrace(this.exception) + "}";
    }
    //endregion
}
