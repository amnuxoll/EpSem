package agents.marz;

import framework.Episode;
import framework.Move;
import framework.Sequence;

import java.util.function.Function;

public interface ISuffixNodeBaseProvider<TSuffixNode extends SuffixNodeBase<TSuffixNode>> {
    TSuffixNode getNode(Sequence sequence, Move[] alphabet, Function<Integer, Episode> lookupEpisode);
}
