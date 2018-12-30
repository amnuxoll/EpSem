package agents.discr;

import agents.marz.ISuffixNodeBaseProvider;
import agents.marz.MaRzAgent;
import agents.marz.SuffixNodeBase;
import framework.Episode;
import framework.Move;
import utils.Discriminator;
import framework.Sequence;
import utils.Semsode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

/**
 *
 * @author Zachary Paul Faltersack
 * @version 0.95
 */
public class MaRzLearner<TSuffixNode extends SuffixNodeBase<TSuffixNode>> extends MaRzAgent<TSuffixNode> {
    //region Class Variables
    private Discriminator discriminator = new Discriminator();
    private ArrayList<Integer> goalIndices = new ArrayList<>();
    private int learnerWindow = 2; // start at 2 because we want one extra sensorData
    private Sequence goalSequence = null;
    private HashMap<Semsode, Move> goalSemsodes = new HashMap<>();
    private HashSet<Move> goalMoves = new HashSet<>();
    //endregion

    //region Constructors
    /**
     * MaRzAgent
     *
     * @param nodeProvider
     */
    public MaRzLearner(ISuffixNodeBaseProvider<TSuffixNode> nodeProvider) {
        super(nodeProvider);
    }
    //endregion

    //region MaRzAgent<TSuffixNode> Overrides
    @Override
    protected void markSuccess() {
        // We'll get inconsistent data if we allow this case through due to the fact that a goal sensor triggers a wormhole event.
        // This is a definite weakness of the algorithm right now
        Episode episode = this.episodicMemory.getFromOffset(1);
        if (!episode.hitGoal()) {
            this.discriminator.add(episode.getSensorData(), this.episodicMemory.current().getMove());
            System.out.println(this.discriminator);
        }
        this.goalIndices.add(this.episodicMemory.currentIndex());
        if (!this.goalMoves.contains(this.episodicMemory.current().getMove()))
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

    @Override
    protected Sequence selectNextSequence() {
        if (this.goalSequence == null)
            return super.selectNextSequence();
        super.setActiveNode(super.suffixTree.findBestMatch(this.goalSequence));
        return this.goalSequence;
    }

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
    //endregion

    //region Private Methods
    private void generateGoalSemsodes() {
        this.goalSemsodes.clear();
        this.goalMoves.clear();
        for (int goalIndex : this.goalIndices)
        {
            Move goalMove = this.episodicMemory.get(goalIndex).getMove();
            if (!goalMoves.contains(goalMove))
            {
                Semsode semsode = new Semsode(this.episodicMemory.subset(goalIndex - learnerWindow, goalIndex));
                this.goalSemsodes.put(semsode, goalMove);
                goalMoves.add(goalMove);
            }
        }
    }
    //endregion
}
