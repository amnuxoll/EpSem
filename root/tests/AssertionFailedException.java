package tests;

/**
 *
 * @author Zachary Paul Faltersack
 * @version 0.95
 */
public class AssertionFailedException extends RuntimeException {
    //Region Constructors
    public AssertionFailedException(String reason) {
        super(reason);
    }

    public AssertionFailedException(String reason, Exception innerException) {
        super(reason, innerException);
    }
    //endregion
}
