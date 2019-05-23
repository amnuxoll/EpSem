package utils;

import framework.Episode;
import framework.Move;
import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Provides helper methods for operating against {@link Episode}.
 *
 * @author Zachary Paul Faltersack
 * @version 0.95
 */
public class EpisodeUtils {
    //region Public Static Methods
    /**
     * Extracts the array of {@link Move} from the array of {@link Episode}.
     *
     * @param episodes the {@link Episode} from which {@link Move} should be extracted.
     * @return the array of {@link Move} from the episodes.
     */
    public static Move[] selectMoves(Episode[] episodes) {
        if (episodes == null)
            throw new IllegalArgumentException("episodes cannot be null.");
        return Arrays.stream(episodes).map(ep -> ep.getMove()).collect(Collectors.toList()).toArray(new Move[0]);
    }
    //endregion
}
