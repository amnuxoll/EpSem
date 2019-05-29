package tests.agents.marz;

import agents.marz.SuffixNode;
import agents.marz.SuffixTree;
import framework.Action;
import framework.Sequence;

import tests.EpSemTest;
import tests.EpSemTestClass;
import static tests.Assertions.*;

@EpSemTestClass
public class SuffixTreeTest {

    // constructor Tests
    @EpSemTest
    public void constructorMaxSizeLessThanOneThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> new SuffixTree(0, new SuffixNode(Sequence.EMPTY, new Action[] { new Action("a") }, index -> null)));
    }

    @EpSemTest
    public void constructorNullRootNodeThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> new SuffixTree(10, null));
    }

    // splitSuffix Tests
    @EpSemTest
    public void splitSuffixNullSuffixThrowsException() {
        SuffixTree suffixTree = new SuffixTree(20, new SuffixNode(Sequence.EMPTY, new Action[] { new Action("a") }, index -> null));
        assertThrows(IllegalArgumentException.class, () -> suffixTree.splitSuffix(null));
    }

    @EpSemTest
    public void splitSuffixNotFoundReturnsFalse() {
        Sequence toSplit = new Sequence(new Action[] { new Action("a"), new Action("b"), new Action("c") });
        SuffixTree suffixTree = new SuffixTree(20, new SuffixNode(Sequence.EMPTY, new Action[] { new Action("a"), new Action("b"), new Action("c") }, index -> null));
        assertFalse(suffixTree.splitSuffix(toSplit));
    }

    @EpSemTest
    public void splitSuffixAddsChildrenRemovesSelf() {
        Sequence toSplit = new Sequence(new Action[] { new Action("a"), new Action("b"), new Action("c") });
        SuffixNode node = new SuffixNode(toSplit, new Action[] { new Action("a"), new Action("b"), new Action("c") }, index -> null);
        SuffixTree suffixTree = new SuffixTree(20, node);
        assertTrue(suffixTree.splitSuffix(toSplit));
        assertFalse(suffixTree.containsSuffix(toSplit));
        SuffixNode[] children = node.split();
        for (SuffixNode child : children) {
            assertTrue(suffixTree.containsSuffix(child.getSuffix()));
        }
    }

    // findBestNodeToTry Tests
    @EpSemTest
    public void findBestNodeToTryTakesHighestWeightNode() {
        Sequence toSplit = new Sequence(new Action[] { new Action("a"), new Action("b"), new Action("c") });
        SuffixNode node = new SuffixNode(toSplit, new Action[] { new Action("a"), new Action("b"), new Action("c") }, index -> null);
        SuffixTree suffixTree = new SuffixTree(20, node);
        SuffixNode bestNode = suffixTree.findBestNodeToTry();
        assertEquals(node, bestNode);
        suffixTree.splitSuffix(toSplit);
        bestNode = suffixTree.findBestNodeToTry();
        assertEquals(new Sequence(new Action[] { new Action("a")}), bestNode.getSuffix());
    }

    // findBestMatch Tests
    @EpSemTest
    public void findBestMatchNullSequenceThrowsException() {
        SuffixTree suffixTree = new SuffixTree(20, new SuffixNode(Sequence.EMPTY, new Action[] { new Action("a") }, index -> null));
        assertThrows(IllegalArgumentException.class, () -> suffixTree.findBestMatch(null));
    }

    @EpSemTest
    public void findBestMatchMatchesFullSequence() {
        Sequence match = new Sequence(new Action[] { new Action("a"), new Action("b"), new Action("c") });
        SuffixNode node = new SuffixNode(match, new Action[] { new Action("a"), new Action("b"), new Action("c") }, index -> null);
        SuffixTree suffixTree = new SuffixTree(20, node);
        SuffixNode foundNode = suffixTree.findBestMatch(match);
        assertEquals(node, foundNode);
    }

    @EpSemTest
    public void findBestMatchMatchesSubsequence() {
        Sequence subsequence = new Sequence(new Action[] { new Action("a"), new Action("b"), new Action("c") });
        SuffixNode node = new SuffixNode(subsequence, new Action[] { new Action("a"), new Action("b"), new Action("c"), new Action("x"), new Action("y"), new Action("z") }, index -> null);
        SuffixTree suffixTree = new SuffixTree(20, node);
        Sequence match = subsequence.buildChildSequence(new Action("x")).buildChildSequence(new Action("y")).buildChildSequence(new Action("z"));
        SuffixNode foundNode = suffixTree.findBestMatch(match);
        assertEquals(node, foundNode);
    }

    @EpSemTest
    public void findBestMatchNotFoundReturnsNull() {
        Sequence match = new Sequence(new Action[] { new Action("a"), new Action("b"), new Action("c") });
        SuffixNode node = new SuffixNode(match, new Action[] { new Action("a"), new Action("b"), new Action("c") }, index -> null);
        SuffixTree suffixTree = new SuffixTree(20, node);
        SuffixNode foundNode = suffixTree.findBestMatch(new Sequence(new Action[] { new Action("x"), new Action("y"), new Action("z") }));
        assertNull(foundNode);
    }

    // containsSuffix Tests
    @EpSemTest
    public void containsSuffixNullSequenceThrowsException() {
        SuffixTree suffixTree = new SuffixTree(20, new SuffixNode(Sequence.EMPTY, new Action[] { new Action("a") }, index -> null));
        assertThrows(IllegalArgumentException.class, () -> suffixTree.containsSuffix(null));
    }

    @EpSemTest
    public void containsSuffixTrue() {
        Sequence suffix = new Sequence(new Action[] { new Action("a"), new Action("b"), new Action("c") });
        SuffixNode node = new SuffixNode(suffix, new Action[] { new Action("a"), new Action("b"), new Action("c") }, index -> null);
        SuffixTree suffixTree = new SuffixTree(20, node);
        assertTrue(suffixTree.containsSuffix(suffix));
    }

    @EpSemTest
    public void containsSuffixFalse() {
        Sequence suffix = new Sequence(new Action[] { new Action("a"), new Action("b"), new Action("c") });
        SuffixNode node = new SuffixNode(suffix, new Action[] { new Action("a"), new Action("b"), new Action("c"), new Action("x") }, index -> null);
        SuffixTree suffixTree = new SuffixTree(20, node);
        assertFalse(suffixTree.containsSuffix(suffix.buildChildSequence(new Action("x"))));
    }
}