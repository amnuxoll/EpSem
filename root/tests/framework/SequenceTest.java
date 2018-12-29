package framework;

import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.jupiter.api.Assertions.*;

public class SequenceTest {
    //region constructor Tests
    @Test
    public void constructorNullMovesThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> new Sequence(null));
    }
    //endregion

    //region endsWith Tests
    @Test
    public void endsWithNullSequenceThrowsException() {
        Sequence sequence = new Sequence(new Move[0]);
        assertThrows(IllegalArgumentException.class, () -> sequence.endsWith(null));
    }

    @Test
    public void endsWithFalseIfGivenSequenceIsLongerThanSelf() {
        Sequence toTest = new Sequence(new Move[] { new Move("a"), new Move("b"), new Move("c") });
        Sequence master = new Sequence(new Move[] { new Move("a"), new Move("b")});
        assertFalse(master.endsWith(toTest));
    }

    @Test
    public void endsWithTrueIfGivenEmptySequence() {
        Sequence toTest = new Sequence(new Move[0]);
        Sequence master = new Sequence(new Move[] { new Move("a"), new Move("b"), new Move("c")});
        assertTrue(master.endsWith(toTest));
    }

    @Test
    public void endsWithTrueIfGivenSequenceSameAsSelf() {
        Sequence toTest = new Sequence(new Move[] { new Move("a"), new Move("b"), new Move("c") });
        Sequence master = new Sequence(new Move[] { new Move("a"), new Move("b"), new Move("c")});
        assertTrue(master.endsWith(toTest));
    }

    @Test
    public void endsWithTrueIfGivenSequenceThatMatchesEndOfSelf() {
        Sequence toTest = new Sequence(new Move[] { new Move("b"), new Move("c") });
        Sequence master = new Sequence(new Move[] { new Move("a"), new Move("b"), new Move("c")});
        assertTrue(master.endsWith(toTest));
    }
    //endregion

    //region getMoves Tests
    @Test
    public void getMoves()
    {
        Move[] moves = new Move[] { new Move("one"), new Move("two") };
        Sequence sequence = new Sequence(moves);
        assertSame(moves, sequence.getMoves());
    }
    //endregion

    //region getSubsequence Tests
    @Test
    public void getSubsequenceIndexLessThanZeroThrowsException() {
        Sequence sequence = new Sequence(new Move[] { new Move("a"), new Move("b"), new Move("c")});
        assertThrows(IllegalArgumentException.class, () -> sequence.getSubsequence(-1));
    }

    @Test
    public void getSubsequenceStartAtZeroReturnsEqualSequence() {
        Sequence sequence = new Sequence(new Move[] { new Move("a"), new Move("b"), new Move("c")});
        Sequence subsequence = sequence.getSubsequence(0);
        assertEquals(sequence, subsequence);
    }

    @Test
    public void getSubsequenceReturnsProperSubsequence() {
        Sequence sequence = new Sequence(new Move[] { new Move("a"), new Move("b"), new Move("c")});
        Sequence subsequence = sequence.getSubsequence(1);
        Sequence expected = new Sequence(new Move[] { new Move("b"), new Move("c")});
        assertEquals(expected, subsequence);
    }

    @Test
    public void getSubsequenceReturnsProperSubsequenceLastItem() {
        Sequence sequence = new Sequence(new Move[] { new Move("a"), new Move("b"), new Move("c")});
        Sequence subsequence = sequence.getSubsequence(2);
        Sequence expected = new Sequence(new Move[] { new Move("c")});
        assertEquals(expected, subsequence);
    }

    @Test
    public void getSubsequenceLargerThanSequenceGivesEmptySequence() {
        Sequence sequence = new Sequence(new Move[] { new Move("a"), new Move("b"), new Move("c")});
        Sequence subsequence = sequence.getSubsequence(3);
        Sequence expected = new Sequence(new Move[0]);
        assertEquals(expected, subsequence);
    }
    //endregion

    //region getLength Tests
    @Test
    public void getLengthGivesEmptySequenceLength() {
        Sequence sequence = new Sequence(new Move[0]);
        assertEquals(0, sequence.getLength());
    }

    @Test
    public void getLengthGivesSequenceLength() {
        Sequence sequence = new Sequence(new Move[5]);
        assertEquals(5, sequence.getLength());
    }
    //endregion

    //region buildChildSequence Tests
    @Test
    public void buildChildSequenceNullMoveThrowsException() {
        Sequence sequence = new Sequence(new Move[0]);
        assertThrows(IllegalArgumentException.class, () -> sequence.buildChildSequence(null));
    }

    @Test
    public void buildChildSequenceAppendsToEmptySequence() {
        Sequence sequence = new Sequence(new Move[0]);
        Sequence childSequence = sequence.buildChildSequence(new Move("a"));
        Sequence expected = new Sequence(new Move[] { new Move("a")});
        assertEquals(expected, childSequence);
    }

    @Test
    public void buildChildSequencePrependsNewMove() {
        Sequence sequence = new Sequence(new Move[] { new Move("b"), new Move("a") });
        Sequence childSequence = sequence.buildChildSequence(new Move("c"));
        Sequence expected = new Sequence(new Move[] { new Move("c"), new Move("b"), new Move("a")});
        assertEquals(expected, childSequence);
    }
    //endregion

    //region hasNext Tests
    @Test
    public void hasNextFalseForEmptySequence() {
        Sequence sequence = new Sequence(new Move[0]);
        assertFalse(sequence.hasNext());
    }

    @Test
    public void hasNextTrueForNonEmptySequence() {
        Sequence sequence = new Sequence(new Move[] { new Move("a")});
        assertTrue(sequence.hasNext());
    }
    //endregion

    //region next Tests
    @Test
    public void nextNoNextItemThrowsException() {
        Sequence sequence = new Sequence(new Move[0]);
        assertThrows(RuntimeException.class, () -> sequence.next());
    }

    @Test
    public void nextReturnsNextItem() {
        Sequence sequence = new Sequence(new Move[] { new Move("a") });
        Move move = sequence.next();
        assertEquals(new Move("a"), move);
    }
    //endregion

    //region reset (iteration Integration) Tests
    @Test
    public void iterationIntegration() {
        Move a = new Move("a");
        Move b = new Move("b");
        Move c = new Move("c");
        Sequence sequence = new Sequence(new Move[] { new Move("a"), new Move("b"), new Move("c") });
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
    @Test
    public void getCurrentIndex()
    {

        Move[] moves = new Move[] { new Move("one"), new Move("two") };
        Sequence sequence = new Sequence(moves);
        assertEquals(-1, sequence.getCurrentIndex());
        sequence.next();
        assertEquals(0, sequence.getCurrentIndex());
    }
    //endregion

    //region equals Tests
    @Test
    public void equalsAreEqual() {
        Sequence sequence1 = new Sequence(new Move[] { new Move("a"), new Move("b"), new Move("c") });
        Sequence sequence2 = new Sequence(new Move[] { new Move("a"), new Move("b"), new Move("c") });
        assertEquals(sequence1, sequence2);
    }

    @Test
    public void equalsAreEqualSameObject() {
        Sequence sequence1 = new Sequence(new Move[] { new Move("a"), new Move("b"), new Move("c") });
        assertTrue(sequence1.equals(sequence1));
    }

    @Test
    public void equalsAreNotEqualDifferentMoves() {
        Sequence sequence1 = new Sequence(new Move[] { new Move("d"), new Move("b"), new Move("c") });
        Sequence sequence2 = new Sequence(new Move[] { new Move("a"), new Move("b"), new Move("c") });
        assertNotEquals(sequence1, sequence2);
    }

    @Test
    public void equalsAreNotEqualDifferentMoveCounts() {
        Sequence sequence1 = new Sequence(new Move[] { new Move("b"), new Move("c") });
        Sequence sequence2 = new Sequence(new Move[] { new Move("a"), new Move("b"), new Move("c") });
        assertNotEquals(sequence1, sequence2);
    }

    @Test
    public void equalsAreNotEqualDifferentTypes() {
        Sequence sequence1 = new Sequence(new Move[] { new Move("b"), new Move("c") });
        assertFalse(sequence1.equals(new Move("a")));
    }
    //endregion

    //region hashcode Tests
    @Test
    public void hashCodeSameForEqualSequences() {
        Sequence sequence1 = new Sequence(new Move[] { new Move("a"), new Move("b"), new Move("c") });
        Sequence sequence2 = new Sequence(new Move[] { new Move("a"), new Move("b"), new Move("c") });
        assertEquals(sequence1.hashCode(), sequence2.hashCode());
    }
    //endregion

    //region take Tests
    @Test
    public void takeLengthLessThanZeroThrowsException() {
        Sequence sequence = new Sequence(new Move[0]);
        assertThrows(IllegalArgumentException.class, () -> sequence.take(-1));
    }

    @Test
    public void takeLengthBiggerThanSequenceThrowsException() {
        Sequence sequence = new Sequence(new Move[] { new Move("a") });
        assertThrows(IllegalArgumentException.class, () -> sequence.take(2));
    }

    @Test
    public void takeNothingGivesEmptySequence() {
        Sequence sequence = new Sequence(new Move[] { new Move("a") });
        Sequence subsequence = sequence.take(0);
        assertEquals(new Sequence(new Move[0]), subsequence);
    }

    @Test
    public void takeGivesSingleItem() {
        Sequence sequence = new Sequence(new Move[] { new Move("a") });
        Sequence subsequence = sequence.take(1);
        assertEquals(sequence, subsequence);
    }

    @Test
    public void takeGivesFirstNItems() {
        Sequence sequence = new Sequence(new Move[] { new Move("a"), new Move("b"), new Move("c") });
        Sequence subsequence = sequence.take(2);
        Sequence expected = new Sequence(new Move[] { new Move("a"), new Move("b") });
        assertEquals(expected, subsequence);
    }
    //endregion

    //region comparable Tests
    @Test
    public void comparableSortsByLength() {
        Sequence length1 = new Sequence(new Move[] { new Move("a")});
        Sequence length21 = new Sequence(new Move[] { new Move("a"), new Move("b") });
        Sequence length22 = new Sequence(new Move[] { new Move("c"), new Move("d") });
        Sequence length3 = new Sequence(new Move[] { new Move("a"), new Move("b"), new Move("c") });
        Sequence length4 = new Sequence(new Move[] { new Move("a"), new Move("b"), new Move("c"), new Move("d"), });
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
}
