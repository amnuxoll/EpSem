package framework;

/**
 *
 * @author Zachary Paul Faltersack
 * @version 0.95
 */
public interface IResultWriterProvider {
    //region Methods
    IResultWriter getResultWriter(String agent, String file) throws Exception;
    //endregion
}
