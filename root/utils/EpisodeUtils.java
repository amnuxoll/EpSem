package utils;

import framework.Episode;
import framework.Move;
import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Provides helper methods for operating against {@link Episode}.
 * @author Zachary Paul Faltersack
 * @version 0.95
 */
public class EpisodeUtils {
    //region Public Static Methods
    public static Move[] selectMoves(Episode[] episodes)
    {
        return Arrays.stream(episodes).map(ep -> ep.getMove()).collect(Collectors.toList()).toArray(new Move[0]);
    }
    //endregion
}
