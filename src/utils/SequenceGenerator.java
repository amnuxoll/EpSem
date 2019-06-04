package utils;

import framework.Action;
import framework.Sequence;

import java.util.*;

/**
 * SequenceGenerator
 * Builds sequences based on sets of {@link Action} with a canonical ordering.
 *
 * @author Zachary Paul Faltersack
 * @version 0.95
 */
public class SequenceGenerator {

    //region Class Variables

    private Action[] actions;

    private HashMap<Action, Integer> moveIndex = new HashMap<>();

    //endregion

    //region Constructors

    /**
     * Create an instance of a SequenceGenerator based on the given actions.
     *
     * @param actions The Action[] to build sequences from.
     */
    public SequenceGenerator(Action[] actions) {
        if (actions == null)
            throw new IllegalArgumentException("actions cannot be null.");
        this.actions = actions;
        for (int i = 0; i < this.actions.length; i++) {
            this.moveIndex.put(this.actions[i], i);
        }
    }

    //endregion

    //region Public Methods

    /**
     * Gets the permutation at the given index.
     *
     * @param index The index to retrieve the permutation for.
     * @return The Sequence that contains the move permutation.
     */
    public Sequence nextPermutation(long index) {
        if (index <= 0) {
            throw new IndexOutOfBoundsException("index must be a positive number.  Has your next permutation index overflowed?");
        }// if

        if (this.actions.length == 0)
            return Sequence.EMPTY;

        List<Action> nextSequence = new ArrayList<>();
        if (index <= this.actions.length) {
            nextSequence.add(this.actions[(int)index - 1]);
        }// if
        else {
            while (index > 0) {
                index--;
                long movesIndex= index % this.actions.length;
                nextSequence.add(0, this.actions[(int)movesIndex]);
                index /= this.actions.length;
            }// while
        }

        return new Sequence(nextSequence.toArray(new Action[0]));
    }// nextPermutation

    /**
     * Calculates the canonical index of the given sequence.
     *
     * @param sequence The sequence to get an ordering for.
     * @return The sequence index.
     */
    public long getCanonicalIndex(Sequence sequence) {
        if (sequence == null)
            throw new IllegalArgumentException("sequence cannot be null");
        ArrayList<Action> actions = new ArrayList<>(Arrays.asList(sequence.getActions()));
        Collections.reverse(actions);
        double total = 0;
        for (int i = 0; i < actions.size(); i++)
        {
            int index = this.moveIndex.get(actions.get(i)) + 1;
            total += Math.pow(this.actions.length, i) * index;
        }
        return (long)total;
    }

    //endregion
}
