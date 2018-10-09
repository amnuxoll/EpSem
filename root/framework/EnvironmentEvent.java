package framework;

import java.util.EventObject;

/**
 * EnvironmentEvent is an event that an {@link Environment} can fire.
 * @author Zachary Paul Faltersack
 * @version 0.95
 */
public class EnvironmentEvent extends EventObject {
    private EventType type;

    /**
     * Constructs a prototypical Event.
     *
     * @param source The object on which the Event initially occurred.
     * @throws IllegalArgumentException if source is null.
     */
    public EnvironmentEvent(Object source, EventType type) {
        super(source);
        this.type= type;
    }

    /**
     * Get the number of steps taken before the goal was reached.
     * @return the number of steps.
     */
    public EventType getEventType() {
        return this.type;
    }

    public enum EventType{
        DATA_BREAK;
    }
}
