package agents.dart;

import agents.nsm.QEpisode;
import framework.*;
import utils.EpisodicMemory;

import java.util.ArrayList;

/**
 * Created by Ryan on 2/11/2019.
 */
public class DartAgent implements IAgent {
    private Ruleset ruleset;
    private Action previousAction;
    private IIntrospector introspector;
    private RuleNodeRoot root;
    private Heuristic heuristic;
    private int explores = 0;
    private Action[] actions;
    private int actionsSinceGoal = 0;
    private EpisodicMemory<Episode> episodicMemory;
    private SensorData lastSense;

    public DartAgent(Heuristic heuristic) {
        this.heuristic = heuristic;
    }

    private ActionSense lookupEpisode(int index){
        Action action = episodicMemory.get(index).getAction();
        if(index == episodicMemory.length() - 1){
            return new ActionSense(action, lastSense);
        }
        return new ActionSense(action, episodicMemory.get(index + 1).getSensorData());
    }

    @Override
    public void initialize(Action[] actions, IIntrospector introspector) {
        this.introspector = introspector;
        this.actions = actions;
        episodicMemory = new EpisodicMemory<>();
    }

    @Override
    public Action getNextAction(SensorData sensorData) {
        lastSense = sensorData;
        //never made a move before, so create the ruleset
        if (previousAction == null){
            root = new RuleNodeRoot(actions, sensorData.getSensorNames().toArray(new String[0]), this::lookupEpisode);
            this.ruleset = new Ruleset(root, heuristic);
        }
        ruleset.update(previousAction, sensorData, episodicMemory.length());
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
        System.out.print(previousAction);
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
        System.out.println();
        ArrayList<Datum> data = new ArrayList<>();
        data.add(new Datum("goal probability", heuristic.getHeuristic(0)));
        data.add(new Datum("explore", explores));
        data.add(new Datum("root information", root.getCachedProposal().cost));
        return data;
    }
}
