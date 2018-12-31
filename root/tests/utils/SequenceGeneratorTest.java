package tests.utils;

import framework.Move;
import framework.Sequence;
import utils.SequenceGenerator;

import tests.EpSemTest;
import tests.EpSemTestClass;
import static tests.Assertions.*;

/**
 *
 * @author Zachary Paul Faltersack
 * @version 0.95
 */
@EpSemTestClass
public class SequenceGeneratorTest {

    // constructor Tests
    @EpSemTest
    public void constructorNullMovesThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> new SequenceGenerator(null));
    }

    // nextPermutation Tests
    @EpSemTest
    public void nextPermutationIndexLessThanZeroThrowsException() {
        SequenceGenerator sequenceGenerator = new SequenceGenerator(new Move[0]);
        assertThrows(IndexOutOfBoundsException.class, () -> sequenceGenerator.nextPermutation(0));
    }

    @EpSemTest
    public void nextPermutationNoMovesAlwaysYieldsEmptySequence() {
        SequenceGenerator sequenceGenerator = new SequenceGenerator(new Move[0]);
        assertEquals(Sequence.EMPTY, sequenceGenerator.nextPermutation(1));
        assertEquals(Sequence.EMPTY, sequenceGenerator.nextPermutation(3));
        assertEquals(Sequence.EMPTY, sequenceGenerator.nextPermutation(42));
    }

    @EpSemTest
    public void nextPermutationPermutesSingleMove() {
        Move a = new Move("a");
        SequenceGenerator sequenceGenerator = new SequenceGenerator(new Move[] { a });
        assertEquals(new Sequence(new Move[] { a }), sequenceGenerator.nextPermutation(1));
        assertEquals(new Sequence(new Move[] { a, a }), sequenceGenerator.nextPermutation(2));
        assertEquals(new Sequence(new Move[] { a, a, a }), sequenceGenerator.nextPermutation(3));
        assertEquals(new Sequence(new Move[] { a, a, a, a, a, a, a, a }), sequenceGenerator.nextPermutation(8));
    }

    @EpSemTest
    public void nextPermutationPermutesTwoMoves() {
        Move a = new Move("a");
        Move b = new Move("b");
        SequenceGenerator sequenceGenerator = new SequenceGenerator(new Move[] { a, b });
        assertEquals(new Sequence(new Move[] { a }), sequenceGenerator.nextPermutation(1));
        assertEquals(new Sequence(new Move[] { b }), sequenceGenerator.nextPermutation(2));
        assertEquals(new Sequence(new Move[] { a, a }), sequenceGenerator.nextPermutation(3));
        assertEquals(new Sequence(new Move[] { a, b}), sequenceGenerator.nextPermutation(4));
        assertEquals(new Sequence(new Move[] { b, a}), sequenceGenerator.nextPermutation(5));
        assertEquals(new Sequence(new Move[] { b, b}), sequenceGenerator.nextPermutation(6));
        assertEquals(new Sequence(new Move[] { a, a, a}), sequenceGenerator.nextPermutation(7));
        assertEquals(new Sequence(new Move[] { a, a, b}), sequenceGenerator.nextPermutation(8));
        assertEquals(new Sequence(new Move[] { a, b, a}), sequenceGenerator.nextPermutation(9));
        assertEquals(new Sequence(new Move[] { a, b, b}), sequenceGenerator.nextPermutation(10));
        assertEquals(new Sequence(new Move[] { b, a, a}), sequenceGenerator.nextPermutation(11));
        assertEquals(new Sequence(new Move[] { b, a, b}), sequenceGenerator.nextPermutation(12));
        assertEquals(new Sequence(new Move[] { b, b, a}), sequenceGenerator.nextPermutation(13));
        assertEquals(new Sequence(new Move[] { b, b, b}), sequenceGenerator.nextPermutation(14));
    }

    // getCanonicalOderering
    @EpSemTest
    public void getCanonicalOrderingNullSequenceThrowsException() {
        Move a = new Move("a");
        Move b = new Move("b");
        SequenceGenerator sequenceGenerator = new SequenceGenerator(new Move[] { a, b });
        assertThrows(IllegalArgumentException.class, () -> sequenceGenerator.getCanonicalIndex(null));
    }

    @EpSemTest
    public void getCanonicalOrderingBinary() {
        Move a = new Move("a");
        Move b = new Move("b");
        SequenceGenerator sequenceGenerator = new SequenceGenerator(new Move[] { a, b });
        assertEquals(1L, sequenceGenerator.getCanonicalIndex(new Sequence(new Move[] { a })));
        assertEquals(2L, sequenceGenerator.getCanonicalIndex(new Sequence(new Move[] { b })));
        assertEquals(3L, sequenceGenerator.getCanonicalIndex(new Sequence(new Move[] { a, a })));
        assertEquals(4L, sequenceGenerator.getCanonicalIndex(new Sequence(new Move[] { a, b})));
        assertEquals(5L, sequenceGenerator.getCanonicalIndex(new Sequence(new Move[] { b, a})));
        assertEquals(6L, sequenceGenerator.getCanonicalIndex(new Sequence(new Move[] { b, b})));
        assertEquals(7L, sequenceGenerator.getCanonicalIndex(new Sequence(new Move[] { a, a, a})));
        assertEquals(8L, sequenceGenerator.getCanonicalIndex(new Sequence(new Move[] { a, a, b})));
        assertEquals(9L, sequenceGenerator.getCanonicalIndex(new Sequence(new Move[] { a, b, a})));
        assertEquals(10L, sequenceGenerator.getCanonicalIndex(new Sequence(new Move[] { a, b, b})));
        assertEquals(11L, sequenceGenerator.getCanonicalIndex(new Sequence(new Move[] { b, a, a})));
        assertEquals(12L, sequenceGenerator.getCanonicalIndex(new Sequence(new Move[] { b, a, b})));
        assertEquals(13L, sequenceGenerator.getCanonicalIndex(new Sequence(new Move[] { b, b, a})));
        assertEquals(14L, sequenceGenerator.getCanonicalIndex(new Sequence(new Move[] { b, b, b})));
    }

    @EpSemTest
    public void getCanonicalOrderingTernary() {
        Move a = new Move("a");
        Move b = new Move("b");
        Move c = new Move("c");
        SequenceGenerator sequenceGenerator = new SequenceGenerator(new Move[] { a, b, c });
        assertEquals(1L, sequenceGenerator.getCanonicalIndex(new Sequence(new Move[] { a })));
        assertEquals(2L, sequenceGenerator.getCanonicalIndex(new Sequence(new Move[] { b })));
        assertEquals(3L, sequenceGenerator.getCanonicalIndex(new Sequence(new Move[] { c })));
        assertEquals(4L, sequenceGenerator.getCanonicalIndex(new Sequence(new Move[] { a, a })));
        assertEquals(5L, sequenceGenerator.getCanonicalIndex(new Sequence(new Move[] { a, b })));
        assertEquals(6L, sequenceGenerator.getCanonicalIndex(new Sequence(new Move[] { a, c })));
        assertEquals(7L, sequenceGenerator.getCanonicalIndex(new Sequence(new Move[] { b, a })));
        assertEquals(8L, sequenceGenerator.getCanonicalIndex(new Sequence(new Move[] { b, b})));
        assertEquals(9L, sequenceGenerator.getCanonicalIndex(new Sequence(new Move[] { b, c })));
        assertEquals(10L, sequenceGenerator.getCanonicalIndex(new Sequence(new Move[] { c, a })));
        assertEquals(11L, sequenceGenerator.getCanonicalIndex(new Sequence(new Move[] { c, b })));
        assertEquals(12L, sequenceGenerator.getCanonicalIndex(new Sequence(new Move[] { c, c })));
        assertEquals(13L, sequenceGenerator.getCanonicalIndex(new Sequence(new Move[] { a, a, a })));
        assertEquals(14L, sequenceGenerator.getCanonicalIndex(new Sequence(new Move[] { a, a, b })));
    }
}
