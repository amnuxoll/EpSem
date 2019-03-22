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
    protected ArrayList<TEpisode> episodicMemory = new ArrayList<>();
    //endregion

    //region Public Methods
    public boolean any() {
        return this.episodicMemory.size() > 0;
    }

    public TEpisode current() {
        if (this.any())
            return this.episodicMemory.get(this.currentIndex());
        return null;
    }

    public int currentIndex() {
        return this.episodicMemory.size() - 1;
    }

    public int length() {
        return this.episodicMemory.size();
    }

    public void add(TEpisode episode) {
        if (episode == null)
            throw new IllegalArgumentException("episode cannot be null.");
        this.episodicMemory.add(episode);
    }

    public TEpisode get(int index) {
        // TODO -- update this so that negative is an offset index from the end and get rid of getFromOffset
        if (index < 0 || index >= this.episodicMemory.size())
            throw new IllegalArgumentException("index out of range.");
        return this.episodicMemory.get(index);
    }

    public TEpisode getFromOffset(int offset) {
        // TODO -- This used to be "this.episodicMemory.size() - offset" but was updated to allow for 0-based offset
        // in particular, ensure that NSMAgent hasn't been negatively affected and that Semsode still functions
        // MaRzLearner should be ok.
        if (offset < 0 || offset >= this.episodicMemory.size())
            throw new IllegalArgumentException("index out of range.");
        return this.get(this.currentIndex() - offset);
    }

    public void trim(int count) {
        if (count < 0)
            throw new IllegalArgumentException("count cannot be less than 0");
        for (int i = count; i > 0; i--)
        {
            episodicMemory.remove(episodicMemory.size() - 1);
        }
    }

    public Episode[] subset(int startIndex) {
        return this.subset(startIndex, this.episodicMemory.size());
    }

    public Episode[] subset(int startIndex, int endIndex) {
        if (startIndex < 0)
            throw new IllegalArgumentException("startIndex cannot be less than 0.");
        if (endIndex > this.episodicMemory.size())
            throw new IllegalArgumentException("endIndex cannot be greater than the size of memory.");
        if (endIndex < startIndex)
            throw new IllegalArgumentException("endIndex cannot be less than startIndex");
        return this.episodicMemory.subList(startIndex, endIndex).toArray(new Episode[0]);
    }

    public int lastGoalIndex(int startIndex) {
        if (startIndex < 0)
            throw new IllegalArgumentException("startIndex cannot be less than 0");
        // It's tempting to think we can cache goal indices on Add but beware!
        // When episodes get added they do not yet have sensorData. This is added after the action has been taken
        // and there's something to introduce.
        for(int i = startIndex; i >= 0; i--) {
            if(this.episodicMemory.get(i).hitGoal())
                return i;
        }
        return -1;
    }

    /**
     * Returns that last n number of episodes in the memory.
     * @param count The number of episodes to return.
     * @return The last count episodes.
     */
    public Episode[] last(int count) {
        if (count <= 0)
            return new Episode[0];
        int epMemSize = this.episodicMemory.size();
        if (count > epMemSize)
            count = epMemSize;
        return this.subset(epMemSize - count);
    }
    //endregion

    @Override
    public String toString(){
        StringBuilder value = new StringBuilder();
        for (TEpisode episode : episodicMemory){
            value.append(episode.toString());
            value.append(",");
        }
        return value.toString();
    }

    /**
     * Builds a string of the last count episodes.
     * @param count The number of episodes to include.
     * @return A string of the last count episodes.
     */
    public String toString(int count) {
        StringBuilder value = new StringBuilder();
        for (Episode episode : this.last(count)){
            value.append(episode.toString());
            value.append(",");
        }
        return value.toString();
    }
}
