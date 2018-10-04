package agents.marz.nodes;

import utils.Sequence;
import agents.marz.SuffixNodeBase;
import framework.Episode;
import framework.Move;

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
public class SuffixNode extends SuffixNodeBase<SuffixNode> {
    // the likeliness to jump back to another node
    // (should be in the range (0.0 - 1.0)
    private static final double G_WEIGHT = 0.05;

    /*--==Instance Variables==--*/
    private int g; // distance from root (ala A* search)
    private Move[] possibleMoves;
    private Function<Integer, Episode> lookupEpisode;

    /**
     * SuffixNode default ctor inits variables for a root node.
     *
     * NOTE: If creating a non-root node (@see #splitNode) these values will
     * need to be initialized properly. It can't be done in ctor without
     * creating inefficiencies.
     *
     */
    SuffixNode(Sequence sequence, Move[] possibleMoves, Function<Integer, Episode> lookupEpisode) {
        super(sequence);
        this.g = 0;
        this.possibleMoves = possibleMoves;
        this.lookupEpisode = lookupEpisode;
    }// ctor

    @Override
    public SuffixNode[] split() {
        HashMap<Move, SuffixNode> children = new HashMap<>();
        for (Move move : this.possibleMoves) {
            SuffixNode child = new SuffixNode(this.getSuffix().buildChildSequence(move), this.possibleMoves, this.lookupEpisode);
            child.g = this.g + 1;
            children.put(move, child);
        }// for

        //Divy the successes and failures among the children
        divyIndexes(children, true);
        divyIndexes(children, false);

        // Do not split of the children aren't viable
        SuffixNode[] childArray = children.values().toArray(new SuffixNode[0]);
        for (SuffixNode child : childArray) {
            if (child.getFailIndices().size() == 0)
                return null;
        }

        return childArray;
    }

    @Override
    protected boolean canSplit() {
        // We can split after we've found the goal but then failed (or failed and then found the goal ;p)
        return super.getFoundGoal() && this.getFailIndices().size() != 0;
    }

    /**
     * updateHeuristic
     *
     * Recalculate this node's heuristic value (h) and overall value(f)
     */
    @Override
    protected void updateHeuristic() {
        double gWeight = this.g * G_WEIGHT;

        this.f= gWeight + getNormalizedWeight();

    }// updateHeuristic

    @Override
    public double getNormalizedWeight(){
        int successCount = this.getSuccessIndices().size();
        int failCount = this.getFailIndices().size();

        if (successCount + failCount == 0) {
            return 0;
        }// if

        return (double) failCount / ((double) failCount + (double) successCount);
    }

    private void divyIndexes(HashMap<Move, SuffixNode> children, boolean success) {
        List<Integer> parentList = (success ? this.getSuccessIndices() : this.getFailIndices());
        for (int parentIndex : parentList) {
            int index = parentIndex - 1;  //the -1 because child adds a letter
            //If we fall off the back of the epmem then it can't be matched
            if (index < 0)
                continue;

            Episode episode = this.lookupEpisode.apply(index);
            //If we've backed into the previous goal then we can't match either
            if ((this.getSuffix().getLength() > 0) && episode.getSensorData().isGoal()) {
                continue;
            }// if

            Move move = episode.getMove();
            if (success)
                children.get(move).addSuccessIndex(index);
            else
                children.get(move).addFailIndex(index);
        }// for
    }//divyIndexes

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
}
