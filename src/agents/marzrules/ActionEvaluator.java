package agents.marzrules;

import framework.Action;
import framework.Episode;
import framework.SensorData;

import java.util.*;
import java.util.function.Function;

import static java.util.Collections.shuffle;

public class ActionEvaluator {
    private ArrayList<RuleTree> ruleTrees;
    private Action[] alphabet;
    private int maxDepth;
    private Heuristic heuristic;
    private boolean independentDrivers;
    private boolean resetWithAnyOffroad;
    private boolean singleDriver;
    private RuleTree drivingTree;
    private int noSensorSteps = 0;
    private final int FOREST_SIZE = 5;
    private ArrayDeque<RuleTree> potentialTrees;


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

    public ActionEvaluator(Action[] alphabet, int maxDepth, Heuristic heuristic, boolean independentDrivers, boolean resetWithAnyOffroad, boolean singleDriver){
        ruleTrees = new ArrayList<>();
        this.alphabet = alphabet;
        this.maxDepth = maxDepth;
        this.heuristic = heuristic;
        this.independentDrivers = independentDrivers;
        this.resetWithAnyOffroad = resetWithAnyOffroad;
        this.singleDriver = singleDriver;
        //ruleTrees.add(new RuleTree(new Ruleset(alphabet, maxDepth, heuristic), (SensorData data) -> 0));//no sensor RuleTree
        potentialTrees = new ArrayDeque<>();
    }

    private void initialize(SensorData sensorData){
        addRuleTree((SensorData data) -> 0, "NO_SENSOR");//no sensor RuleTree
        String[] sensorArray = new String[sensorData.getSensorNames().size()];
        sensorData.getSensorNames().toArray(sensorArray);
        List<String> sensors = Arrays.asList(sensorArray);
        Collections.shuffle(sensors, new Random(7));
        for(int i = 0; i < FOREST_SIZE && i < sensors.size(); i++) {
            if(sensors.get(i).equals(SensorData.goalSensor))
                continue;
            addRuleTree(sensorHash(sensors.get(i)), sensors.get(i));
        }
        for(int i = FOREST_SIZE; i < sensors.size(); i++){
            if(sensors.get(i).equals(SensorData.goalSensor))
                continue;
            queueRuleTree(sensorHash(sensors.get(i)), sensors.get(i));
        }
        //driver gets set automatically when update is called, so we do not need to set it here.
    }

    private void addRuleTree(Function<SensorData, Integer> hash, String name){
        System.out.println("Added: " + name);
        ruleTrees.add(new RuleTree(new Ruleset(alphabet, maxDepth, heuristic), hash, name));
    }

    private void queueRuleTree(Function<SensorData, Integer> hash, String name){
        potentialTrees.add(new RuleTree(new Ruleset(alphabet, maxDepth, heuristic), hash, name));
    }

    public void update(Action action, SensorData sensorData){
        if(ruleTrees.isEmpty())
            initialize(sensorData);
        for(RuleTree ruleTree:ruleTrees){
            ruleTree.update(action, sensorData);
        }
        if(sensorData.isGoal()) {
            noSensorSteps = 0;
            loadBus();
        }
    }

    public void evaluateForest(Episode[] recentEpisodes){
        if(!potentialTrees.isEmpty()) {
            double worstEV = (ruleTrees.get(0).getRootEV(heuristic) * Math.log(ruleTrees.get(0).getRuleset().getAvgBitStateEstimate()));
            int worstTree = 0;
            for (int i = 1; i < ruleTrees.size(); i++){
                RuleTree ruleTree = ruleTrees.get(i);
                double ev = (ruleTree.getRootEV(heuristic) * Math.log(ruleTree.getRuleset().getAvgBitStateEstimate()));
                if(ev > worstEV){
                    worstEV = ev;
                    worstTree = i;
                }
            }
            RuleTree newTree = growTree(recentEpisodes);
            if((newTree.getRootEV(heuristic) * Math.log(newTree.getRuleset().getAvgBitStateEstimate())) < worstEV) {
                System.out.println("Deleted: " + ruleTrees.get(worstTree).getName());
                ruleTrees.set(worstTree, newTree);
                System.out.println("Added: " + newTree.getName());
            }
            else{
                System.out.println("Deleted: " + newTree.getName());
            }
        }
    }

    private RuleTree growTree(Episode[] recentEpisodes){
        RuleTree tree = potentialTrees.remove();
        Action prev = null;
        for(Episode episode:recentEpisodes){
            tree.update(prev, episode.getSensorData());
            prev = episode.getAction();
        }
        return tree;
    }

    public ActionProposal getBestMove(){
        boolean offroaded = offroadDrivers();
        if(offroaded && resetWithAnyOffroad) {
            ActionProposal proposal = findNewDrivers();
            if(proposal.ruleNode == ruleTrees.get(0).getRuleset().getDriver())
                noSensorSteps++;
            return proposal;
        }
        if(singleDriver) {
            if(drivingTree == null || drivingTree.getRuleset().getDriver() == null) {
                ActionProposal proposal = findNewDrivers();
                if (proposal.ruleNode == ruleTrees.get(0).getRuleset().getDriver())
                    noSensorSteps++;
                return proposal;
            }
            ActionProposal proposal = drivingTree.getRuleset().getDriverMove();
            if(drivingTree == ruleTrees.get(0))
                noSensorSteps++;
            if(proposal.infinite)
                return findNewDrivers();
            return proposal;
        }
        ActionProposal bestAction = ruleTrees.get(0).getRuleset().getDriverMove();
        boolean betterTree = false;
        boolean foundProposal = !bestAction.infinite;
        for(int i = 1; i < ruleTrees.size(); i++){
            RuleTree tree = ruleTrees.get(i);
            if(!tree.isOnBus())
                continue;
            ActionProposal proposal = tree.getRuleset().getDriverMove();
            if(proposal.compareTo(bestAction) < 0) {
                betterTree = true;
                bestAction = proposal;
                foundProposal = true;
            }
        }
        if(!betterTree)
            noSensorSteps++;
        if(!foundProposal)
            return findNewDrivers();
        return bestAction;
    }

    public int getNoSensorSteps(){
        return noSensorSteps;
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
        RuleTree bestTree = ruleTrees.get(0);
        ActionProposal bestProposal = bestTree.getRuleset().getBestMove();
        for(int i = 1; i < ruleTrees.size(); i++){
            RuleTree tree = ruleTrees.get(i);
            ActionProposal proposal = tree.getRuleset().getBestMove();
            if(proposal.compareTo(bestProposal) < 0) {
                bestTree = tree;
                bestProposal = proposal;
            }
        }
        if(singleDriver)
            drivingTree = bestTree;
        if(!independentDrivers){
            RuleNode driver = bestProposal.ruleNode;
            for(RuleTree ruleTree:ruleTrees){
                ruleTree.getRuleset().setDriver(driver.getCurrentDepth());
            }
        }
        return bestProposal;
    }

    public static Function<SensorData, Integer> sensorHash(String sensorName){
        return (SensorData data) -> (boolean) data.getSensor(sensorName) ? 1 : 0;
    }

    public double getAverageNoSensorBits(){
        return ruleTrees.get(0).getRuleset().getAvgBitStateEstimate();
    }
}
