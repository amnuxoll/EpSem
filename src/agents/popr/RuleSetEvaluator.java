package agents.popr;

import framework.Action;
import framework.NamedOutput;
import framework.Sequence;
import agents.marzrules.RuleNode;
import agents.marzrules.Ruleset;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * A RuleSetEvaluator will use a given ruleset and configuration to evaluate a set of possible sequences
 * to try based on the current ruleset.
 * @author Zachary Paul Faltersack
 * @version 0.95
 */
public class RuleSetEvaluator {
    //region Class Variables
    Sequence[] sequences;
    ArrayList<ArrayList<Action>> suffixSequences;
    //endregion

    //region Constructors
    public RuleSetEvaluator(Sequence[] sequences) {
        this.sequences = sequences;
        this.suffixSequences = new ArrayList<>();
        for (Sequence suffix : sequences) {
            suffixSequences.add(new ArrayList<>(Arrays.asList(suffix.getActions())));
        }
    }
    //endregion

    //region Public Methods
    public void evaluate(Ruleset ruleSet)
    {
        NamedOutput namedOutput = NamedOutput.getInstance();
        namedOutput.writeLine("RuleSetEvaluator");
        this.writeSuffixLine();
        for (RuleNode node : ruleSet.getCurrent()) {
           namedOutput.write("RuleSetEvaluator", node.toString() + ",");
           for (ArrayList<Action> actions : this.suffixSequences) {
               namedOutput.write("RuleSetEvaluator", node.getGoalProbability(actions, 0) + ",");
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
        for (Sequence suffix : this.sequences) {
            namedOutput.write("RuleSetEvaluator", suffix + ",");
        }
        namedOutput.write("RuleSetEvaluator", "\n");
    }
    //endregion
}
