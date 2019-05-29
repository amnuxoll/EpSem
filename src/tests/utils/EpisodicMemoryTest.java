package tests.utils;

import framework.Action;
import framework.Episode;
import framework.SensorData;
import utils.EpisodicMemory;

import tests.EpSemTest;
import tests.EpSemTestClass;
import static tests.Assertions.*;

/**
 *
 * @author Zachary Paul Faltersack
 * @version 0.95
 */
@EpSemTestClass
public class EpisodicMemoryTest {
    //region any Tests
    @EpSemTest
    public void anyEmpty() {
        EpisodicMemory<Episode> episodicMemory = new EpisodicMemory<>();
        assertFalse(episodicMemory.any());
    }

    @EpSemTest
    public void anyNotEmpty() {
        EpisodicMemory<Episode> episodicMemory = new EpisodicMemory<>();
        episodicMemory.add(new Episode(new SensorData(false), new Action("move")));
        assertTrue(episodicMemory.any());
    }
    //endregion

    //region current Tests
    @EpSemTest
    public void currentEmptyReturnsNull() {
        EpisodicMemory<Episode> episodicMemory = new EpisodicMemory<>();
        assertNull(episodicMemory.current());
    }

    @EpSemTest
    public void currentReturnsLastItem() {
        EpisodicMemory<Episode> episodicMemory = new EpisodicMemory<>();
        episodicMemory.add(new Episode(new SensorData(false), new Action("move1")));
        episodicMemory.add(new Episode(new SensorData(false), new Action("move2")));
        episodicMemory.add(new Episode(new SensorData(false), new Action("move3")));
        Episode currrent = episodicMemory.current();
        assertEquals(new Action("move3"), currrent.getAction());
    }
    //endregion

    //region currentIndex Tests
    @EpSemTest
    public void currentIndexNegativeOneWhenEmpty() {
        EpisodicMemory<Episode> episodicMemory = new EpisodicMemory<>();
        assertEquals(-1, episodicMemory.currentIndex());
    }

    @EpSemTest
    public void currentIndex() {
        EpisodicMemory<Episode> episodicMemory = new EpisodicMemory<>();
        episodicMemory.add(new Episode(new SensorData(false), new Action("move1")));
        episodicMemory.add(new Episode(new SensorData(false), new Action("move2")));
        episodicMemory.add(new Episode(new SensorData(false), new Action("move3")));
        assertEquals(2, episodicMemory.currentIndex());
    }
    //endregion

    //region length Tests
    @EpSemTest
    public void lengthWhenEmpty() {
        EpisodicMemory<Episode> episodicMemory = new EpisodicMemory<>();
        assertEquals(0, episodicMemory.length());
    }

    @EpSemTest
    public void length() {
        EpisodicMemory<Episode> episodicMemory = new EpisodicMemory<>();
        episodicMemory.add(new Episode(new SensorData(false), new Action("move1")));
        episodicMemory.add(new Episode(new SensorData(false), new Action("move2")));
        episodicMemory.add(new Episode(new SensorData(false), new Action("move3")));
        assertEquals(3, episodicMemory.length());
    }
    //endregion

    //region add Tests
    @EpSemTest
    public void add() {
        Episode expected = new Episode(new SensorData(false), new Action("move"));
        EpisodicMemory<Episode> episodicMemory = new EpisodicMemory<>();
        episodicMemory.add(expected);
        Episode actual = episodicMemory.current();
        assertSame(expected, actual);
    }

    @EpSemTest
    public void addNullEpisodeThrowsException() {
        EpisodicMemory<Episode> episodicMemory = new EpisodicMemory<>();
        assertThrows(IllegalArgumentException.class, () -> episodicMemory.add(null));
    }
    //endregion

    //region get Tests
    @EpSemTest
    public void getFirst() {
        EpisodicMemory<Episode> episodicMemory = new EpisodicMemory<>();
        episodicMemory.add(new Episode(new SensorData(false), new Action("move1")));
        assertEquals(new Episode(new SensorData(false), new Action("move1")), episodicMemory.get(0));
    }

    @EpSemTest
    public void getLast() {
        EpisodicMemory<Episode> episodicMemory = new EpisodicMemory<>();
        episodicMemory.add(new Episode(new SensorData(false), new Action("move1")));
        episodicMemory.add(new Episode(new SensorData(false), new Action("move2")));
        episodicMemory.add(new Episode(new SensorData(false), new Action("move3")));
        assertEquals(new Episode(new SensorData(false), new Action("move3")), episodicMemory.get(2));
    }

    @EpSemTest
    public void getNegativeIndexThrowsException() {
        EpisodicMemory<Episode> episodicMemory = new EpisodicMemory<>();
        assertThrows(IllegalArgumentException.class, () -> episodicMemory.get(-1));
    }

    @EpSemTest
    public void getEmptyMemoryThrowsException() {
        EpisodicMemory<Episode> episodicMemory = new EpisodicMemory<>();
        assertThrows(IllegalArgumentException.class, () -> episodicMemory.get(0));
    }

    @EpSemTest
    public void getIndexTooLargeThrowsException() {
        EpisodicMemory<Episode> episodicMemory = new EpisodicMemory<>();
        episodicMemory.add(new Episode(new SensorData(false), new Action("move1")));
        assertThrows(IllegalArgumentException.class, () -> episodicMemory.get(1));
    }
    //endregion

    //region getFromOffset Tests
    @EpSemTest
    public void getFromOffsetFirst() {
        EpisodicMemory<Episode> episodicMemory = new EpisodicMemory<>();
        episodicMemory.add(new Episode(new SensorData(false), new Action("move1")));
        assertEquals(new Episode(new SensorData(false), new Action("move1")), episodicMemory.getFromOffset(0));
    }

    @EpSemTest
    public void getFromOffsetLast() {
        EpisodicMemory<Episode> episodicMemory = new EpisodicMemory<>();
        episodicMemory.add(new Episode(new SensorData(false), new Action("move1")));
        episodicMemory.add(new Episode(new SensorData(false), new Action("move2")));
        episodicMemory.add(new Episode(new SensorData(false), new Action("move3")));
        assertEquals(new Episode(new SensorData(false), new Action("move1")), episodicMemory.getFromOffset(2));
    }

    @EpSemTest
    public void getFromOffsetNegativeIndexThrowsException() {
        EpisodicMemory<Episode> episodicMemory = new EpisodicMemory<>();
        assertThrows(IllegalArgumentException.class, () -> episodicMemory.getFromOffset(-1));
    }

    @EpSemTest
    public void getFromOffsetEmptyMemoryThrowsException() {
        EpisodicMemory<Episode> episodicMemory = new EpisodicMemory<>();
        assertThrows(IllegalArgumentException.class, () -> episodicMemory.getFromOffset(0));
    }

    @EpSemTest
    public void getFromOffsetIndexTooLargeThrowsException() {
        EpisodicMemory<Episode> episodicMemory = new EpisodicMemory<>();
        episodicMemory.add(new Episode(new SensorData(false), new Action("move1")));
        assertThrows(IllegalArgumentException.class, () -> episodicMemory.getFromOffset(1));
    }
    //endregion

    //region trim Tests
    @EpSemTest
    public void trim() {
        EpisodicMemory<Episode> episodicMemory = new EpisodicMemory<>();
        episodicMemory.add(new Episode(new SensorData(false), new Action("move1")));
        episodicMemory.add(new Episode(new SensorData(false), new Action("move2")));
        episodicMemory.add(new Episode(new SensorData(false), new Action("move3")));
        episodicMemory.trim(2);
        assertEquals(1, episodicMemory.length());
    }

    @EpSemTest
    public void trimNegativeCountThrowsException() {
        EpisodicMemory<Episode> episodicMemory = new EpisodicMemory<>();
        assertThrows(IllegalArgumentException.class, () -> episodicMemory.trim(-1));
    }
    //endregion

    //region subset Tests
    @EpSemTest
    public void subsetTakesRest() {
        EpisodicMemory<Episode> episodicMemory = new EpisodicMemory<>();
        episodicMemory.add(new Episode(new SensorData(false), new Action("move1")));
        episodicMemory.add(new Episode(new SensorData(false), new Action("move2")));
        episodicMemory.add(new Episode(new SensorData(false), new Action("move3")));
        Episode[] subset = episodicMemory.subset(1);
        Episode[] expected = new Episode[] {
                new Episode(new SensorData(false), new Action("move2")),
                new Episode(new SensorData(false), new Action("move3")),
        };
        assertArrayEquals(expected, subset);
    }

    @EpSemTest
    public void subset() {
        EpisodicMemory<Episode> episodicMemory = new EpisodicMemory<>();
        episodicMemory.add(new Episode(new SensorData(false), new Action("move1")));
        episodicMemory.add(new Episode(new SensorData(false), new Action("move2")));
        episodicMemory.add(new Episode(new SensorData(false), new Action("move3")));
        Episode[] subset = episodicMemory.subset(0, 2);
        Episode[] expected = new Episode[] {
                new Episode(new SensorData(false), new Action("move1")),
                new Episode(new SensorData(false), new Action("move2")),
        };
        assertArrayEquals(expected, subset);
    }

    @EpSemTest
    public void subsetStartLessThanZeroThrowsException() {
        EpisodicMemory<Episode> episodicMemory = new EpisodicMemory<>();
        assertThrows(IllegalArgumentException.class, () -> episodicMemory.subset(-1));
    }

    @EpSemTest
    public void subsetEndGreaterThanLengthThrowsException() {
        EpisodicMemory<Episode> episodicMemory = new EpisodicMemory<>();
        episodicMemory.add(new Episode(new SensorData(false), new Action("move1")));
        assertThrows(IllegalArgumentException.class, () -> episodicMemory.subset(0, 2));
    }

    @EpSemTest
    public void subsetEndLessThanStartThrowsException() {
        EpisodicMemory<Episode> episodicMemory = new EpisodicMemory<>();
        assertThrows(IllegalArgumentException.class, () -> episodicMemory.subset(1, 0));
    }
    //endregion

    //region lastGoalIndex Tests
    @EpSemTest
    public void lastGoalIndex() {
        // 2 and 5 will be goal
        EpisodicMemory<Episode> episodicMemory = new EpisodicMemory<>();
        for (int i = 0; i < 6; i++) {
            episodicMemory.add(new Episode(new SensorData(i % 3 == 2), new Action(Integer.toString(i))));
        }
        assertEquals(5, episodicMemory.lastGoalIndex(5));
        assertEquals(2, episodicMemory.lastGoalIndex(4));
    }

    @EpSemTest
    public void lastGoalIndexNegativeStartThrowsException() {
        EpisodicMemory<Episode> episodicMemory = new EpisodicMemory<>();
        assertThrows(IllegalArgumentException.class, () -> episodicMemory.lastGoalIndex(-1));
    }
    //endregion

    //region last Tests
    @EpSemTest
    public void countLessThanZeroReturnsEmptyArray() {
        EpisodicMemory<Episode> episodicMemory = new EpisodicMemory<>();
        episodicMemory.add(new Episode(new SensorData(false), new Action("move1")));
        episodicMemory.add(new Episode(new SensorData(false), new Action("move2")));
        episodicMemory.add(new Episode(new SensorData(false), new Action("move3")));
        Episode[] subset = episodicMemory.last(-1);
        Episode[] expected = new Episode[0];
        assertArrayEquals(expected, subset);
    }

    @EpSemTest
    public void countZeroReturnsEmptyArray() {
        EpisodicMemory<Episode> episodicMemory = new EpisodicMemory<>();
        episodicMemory.add(new Episode(new SensorData(false), new Action("move1")));
        episodicMemory.add(new Episode(new SensorData(false), new Action("move2")));
        episodicMemory.add(new Episode(new SensorData(false), new Action("move3")));
        Episode[] subset = episodicMemory.last(0);
        Episode[] expected = new Episode[0];
        assertArrayEquals(expected, subset);
    }

    @EpSemTest
    public void countOneReturnsLastEpisode() {
        EpisodicMemory<Episode> episodicMemory = new EpisodicMemory<>();
        episodicMemory.add(new Episode(new SensorData(false), new Action("move1")));
        episodicMemory.add(new Episode(new SensorData(false), new Action("move2")));
        episodicMemory.add(new Episode(new SensorData(false), new Action("move3")));
        Episode[] subset = episodicMemory.last(1);
        Episode[] expected = new Episode[] {
                new Episode(new SensorData(false), new Action("move3"))
        };
        assertArrayEquals(expected, subset);
    }

    @EpSemTest
    public void countMemoryLengthReturnsAllEpisodes() {
        EpisodicMemory<Episode> episodicMemory = new EpisodicMemory<>();
        episodicMemory.add(new Episode(new SensorData(false), new Action("move1")));
        episodicMemory.add(new Episode(new SensorData(false), new Action("move2")));
        episodicMemory.add(new Episode(new SensorData(false), new Action("move3")));
        Episode[] subset = episodicMemory.last(3);
        Episode[] expected = new Episode[] {
                new Episode(new SensorData(false), new Action("move1")),
                new Episode(new SensorData(false), new Action("move2")),
                new Episode(new SensorData(false), new Action("move3"))
        };
        assertArrayEquals(expected, subset);
    }

    @EpSemTest
    public void countLargerThanMemoryLengthReturnsAllEpisodes() {
        EpisodicMemory<Episode> episodicMemory = new EpisodicMemory<>();
        episodicMemory.add(new Episode(new SensorData(false), new Action("move1")));
        episodicMemory.add(new Episode(new SensorData(false), new Action("move2")));
        episodicMemory.add(new Episode(new SensorData(false), new Action("move3")));
        Episode[] subset = episodicMemory.last(10);
        Episode[] expected = new Episode[] {
                new Episode(new SensorData(false), new Action("move1")),
                new Episode(new SensorData(false), new Action("move2")),
                new Episode(new SensorData(false), new Action("move3"))
        };
        assertArrayEquals(expected, subset);
    }
    //endregion
}
