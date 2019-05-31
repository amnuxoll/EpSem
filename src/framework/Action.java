package framework;

/**
 *
 * @author Zachary Paul Faltersack
 * @version 0.95
 */
public class Action {
    //region Class Variables
    private String name;
    //endregion

    //region Constructors
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
