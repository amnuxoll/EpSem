package utils;

import framework.Episode;
import framework.Move;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class EpisodeUtilsTest {
    //region selectMoves Tests
    @Test
    public void selectMoves()
    {
        Move[] expectedMoves = new Move[] {
                new Move("move1"),
                new Move("move2"),
                new Move("move3")
        };
        Episode[] episodes = new Episode[] {
                new Episode(expectedMoves[0]),
                new Episode(expectedMoves[1]),
                new Episode(expectedMoves[2])
        };
        Move[] actualMoves = EpisodeUtils.selectMoves(episodes);
        assertArrayEquals(expectedMoves, actualMoves);
    }

    @Test
    public void selectMovesNullEpisodesThrowsException()
    {
        assertThrows(IllegalArgumentException.class, () -> EpisodeUtils.selectMoves(null));
    }
    //endregion
}
