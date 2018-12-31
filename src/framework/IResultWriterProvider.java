package framework;

import java.io.File;

/**
 *
 * @author Zachary Paul Faltersack
 * @version 0.95
 */
public interface IResultWriterProvider {
    //region Methods
    IResultWriter getResultWriter(String agent, String file) throws Exception;

    File getOutputDirectory();
    //endregion
}
