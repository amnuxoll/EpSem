package framework;

/**
 *
 * @author Zachary Paul Faltersack
 * @version 0.95
 */
public interface IResultWriterProvider {
    IResultWriter getResultWriter(String agent) throws Exception;
}
