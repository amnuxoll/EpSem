package tests.framework;

import framework.Datum;
import framework.GoalEvent;

import java.util.ArrayList;

import tests.EpSemTest;
import tests.EpSemTestClass;
import static tests.Assertions.*;

/**
 *
 * @author Zachary Paul Faltersack
 * @version 0.95
 */
@EpSemTestClass
public class GoalEventTest {
    //region Constructor Tests
    @EpSemTest
    public void constructorStepCountLessThan1ThrowsException()
    {
        assertThrows(IllegalArgumentException.class, () -> new GoalEvent(this, 0, 0, null));
    }
    //endregion

    //region getAgentData Tests
    @EpSemTest
    public void getAgentDataNoneProvidedYieldsEmptyList()
    {
        GoalEvent event = new GoalEvent(this, 0, 13, null);
        assertEquals(1, event.getAgentData().size());
    }

    @EpSemTest
    public void getAgentData()
    {
        ArrayList<Datum> agentData = new ArrayList<>();
        agentData.add(new Datum("statistic", 13));
        GoalEvent event = new GoalEvent(this, 0, 13, agentData);
        assertSame(agentData, event.getAgentData());
    }
    //endregion
}
