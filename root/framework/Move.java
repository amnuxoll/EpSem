package framework;

/**
 *
 * @author Zachary Paul Faltersack
 * @version 0.95
 */
public class Move {
    private String name;

    public Move(String name) throws IllegalArgumentException {
        if (name == null)
            throw new IllegalArgumentException("name cannot be null");
        if (name == "")
            throw new IllegalArgumentException("name cannot be empty");
        this.name = name;
    }

    public String getName() {
        return this.name;
    }

    @Override
    public String toString() {
        return this.name;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof Move)) {
            return false;
        }
        Move move = (Move)o;
        return this.name.equals(move.name);
    }

    @Override
    public int hashCode() {
        return this.name.hashCode();
    }
}
