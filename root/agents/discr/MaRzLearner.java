package agents.discr;

import agents.marz.ISuffixNodeBaseProvider;
import agents.marz.MaRzAgent;
import agents.marz.SuffixNodeBase;
import framework.Episode;
import framework.Move;
//import javafx.util.Pair;
import utils.Discriminator;
import framework.Sequence;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

public class MaRzLearner<TSuffixNode extends SuffixNodeBase<TSuffixNode>> extends MaRzAgent<TSuffixNode> {

    private Discriminator discriminator = new Discriminator();

    /**
     * MaRzAgent
     *
     * @param nodeProvider
     */
    public MaRzLearner(ISuffixNodeBaseProvider<TSuffixNode> nodeProvider) {
        super(nodeProvider);
    }

    private ArrayList<Integer> goalIndices = new ArrayList<>();

    @Override
    protected void markSuccess() {
        // We'll get inconsistent data if we allow this case through due to the fact that a goal sensor triggers a wormhole event.
        // This is a definite weakness of the algorithm right now
        if (!this.episodicMemory.get(this.episodicMemory.size() - 2).getSensorData().isGoal()) {
            this.discriminator.add(this.episodicMemory.get(this.episodicMemory.size() - 2).getSensorData(), this.episodicMemory.get(this.episodicMemory.size() - 1).getMove());
            System.out.println(this.discriminator);
        }
        this.goalIndices.add(this.episodicMemory.size() - 1);
        if (!this.goalMoves.contains(this.episodicMemory.get(this.episodicMemory.size() - 1).getMove()))
            this.generateGoalSemsodes();
        if (this.goalSequence != null)
            System.out.println("My sequence worked.");
        super.markSuccess();
    }

    @Override
    protected void markFailure() {
        super.markFailure();
        if (this.goalSequence != null)
        {
            this.learnerWindow++;
            this.generateGoalSemsodes();
            System.out.println("My sequence did not work.");
        }
        this.goalSequence = null;
    }

    private int learnerWindow = 2; // start at 2 because we want one extra sensorData

    @Override
    protected Sequence selectNextSequence() {
        if (this.goalSequence == null)
            return super.selectNextSequence();
        super.setActiveNode(super.suffixTree.findBestMatch(this.goalSequence));
        return this.goalSequence;
    }

    private Sequence goalSequence = null;

    @Override
    protected boolean shouldBail() {
        // allow our own sequence to complete
        if (this.goalSequence != null)
            return false;
        for (Map.Entry<Semsode, Move> semsodes : this.goalSemsodes.entrySet())
        {
            if (semsodes.getKey().matches(this.episodicMemory, this.discriminator))
            {
                this.goalSequence = new Sequence(new Move[] { semsodes.getValue() });
                System.out.println("I know where I am: " + semsodes.getKey());
                System.out.println("Selecting sequence: " + this.goalSequence);
                return true;
            }
        }
        return false;
    }

    private HashMap<Semsode, Move> goalSemsodes = new HashMap<>();
    private HashSet<Move> goalMoves = new HashSet<>();

    private void generateGoalSemsodes()
    {
        this.goalSemsodes.clear();
        this.goalMoves.clear();
        for (int goalIndex : this.goalIndices)
        {
            Move goalMove = this.episodicMemory.get(goalIndex).getMove();
            if (!goalMoves.contains(goalMove))
            {
                ArrayList<Episode> episodes = new ArrayList<>();
                for (int i = goalIndex - learnerWindow; i < goalIndex; i++)
                {
                    episodes.add(this.episodicMemory.get(i));
                }
                this.goalSemsodes.put(new Semsode(episodes.toArray(new Episode[0])), goalMove);
                goalMoves.add(goalMove);
            }
        }
    }

    private class Semsode
    {
        private Episode[] episodes;

        public Semsode(Episode[] episodes)
        {
            this.episodes = episodes;
        }

        public boolean matches(ArrayList<Episode> episodicMemory, Discriminator discriminator)
        {
            if (!discriminator.match(episodes[0].getSensorData(), episodicMemory.get(episodicMemory.size() - this.episodes.length).getSensorData()))
                return false;
            for (int i = 1; i < this.episodes.length; i++)
            {
                Episode episode = episodicMemory.get(episodicMemory.size() - (this.episodes.length - i));
                if (!episode.getMove().equals(this.episodes[i].getMove()))
                    return false;
                if (!discriminator.match(episode.getSensorData(), this.episodes[i].getSensorData()))
                    return false;
            }
            return true;
        }

        @Override
        public String toString() {
            String me = "";
            for (Episode episode : this.episodes)
            {
                me += episode;
            }
            return me;
        }
    }
}
