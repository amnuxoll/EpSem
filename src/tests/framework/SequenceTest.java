package tests.framework;

import framework.Action;
import framework.Sequence;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import tests.EpSemTest;
import tests.EpSemTestClass;
import static tests.Assertions.*;

/**
 *
 * @author Zachary Paul Faltersack
 * @version 0.95
 */
@EpSemTestClass
public class SequenceTest {
    //region endsWith Tests
    @EpSemTest
    public void endsWithNullSequenceThrowsException() {
        Sequence sequence = new Sequence(new Action[0]);
        assertThrows(IllegalArgumentException.class, () -> sequence.endsWith(null));
    }

    @EpSemTest
    public void endsWithFalseIfGivenSequenceIsLongerThanSelf() {
        Sequence toTest = new Sequence(new Action[] { new Action("a"), new Action("b"), new Action("c") });
        Sequence master = new Sequence(new Action[] { new Action("a"), new Action("b")});
        assertFalse(master.endsWith(toTest));
    }

    @EpSemTest
    public void endsWithTrueIfGivenEmptySequence() {
        Sequence toTest = new Sequence(new Action[0]);
        Sequence master = new Sequence(new Action[] { new Action("a"), new Action("b"), new Action("c")});
        assertTrue(master.endsWith(toTest));
    }

    @EpSemTest
    public void endsWithTrueIfGivenSequenceSameAsSelf() {
        Sequence toTest = new Sequence(new Action[] { new Action("a"), new Action("b"), new Action("c") });
        Sequence master = new Sequence(new Action[] { new Action("a"), new Action("b"), new Action("c")});
        assertTrue(master.endsWith(toTest));
    }

    @EpSemTest
    public void endsWithTrueIfGivenSequenceThatMatchesEndOfSelf() {
        Sequence toTest = new Sequence(new Action[] { new Action("b"), new Action("c") });
        Sequence master = new Sequence(new Action[] { new Action("a"), new Action("b"), new Action("c")});
        assertTrue(master.endsWith(toTest));
    }
    //endregion

    //region getActions Tests
    @EpSemTest
    public void getMoves()
    {
        Action[] actions = new Action[] { new Action("one"), new Action("two") };
        Sequence sequence = new Sequence(actions);
        assertSame(actions, sequence.getActions());
    }
    //endregion

    //region getSubsequence Tests
    @EpSemTest
    public void getSubsequenceIndexLessThanZeroThrowsException() {
        Sequence sequence = new Sequence(new Action[] { new Action("a"), new Action("b"), new Action("c")});
        assertThrows(IllegalArgumentException.class, () -> sequence.getSubsequence(-1));
    }

    @EpSemTest
    public void getSubsequenceStartAtZeroReturnsEqualSequence() {
        Sequence sequence = new Sequence(new Action[] { new Action("a"), new Action("b"), new Action("c")});
        Sequence subsequence = sequence.getSubsequence(0);
        assertEquals(sequence, subsequence);
    }

    @EpSemTest
    public void getSubsequenceReturnsProperSubsequence() {
        Sequence sequence = new Sequence(new Action[] { new Action("a"), new Action("b"), new Action("c")});
        Sequence subsequence = sequence.getSubsequence(1);
        Sequence expected = new Sequence(new Action[] { new Action("b"), new Action("c")});
        assertEquals(expected, subsequence);
    }

    @EpSemTest
    public void getSubsequenceReturnsProperSubsequenceLastItem() {
        Sequence sequence = new Sequence(new Action[] { new Action("a"), new Action("b"), new Action("c")});
        Sequence subsequence = sequence.getSubsequence(2);
        Sequence expected = new Sequence(new Action[] { new Action("c")});
        assertEquals(expected, subsequence);
    }

    @EpSemTest
    public void getSubsequenceLargerThanSequenceGivesEmptySequence() {
        Sequence sequence = new Sequence(new Action[] { new Action("a"), new Action("b"), new Action("c")});
        Sequence subsequence = sequence.getSubsequence(3);
        Sequence expected = new Sequence(new Action[0]);
        assertEquals(expected, subsequence);
    }
    //endregion

    //region getLength Tests
    @EpSemTest
    public void getLengthGivesEmptySequenceLength() {
        Sequence sequence = new Sequence(new Action[0]);
        assertEquals(0, sequence.getLength());
    }

    @EpSemTest
    public void getLengthGivesSequenceLength() {
        Sequence sequence = new Sequence(new Action[5]);
        assertEquals(5, sequence.getLength());
    }
    //endregion

    //region buildChildSequence Tests
    @EpSemTest
    public void buildChildSequenceNullMoveThrowsException() {
        Sequence sequence = new Sequence(new Action[0]);
        assertThrows(IllegalArgumentException.class, () -> sequence.buildChildSequence(null));
    }

    @EpSemTest
    public void buildChildSequenceAppendsToEmptySequence() {
        Sequence sequence = new Sequence(new Action[0]);
        Sequence childSequence = sequence.buildChildSequence(new Action("a"));
        Sequence expected = new Sequence(new Action[] { new Action("a")});
        assertEquals(expected, childSequence);
    }

    @EpSemTest
    public void buildChildSequencePrependsNewMove() {
        Sequence sequence = new Sequence(new Action[] { new Action("b"), new Action("a") });
        Sequence childSequence = sequence.buildChildSequence(new Action("c"));
        Sequence expected = new Sequence(new Action[] { new Action("c"), new Action("b"), new Action("a")});
        assertEquals(expected, childSequence);
    }
    //endregion

    //region hasNext Tests
    @EpSemTest
    public void hasNextFalseForEmptySequence() {
        Sequence sequence = new Sequence(new Action[0]);
        assertFalse(sequence.hasNext());
    }

    @EpSemTest
    public void hasNextTrueForNonEmptySequence() {
        Sequence sequence = new Sequence(new Action[] { new Action("a")});
        assertTrue(sequence.hasNext());
    }
    //endregion

    //region next Tests
    @EpSemTest
    public void nextNoNextItemThrowsException() {
        Sequence sequence = new Sequence(new Action[0]);
        assertThrows(RuntimeException.class, () -> sequence.next());
    }

    @EpSemTest
    public void nextReturnsNextItem() {
        Sequence sequence = new Sequence(new Action[] { new Action("a") });
        Action action = sequence.next();
        assertEquals(new Action("a"), action);
    }
    //endregion

    //region reset (iteration Integration) Tests
    @EpSemTest
    public void iterationIntegration() {
        Action a = new Action("a");
        Action b = new Action("b");
        Action c = new Action("c");
        Sequence sequence = new Sequence(new Action[] { new Action("a"), new Action("b"), new Action("c") });
        assertTrue(sequence.hasNext());
        assertEquals(a, sequence.next());
        assertTrue(sequence.hasNext());
        assertEquals(b, sequence.next());
        assertTrue(sequence.hasNext());
        sequence.reset();
        assertTrue(sequence.hasNext());
        assertEquals(a, sequence.next());
        assertTrue(sequence.hasNext());
        assertEquals(b, sequence.next());
        assertTrue(sequence.hasNext());
        assertEquals(c, sequence.next());
        assertFalse(sequence.hasNext());
        sequence.reset();
        assertTrue(sequence.hasNext());
        assertEquals(a, sequence.next());
        assertTrue(sequence.hasNext());
        assertEquals(b, sequence.next());
        assertTrue(sequence.hasNext());
        assertEquals(c, sequence.next());
        assertFalse(sequence.hasNext());
    }
    //endregion

    //region getCurrentIndex Tests
    @EpSemTest
    public void getCurrentIndex()
    {

        Action[] actions = new Action[] { new Action("one"), new Action("two") };
        Sequence sequence = new Sequence(actions);
        assertEquals(-1, sequence.getCurrentIndex());
        sequence.next();
        assertEquals(0, sequence.getCurrentIndex());
    }
    //endregion

    //region equals Tests
    @EpSemTest
    public void equalsAreEqual() {
        Sequence sequence1 = new Sequence(new Action[] { new Action("a"), new Action("b"), new Action("c") });
        Sequence sequence2 = new Sequence(new Action[] { new Action("a"), new Action("b"), new Action("c") });
        assertEquals(sequence1, sequence2);
    }

    @EpSemTest
    public void equalsAreEqualSameObject() {
        Sequence sequence1 = new Sequence(new Action[] { new Action("a"), new Action("b"), new Action("c") });
        assertTrue(sequence1.equals(sequence1));
    }

    @EpSemTest
    public void equalsAreNotEqualDifferentMoves() {
        Sequence sequence1 = new Sequence(new Action[] { new Action("d"), new Action("b"), new Action("c") });
        Sequence sequence2 = new Sequence(new Action[] { new Action("a"), new Action("b"), new Action("c") });
        assertNotEquals(sequence1, sequence2);
    }

    @EpSemTest
    public void equalsAreNotEqualDifferentMoveCounts() {
        Sequence sequence1 = new Sequence(new Action[] { new Action("b"), new Action("c") });
        Sequence sequence2 = new Sequence(new Action[] { new Action("a"), new Action("b"), new Action("c") });
        assertNotEquals(sequence1, sequence2);
    }

    @EpSemTest
    public void equalsAreNotEqualDifferentTypes() {
        Sequence sequence1 = new Sequence(new Action[] { new Action("b"), new Action("c") });
        assertFalse(sequence1.equals(new Action("a")));
    }
    //endregion

    //region hashcode Tests
    @EpSemTest
    public void hashCodeSameForEqualSequences() {
        Sequence sequence1 = new Sequence(new Action[] { new Action("a"), new Action("b"), new Action("c") });
        Sequence sequence2 = new Sequence(new Action[] { new Action("a"), new Action("b"), new Action("c") });
        assertEquals(sequence1.hashCode(), sequence2.hashCode());
    }
    //endregion

    //region take Tests
    @EpSemTest
    public void takeLengthLessThanZeroThrowsException() {
        Sequence sequence = new Sequence(new Action[0]);
        assertThrows(IllegalArgumentException.class, () -> sequence.take(-1));
    }

    @EpSemTest
    public void takeLengthBiggerThanSequenceThrowsException() {
        Sequence sequence = new Sequence(new Action[] { new Action("a") });
        assertThrows(IllegalArgumentException.class, () -> sequence.take(2));
    }

    @EpSemTest
    public void takeNothingGivesEmptySequence() {
        Sequence sequence = new Sequence(new Action[] { new Action("a") });
        Sequence subsequence = sequence.take(0);
        assertEquals(new Sequence(new Action[0]), subsequence);
    }

    @EpSemTest
    public void takeGivesSingleItem() {
        Sequence sequence = new Sequence(new Action[] { new Action("a") });
        Sequence subsequence = sequence.take(1);
        assertEquals(sequence, subsequence);
    }

    @EpSemTest
    public void takeGivesFirstNItems() {
        Sequence sequence = new Sequence(new Action[] { new Action("a"), new Action("b"), new Action("c") });
        Sequence subsequence = sequence.take(2);
        Sequence expected = new Sequence(new Action[] { new Action("a"), new Action("b") });
        assertEquals(expected, subsequence);
    }
    //endregion

    //region comparable Tests
    @EpSemTest
    public void comparableSortsByLength() {
        Sequence length1 = new Sequence(new Action[] { new Action("a")});
        Sequence length21 = new Sequence(new Action[] { new Action("a"), new Action("b") });
        Sequence length22 = new Sequence(new Action[] { new Action("c"), new Action("d") });
        Sequence length3 = new Sequence(new Action[] { new Action("a"), new Action("b"), new Action("c") });
        Sequence length4 = new Sequence(new Action[] { new Action("a"), new Action("b"), new Action("c"), new Action("d"), });
        List<Sequence> actual = new ArrayList<>();
        actual.add(length1);
        actual.add(length21);
        actual.add(length22);
        actual.add(length3);
        actual.add(length4);
        Collections.sort(actual);
        List<Sequence> expected = new ArrayList<>();
        expected.add(length4);
        expected.add(length3);
        expected.add(length21);
        expected.add(length22);
        expected.add(length1);
        assertArrayEquals(expected.toArray(new Sequence[0]), actual.toArray(new Sequence[0]));
    }
    //endregion

    //region concat Tests
    @EpSemTest
    public void concatAppendsSequence() {
        Sequence start = new Sequence(new Action[] { new Action("a"), new Action("b") });
        Sequence end = new Sequence(new Action[] { new Action("c"), new Action("d") });
        Sequence expected = new Sequence(new Action[] { new Action("a"), new Action("b"), new Action("c"), new Action("d") });
        Sequence actual = start.concat(end);
        assertEquals(expected, actual);
    }

    @EpSemTest
    public void concatNullSequenceThrowsException() {
        Sequence start = new Sequence(new Action[] { new Action("a"), new Action("b") });
        assertThrows(IllegalArgumentException.class, () -> start.concat(null));
    }

    
    //endregion

    //region stepsRemaining Tests
    @EpSemTest
    public void testStepsRemainingEmptySequence() {
        assertEquals(Sequence.EMPTY.stepsRemaining(), 0);
    }

    @EpSemTest
    public void testStepsRemainingNormal() {
        Sequence testme = new Sequence(new Action[] { new Action("a"), new Action("b"), new Action("c"), new Action("d") });
        assertEquals(4, testme.stepsRemaining());
        testme.next();
        assertEquals(3, testme.stepsRemaining());
        testme.next();
        assertEquals(2, testme.stepsRemaining());
        testme.next();
        assertEquals(1, testme.stepsRemaining());
        testme.next();
        assertEquals(0, testme.stepsRemaining());
    }

    //endregion



}
