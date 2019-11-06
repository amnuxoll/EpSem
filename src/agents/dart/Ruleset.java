package agents.dart;

import framework.Action;
import framework.Heuristic;
import framework.SensorData;

import java.util.ArrayList;

public class Ruleset {
    ArrayList<RuleNode> current;
    RuleNode driver;
    RuleNodeRoot root;
    Heuristic heuristic;

    public Ruleset(RuleNodeRoot root, Heuristic heuristic){
        this.root = root;
        current = new ArrayList<>();
        current.add(root);
        this.heuristic = heuristic;
        driver = root;
    }

    public void update(Action action, SensorData data, int episodeIndex){
        if(data.isGoal()){
            for(RuleNode ruleNode:current){
                ruleNode.updateExtendGoal(action);
            }
            root.occurs(episodeIndex);
            current.clear();
            current.add(root);
            root.reachedGoalRoot(heuristic, episodeIndex);
            driver = root;
            action = null;
        }
        ArrayList<RuleNode> futureCurrent = new ArrayList<>();
        for(int i = 0; i < current.size(); i++){
            RuleNode node = current.get(i);
            for(RuleNode child:node.updateExtend(action, data, episodeIndex)){
                if(child != null)
                    futureCurrent.add(child);
            }
            if(driver == node){
                ActionProposal proposal = driver.getCachedProposal();
                if(proposal.infinite)
                    driver = null;
                else
                    driver = node.getChild(action, proposal.sensorKey,
                        RuleNode.sensorHash(proposal.sensorKey, data));
            }
        }
        current = futureCurrent;
        current.add(root);
        root.occurs(episodeIndex);
        heuristic.setGoalProbability(root.getGoalProbability());
    }

    public ActionProposal getBestMove(){
        if(driver != null){
            ActionProposal proposal = driver.getCachedProposal();
            if(!proposal.infinite){
                return proposal;
            }
        }
        //if we have no driver or the driver has an infinite cost move, find a new driver
        ActionProposal best = root.getBestProposal(heuristic);
        for(RuleNode node:current){
            ActionProposal proposal = node.getCachedProposal();
            if(proposal.compareTo(best) < 0) {
                best = proposal;
            }
        }
        driver = best.ruleNode;
        return best;
    }
}
