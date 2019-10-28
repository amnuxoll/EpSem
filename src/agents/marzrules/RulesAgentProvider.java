package agents.marzrules;

import framework.Heuristic;
import framework.IAgent;
import framework.IAgentProvider;

/**
 * Created by Ryan on 2/11/2019.
 */
public class RulesAgentProvider implements IAgentProvider {
    private Heuristic heuristic;
    private int depthLimit;
    private boolean independentDrivers = true;//configures the RulesAgent to have each tree select its driver independent of other trees.
    //(if false all drivers must be at the same depth)
    private boolean resetWithAnyOffroad = true;//configure RulesAgent to have a full driver reset if any driver offroads.
    //(if false all drivers must offroad before a reset occurs)
    private boolean singleDriver = true;//configure RulesAgent to only use one driver from one tree at a time.
    //(if false it may switch between trees every move)

    public RulesAgentProvider(Heuristic heuristic, int depthLimit, boolean independentDrivers, boolean resetWithAnyOffroad, boolean singleDriver) {
        this.heuristic = heuristic;
        this.depthLimit = depthLimit;
        this.independentDrivers = independentDrivers;
        this.resetWithAnyOffroad = resetWithAnyOffroad;
        this.singleDriver = singleDriver;
    }

    public RulesAgentProvider(Heuristic heuristic, int depthLimit) {
        this.heuristic = heuristic;
        this.depthLimit = depthLimit;
    }

    public RulesAgentProvider(Heuristic heuristic) {
        this.heuristic = heuristic;
        this.depthLimit = 10000;
    }

    @Override
    public IAgent getAgent(){
        return new RulesAgent(heuristic, depthLimit, independentDrivers, resetWithAnyOffroad, singleDriver);
    }

    @Override
    public String getAlias() {return "RulesAgent";}
}
