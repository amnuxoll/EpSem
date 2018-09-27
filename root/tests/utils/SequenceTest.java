package utils;

import framework.Episode;
import framework.Move;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.jupiter.api.Assertions.*;

public class SequenceTest {

    // constructor Tests
    @Test
    public void constructorNullMovesThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> new Sequence(null));
    }

    @Test
    public void constructorEpisodesThrowsException(){
        assertThrows(IllegalArgumentException.class, () -> new Sequence(null,4,5));

        ArrayList<Episode> episodes= new ArrayList<>();
        Move move= new Move("a");
        //add 12 episodes
        for(int i=0;i<12;i++)
            episodes.add(new Episode(move));

        assertThrows(IllegalArgumentException.class, () -> new Sequence(episodes,-1,5));
        assertThrows(IllegalArgumentException.class, () -> new Sequence(episodes,0,13));
        assertThrows(IllegalArgumentException.class, () -> new Sequence(episodes,5,4));
    }

    @Test
    public void constructorEpisodesGivesCorretSequence(){
        ArrayList<Episode> episodes= new ArrayList<>();
        Move[] moves= {
                new Move("a"),
                new Move("b"),
                new Move("b"),
                new Move("c")
        };

        Move[] correctMoves= new Move[12];

        //add 12 episodes
        for(int i=0;i<12;i++) {
            episodes.add(new Episode(moves[i % moves.length]));
            correctMoves[i]= moves[i % moves.length];
        }

        assertArrayEquals(Arrays.copyOfRange(correctMoves,0,12), new Sequence(episodes,0,12).getMoves());
        assertArrayEquals(Arrays.copyOfRange(correctMoves,5,7), new Sequence(episodes,5,7).getMoves());
    }

    // endsWith Tests
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

    // getSubsequence Tests
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

    // getLength Tests
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

    // buildChildSequence Tests
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

    // hasNext Tests
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

    // next Tests
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

    // iteration Integration Tests
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

    // equals Tests
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

    // hashcode Tests
    @Test
    public void hashCodeSameForEqualSequences() {
        Sequence sequence1 = new Sequence(new Move[] { new Move("a"), new Move("b"), new Move("c") });
        Sequence sequence2 = new Sequence(new Move[] { new Move("a"), new Move("b"), new Move("c") });
        assertEquals(sequence1.hashCode(), sequence2.hashCode());
    }

    // take Tests
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
}
