package utils;

import framework.Episode;

/**
 *
 * @author Zachary Paul Faltersack
 * @version 0.95
 */
public class Semsode {
    //region Class Variables
    private Episode[] episodes;
    //endregion

    //region Constructors
    public Semsode(Episode[] episodes)
    {
        this.episodes = episodes;
    }
    //endregion

    //region Public Methods
    public boolean matches(EpisodicMemory<Episode> episodicMemory, Discriminator discriminator) {
        if (!discriminator.match(episodes[0].getSensorData(), episodicMemory.getFromOffset(this.episodes.length).getSensorData()))
            return false;
        for (int i = 1; i < this.episodes.length; i++)
        {
            Episode episode = episodicMemory.getFromOffset(this.episodes.length - i);
            if (!episode.getMove().equals(this.episodes[i].getMove()))
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
