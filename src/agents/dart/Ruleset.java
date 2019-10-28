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

    public void update(Action action, SensorData data){
        if(data.isGoal()){
            for(RuleNode ruleNode:current){
                ruleNode.updateExtendGoal(action);
            }
            root.occurs();
            current.clear();
            current.add(root);
            root.reachedGoal(heuristic);
            driver = root;
            action = null;
        }
        ArrayList<RuleNode> futureCurrent = new ArrayList<>();
        for(int i = 0; i < current.size(); i++){
            RuleNode node = current.get(i);
            for(RuleNode child:node.updateExtend(action, data)){
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
        root.occurs();
        heuristic.setGoalProbability(root.getGoalProbability());
    }
}
