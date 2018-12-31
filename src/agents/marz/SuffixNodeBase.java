package agents.marz;

import framework.Sequence;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * SuffixNodeBase
 * Represents a node in a suffix tree.
 *
 * @author Zachary Paul Faltersack
 * @version 0.95
 */
public abstract class SuffixNodeBase<TNodeType extends SuffixNodeBase<TNodeType>> {
    //region Class Variables
    protected double f; // the current overall potential of this suffix (f = g + h)
    private Sequence suffix;
    private ArrayList<Integer> successIndexList = new ArrayList<>();
    private ArrayList<Integer> failsIndexList = new ArrayList<>();
    private boolean foundGoal = false;
    //endregion

    //region Constructors
    /**
     * Create an instance of a SuffixNodeBase
     * @param suffix The suffix for this node.
     */
    public SuffixNodeBase(Sequence suffix) {
        this.suffix = suffix;
        this.f = 0.0;
    }
    //endregion

    //region Public Methods
    /**
     * Gets the suffix for this node.
     * @return The sequence that contains the suffix of this node.
     */
    public Sequence getSuffix() {
        return this.suffix;
    }

    /**
     * Gets the weight of this node.
     * @return The weight value as a double of this node.
     */
    public double getWeight() {
        this.updateHeuristic();
        return this.f;
    }

    public void setFoundGoal() {
        this.foundGoal = true;
    }

    public void addSuccessIndex(int index) {
        this.successIndexList.add(index);
    }

    public void addFailIndex(int index) {
        this.failsIndexList.add(index)
        ;   }
    //endregion

    //region Protected Methods
    protected boolean getFoundGoal() {
        return this.foundGoal;
    }

    protected List<Integer> getSuccessIndices() {
        return Collections.unmodifiableList(this.successIndexList);
    }

    protected List<Integer> getFailIndices() {
        return Collections.unmodifiableList(this.failsIndexList);
    }
    //endregion

    //region Abstract Methods
    /**
     * Gets the normalized weight of this node in range [0,1]
     * @return the weight of this node in range [0,1]
     */
    public abstract double getNormalizedWeight();

    /**
     * Splits this node out into its children.
     * @return The set of child nodes.
     */
    public abstract TNodeType[] split();

    /**
     * Recalculates the weight of this Node.
     */
    protected abstract void updateHeuristic();

    protected abstract boolean canSplit();
    //endregion
}
