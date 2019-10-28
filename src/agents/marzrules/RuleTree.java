package agents.marzrules;

import framework.Action;
import framework.Heuristic;
import framework.SensorData;

import java.util.ArrayList;
import java.util.Optional;
import java.util.function.Function;

public class RuleTree {
    private Ruleset ruleset;
    private Function<SensorData, Integer> hash;
    private String name;
    private boolean onBus = true;//If this RuleSet has a bad driver, we kick them out. Reset if we run out of drivers or the bus goes off a cliff

    public RuleTree(Ruleset ruleset, Function<SensorData, Integer> hash, String name){
        this.ruleset = ruleset;
        this.hash = hash;
        this.name = name;
    }

    public void update(Action action, SensorData sensorData){
        int sense = hash.apply(sensorData);
        boolean isGoal = sensorData.isGoal();
        ruleset.update(action, sense, isGoal);
    }

    public double getRootEV(Heuristic heuristic) {
        Optional<Double> ev = ruleset.getRoot().getExpectation(new ArrayList<>(), true, heuristic);
        return ev.orElse(Double.MAX_VALUE);
    }

    public Ruleset getRuleset() {
        return ruleset;
    }

    public boolean isOnBus() {
        return onBus;
    }

    public void setOnBus(boolean onBus) {
        this.onBus = onBus;
    }

    public String getName() {
        return name;
    }
}
