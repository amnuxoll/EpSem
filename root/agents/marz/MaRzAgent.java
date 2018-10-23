package agents.marz;

import framework.*;
import utils.Sequence;
import utils.SequenceGenerator;

import java.util.*;
//test comment

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
 */
public class MaRzAgent<TSuffixNode extends SuffixNodeBase<TSuffixNode>> implements IAgent {
	private static final int NODE_LIST_SIZE = 10000;

	/*---==== MEMBER VARIABLES ===---*/

	/** this is the node we're currently using to search with */
	private TSuffixNode activeNode = null;

	/**
	 * each permutation has a number associated with it. This is used to track
	 * the last permutation the agent tried.
	 */
	private long lastPermutationIndex = 0;// set to 1 because we hard coded the first
	// permutation to be 'a'

	/**
	 * the next sequence to consider testing (typically generated via
	 * lastPermutationIndex
	 */
	protected Sequence currentSequence = null; // 'a' is always safe because of how

	/**
	 * the last sequence that was successful (used for reporting and not
	 * required for the algorithm)
	 */
	private Sequence lastSuccessfulSequence = null;

	private SequenceGenerator sequenceGenerator;

	protected int lastGoalIndex = 0;

	//Instance Variables
	protected Move[] alphabet;
	protected ArrayList<Episode> episodicMemory = new ArrayList<>();

	/** Number of episodes per run */
	public static final int MAX_EPISODES = 2000000;

	protected SuffixTree<TSuffixNode> suffixTree;

	/** Turn this on to print debugging messages */
	public static boolean debug = false;
	/** println for debug messages only */
	public static void debugPrintln(String s) { if (debug) System.out.println(s); }

	private HashMap<TSuffixNode, Long> permutationQueues = new HashMap<>();

	protected ISuffixNodeBaseProvider<TSuffixNode> nodeProvider;

	/**
	 * MaRzAgent
	 *
	 */
	public MaRzAgent(ISuffixNodeBaseProvider<TSuffixNode> nodeProvider) {
		this.nodeProvider = nodeProvider;
	}// ctor

	/**
	 * Sets up the state of the agent based on the given moves.
	 * @param moves An array of {@link Move} representing the moves available to the agent.
	 */
	@Override
	public void initialize(Move[] moves) {
		this.alphabet = moves;
		this.sequenceGenerator = new SequenceGenerator(this.alphabet);
		this.activeNode = this.nodeProvider.getNode(Sequence.EMPTY, this.alphabet, (index) -> this.episodicMemory.get(index));
		this.suffixTree = new SuffixTree<>(MaRzAgent.NODE_LIST_SIZE, this.activeNode);
		this.currentSequence = this.activeNode.getSuffix();
	}

	/**
	 * Gets a subsequent move based on the provided sensorData.
	 *
	 * @param sensorData The {@link SensorData} from the last move.
	 * @return the next Move to try.
	 */
	@Override
	public Move getNextMove(SensorData sensorData) {
		if (episodicMemory.size() > 0)
			episodicMemory.get(episodicMemory.size() - 1).setSensorData(sensorData);
		if (sensorData == null) {
			// Very beginning of time so we need to select our very first sequence
			this.currentSequence = this.nextPermutation();
		}
		else if (sensorData.isGoal()) {
			this.markSuccess();
			// if the sequence succeeded then try again!
			this.currentSequence.reset();
		}
		else if (!this.currentSequence.hasNext()) {
			this.markFailure();
			this.currentSequence= selectNextSequence();
		}
		Move nextMove = this.currentSequence.next();
		episodicMemory.add(new Episode(nextMove));
		return nextMove;
	}

	protected void markFailure() {
		if(activeNode == null){
			return;
		}

		this.activeNode.addFailIndex(this.episodicMemory.size() - this.activeNode.getSuffix().getLength());
		if (this.activeNode.canSplit() && this.suffixTree.splitSuffix(this.activeNode.getSuffix())) {
			this.permutationQueues.remove(this.activeNode);
		}// if
	}

	protected void markSuccess() {
		if(activeNode == null){
			return;
		}

		this.lastSuccessfulSequence = this.currentSequence;
		if (this.currentSequence.hasNext()) {
			// Was partial match so find the best node to update
			Sequence goalSequence = this.sequenceSinceLastGoal();
			TSuffixNode node = this.suffixTree.findBestMatch(goalSequence);
			// This will happen if we find the goal in fewer moves than a suffix that would exist in the fringe of our tree.
			if (node != null) {
				int index = this.episodicMemory.size() - node.getSuffix().getLength();
				node.addSuccessIndex(index);
			}
		}
		//we hit the goal at the end of current sequence
		else {
			this.activeNode.addSuccessIndex(this.episodicMemory.size() - this.activeNode.getSuffix().getLength());
			this.activeNode.setFoundGoal();
		}
		this.lastGoalIndex = this.episodicMemory.size() - 1;
	}

	/**
	 * nextPermutation
	 *
	 * increments nextSeqToTry
	 */
	private Sequence nextPermutation() {
		this.lastPermutationIndex++;
		return this.sequenceGenerator.nextPermutation(this.lastPermutationIndex);
	}// nextPermutation

	protected Sequence selectNextSequence() {

		TSuffixNode oldActiveNode= activeNode;
		TSuffixNode newBestNode = this.suffixTree.findBestNodeToTry();

		this.setActiveNode(newBestNode);

		this.lastPermutationIndex= this.lastPermutationIndex + (long)Math.pow(alphabet.length, this.activeNode.getSuffix().getLength());

		Sequence sequence = this.sequenceGenerator.nextPermutation(lastPermutationIndex);
		if (!sequence.endsWith(this.activeNode.getSuffix()))
			throw new RuntimeException("oops, they didn't match.  Has your next permutation index overflowed?");
		return sequence;
	}

	private Sequence sequenceSinceLastGoal() {
		List<Move> moves = new ArrayList<>();
		for (int i = this.lastGoalIndex + 1; i < this.episodicMemory.size(); i++) {
			moves.add(this.episodicMemory.get(i).getMove());
		}
		return new Sequence(moves.toArray(new Move[0]));
	}

	/**
	 * set Marz's active node to the node in the suffix tree which best
	 * matches newSequence
	 *
	 * @param newBestNode the sequence whcih marz will now be trying
	 */
	protected void setActiveNode(TSuffixNode newBestNode){

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

	protected TSuffixNode getActiveNode() {
		return activeNode;
	}
}// MaRzAgent
