package agents.juno;

import agents.marz.ISuffixNodeBaseProvider;
import agents.marz.MaRzAgent;
import framework.Episode;
import framework.EpisodeWeights;
import framework.Move;
import framework.SensorData;
import utils.Sequence;


import java.util.ArrayList;

public class JunoAgent extends MaRzAgent {
    ArrayList<EpisodeWeights> weightTable= null;

    //how many episodes we match at a time
    private int WINDOW_SIZE= 5;

    /**
     * JunoAgent
     *
     * @param nodeProvider
     */
    public JunoAgent(ISuffixNodeBaseProvider nodeProvider) {
        super(nodeProvider);


    }


    @Override
    protected Sequence selectNextSequence(){
        Sequence marzSuggestion= super.selectNextSequence();

        if(this.lastGoalIndex <= 0){
            return marzSuggestion;
        }

        return null;

    }

}
