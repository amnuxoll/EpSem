package framework;

import java.io.File;

/**
 * Provides instances of an {@link IResultWriter} for consecutive test runs.
 *
 * @author Zachary Paul Faltersack
 * @version 0.95
 */
public interface IResultWriterProvider {

    //region Methods

    /**
     * Creates an instance of a {@link IResultWriter} configured for the given agent.
     *
     * @param agent the agent bring tracked.
     * @param file the file name indicating the type of statistic being tracked.
     * @return a new instance of a {@link IResultWriter}.
     * @throws Exception
     */
    IResultWriter getResultWriter(String agent, String file) throws Exception;

    /**
     * Gets a reference to the output directory where all statistic files are located.
     *
     * TODO: This is a kludge. The provider shouldn't be specific enough to have an understanding that
     * these result writers are going to a filesystem... Future work.
     *
     * @return the {@link File} indicating the directory being written to.
     */
    File getOutputDirectory();

    //endregion

}
