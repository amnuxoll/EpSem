package framework;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 *
 * @author Zachary Paul Faltersack
 * @version 0.95
 */
public class GoalEventTest {
    //region Constructor Tests
    @Test
    public void constructorStepCountLessThan1ThrowsException()
    {
        assertThrows(IllegalArgumentException.class, () -> new GoalEvent(this, 0, null));
    }
    //endregion

    //region getStepCountToGoal Tests
    @Test
    public void getStepCountToGoal()
    {
        GoalEvent event = new GoalEvent(this, 13, null);
        assertEquals("13", event.getStepCountToGoal());
    }
    //endregion

    //region getAgentData Tests
    @Test
    public void getAgentDataNoneProvidedYieldsEmptyList()
    {
        GoalEvent event = new GoalEvent(this, 13, null);
        assertEquals(0, event.getAgentData().size());
    }

    @Test
    public void getAgentData()
    {
        ArrayList<Datum> agentData = new ArrayList<>();
        agentData.add(new Datum("statistic", 13));
        GoalEvent event = new GoalEvent(this, 13, agentData);
        assertSame(agentData, event.getAgentData());
    }
    //endregion
}
