package agents.marz;

import framework.Episode;
import framework.Move;
import framework.Sequence;
import java.util.function.Function;

/**
 *
 * @author Zachary Paul Faltersack
 * @version 0.95
 */
public interface ISuffixNodeBaseProvider<TSuffixNode extends SuffixNodeBase<TSuffixNode>> {
    //region Methods
    TSuffixNode getNode(Sequence sequence, Move[] alphabet, Function<Integer, Episode> lookupEpisode);
    //endregion
}
