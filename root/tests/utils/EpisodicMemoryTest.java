package utils;

import framework.Episode;
import framework.Move;
import framework.SensorData;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 *
 * @author Zachary Paul Faltersack
 * @version 0.95
 */
public class EpisodicMemoryTest {
    //region any Tests
    @Test
    public void anyEmpty() {
        EpisodicMemory<Episode> episodicMemory = new EpisodicMemory<>();
        assertFalse(episodicMemory.any());
    }

    @Test
    public void anyNotEmpty() {
        EpisodicMemory<Episode> episodicMemory = new EpisodicMemory<>();
        episodicMemory.add(new Episode(new Move("move")));
        assertTrue(episodicMemory.any());
    }
    //endregion

    //region current Tests
    @Test
    public void currentEmptyReturnsNull() {
        EpisodicMemory<Episode> episodicMemory = new EpisodicMemory<>();
        assertNull(episodicMemory.current());
    }

    @Test
    public void currentReturnsLastItem() {
        EpisodicMemory<Episode> episodicMemory = new EpisodicMemory<>();
        episodicMemory.add(new Episode(new Move("move1")));
        episodicMemory.add(new Episode(new Move("move2")));
        episodicMemory.add(new Episode(new Move("move3")));
        Episode currrent = episodicMemory.current();
        assertEquals(new Move("move3"), currrent.getMove());
    }
    //endregion

    //region currentIndex Tests
    @Test
    public void currentIndexNegativeOneWhenEmpty() {
        EpisodicMemory<Episode> episodicMemory = new EpisodicMemory<>();
        assertEquals(-1, episodicMemory.currentIndex());
    }

    @Test
    public void currentIndex() {
        EpisodicMemory<Episode> episodicMemory = new EpisodicMemory<>();
        episodicMemory.add(new Episode(new Move("move1")));
        episodicMemory.add(new Episode(new Move("move2")));
        episodicMemory.add(new Episode(new Move("move3")));
        assertEquals(2, episodicMemory.currentIndex());
    }
    //endregion

    //region length Tests
    @Test
    public void lengthWhenEmpty() {
        EpisodicMemory<Episode> episodicMemory = new EpisodicMemory<>();
        assertEquals(0, episodicMemory.length());
    }

    @Test
    public void length() {
        EpisodicMemory<Episode> episodicMemory = new EpisodicMemory<>();
        episodicMemory.add(new Episode(new Move("move1")));
        episodicMemory.add(new Episode(new Move("move2")));
        episodicMemory.add(new Episode(new Move("move3")));
        assertEquals(3, episodicMemory.length());
    }
    //endregion

    //region add Tests
    @Test
    public void add() {
        Episode expected = new Episode(new Move("move"));
        EpisodicMemory<Episode> episodicMemory = new EpisodicMemory<>();
        episodicMemory.add(expected);
        Episode actual = episodicMemory.current();
        assertSame(expected, actual);
    }

    @Test
    public void addNullEpisodeThrowsException() {
        EpisodicMemory<Episode> episodicMemory = new EpisodicMemory<>();
        assertThrows(IllegalArgumentException.class, () -> episodicMemory.add(null));
    }
    //endregion

    //region get Tests
    @Test
    public void getFirst() {
        EpisodicMemory<Episode> episodicMemory = new EpisodicMemory<>();
        episodicMemory.add(new Episode(new Move("move1")));
        assertEquals(new Episode(new Move("move1")), episodicMemory.get(0));
    }

    @Test
    public void getLast() {
        EpisodicMemory<Episode> episodicMemory = new EpisodicMemory<>();
        episodicMemory.add(new Episode(new Move("move1")));
        episodicMemory.add(new Episode(new Move("move2")));
        episodicMemory.add(new Episode(new Move("move3")));
        assertEquals(new Episode(new Move("move3")), episodicMemory.get(2));
    }

    @Test
    public void getNegativeIndexThrowsException() {
        EpisodicMemory<Episode> episodicMemory = new EpisodicMemory<>();
        assertThrows(IllegalArgumentException.class, () -> episodicMemory.get(-1));
    }

    @Test
    public void getEmptyMemoryThrowsException() {
        EpisodicMemory<Episode> episodicMemory = new EpisodicMemory<>();
        assertThrows(IllegalArgumentException.class, () -> episodicMemory.get(0));
    }

    @Test
    public void getIndexTooLargeThrowsException() {
        EpisodicMemory<Episode> episodicMemory = new EpisodicMemory<>();
        episodicMemory.add(new Episode(new Move("move1")));
        assertThrows(IllegalArgumentException.class, () -> episodicMemory.get(1));
    }
    //endregion

    //region getFromOffset Tests
    @Test
    public void getFromOffsetFirst() {
        EpisodicMemory<Episode> episodicMemory = new EpisodicMemory<>();
        episodicMemory.add(new Episode(new Move("move1")));
        assertEquals(new Episode(new Move("move1")), episodicMemory.getFromOffset(0));
    }

    @Test
    public void getFromOffsetLast() {
        EpisodicMemory<Episode> episodicMemory = new EpisodicMemory<>();
        episodicMemory.add(new Episode(new Move("move1")));
        episodicMemory.add(new Episode(new Move("move2")));
        episodicMemory.add(new Episode(new Move("move3")));
        assertEquals(new Episode(new Move("move1")), episodicMemory.getFromOffset(2));
    }

    @Test
    public void getFromOffsetNegativeIndexThrowsException() {
        EpisodicMemory<Episode> episodicMemory = new EpisodicMemory<>();
        assertThrows(IllegalArgumentException.class, () -> episodicMemory.getFromOffset(-1));
    }

    @Test
    public void getFromOffsetEmptyMemoryThrowsException() {
        EpisodicMemory<Episode> episodicMemory = new EpisodicMemory<>();
        assertThrows(IllegalArgumentException.class, () -> episodicMemory.getFromOffset(0));
    }

    @Test
    public void getFromOffsetIndexTooLargeThrowsException() {
        EpisodicMemory<Episode> episodicMemory = new EpisodicMemory<>();
        episodicMemory.add(new Episode(new Move("move1")));
        assertThrows(IllegalArgumentException.class, () -> episodicMemory.getFromOffset(1));
    }
    //endregion

    //region trim Tests
    @Test
    public void trim() {
        EpisodicMemory<Episode> episodicMemory = new EpisodicMemory<>();
        episodicMemory.add(new Episode(new Move("move1")));
        episodicMemory.add(new Episode(new Move("move2")));
        episodicMemory.add(new Episode(new Move("move3")));
        episodicMemory.trim(2);
        assertEquals(1, episodicMemory.length());
    }

    @Test
    public void trimNegativeCountThrowsException() {
        EpisodicMemory<Episode> episodicMemory = new EpisodicMemory<>();
        assertThrows(IllegalArgumentException.class, () -> episodicMemory.trim(-1));
    }
    //endregion

    //region subset Tests
    @Test
    public void subsetTakesRest() {
        EpisodicMemory<Episode> episodicMemory = new EpisodicMemory<>();
        episodicMemory.add(new Episode(new Move("move1")));
        episodicMemory.add(new Episode(new Move("move2")));
        episodicMemory.add(new Episode(new Move("move3")));
        Episode[] subset = episodicMemory.subset(1);
        Episode[] expected = new Episode[] {
                new Episode(new Move("move2")),
                new Episode(new Move("move3")),
        };
        assertArrayEquals(expected, subset);
    }

    @Test
    public void subset() {
        EpisodicMemory<Episode> episodicMemory = new EpisodicMemory<>();
        episodicMemory.add(new Episode(new Move("move1")));
        episodicMemory.add(new Episode(new Move("move2")));
        episodicMemory.add(new Episode(new Move("move3")));
        Episode[] subset = episodicMemory.subset(0, 2);
        Episode[] expected = new Episode[] {
                new Episode(new Move("move1")),
                new Episode(new Move("move2")),
        };
        assertArrayEquals(expected, subset);
    }

    @Test
    public void subsetStartLessThanZeroThrowsException() {
        EpisodicMemory<Episode> episodicMemory = new EpisodicMemory<>();
        assertThrows(IllegalArgumentException.class, () -> episodicMemory.subset(-1));
    }

    @Test
    public void subsetEndGreaterThanLengthThrowsException() {
        EpisodicMemory<Episode> episodicMemory = new EpisodicMemory<>();
        episodicMemory.add(new Episode(new Move("move1")));
        assertThrows(IllegalArgumentException.class, () -> episodicMemory.subset(0, 2));
    }

    @Test
    public void subsetEndLessThanStartThrowsException() {
        EpisodicMemory<Episode> episodicMemory = new EpisodicMemory<>();
        assertThrows(IllegalArgumentException.class, () -> episodicMemory.subset(1, 0));
    }
    //endregion

    //region lastGoalIndex Tests
    @Test
    public void lastGoalIndex() {
        // 2 and 5 will be goal
        EpisodicMemory<Episode> episodicMemory = new EpisodicMemory<>();
        for (int i = 0; i < 6; i++) {
            Episode episode = new Episode(new Move(Integer.toString(i)));
            episode.setSensorData(new SensorData(i % 3 == 2));
            episodicMemory.add(episode);
        }
        assertEquals(5, episodicMemory.lastGoalIndex(5));
        assertEquals(2, episodicMemory.lastGoalIndex(4));
    }

    @Test
    public void lastGoalIndexNegativeStartThrowsException() {
        EpisodicMemory<Episode> episodicMemory = new EpisodicMemory<>();
        assertThrows(IllegalArgumentException.class, () -> episodicMemory.lastGoalIndex(-1));
    }
    //endregion
}
