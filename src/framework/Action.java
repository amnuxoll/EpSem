package framework;

/**
 * An Action represents something an {@link IAgent} is able to perform in an {@link IEnvironment}.
 *
 * @author Zachary Paul Faltersack
 * @version 0.95
 */
public class Action {
    //region Class Variables

    /** A developer-friendly name to give the {@link Action}. An agent should not try to use this. */
    private String name;

    //endregion

    //region Constructors

    /**
     * Creates an instance of an {@link Action} with the given {@link Action#name}
     *
     * @param name the name to give the action.
     */
    public Action(String name) {
        if (name == null)
            throw new IllegalArgumentException("name cannot be null");
        if (name.isEmpty())
            throw new IllegalArgumentException("name cannot be empty");
        this.name = name;
    }

    /**
     * copy constructor (deep copy)
     */
    public Action(Action other) {
        if (other == null)
            throw new IllegalArgumentException("cannot copy a null Action");
        this.name = other.name;
    }

    //endregion

    //region Public Methods

    /**
     * @return the name of the {@link Action}.
     */
    public String getName() {
        return this.name;
    }

    //endregion

    //region Object Overrides

    @Override
    public String toString() {
        return this.name;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof Action)) {
            return false;
        }
        Action action = (Action)o;
        return this.name.equals(action.name);
    }

    @Override
    public int hashCode() {
        return this.name.hashCode();
    }

    //endregion
}
