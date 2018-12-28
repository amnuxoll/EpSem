package utils;

import framework.Episode;

import java.util.ArrayList;

/**
 *
 * @author Zachary Paul Faltersack
 * @version 0.95
 */
public class EpisodicMemory<TEpisode extends Episode> {
    //region Class Variables
    private ArrayList<TEpisode> episodicMemory = new ArrayList<>();
    //endregion

    //region Public Methods
    public boolean any()
    {
        return this.episodicMemory.size() > 0;
    }

    public TEpisode current()
    {
        return this.episodicMemory.get(this.currentIndex());
    }

    public int currentIndex()
    {
        return this.episodicMemory.size() - 1;
    }

    public int length()
    {
        return this.episodicMemory.size();
    }

    public void add(TEpisode episode) {
        this.episodicMemory.add(episode);
    }

    public TEpisode get(int index)
    {
        return this.episodicMemory.get(index);
    }

    public TEpisode getFromOffset(int offset)
    {
        return this.get(this.episodicMemory.size() - offset);
    }

    public void trim(int count) {
        for (int i = count; i > 0; i--)
        {
            episodicMemory.remove(episodicMemory.size() - 1);
        }
    }

    public Episode[] subset(int startIndex)
    {
        return this.subset(startIndex, this.episodicMemory.size());
    }

    public Episode[] subset(int startIndex, int endIndex) {
        return this.episodicMemory.subList(startIndex, endIndex).toArray(new Episode[0]);
    }

    public int lastGoalIndex(int startIndex) {
        // It's tempting to think we can cache goal indices on Add but beware!
        // When episodes get added they do not yet have sensorData. This is added after the action has been taken
        // and there's something to introduce.
        for(int i = startIndex; i >= 0; i--) {
            if(this.episodicMemory.get(i).hitGoal())
                return i;
        }
        return -1;
    }
    //endregion
}
