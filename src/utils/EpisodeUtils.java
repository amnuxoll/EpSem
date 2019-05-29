package utils;

import framework.Action;
import framework.Episode;

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
     * Extracts the array of {@link Action} from the array of {@link Episode}.
     *
     * @param episodes the {@link Episode} from which {@link Action} should be extracted.
     * @return the array of {@link Action} from the episodes.
     */
    public static Action[] selectMoves(Episode[] episodes) {
        if (episodes == null)
            throw new IllegalArgumentException("episodes cannot be null.");
        return Arrays.stream(episodes).map(ep -> ep.getAction()).collect(Collectors.toList()).toArray(new Action[0]);
    }
    //endregion
}
