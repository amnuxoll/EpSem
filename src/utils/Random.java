package utils;

/**
 *
 * @author Zachary Paul Faltersack
 * @version 0.95
 */
public class Random {
    //region Public Static Methods
    public static java.util.Random getTrue() {
        return new java.util.Random(System.currentTimeMillis());
    }

    public static java.util.Random getFalse() {
        return new java.util.Random(13);
    }
    //endregion
}
