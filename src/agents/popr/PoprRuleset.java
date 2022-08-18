package agents.popr;

import agents.marzrules.Heuristic;
import agents.marzrules.RuleNode;
import agents.marzrules.RuleNodeRoot;
import agents.marzrules.Ruleset;
import framework.Action;

import java.util.ArrayDeque;
import java.util.ArrayList;

public class PoprRuleset extends Ruleset {

    public PoprRuleset(Action[] alphabet, int maxDepth, Heuristic heuristic) {
        super(alphabet, maxDepth, heuristic);
    }

    public Action falsify() {
        Action toTake = null;//TODO: make random?
        int bestDepth = 500; //arbitrary; right now just max depth limit
        for (int i = 0; i < current.size(); i++) {
            if ( current.get(i) instanceof RuleNodeRoot) {
                // I don't think we need to look at 0-deep rules, might be wrong
                continue;
            }
            // because current has one rule of each depth, i also effectively measures depth of parent RuleNode
            if (i >= bestDepth){
                break;
            }

            for (Action action : alphabet) {
                RuleNode superstition = getNextTestable(current.get(i), action);
                //TODO: this assumes depth means something more intuitive than what I think is reflected in our implementation

                if (superstition.getCurrentDepth() < bestDepth){ //TODO: superstition.depth + 1 to enforce choice of simplest rules?
                    toTake = action;
                }
            }
        }
        return toTake;
    }
    //breadth first search for nearest testable child
    //this function is called twice (in a 2-alphabet) per node in current, which I'd like do more elegantly but alas alack

    public RuleNode getNextTestable(RuleNode parent, Action action) {
        ArrayDeque<RuleNode> queue = new ArrayDeque<>();
        RuleNode p = parent;
        ArrayList<RuleNode> moveChildren = p.getChild(action);
        queue.addAll(moveChildren); //initial queue is just the specific branch of children that the caller of this function was looking at
        while(queue.size() > 0) {
            p = queue.remove();

            if (current.contains(p)){ //if p is in current, continue b/c this is redundant
                continue;
            }

            for (Action m : alphabet){ //to get each set of child nodes
                moveChildren = p.getChild(m);
                if (moveChildren.size() == 1){ //i.e. this node has never been expanded, only instantiated with goal child
                    return p;
                } else {
                    queue.addAll(moveChildren);
                }
            }
        }
        return null;
    }
}
