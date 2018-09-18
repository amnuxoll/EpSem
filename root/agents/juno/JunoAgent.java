package agents.juno;

import agents.marz.ISuffixNodeBaseProvider;
import agents.marz.MaRzAgent;
import environments.fsm.FSMDescription;
import framework.Episode;
import framework.Move;
import environments.fsm.FSMDescription.Sensor;


import java.util.HashMap;

public class JunoAgent extends MaRzAgent {



    /**
     * MaRzAgent
     *
     * @param nodeProvider
     */
    public JunoAgent(ISuffixNodeBaseProvider nodeProvider) {
        super(nodeProvider);

    }


}
