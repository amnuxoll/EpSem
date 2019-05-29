package tests.utils;

import framework.Action;
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
        SequenceGenerator sequenceGenerator = new SequenceGenerator(new Action[0]);
        assertThrows(IndexOutOfBoundsException.class, () -> sequenceGenerator.nextPermutation(0));
    }

    @EpSemTest
    public void nextPermutationNoMovesAlwaysYieldsEmptySequence() {
        SequenceGenerator sequenceGenerator = new SequenceGenerator(new Action[0]);
        assertEquals(Sequence.EMPTY, sequenceGenerator.nextPermutation(1));
        assertEquals(Sequence.EMPTY, sequenceGenerator.nextPermutation(3));
        assertEquals(Sequence.EMPTY, sequenceGenerator.nextPermutation(42));
    }

    @EpSemTest
    public void nextPermutationPermutesSingleMove() {
        Action a = new Action("a");
        SequenceGenerator sequenceGenerator = new SequenceGenerator(new Action[] { a });
        assertEquals(new Sequence(new Action[] { a }), sequenceGenerator.nextPermutation(1));
        assertEquals(new Sequence(new Action[] { a, a }), sequenceGenerator.nextPermutation(2));
        assertEquals(new Sequence(new Action[] { a, a, a }), sequenceGenerator.nextPermutation(3));
        assertEquals(new Sequence(new Action[] { a, a, a, a, a, a, a, a }), sequenceGenerator.nextPermutation(8));
    }

    @EpSemTest
    public void nextPermutationPermutesTwoMoves() {
        Action a = new Action("a");
        Action b = new Action("b");
        SequenceGenerator sequenceGenerator = new SequenceGenerator(new Action[] { a, b });
        assertEquals(new Sequence(new Action[] { a }), sequenceGenerator.nextPermutation(1));
        assertEquals(new Sequence(new Action[] { b }), sequenceGenerator.nextPermutation(2));
        assertEquals(new Sequence(new Action[] { a, a }), sequenceGenerator.nextPermutation(3));
        assertEquals(new Sequence(new Action[] { a, b}), sequenceGenerator.nextPermutation(4));
        assertEquals(new Sequence(new Action[] { b, a}), sequenceGenerator.nextPermutation(5));
        assertEquals(new Sequence(new Action[] { b, b}), sequenceGenerator.nextPermutation(6));
        assertEquals(new Sequence(new Action[] { a, a, a}), sequenceGenerator.nextPermutation(7));
        assertEquals(new Sequence(new Action[] { a, a, b}), sequenceGenerator.nextPermutation(8));
        assertEquals(new Sequence(new Action[] { a, b, a}), sequenceGenerator.nextPermutation(9));
        assertEquals(new Sequence(new Action[] { a, b, b}), sequenceGenerator.nextPermutation(10));
        assertEquals(new Sequence(new Action[] { b, a, a}), sequenceGenerator.nextPermutation(11));
        assertEquals(new Sequence(new Action[] { b, a, b}), sequenceGenerator.nextPermutation(12));
        assertEquals(new Sequence(new Action[] { b, b, a}), sequenceGenerator.nextPermutation(13));
        assertEquals(new Sequence(new Action[] { b, b, b}), sequenceGenerator.nextPermutation(14));
    }

    // getCanonicalOderering
    @EpSemTest
    public void getCanonicalOrderingNullSequenceThrowsException() {
        Action a = new Action("a");
        Action b = new Action("b");
        SequenceGenerator sequenceGenerator = new SequenceGenerator(new Action[] { a, b });
        assertThrows(IllegalArgumentException.class, () -> sequenceGenerator.getCanonicalIndex(null));
    }

    @EpSemTest
    public void getCanonicalOrderingBinary() {
        Action a = new Action("a");
        Action b = new Action("b");
        SequenceGenerator sequenceGenerator = new SequenceGenerator(new Action[] { a, b });
        assertEquals(1L, sequenceGenerator.getCanonicalIndex(new Sequence(new Action[] { a })));
        assertEquals(2L, sequenceGenerator.getCanonicalIndex(new Sequence(new Action[] { b })));
        assertEquals(3L, sequenceGenerator.getCanonicalIndex(new Sequence(new Action[] { a, a })));
        assertEquals(4L, sequenceGenerator.getCanonicalIndex(new Sequence(new Action[] { a, b})));
        assertEquals(5L, sequenceGenerator.getCanonicalIndex(new Sequence(new Action[] { b, a})));
        assertEquals(6L, sequenceGenerator.getCanonicalIndex(new Sequence(new Action[] { b, b})));
        assertEquals(7L, sequenceGenerator.getCanonicalIndex(new Sequence(new Action[] { a, a, a})));
        assertEquals(8L, sequenceGenerator.getCanonicalIndex(new Sequence(new Action[] { a, a, b})));
        assertEquals(9L, sequenceGenerator.getCanonicalIndex(new Sequence(new Action[] { a, b, a})));
        assertEquals(10L, sequenceGenerator.getCanonicalIndex(new Sequence(new Action[] { a, b, b})));
        assertEquals(11L, sequenceGenerator.getCanonicalIndex(new Sequence(new Action[] { b, a, a})));
        assertEquals(12L, sequenceGenerator.getCanonicalIndex(new Sequence(new Action[] { b, a, b})));
        assertEquals(13L, sequenceGenerator.getCanonicalIndex(new Sequence(new Action[] { b, b, a})));
        assertEquals(14L, sequenceGenerator.getCanonicalIndex(new Sequence(new Action[] { b, b, b})));
    }

    @EpSemTest
    public void getCanonicalOrderingTernary() {
        Action a = new Action("a");
        Action b = new Action("b");
        Action c = new Action("c");
        SequenceGenerator sequenceGenerator = new SequenceGenerator(new Action[] { a, b, c });
        assertEquals(1L, sequenceGenerator.getCanonicalIndex(new Sequence(new Action[] { a })));
        assertEquals(2L, sequenceGenerator.getCanonicalIndex(new Sequence(new Action[] { b })));
        assertEquals(3L, sequenceGenerator.getCanonicalIndex(new Sequence(new Action[] { c })));
        assertEquals(4L, sequenceGenerator.getCanonicalIndex(new Sequence(new Action[] { a, a })));
        assertEquals(5L, sequenceGenerator.getCanonicalIndex(new Sequence(new Action[] { a, b })));
        assertEquals(6L, sequenceGenerator.getCanonicalIndex(new Sequence(new Action[] { a, c })));
        assertEquals(7L, sequenceGenerator.getCanonicalIndex(new Sequence(new Action[] { b, a })));
        assertEquals(8L, sequenceGenerator.getCanonicalIndex(new Sequence(new Action[] { b, b})));
        assertEquals(9L, sequenceGenerator.getCanonicalIndex(new Sequence(new Action[] { b, c })));
        assertEquals(10L, sequenceGenerator.getCanonicalIndex(new Sequence(new Action[] { c, a })));
        assertEquals(11L, sequenceGenerator.getCanonicalIndex(new Sequence(new Action[] { c, b })));
        assertEquals(12L, sequenceGenerator.getCanonicalIndex(new Sequence(new Action[] { c, c })));
        assertEquals(13L, sequenceGenerator.getCanonicalIndex(new Sequence(new Action[] { a, a, a })));
        assertEquals(14L, sequenceGenerator.getCanonicalIndex(new Sequence(new Action[] { a, a, b })));
    }
}
