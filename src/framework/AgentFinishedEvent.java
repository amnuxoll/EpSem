package framework;

import java.util.ArrayList;
import java.util.EventObject;


/**
 * @Author Patrick Maloney
 * @Version 0.90
 */
public class AgentFinishedEvent extends EventObject {
    ArrayList<Datum> agentData;

    public AgentFinishedEvent(Object source, ArrayList<Datum> agentData) {
        super(source);

        if (agentData == null)
            this.agentData = new ArrayList<>();
        else
            this.agentData = agentData;
    }

    public ArrayList<Datum> getAgentData(){
        return this.agentData;
    }
}
