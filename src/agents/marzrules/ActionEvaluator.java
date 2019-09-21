package agents.marzrules;

import agents.predr.Rule;
import framework.Action;
import framework.SensorData;

import java.util.ArrayList;
import java.util.function.Function;

public class ActionEvaluator {
    private ArrayList<RuleTree> ruleTrees;
    private Action[] alphabet;
    private int maxDepth;
    private Heuristic heuristic;
    private boolean independentDrivers;
    private boolean resetWithAnyOffroad;
    public static final Function<SensorData, Integer> ALL_SENSOR_HASH = (SensorData sensorData) -> {
        int sense = 0;
        for (String sensor : sensorData.getSensorNames()) {
            if (sensor.equals(SensorData.goalSensor)) {
                continue;
            }

            int value = (boolean) sensorData.getSensor(sensor) ? 1 : 0;
            sense *= 2;
            sense += value;
        }
        return sense;
    };

    public ActionEvaluator(Action[] alphabet, int maxDepth, Heuristic heuristic, boolean independentDrivers, boolean resetWithAnyOffroad){
        ruleTrees = new ArrayList<>();
        this.alphabet = alphabet;
        this.maxDepth = maxDepth;
        this.heuristic = heuristic;
        this.independentDrivers = independentDrivers;
        this.resetWithAnyOffroad = resetWithAnyOffroad;
        ruleTrees.add(new RuleTree(new Ruleset(alphabet, maxDepth, heuristic), (SensorData data) -> 0));//no sensor RuleTree
    }

    public void addRuleTree(Function<SensorData, Integer> hash){
        ruleTrees.add(new RuleTree(new Ruleset(alphabet, maxDepth, heuristic), hash));
    }

    public void update(Action action, SensorData sensorData){
        for(RuleTree ruleTree:ruleTrees){
            ruleTree.update(action, sensorData);
        }
        if(sensorData.isGoal())
            loadBus();
    }

    public ActionProposal getBestMove(){
        boolean offroaded = offroadDrivers();
        if(offroaded && resetWithAnyOffroad)
            return findNewDrivers();
        ActionProposal bestAction = ruleTrees.get(0).getRuleset().getDriverMove();
        boolean foundProposal = !bestAction.infinite;
        for(int i = 1; i < ruleTrees.size(); i++){
            RuleTree tree = ruleTrees.get(i);
            if(!tree.isOnBus())
                continue;
            ActionProposal proposal = tree.getRuleset().getDriverMove();
            if(proposal.compareTo(bestAction) < 0) {
                bestAction = proposal;
                foundProposal = true;
            }
        }
        if(!foundProposal)
            return findNewDrivers();
        return bestAction;
    }

    public boolean offroadDrivers(){
        boolean offroadAny = false;
        for(RuleTree ruleTree:ruleTrees){
            RuleNode driver = ruleTree.getRuleset().getDriver();
            if(ruleTree.isOnBus() && (driver == null || driver.getFrequency() == 1)){
                ruleTree.setOnBus(false);
                offroadAny = true;
            }
        }
        return offroadAny;
    }

    protected void loadBus(){
        for(RuleTree ruleTree:ruleTrees){
            ruleTree.setOnBus(true);
        }
    }

    protected ActionProposal findNewDrivers(){
        loadBus();
        ActionProposal bestProposal = ruleTrees.get(0).getRuleset().getBestMove();
        for(int i = 1; i < ruleTrees.size(); i++){
            RuleTree tree = ruleTrees.get(i);
            ActionProposal proposal = tree.getRuleset().getBestMove();
            if(proposal.compareTo(bestProposal) < 0) {
                bestProposal = proposal;
            }
        }
        if(!independentDrivers){
            RuleNode driver = bestProposal.ruleNode;
            for(RuleTree ruleTree:ruleTrees){
                ruleTree.getRuleset().setDriver(driver.getCurrentDepth());
            }
        }
        return bestProposal;
    }

    public static Function<SensorData, Integer> sensorHash(String sensorName){
        return (SensorData data) -> (int) data.getSensor(sensorName);
    }
}
