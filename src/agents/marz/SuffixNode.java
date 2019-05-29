package agents.marz;

import framework.Sequence;
import framework.Episode;
import framework.Action;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.function.Function;

/**
 * SuffixNode
 * Represents a node in a suffix tree.
 *
 * @author Zachary Paul Faltersack
 * @version 0.95
 */
public class SuffixNode {
    //region Static Variables
    // the likeliness to jump back to another node
    // (should be in the range (0.0 - 1.0)
    public static final double G_WEIGHT = 0.05;
    //endregion

    //region Class Variables
    private int g; // distance from root (ala A* search)
    private Action[] possibleActions;
    private Function<Integer, Episode> lookupEpisode;
    private double f; // the current overall potential of this suffix (f = g + h)
    private Sequence suffix;
    private ArrayList<Integer> successIndexList = new ArrayList<>();
    private ArrayList<Integer> failsIndexList = new ArrayList<>();
    private boolean foundGoal = false;
    //endregion

    //region Constructors
    /**
     * SuffixNode default ctor inits variables for a root node.
     *
     * NOTE: If creating a non-root node (@see #splitNode) these values will
     * need to be initialized properly. It can't be done in ctor without
     * creating inefficiencies.
     *
     */
    public SuffixNode(Sequence sequence, Action[] possibleActions, Function<Integer, Episode> lookupEpisode) {
        this.suffix = sequence;
        this.f = 0.0;
        this.g = 0;
        this.possibleActions = possibleActions;
        this.lookupEpisode = lookupEpisode;
    }// ctor
    //endregion

    //region Public Methods
    public SuffixNode[] split() {
        HashMap<Action, SuffixNode> children = new HashMap<>();
        for (Action action : this.possibleActions) {
            SuffixNode child = new SuffixNode(this.getSuffix().buildChildSequence(action), this.possibleActions, this.lookupEpisode);
            child.g = this.g + 1;
            children.put(action, child);
        }// for

        //Divy the successes and failures among the children
        divyIndexes(children, true);
        divyIndexes(children, false);

        // Do not split of the children aren't viable
        SuffixNode[] childArray = children.values().toArray(new SuffixNode[0]);
        for (SuffixNode child : childArray) {
            if (child.failsIndexList.size() == 0)
                return null;
        }

        return childArray;
    }

    public boolean canSplit() {
        // We can split after we've found the goal but then failed (or failed and then found the goal ;p)
        return this.foundGoal && this.failsIndexList.size() != 0;
    }

    public double getNormalizedWeight(){
        int successCount = this.successIndexList.size();
        int failCount = this.failsIndexList.size();

        if (successCount + failCount == 0) {
            return 0;
        }// if

        return (double) failCount / ((double) failCount + (double) successCount);
    }

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
        this.failsIndexList.add(index);
    }
    //endregion

    //region Private Methods
    private void updateHeuristic() {
        double gWeight = this.g * G_WEIGHT;

        this.f= gWeight + getNormalizedWeight();

    }// updateHeuristic

    private void divyIndexes(HashMap<Action, SuffixNode> children, boolean success) {
        List<Integer> parentList = (success ? this.successIndexList : this.failsIndexList);
        for (int parentIndex : parentList) {
            int index = parentIndex - 1;  //the -1 because child adds a letter
            //If we fall off the back of the epmem then it can't be matched
            if (index < 0)
                continue;

            Episode episode = this.lookupEpisode.apply(index);
            //If we've backed into the previous goal then we can't match either
            if ((this.getSuffix().getLength() > 0) && episode.hitGoal()) {
                continue;
            }// if

            Action action = episode.getAction();
            if (success)
                children.get(action).addSuccessIndex(index);
            else
                children.get(action).addFailIndex(index);
        }// for
    }//divyIndexes
    //endregion

    //region Object Overrides
    /**
     * toString
     *
     * @see Object#toString()
     */
    @Override
    public String toString() {
        this.updateHeuristic();
        return this.getSuffix().toString() + "_" + this.f;
    }
    //endregion
}
