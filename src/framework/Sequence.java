package framework;

import java.util.*;

/**
 * Sequence
 * Represents a sequence of {@link Action}s.
 *
 * @author Zachary Paul Faltersack
 * @version 0.95
 */
public class Sequence implements Comparable<Sequence> {
    //region Static Sequences
    public static final Sequence EMPTY = new Sequence(new Action[0]);
    //endregion

    //region Class Variables
    private Action[] actions;
    private int currentIndex = -1;
    //endregion

    //region Constructors
    /**
     * Create an instance of a Sequence.
     * @param actions The actions in the sequence.
     */
    public Sequence(Action[] actions) {
        if (actions == null)
            throw new IllegalArgumentException("actions cannot be null");
        this.actions = actions;
    }
    //endregion

    //region Public Methods
    /**
     * Determines whether or not this sequence ends with the provided sequence.
     * @param sequence the sequent to check as a suffix.
     * @return true if this sequence ends with the given sequence; otherwise false.
     */
    public boolean endsWith(Sequence sequence) {
        if (sequence == null)
            throw new IllegalArgumentException("sequence cannot be null.");
        if (sequence.actions.length > this.actions.length)
            return false;
        if (sequence.actions.length == 0)
            return true;
        List<Action> myActions = new ArrayList<>(Arrays.asList(this.actions));
        List<Action> otherActions = new ArrayList<>(Arrays.asList(sequence.actions));
        Collections.reverse(myActions);
        Collections.reverse(otherActions);
        for (int i = 0; i < otherActions.size(); i++) {
            if (!myActions.get(i).equals(otherActions.get(i)))
                return false;
        }
        return true;
    }

    /**
     * Gets the actions in this sequence.
     * @return the Action[].
     */
    public Action[] getActions() {
        return this.actions;
    }

    /**
     * Gets a subsequence of this sequence starting from the given index.
     * @param startIndex the index to start from.
     * @return A subsequence starting at the given index.
     */
    public Sequence getSubsequence(int startIndex) {
        if (startIndex < 0)
            throw new IllegalArgumentException("startIndex cannot be less than 0");
        if (startIndex >= this.actions.length)
            return Sequence.EMPTY;
        Action[] subsequence = Arrays.copyOfRange(this.actions, startIndex, this.actions.length);
        return new Sequence(subsequence);
    }

    /**
     * Gets a subsequence starting from the beginning of this sequence that is of the indicated length.
     * @param length the length of the subsequence to retrieve.
     * @return the new Sequence.
     */
    public Sequence take(int length) {
        if (length < 0)
            throw new IllegalArgumentException("length cannot be less than zero.");
        if (length > this.actions.length)
            throw new IllegalArgumentException("length is too large.");
        if (length == 0)
            return Sequence.EMPTY;
        Action[] subsequence = Arrays.copyOfRange(this.actions, 0, length);
        return new Sequence(subsequence);
    }

    /**
     * Gets the length of this sequence.
     * @return the length of the sequence.
     */
    public int getLength() {
        return this.actions.length;
    }

    /**
     * Builds a new sequence by prepending the given move to the front of this sequence.
     * @param newAction The new move to prepend to this sequence.
     * @return The new sequence.
     */
    public Sequence buildChildSequence(Action newAction) {
        if (newAction == null)
            throw new IllegalArgumentException("newAction cannot be null");
        List<Action> childActions = new LinkedList<>(Arrays.asList(this.actions));
        childActions.add(0, newAction);
        return new Sequence(childActions.toArray(new Action[0]));
    }

    /**
     * Concatenates two sequences.
     * @param sequence The sequence to concatenate to this sequence.
     * @return A new sequence that starts with this sequence and ends with the given sequence.
     */
    public Sequence concat(Sequence sequence) {
        if (sequence == null)
            throw new IllegalArgumentException("sequence cannot be null.");
        List<Action> concatenated = new LinkedList<>(Arrays.asList(this.actions));
        concatenated.addAll(Arrays.asList(sequence.getActions()));
        return new Sequence(concatenated.toArray(new Action[0]));
    }

    /**
     * Indicates whether or not there is a next move in the sequence.
     * @return true if a next move exists; otherwise false.
     */
    public boolean hasNext() {
        return this.currentIndex < (this.actions.length - 1);
    }

    /**
     * Increments to the next index and returns the move at that index.
     * @return The next move in the sequence.
     */
    public Action next() {
        if (!this.hasNext())
            // For simplicity just use a generic unchecked exception
            throw new RuntimeException("Sequence has no next Action.");
        this.currentIndex++;
        return this.actions[this.currentIndex];
    }

    /**
     * Resets the current move index in this sequence.
     */
    public void reset() {
        this.currentIndex = -1;
    }

    public int getCurrentIndex() {
        return currentIndex;
    }
    //endregion

    //region Comparable<Sequence> Members
    @Override
    public int compareTo(Sequence o) {
        // Sort from longest to shortest
        if (this.actions.length > o.actions.length)
            return -1;
        if (this.actions.length < o.actions.length)
            return 1;
        return 0;
    }
    //endregion

    //region Object Overrides
    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof Sequence)) {
            return false;
        }
        Sequence sequence = (Sequence) o;
        if (this.actions.length != sequence.actions.length)
            return false;
        for (int i = 0; i < this.actions.length; i++) {
            if (!this.actions[i].equals(sequence.actions[i]))
                return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(this.actions);
    }

    @Override
    public String toString() {
        StringBuilder representation = new StringBuilder();
        for (Action action : this.actions) {
            representation.append(action.toString());
        }
        return representation.toString();
    }
    //endregion
}
