package tests.utils;

import framework.Episode;
import framework.Move;
import utils.EpisodeUtils;

import tests.EpSemTest;
import tests.EpSemTestClass;
import static tests.Assertions.*;

/**
 *
 * @author Zachary Paul Faltersack
 * @version 0.95
 */
@EpSemTestClass
public class EpisodeUtilsTest {
    //region selectMoves Tests
    @EpSemTest
    public void selectMoves()
    {
        Move[] expectedMoves = new Move[] {
                new Move("move1"),
                new Move("move2"),
                new Move("move3")
        };
        Episode[] episodes = new Episode[] {
                new Episode(null, expectedMoves[0]),
                new Episode(null, expectedMoves[1]),
                new Episode(null, expectedMoves[2])
        };
        Move[] actualMoves = EpisodeUtils.selectMoves(episodes);
        assertArrayEquals(expectedMoves, actualMoves);
    }

    @EpSemTest
    public void selectMovesNullEpisodesThrowsException()
    {
        assertThrows(IllegalArgumentException.class, () -> EpisodeUtils.selectMoves(null));
    }
    //endregion
}
