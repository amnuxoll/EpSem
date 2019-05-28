package agents.marz;

import framework.NamedOutput;
import framework.Sequence;
import java.util.*;

/**
 * SuffixTree
 * Represents a tree of SuffixNodeBase
 *
 * @author Zachary Paul Faltersack
 * @version 0.95
 */
public class SuffixTree {
    //region Class Variables
    /** hash table of all nodes on the fringe of our search */
    private HashMap<Sequence, SuffixNode> hashFringe = new HashMap<>();
    private int maxSize;
    //endregion

    //region Constructors
    /**
     * Create an instance of a SuffixTree with a given size and root node.
     * @param maxSize The max allowed size of this tree.
     * @param rootNode The root node of the tree.
     */
    public SuffixTree(int maxSize, SuffixNode rootNode) {
        if (maxSize < 1)
            throw new IllegalArgumentException("maxSize must be greater than 0");
        if (rootNode == null)
            throw new IllegalArgumentException("rootNode cannot be null");
        this.maxSize = maxSize;
        this.addNode(rootNode);
    }
    //endregion

    //region Public Methods
    /**
     * Split the node with the given suffix.
     * @param sequence The suffix to split.
     * @return True if the suffix node is split; otherwise false.
     */
    public boolean splitSuffix(Sequence sequence) {
        if (sequence == null)
            throw new IllegalArgumentException("sequence cannot be null");
        SuffixNode node = this.hashFringe.get(sequence);
        if (node == null)
            return false;
        SuffixNode[] children = node.split();

        if (children == null)
            return false;

        //Ready to commit:  add the children to the fringe and remove the parent
        for (SuffixNode aChildren : children) {
            this.addNode(aChildren);
        }// for

        this.hashFringe.remove(node.getSuffix());
        return true;
    }

    /**
     * findBestNodeToTry
     *
     * finds node with lowest heuristic
     */
    public SuffixNode findBestNodeToTry() {
        double bestWeight = Double.MAX_VALUE;
        SuffixNode bestNode = null;
        for (SuffixNode node : this.hashFringe.values()) {
            double nodeWeight = node.getWeight();
            if (nodeWeight < bestWeight) {
                bestWeight = nodeWeight;
                bestNode = node;
            }
        }
        return bestNode;
    }// findBestNodeToTry

    /**
     * Find the node with the longest matching suffix to the sequence.
     * @param sequence The sequence to find a node for.
     * @return The Node that best matched the given Sequence.
     */
    public SuffixNode findBestMatch(Sequence sequence) {
        if (sequence == null)
            throw new IllegalArgumentException("sequence cannot be null");
        List<Sequence> suffixKeys = Arrays.asList(this.hashFringe.keySet().toArray(new Sequence[0]));
        Collections.sort(suffixKeys);  //Shortest to longest
        for (Sequence suffixKey : suffixKeys)
        {
            if (sequence.endsWith(suffixKey))
                return this.hashFringe.get(suffixKey);
        }
        return null;
    }

    /**
     * Indicates whether or not the tree contains the given suffix.
     * @param suffix The suffix to check for.
     * @return True if the suffix is found in the tree; otherwise false.
     */
    public boolean containsSuffix(Sequence suffix) {
        if (suffix == null)
            throw new IllegalArgumentException("suffix cannot be null");
        return this.hashFringe.containsKey(suffix);
    }

    /**
     * Prints the nodes in the suffix tree.
     */
    public void printTree() {
        NamedOutput output = NamedOutput.getInstance();
        for (SuffixNode node : this.hashFringe.values()) {
            output.writeLine("SUFFIX_TREE", "Node: " + node);
        }
    }
    //endregion

    //region Private Methods
    private void addNode(SuffixNode node) {
        // Erase worst node in the hashFringe once we hit our Constant limit
        while (hashFringe.size() > this.maxSize) {
            Sequence worstSequence = this.findWorstNodeToTry();
            hashFringe.remove(worstSequence);
        }// if
        this.hashFringe.put(node.getSuffix(), node);
    }

    private Sequence findWorstNodeToTry() {
        double worstWeight = Double.MIN_VALUE;
        Sequence worstSequence = null;
        for (Sequence sequence : this.hashFringe.keySet()) {
            SuffixNode node = this.hashFringe.get(sequence);
            double nodeWeight = node.getWeight();
            if (nodeWeight > worstWeight) {
                worstWeight = nodeWeight;
                worstSequence = sequence;
            }
        }
        return worstSequence;
    }// findWorstNodeToTry
    //endregion
}
