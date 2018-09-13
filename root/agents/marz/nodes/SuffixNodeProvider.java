package agents.marz.nodes;

import agents.marz.ISuffixNodeBaseProvider;
import utils.Sequence;
import framework.Episode;
import framework.Move;

import java.util.function.Function;

public class SuffixNodeProvider implements ISuffixNodeBaseProvider<SuffixNode> {

    public SuffixNode getNode(Sequence sequence, Move[] alphabet, Function<Integer, Episode> lookupEpisode) {
        return new SuffixNode(sequence, alphabet, lookupEpisode);
    }
}
