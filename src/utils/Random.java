package utils;

/**
 * Small helper class to make it more reliable to get true random generators or static ones to use for unit tests
 * or experiments.
 *
 * @author Zachary Paul Faltersack
 * @version 0.95
 */
public class Random {

    //region Public Static Methods

    /**
     * Gets a truly random generator seeded by the system clock.
     *
     * @return a {@link java.util.Random}.
     */
    public static java.util.Random getTrue() {
        return new java.util.Random(System.currentTimeMillis());
    }

    /**
     * Gets a constant seeded random generator.
     *
     * @return a {@link java.util.Random}.
     */
    public static java.util.Random getFalse() {
        return new java.util.Random(13);
    }

    //endregion
}
