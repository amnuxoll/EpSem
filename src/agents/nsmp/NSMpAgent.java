package agents.nsmp;

import agents.nsm.NSMAgent;
import agents.nsm.QEpisodicMemory;
import agents.nsm.QLearningConfiguration;
import framework.Action;
import framework.IIntrospector;

import javax.swing.text.html.HTML;

public class NSMpAgent extends NSMAgent {

    public NSMpAgent(QLearningConfiguration qLearningConfiguration) {
        super(qLearningConfiguration);
    }

    @Override
    public void initialize(Action[] actions, IIntrospector introspector) {
        this.actions = actions;
        this.episodicMemory = new pQEpisodicMemory();
    }
}
