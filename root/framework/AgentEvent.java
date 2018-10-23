package framework;

import java.util.EventObject;

public class AgentEvent extends EventObject {

    private EventType type;
    private int numDecisions;

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

    public enum EventType{
        DECISION_MADE
    }

    public EventType getType(){ return this.type; }
}
