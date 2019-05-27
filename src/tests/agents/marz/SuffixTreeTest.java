package tests.agents.marz;

import agents.marz.SuffixNode;
import agents.marz.SuffixTree;
import framework.Move;
import framework.Sequence;

import tests.EpSemTest;
import tests.EpSemTestClass;
import static tests.Assertions.*;

@EpSemTestClass
public class SuffixTreeTest {

    private static SuffixNode DEFAULT_NODE = new SuffixNode(new Sequence(new Move[] { new Move("A")}), new Move[] { new Move("A")}, index -> null);

    // constructor Tests
    @EpSemTest
    public void constructorMaxSizeLessThanOneThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> new SuffixTree(0, SuffixTreeTest.DEFAULT_NODE));
    }

    @EpSemTest
    public void constructorNullRootNodeThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> new SuffixTree(10, null));
    }

    // splitSuffix Tests
    @EpSemTest
    public void splitSuffixNullSuffixThrowsException() {
        SuffixTree suffixTree = new SuffixTree(20, SuffixTreeTest.DEFAULT_NODE);
        assertThrows(IllegalArgumentException.class, () -> suffixTree.splitSuffix(null));
    }

    @EpSemTest
    public void splitSuffixNotFoundReturnsFalse() {
        Sequence toSplit = new Sequence(new Move[] { new Move("a"), new Move("b"), new Move("c") });
        SuffixTree suffixTree = new SuffixTree(20, SuffixTreeTest.DEFAULT_NODE);
        assertFalse(suffixTree.splitSuffix(toSplit));
    }

    @EpSemTest
    public void splitSuffixAddsChildrenRemovesSelf() {
        Sequence toSplit = new Sequence(new Move[] { new Move("a"), new Move("b"), new Move("c") });
        SuffixNode node = SuffixTreeTest.DEFAULT_NODE;
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
        Sequence toSplit = new Sequence(new Move[] { new Move("a"), new Move("b"), new Move("c") });
        SuffixNode node = SuffixTreeTest.DEFAULT_NODE;
        SuffixTree suffixTree = new SuffixTree(20, node);
        SuffixNode bestNode = suffixTree.findBestNodeToTry();
        assertEquals(node, bestNode);
        suffixTree.splitSuffix(toSplit);
        bestNode = suffixTree.findBestNodeToTry();
        assertEquals(new Sequence(new Move[] { new Move("a")}), bestNode.getSuffix());
    }

    // findBestMatch Tests
    @EpSemTest
    public void findBestMatchNullSequenceThrowsException() {
        SuffixTree suffixTree = new SuffixTree(20, SuffixTreeTest.DEFAULT_NODE);
        assertThrows(IllegalArgumentException.class, () -> suffixTree.findBestMatch(null));
    }

    @EpSemTest
    public void findBestMatchMatchesFullSequence() {
        Sequence match = new Sequence(new Move[] { new Move("a"), new Move("b"), new Move("c") });
        SuffixNode node = SuffixTreeTest.DEFAULT_NODE;
        SuffixTree suffixTree = new SuffixTree(20, node);
        SuffixNode foundNode = suffixTree.findBestMatch(match);
        assertEquals(node, foundNode);
    }

    @EpSemTest
    public void findBestMatchMatchesSubsequence() {
        Sequence subsequence = new Sequence(new Move[] { new Move("a"), new Move("b"), new Move("c") });
        SuffixNode node = SuffixTreeTest.DEFAULT_NODE;
        SuffixTree suffixTree = new SuffixTree(20, node);
        Sequence match = subsequence.buildChildSequence(new Move("x")).buildChildSequence(new Move("y")).buildChildSequence(new Move("z"));
        SuffixNode foundNode = suffixTree.findBestMatch(match);
        assertEquals(node, foundNode);
    }

    @EpSemTest
    public void findBestMatchNotFoundReturnsNull() {
        Sequence match = new Sequence(new Move[] { new Move("a"), new Move("b"), new Move("c") });
        SuffixNode node = SuffixTreeTest.DEFAULT_NODE;
        SuffixTree suffixTree = new SuffixTree(20, node);
        SuffixNode foundNode = suffixTree.findBestMatch(new Sequence(new Move[] { new Move("x"), new Move("y"), new Move("z") }));
        assertNull(foundNode);
    }

    // containsSuffix Tests
    @EpSemTest
    public void containsSuffixNullSequenceThrowsException() {
        SuffixTree suffixTree = new SuffixTree(20, SuffixTreeTest.DEFAULT_NODE);
        assertThrows(IllegalArgumentException.class, () -> suffixTree.containsSuffix(null));
    }

    @EpSemTest
    public void containsSuffixTrue() {
        Sequence suffix = new Sequence(new Move[] { new Move("a"), new Move("b"), new Move("c") });
        SuffixNode node = SuffixTreeTest.DEFAULT_NODE;
        SuffixTree suffixTree = new SuffixTree(20, node);
        assertTrue(suffixTree.containsSuffix(suffix));
    }

    @EpSemTest
    public void containsSuffixFalse() {
        Sequence suffix = new Sequence(new Move[] { new Move("a"), new Move("b"), new Move("c") });
        SuffixNode node = SuffixTreeTest.DEFAULT_NODE;
        SuffixTree suffixTree = new SuffixTree(20, node);
        assertFalse(suffixTree.containsSuffix(suffix.buildChildSequence(new Move("x"))));
    }

//    public class TestSuffixNode extends SuffixNodeBase<TestSuffixNode> {
//        public double weight;
//        public TestSuffixNode(double weight, Sequence sequence) {
//            super(sequence);
//            this.weight = weight;
//        }
//
//        @Override
//        public TestSuffixNode[] split() {
//            return new TestSuffixNode[] {
//                    new TestSuffixNode(1, new Sequence(new Move[] { new Move("a") })),
//                    new TestSuffixNode(2, new Sequence(new Move[] { new Move("b") })),
//                    new TestSuffixNode(3, new Sequence(new Move[] { new Move("c") }))
//            };
//        }
//
//        @Override
//        protected void updateHeuristic() {
//
//        }
//
//        @Override
//        public double getNormalizedWeight() {
//            return 0;
//        }
//
//        @Override
//        protected boolean canSplit() {
//            return true;
//        }
//    }
}
