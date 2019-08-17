package agents.marz;

import framework.*;
import framework.Sequence;
import utils.EpisodeUtils;
import utils.EpisodicMemory;
import utils.SequenceGenerator;
import java.util.*;

/**
 * MaRzAgent Class
 *
 * @author Zachary Paul Faltersack
 * @version 0.95
 *
 * base on code by: *
 * @author Christian Rodriguez
 * @author Giselle Marston
 * @author Andrew Nuxoll
 *
 *
 *
 */
public class MaRzAgent implements IAgent {
	//region Static Variables
	private static final int NODE_LIST_SIZE = 10000;
	//endregion

	//region Class Variables
	protected Sequence currentSequence = null;
	protected int lastGoalIndex = 0;
	protected EpisodicMemory<Episode> episodicMemory = new EpisodicMemory<>();
	protected SuffixTree suffixTree;

	/** this is the node we're currently using to search with */
	private SuffixNode activeNode = null;

	/**
	 * each permutation has a number associated with it. This is used to track
	 * the current permutation the agent tried.
	 */
	private long lastPermutationIndex = 0;

	/**
	 * the current sequence that was successful (used for reporting and not
	 * required for the algorithm)
	 */
	private SequenceGenerator sequenceGenerator;
	private Action[] alphabet;
	private HashMap<SuffixNode, Long> permutationQueues = new HashMap<>();
	private IIntrospector introspector;
	//endregion

	//region IAgent Members
	/**
	 * Sets up the state of the agent based on the given actions.
	 * @param actions An array of {@link Action} representing the actions available to the agent.
	 * @param introspector Can be used to gather metadata about the agent's decicions. DO NOT USE to cheat.
	 */
	@Override
	public void initialize(Action[] actions, IIntrospector introspector) {
		this.alphabet = actions;
		this.introspector = introspector;
		this.sequenceGenerator = new SequenceGenerator(this.alphabet);
		this.activeNode = new SuffixNode(Sequence.EMPTY, this.alphabet, (index) -> this.episodicMemory.get(index));
		this.suffixTree = new SuffixTree(MaRzAgent.NODE_LIST_SIZE, this.activeNode);
		this.setCurrentSequence(this.activeNode.getSuffix());
	}

	/**
	 * Gets a subsequent move based on the provided sensorData.
	 *
	 * @param sensorData The {@link SensorData} from the current move.
	 * @return the next Action to try.
	 */
	@Override
	public Action getNextAction(SensorData sensorData) {
		// Our very first pass is going to be used to set up our first sequence.
		// It is important that we don't update any internal state against the first sensor data because it is
		// always going to indicate a goal, given the invariant of the environment. MaRz's algorithm is not set up
		// to blindly handle that case (as of this writing) so instead use the very first call to set up our
		// initial Next Sequence to Try.
		if (episodicMemory.any() == false)
			this.setCurrentSequence(this.nextPermutation());
		else {
			if (sensorData.isGoal()) {
				this.markSuccess();
				// if the sequence succeeded then try again!
				this.currentSequence.reset();
			} else {
				boolean shouldBail = shouldBail();
				if (!this.currentSequence.hasNext() || shouldBail) {
					this.markFailure();
					this.setCurrentSequence(this.selectNextSequence());
				}
			}
		}
		Action nextAction = this.currentSequence.next();
		episodicMemory.add(new Episode(sensorData, nextAction));
		return nextAction;
	}
	//endregion

	//region Protected Methods
	/**
	 * indicates whether we want to give up on this sequence early
	 */
	protected boolean shouldBail() {
		return false;
	}

	protected void markFailure() {
		if (this.activeNode == null) {
			return;
		}
		this.activeNode.addFailIndex(this.episodicMemory.length() - this.activeNode.getSuffix().getLength());
		if (this.activeNode.canSplit() && this.suffixTree.splitSuffix(this.activeNode.getSuffix())) {
			this.permutationQueues.remove(this.activeNode);
		}// if
	}

	protected void markSuccess() {
		if (this.currentSequence.hasNext()) {
			// Was partial match so find the best node to update
			Action[] actions = EpisodeUtils.selectMoves(this.episodicMemory.subset(this.lastGoalIndex + 1));
			Sequence goalSequence = new Sequence(actions);
			SuffixNode node = this.suffixTree.findBestMatch(goalSequence);
			// This will happen if we find the goal in fewer actions than a suffix that would exist in the fringe of our tree.
			if (node != null) {
				node.addSuccessIndex(this.episodicMemory.length() - node.getSuffix().getLength());
			}
		}
		//we hit the goal at the end of current sequence
		else if (this.activeNode != null) {
			this.activeNode.addSuccessIndex(this.episodicMemory.length() - this.activeNode.getSuffix().getLength());
			this.activeNode.setFoundGoal();
		}

		this.lastGoalIndex = this.episodicMemory.currentIndex();
	}

	protected Sequence selectNextSequence() {
		SuffixNode newBestNode = this.suffixTree.findBestNodeToTry();

		this.setActiveNode(newBestNode);

		this.lastPermutationIndex= this.lastPermutationIndex + (long)Math.pow(alphabet.length, this.activeNode.getSuffix().getLength());

		Sequence sequence = this.sequenceGenerator.nextPermutation(lastPermutationIndex);
		if (!sequence.endsWith(this.activeNode.getSuffix()))
			throw new RuntimeException("oops, they didn't match.  Has your next permutation index overflowed?");
		return sequence;
	}

	/**
	 * set Marz's active node to the node in the suffix tree which best
	 * matches newSequence
	 *
	 * @param newBestNode the sequence whcih marz will now be trying
	 */
	protected void setActiveNode(SuffixNode newBestNode){

		if (newBestNode != this.activeNode) {
			if(this.activeNode != null) {
				this.permutationQueues.put(this.activeNode, this.lastPermutationIndex);
			}
			this.activeNode = newBestNode;

			if(this.activeNode != null) {
				if (this.permutationQueues.containsKey(this.activeNode))
					this.lastPermutationIndex = this.permutationQueues.get(this.activeNode);
				else
					this.lastPermutationIndex = this.sequenceGenerator.getCanonicalIndex(this.activeNode.getSuffix());
			}
		}
	}

	protected SuffixNode getActiveNode() {
		return activeNode;
	}
	//endregion

	//region Private Methods
	/**
	 * nextPermutation
	 *
	 * increments nextSeqToTry
	 */
	private Sequence nextPermutation() {
		this.lastPermutationIndex++;
		return this.sequenceGenerator.nextPermutation(this.lastPermutationIndex);
	}// nextPermutation

	private void setCurrentSequence(Sequence sequence) {
		this.currentSequence = sequence;
	}
	//endregion
}// MaRzAgent
