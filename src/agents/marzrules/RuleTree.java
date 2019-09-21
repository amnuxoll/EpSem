package agents.marzrules;

import environments.fsm.FSMEnvironment;
import framework.Action;
import framework.SensorData;

import java.util.function.Function;

public class RuleTree {
    private Ruleset ruleset;
    private Function<SensorData, Integer> hash;
    private boolean onBus = true;//If this RuleSet has a bad driver, we kick them out. Reset if we run out of drivers or the bus goes off a cliff

    public RuleTree(Ruleset ruleset, Function<SensorData, Integer> hash){
        this.ruleset = ruleset;
        this.hash = hash;
    }

    public void update(Action action, SensorData sensorData){
        int sense = hash.apply(sensorData);
        boolean isGoal = sensorData.isGoal();
        ruleset.update(action, sense, isGoal);
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
}
