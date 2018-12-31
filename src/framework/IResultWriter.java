package framework;

import java.io.IOException;

/**
 *
 * @author Zachary Paul Faltersack
 * @version 0.95
 */
public interface IResultWriter {
    //region Methods
    void beginNewRun() throws IOException;

    void logResult(String result) throws IOException;

    void complete() throws IOException;
    //endregion
}
