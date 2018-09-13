package agents.marz;

import framework.Move;
import org.junit.jupiter.api.Test;
import utils.Sequence;

import static org.junit.jupiter.api.Assertions.*;

public class SuffixTreeTest {

    // constructor Tests
    @Test
    public void constructorMaxSizeLessThanOneThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> new SuffixTree<TestSuffixNode>(0, new TestSuffixNode(5, Sequence.EMPTY)));
    }

    @Test
    public void constructorNullRootNodeThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> new SuffixTree<TestSuffixNode>(10, null));
    }

    // splitSuffix Tests
    @Test
    public void splitSuffixNullSuffixThrowsException() {
        SuffixTree<TestSuffixNode> suffixTree = new SuffixTree<>(20, new TestSuffixNode(10, Sequence.EMPTY));
        assertThrows(IllegalArgumentException.class, () -> suffixTree.splitSuffix(null));
    }

    @Test
    public void splitSuffixNotFoundReturnsFalse() {
        Sequence toSplit = new Sequence(new Move[] { new Move("a"), new Move("b"), new Move("c") });
        SuffixTree<TestSuffixNode> suffixTree = new SuffixTree<>(20, new TestSuffixNode(10, Sequence.EMPTY));
        assertFalse(suffixTree.splitSuffix(toSplit));
    }

    @Test
    public void splitSuffixAddsChildrenRemovesSelf() {
        Sequence toSplit = new Sequence(new Move[] { new Move("a"), new Move("b"), new Move("c") });
        TestSuffixNode node = new TestSuffixNode(10, toSplit);
        SuffixTree<TestSuffixNode> suffixTree = new SuffixTree<>(20, node);
        assertTrue(suffixTree.splitSuffix(toSplit));
        assertFalse(suffixTree.containsSuffix(toSplit));
        TestSuffixNode[] children = node.split();
        for (TestSuffixNode child : children) {
            assertTrue(suffixTree.containsSuffix(child.getSuffix()));
        }
    }

    // findBestNodeToTry Tests
    @Test
    public void findBestNodeToTryTakesHighestWeightNode() {
        Sequence toSplit = new Sequence(new Move[] { new Move("a"), new Move("b"), new Move("c") });
        TestSuffixNode node = new TestSuffixNode(10, toSplit);
        SuffixTree<TestSuffixNode> suffixTree = new SuffixTree<>(20, node);
        TestSuffixNode bestNode = suffixTree.findBestNodeToTry();
        assertEquals(node, bestNode);
        suffixTree.splitSuffix(toSplit);
        bestNode = suffixTree.findBestNodeToTry();
        assertEquals(new Sequence(new Move[] { new Move("a")}), bestNode.getSuffix());
    }

    // findBestMatch Tests
    @Test
    public void findBestMatchNullSequenceThrowsException() {
        SuffixTree<TestSuffixNode> suffixTree = new SuffixTree<>(20, new TestSuffixNode(10, Sequence.EMPTY));
        assertThrows(IllegalArgumentException.class, () -> suffixTree.findBestMatch(null));
    }

    @Test
    public void findBestMatchMatchesFullSequence() {
        Sequence match = new Sequence(new Move[] { new Move("a"), new Move("b"), new Move("c") });
        TestSuffixNode node = new TestSuffixNode(10, match);
        SuffixTree<TestSuffixNode> suffixTree = new SuffixTree<>(20, node);
        TestSuffixNode foundNode = suffixTree.findBestMatch(match);
        assertEquals(node, foundNode);
    }

    @Test
    public void findBestMatchMatchesSubsequence() {
        Sequence subsequence = new Sequence(new Move[] { new Move("a"), new Move("b"), new Move("c") });
        TestSuffixNode node = new TestSuffixNode(10, subsequence);
        SuffixTree<TestSuffixNode> suffixTree = new SuffixTree<>(20, node);
        Sequence match = subsequence.buildChildSequence(new Move("x")).buildChildSequence(new Move("y")).buildChildSequence(new Move("z"));
        TestSuffixNode foundNode = suffixTree.findBestMatch(match);
        assertEquals(node, foundNode);
    }

    @Test
    public void findBestMatchNotFoundReturnsNull() {
        Sequence match = new Sequence(new Move[] { new Move("a"), new Move("b"), new Move("c") });
        TestSuffixNode node = new TestSuffixNode(10, match);
        SuffixTree<TestSuffixNode> suffixTree = new SuffixTree<>(20, node);
        TestSuffixNode foundNode = suffixTree.findBestMatch(new Sequence(new Move[] { new Move("x"), new Move("y"), new Move("z") }));
        assertNull(foundNode);
    }

    // containsSuffix Tests
    @Test
    public void containsSuffixNullSequenceThrowsException() {
        SuffixTree<TestSuffixNode> suffixTree = new SuffixTree<>(20, new TestSuffixNode(10, Sequence.EMPTY));
        assertThrows(IllegalArgumentException.class, () -> suffixTree.containsSuffix(null));
    }

    @Test
    public void containsSuffixTrue() {
        Sequence suffix = new Sequence(new Move[] { new Move("a"), new Move("b"), new Move("c") });
        TestSuffixNode node = new TestSuffixNode(10, suffix);
        SuffixTree<TestSuffixNode> suffixTree = new SuffixTree<>(20, node);
        assertTrue(suffixTree.containsSuffix(suffix));
    }

    @Test
    public void containsSuffixFalse() {
        Sequence suffix = new Sequence(new Move[] { new Move("a"), new Move("b"), new Move("c") });
        TestSuffixNode node = new TestSuffixNode(10, suffix);
        SuffixTree<TestSuffixNode> suffixTree = new SuffixTree<>(20, node);
        assertFalse(suffixTree.containsSuffix(suffix.buildChildSequence(new Move("x"))));
    }

    public class TestSuffixNode extends SuffixNodeBase<TestSuffixNode> {
        public double weight;
        public TestSuffixNode(double weight, Sequence sequence) {
            super(sequence);
            this.weight = weight;
        }

        @Override
        public TestSuffixNode[] split() {
            return new TestSuffixNode[] {
                    new TestSuffixNode(1, new Sequence(new Move[] { new Move("a") })),
                    new TestSuffixNode(2, new Sequence(new Move[] { new Move("b") })),
                    new TestSuffixNode(3, new Sequence(new Move[] { new Move("c") }))
            };
        }

        @Override
        protected void updateHeuristic() {

        }

        @Override
        protected boolean canSplit() {
            return true;
        }
    }
}
