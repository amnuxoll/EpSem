package utils;

import framework.Move;
import framework.Sequence;

import java.util.*;

/**
 * SequenceGenerator
 * Builds sequences based on sets of {@link Move} with a canonical ordering.
 *
 * @author Zachary Paul Faltersack
 * @version 0.95
 */
public class SequenceGenerator {
    private Move[] moves;
    private HashMap<Move, Integer> moveIndex = new HashMap<>();

    /**
     * Create an instance of a SequenceGenerator based on the given moves.
     * @param moves The Move[] to build sequences from.
     */
    public SequenceGenerator(Move[] moves) {
        if (moves == null)
            throw new IllegalArgumentException("moves cannot be null.");
        this.moves = moves;
        for (int i = 0; i < this.moves.length; i++) {
            this.moveIndex.put(this.moves[i], i);
        }
    }

    /**
     * Gets the permutation at the given index.
     * @param index The index to retrieve the permutation for.
     * @return The Sequence that contains the move permutation.
     */
    public Sequence nextPermutation(long index) {
        if (index <= 0) {
            throw new IndexOutOfBoundsException("index must be a positive number.  Has your next permutation index overflowed?");
        }// if

        if (this.moves.length == 0)
            return Sequence.EMPTY;

        List<Move> nextSequence = new ArrayList<>();
        if (index <= this.moves.length) {
            nextSequence.add(this.moves[(int)index - 1]);
        }// if
        else {
            while (index > 0) {
                index--;
                long movesIndex= index % this.moves.length;
                nextSequence.add(0, this.moves[(int)movesIndex]);
                index /= this.moves.length;
            }// while
        }

        return new Sequence(nextSequence.toArray(new Move[0]));
    }// nextPermutation

    /**
     * Calculates the canonical index of the given sequence.
     * @param sequence The sequence to get an ordering for.
     * @return The sequence index.
     */
    public long getCanonicalIndex(Sequence sequence) {
        if (sequence == null)
            throw new IllegalArgumentException("sequence cannot be null");
        ArrayList<Move> moves = new ArrayList<>(Arrays.asList(sequence.getMoves()));
        Collections.reverse(moves);
        double total = 0;
        for (int i = 0; i < moves.size(); i++)
        {
            int index = this.moveIndex.get(moves.get(i)) + 1;
            total += Math.pow(this.moves.length, i) * index;
        }
        return (long)total;
    }
}
