package utils;

import framework.Episode;
import framework.Move;

import java.util.*;

/**
 * Sequence
 * Represents a sequence of {@link Move}s.
 *
 * @author Zachary Paul Faltersack
 * @version 0.95
 */
public class Sequence {
    private Move[] moves;

    private int currentIndex = -1;

    public static final Sequence EMPTY = new Sequence(new Move[0]);

    /**
     * Create an instance of a Sequence.
     * @param moves The moves in the sequence.
     */
    public Sequence(Move[] moves) {
        if (moves == null)
            throw new IllegalArgumentException("moves cannot be null");
        this.moves = moves;
    }

    /**
     * Create an instance of sequence representing the sequence
     * that a list of episodes followed
     * @param episodes the list of episodes
     * @param start the inclusive index to start the sequence
     * @param end the exclusive index to end the sequence
     */
    public Sequence(List<Episode> episodes, int start, int end){
        this.moves= new Move[end-start];

        for(int i=start; i<end; i++){
            moves[i]= episodes.get(i).getMove();
        }
    }

    /**
     * Determines whether or not this sequence ends with the provided sequence.
     * @param sequence the sequent to check as a suffix.
     * @return true if this sequence ends with the given sequence; otherwise false.
     */
    public boolean endsWith(Sequence sequence) {
        if (sequence == null)
            throw new IllegalArgumentException("sequence cannot be null.");
        if (sequence.moves.length > this.moves.length)
            return false;
        if (sequence.moves.length == 0)
            return true;
        List<Move> myMoves = new ArrayList<>(Arrays.asList(this.moves));
        List<Move> otherMoves = new ArrayList<>(Arrays.asList(sequence.moves));
        Collections.reverse(myMoves);
        Collections.reverse(otherMoves);
        for (int i = 0; i < otherMoves.size(); i++) {
            if (!myMoves.get(i).equals(otherMoves.get(i)))
                return false;
        }
        return true;
    }

    /**
     * Gets the moves in this sequence.
     * @return the Move[].
     */
    public Move[] getMoves() {
        return this.moves;
    }

    /**
     * Gets a subsequence of this sequence starting from the given index.
     * @param startIndex the index to start from.
     * @return A subsequence starting at the given index.
     */
    public Sequence getSubsequence(int startIndex) {
        if (startIndex < 0)
            throw new IllegalArgumentException("startIndex cannot be less than 0");
        if (startIndex >= this.moves.length)
            return Sequence.EMPTY;
        Move[] subsequence = Arrays.copyOfRange(this.moves, startIndex, this.moves.length);
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
        if (length > this.moves.length)
            throw new IllegalArgumentException("length is too large.");
        if (length == 0)
            return Sequence.EMPTY;
        Move[] subsequence = Arrays.copyOfRange(this.moves, 0, length);
        return new Sequence(subsequence);
    }

    /**
     * Gets the length of this sequence.
     * @return the length of the sequence.
     */
    public int getLength() {
        return this.moves.length;
    }

    /**
     * Builds a new sequence by prepending the given move to the front of this sequence.
     * @param newMove The new move to prepend to this sequence.
     * @return The new sequence.
     */
    public Sequence buildChildSequence(Move newMove) {
        if (newMove == null)
            throw new IllegalArgumentException("newMove cannot be null");
        List<Move> childMoves = new LinkedList<>(Arrays.asList(this.moves));
        childMoves .add(0, newMove);
        return new Sequence(childMoves.toArray(new Move[0]));
    }

    /**
     * Indicates whether or not there is a next move in the sequence.
     * @return true if a next move exists; otherwise false.
     */
    public boolean hasNext() {
        return this.currentIndex < (this.moves.length - 1);
    }

    /**
     * Increments to the next index and returns the move at that index.
     * @return The next move in the sequence.
     */
    public Move next() {
        if (!this.hasNext())
            // For simplicity just use a generic unchecked exception
            throw new RuntimeException("Sequence has no next Move.");
        this.currentIndex++;
        return this.moves[this.currentIndex];
    }

    /**
     * Resets the current move index in this sequence.
     */
    public void reset() {
        this.currentIndex = -1;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof Sequence)) {
            return false;
        }
        Sequence sequence = (Sequence) o;
        if (this.moves.length != sequence.moves.length)
            return false;
        for (int i = 0; i < this.moves.length; i++) {
            if (!this.moves[i].equals(sequence.moves[i]))
                return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(this.moves);
    }

    @Override
    public String toString() {
        StringBuilder representation = new StringBuilder();
        for (Move move : this.moves) {
            representation.append(move.toString());
        }
        return representation.toString();
    }
}
