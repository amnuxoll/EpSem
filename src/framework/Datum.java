package framework;

/**
 * This class is a simple name-value pair that represents statistical data an {@link IAgent} may
 * hand to the test framework for generating CSV files.
 *
 * @author Zachary Paul Faltersack
 * @version 0.95
 */
public class Datum {
    //region Class Variables

    private String statistic;
    private String datum;

    //endregion

    //region Constructors

    /**
     * Creates an instance of a {@link Datum} for the given statistics.
     *
     * @param statistic the name of the statistic.
     * @param datum the value of the statistic.
     */
    public Datum(String statistic, int datum) {
        this(statistic, Integer.toString(datum));
    }

    /**
     * Creates an instance of a {@link Datum} for the given statistics.
     *
     * @param statistic the name of the statistic.
     * @param datum the value of the statistic.
     */
    public Datum(String statistic, double datum) {
        this(statistic, Double.toString(datum));
    }

    private Datum(String statistic, String datum) {
        if (statistic == null || statistic.isEmpty())
            throw new IllegalArgumentException("Statistic requires a name.");
        this.statistic = statistic;
        this.datum = datum;
    }
    //endregion

    //region Public Methods

    /**
     * @return the name of the statistic.
     */
    public String getStatistic()
    {
        return this.statistic;
    }

    /**
     * @return the value of the statistic as a string.
     */
    public String getDatum()
    {
        return this.datum;
    }
    //endregion
}
