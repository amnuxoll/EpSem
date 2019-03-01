package agents.marzrules;

import framework.Episode;
import framework.Move;
import framework.NamedOutput;
import framework.Sequence;
import utils.EpisodeUtils;
import utils.EpisodicMemory;
import utils.Ruleset;
import utils.SequenceGenerator;

/**
 * A RuleSetEvaluator will use a given ruleset and configuration to evaluate a set of possible sequences
 * to try based on the current ruleset.
 * @author Zachary Paul Faltersack
 * @version 0.95
 */
public class RuleSetEvaluator {
    //region Class Variables
    private int evaluationDepth = 0;
    private int maxSuffix = 0;
    private SequenceGenerator sequenceGenerator;
    private Ruleset ruleSet;
    //endregion

    //region Constructors
    public RuleSetEvaluator(Move[] moves, Ruleset ruleSet, int evaluationDepth, int maxSuffix) {
        this.evaluationDepth = evaluationDepth;
        this.maxSuffix = maxSuffix;
        this.sequenceGenerator = new SequenceGenerator(moves);
        this.ruleSet = ruleSet;
    }
    //endregion

    //region Public Methods
    public void evaluateEpisodicMemory(EpisodicMemory<Episode> episodicMemory)
    {
        NamedOutput namedOutput = NamedOutput.getInstance();
        namedOutput.writeLine("RuleSetEvaluator", episodicMemory.toString(10));
        namedOutput.writeLine("RuleSetEvaluator");
        this.writeSuffixLine();
        int targetDepth = Math.min(this.evaluationDepth, episodicMemory.length());
        for (int depth = 0; depth < targetDepth; depth++) {
            Move[] moves = EpisodeUtils.selectMoves(episodicMemory.last(depth));
            Sequence currentMemorySequence = new Sequence(moves);
            namedOutput.write("RuleSetEvaluator", currentMemorySequence + ",");
            for (int suffixIndex = 1; suffixIndex <= this.maxSuffix; suffixIndex++) {
                Sequence currentSuffixSequence = this.sequenceGenerator.nextPermutation(suffixIndex);
                Sequence targetSequence = currentMemorySequence.concat(currentSuffixSequence);
                double probability = this.ruleSet.evaluateMoves(targetSequence.getMoves());
                namedOutput.write("RuleSetEvaluator", probability + ",");
            }
            namedOutput.writeLine("RuleSetEvaluator");
        }
        namedOutput.writeLine("RuleSetEvaluator");
        namedOutput.writeLine("RuleSetEvaluator");
    }
    //endregion

    //region Private Methods
    private void writeSuffixLine() {
        NamedOutput namedOutput = NamedOutput.getInstance();
        namedOutput.write("RuleSetEvaluator", ",");
        for (int suffixIndex = 1; suffixIndex <= this.maxSuffix; suffixIndex++) {
            namedOutput.write("RuleSetEvaluator", this.sequenceGenerator.nextPermutation(suffixIndex) + ",");
        }
        namedOutput.write("RuleSetEvaluator", "\n");
    }
    //endregion
}
