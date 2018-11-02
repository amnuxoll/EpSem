package framework;

import utils.Sequence;

import java.util.EventObject;

public class AgentEvent extends EventObject {

    private EventType type;
    private Sequence chosenSequence= null;

    /**
     * Constructs a prototypical Event.
     *
     * @param source the object on which the Event initially occurred
     * @throws IllegalArgumentException if source is null
     */
    public AgentEvent(Object source, EventType type) {
        super(source);
        this.type = type;
    }

    public AgentEvent(Object source, Sequence chosenSequence){
        this(source, EventType.DECISION_MADE);

        this.chosenSequence= chosenSequence;
    }

    public enum EventType{
        DECISION_MADE,
        BAILED
    }

    public EventType getType(){ return this.type; }

    public Sequence getChosenSequence(){
        if(type != EventType.DECISION_MADE){
            throw new RuntimeException("No chosen sequence");
        }

        return chosenSequence;
    }
}
