package tests.utils;

import framework.Action;
import framework.Episode;
import framework.SensorData;
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
        Action[] expectedActions = new Action[] {
                new Action("move1"),
                new Action("move2"),
                new Action("move3")
        };
        Episode[] episodes = new Episode[] {
                new Episode(new SensorData(false), expectedActions[0]),
                new Episode(new SensorData(false), expectedActions[1]),
                new Episode(new SensorData(false), expectedActions[2])
        };
        Action[] actualActions = EpisodeUtils.selectMoves(episodes);
        assertArrayEquals(expectedActions, actualActions);
    }

    @EpSemTest
    public void selectMovesNullEpisodesThrowsException()
    {
        assertThrows(IllegalArgumentException.class, () -> EpisodeUtils.selectMoves(null));
    }
    //endregion
}
