package utils;

import framework.Action;
import framework.Episode;

/**
 * A {@link Semsode} is just grouping of {@link Episode}.
 *
 * Since an {@link Episode} is defined by the {@link Action} and resulting {@link framework.SensorData}, the
 * {@link Semsode} is considered to be the following:
 *
 * M = Action
 * S = SensorData
 * MS = Episode(Action, SensorData)
 *
 * S,MS,MS,MS = Semsode
 *
 * Of note, this means that the first {@link Episode} in the array of episodes is only counted
 * for its {@link framework.SensorData}.
 *
 * @author Zachary Paul Faltersack
 * @version 0.95
 */
public class Semsode {

    //region Class Variables

    private Episode[] episodes;

    //endregion

    //region Constructors

    /**
     * Creates an instance of a {@link Semsode}.
     *
     * @param episodes the array of {@link Episode} contained in this {@link Semsode}.
     */
    public Semsode(Episode[] episodes) {
        this.episodes = episodes;
    }

    //endregion

    //region Public Methods

    /**
     * Using the given {@link Discriminator}, determine if this {@link Semsode} matches the most recent experiences
     * as defined by the end of the given {@link EpisodicMemory}.
     *
     * @param episodicMemory the {@link EpisodicMemory} to search.
     * @param discriminator the {@link Discriminator} that should be tuned against sensor variance.
     * @return true if the {@link Semsode} appears to match most recent memory; otherwise false.
     */
    public boolean matches(EpisodicMemory<Episode> episodicMemory, Discriminator discriminator) {
        // Due to the definition of a Semsode we first check for just the sensor data in the first episode of the
        // semsode and then we enumerate the rest of the episodes looking for full matches on sensor and move.

        if (!discriminator.match(episodes[0].getSensorData(), episodicMemory.getFromOffset(this.episodes.length).getSensorData()))
            return false;
        for (int i = 1; i < this.episodes.length; i++)
        {
            Episode episode = episodicMemory.getFromOffset(this.episodes.length - i);
            if (!episode.getAction().equals(this.episodes[i].getAction()))
                return false;
            if (!discriminator.match(episode.getSensorData(), this.episodes[i].getSensorData()))
                return false;
        }
        return true;
    }

    //endregion

    //region Object Overrides

    @Override
    public String toString() {
        String me = "";
        for (Episode episode : this.episodes)
        {
            me += episode;
        }
        return me;
    }

    //endregion
}
