package agents.wfc;

import framework.SensorData;
import tests.agents.phujus.PhuJusAgentTest;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Random;
import java.util.Vector;

/**
 * class WFCRule
 * <p>
 * parent class of all rules used by the WFCAgent
 */
public abstract class WFCRule {

    //region Instance Variables

    // To assign a unique id to each rule this shared variable is incremented by the ctor
    private static int nextRuleId = 1;

    // The agent using this rule
    protected final WFCAgent agent;

    // Each rule has a unique integer id
    protected final int ruleId;

    //endregion

    public WFCRule(WFCAgent agent) {
        this.agent = agent;
        this.ruleId = this.nextRuleId++;
    }

    // A shorter string format designed to be used inline
    public abstract String toStringShort();

}//class Rule
