package agents.marz;

import utils.Sequence;

import java.util.*;

/**
 * SuffixTree
 * Represents a tree of SuffixNodeBase
 *
 * @author Zachary Paul Faltersack
 * @version 0.95
 */
public class SuffixTree<TSuffixNode extends SuffixNodeBase<TSuffixNode>> {

    /** hash table of all nodes on the fringe of our search */
    private HashMap<Sequence, TSuffixNode> hashFringe = new HashMap<>();

    private int maxSize;

    /**
     * Create an instance of a SuffixTree with a given size and root node.
     * @param maxSize The max allowed size of this tree.
     * @param rootNode The root node of the tree.
     */
    SuffixTree(int maxSize, TSuffixNode rootNode) {
        if (maxSize < 1)
            throw new IllegalArgumentException("maxSize must be greater than 0");
        if (rootNode == null)
            throw new IllegalArgumentException("rootNode cannot be null");
        this.maxSize = maxSize;
        this.addNode(rootNode);
    }

    /**
     * Split the node with the given suffix.
     * @param sequence The suffix to split.
     * @return True if the suffix node is split; otherwise false.
     */
    public boolean splitSuffix(Sequence sequence) {
        if (sequence == null)
            throw new IllegalArgumentException("sequence cannot be null");
        TSuffixNode node = this.hashFringe.get(sequence);
        if (node == null)
            return false;
        TSuffixNode[] children = node.split();

        if (children == null)
            return false;

        //Ready to commit:  add the children to the fringe and remove the parent
        for (TSuffixNode aChildren : children) {
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
    public TSuffixNode findBestNodeToTry() {
        double bestWeight = Double.MAX_VALUE;
        TSuffixNode bestNode = null;
        for (TSuffixNode node : this.hashFringe.values()) {
            double nodeWeight = node.getWeight();
            //System.out.println("Current best weight: " + bestWeight + " test against: " + nodeWeight);
            if (nodeWeight < bestWeight) {
//                System.out.println("New best weight found.");
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
    public TSuffixNode findBestMatch(Sequence sequence) {
        if (sequence == null)
            throw new IllegalArgumentException("sequence cannot be null");
        Sequence bestMatch = null;
        int index = 0;
        Sequence subsequence;
        do {
            subsequence = sequence.getSubsequence(index++);
            for (Sequence suffixKey : this.hashFringe.keySet())
            {
                if (subsequence.endsWith(suffixKey) && (bestMatch == null || suffixKey.getLength() >  bestMatch.getLength()))
                    bestMatch = suffixKey;
            }
        } while (index < sequence.getLength());

        if (bestMatch == null)
            return null;
        return this.hashFringe.get(bestMatch);
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

    private void addNode(TSuffixNode node) {
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
            TSuffixNode node = this.hashFringe.get(sequence);
            double nodeWeight = node.getWeight();
            if (nodeWeight > worstWeight) {
                worstWeight = nodeWeight;
                worstSequence = sequence;
            }
        }
        return worstSequence;
    }// findWorstNodeToTry

    public void printTree() {
        for (TSuffixNode node : this.hashFringe.values()) {
            System.out.println("Node: " + node);
        }
    }
}
