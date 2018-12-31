package agents.marz.nodes;

import agents.marz.ISuffixNodeBaseProvider;
import framework.Sequence;
import framework.Episode;
import framework.Move;
import java.util.function.Function;

/**
 *
 * @author Zachary Paul Faltersack
 * @version 0.95
 */
public class SuffixNodeProvider implements ISuffixNodeBaseProvider<SuffixNode> {
    //region ISuffixNodeBaseProvider<SuffixNode> Members
    public SuffixNode getNode(Sequence sequence, Move[] alphabet, Function<Integer, Episode> lookupEpisode) {
        return new SuffixNode(sequence, alphabet, lookupEpisode);
    }
    //endregion
}
