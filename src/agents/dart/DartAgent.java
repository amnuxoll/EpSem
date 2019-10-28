package agents.dart;

import framework.*;
import utils.EpisodicMemory;
import utils.SequenceGenerator;

import java.util.ArrayList;

/**
 * Created by Ryan on 2/11/2019.
 */
public class DartAgent implements IAgent {
    //private ActionEvaluator actionEvaluator;
    private Ruleset ruleset;
    private Action previousAction;
    private IIntrospector introspector;
    private Heuristic heuristic;
    private int explores = 0;
    private int actionsSinceGoal = 0;
    private EpisodicMemory<Episode> episodicMemory;

    public DartAgent(Heuristic heuristic) {
        this.heuristic = heuristic;
    }

    @Override
    public void initialize(Action[] actions, IIntrospector introspector) {
        this.introspector = introspector;
        this.ruleset = new Ruleset(actions, maxDepth, heuristic);
        episodicMemory = new EpisodicMemory<>();
    }

    @Override
    public Action getNextAction(SensorData sensorData) {
        if (sensorData != null) {
            ruleset.update(previousAction, sensorData);
        }
        actionsSinceGoal++;

        ActionProposal proposal = ruleset.getBestMove();
        if(proposal.explore){
            explores++;
        }
        if(proposal.infinite){
            System.out.println("Performing infinite cost action!");
            if(!proposal.explore)
                explores++;
        }
        previousAction = proposal.action;
        //System.out.print(previousAction);
        episodicMemory.add(new Episode(sensorData, previousAction));
        return previousAction;
    }

    @Override
    public void onTestRunComplete() {
        //System.out.println(ruleset);
    }

    @Override
    public String[] getStatisticTypes(){
        return new String[] {
                "goal probability",
                "explore",
                "root information"
        };
    }

    @Override
    public ArrayList<Datum> getGoalData() {
        //System.out.println();
        ArrayList<Datum> data = new ArrayList<>();
        data.add(new Datum("goal probability", heuristic.getHeuristic(0)));
        data.add(new Datum("explore", explores));
        data.add(new Datum("root information", ));
        return data;
    }
}
