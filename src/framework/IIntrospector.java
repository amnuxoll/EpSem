package framework;

/**
 * Defines operations that can be used by an {@link IAgent} to gather metadata about its state.
 *
 * @author Zachary Paul Faltersack
 * @version 0.95
 */
public interface IIntrospector {

    //region Methods

    /**
     * Determines if the execution of the given {@link Sequence} would result in hitting a goal at any point.
     *
     * @param sequence the {@link Sequence} to validate.
     * @return true if the goal would be hit; otherwise false.
     */
    boolean validateSequence(Sequence sequence);

    //endregion

}
