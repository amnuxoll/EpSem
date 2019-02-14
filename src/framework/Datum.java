package framework;

/**
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
    public Datum(String statistic, int datum) {
        this(statistic, Integer.toString(datum));
    }

    public Datum(String statistic, double datum) {
        this(statistic, Double.toString(datum));
    }

    public Datum(String statistic, String datum) {
        if (statistic == null || statistic.isEmpty())
            throw new IllegalArgumentException("Statistic requires a name.");
        if (datum == null)
            datum = "";
        if (datum.contains(","))
            throw new IllegalArgumentException("Commas will be poorly handled by the CSV result writer.");
        this.statistic = statistic;
        this.datum = datum;
    }
    //endregion

    //region Public Methods
    public String getStatistic()
    {
        return this.statistic;
    }

    public String getDatum()
    {
        return this.datum;
    }
    //endregion
}
