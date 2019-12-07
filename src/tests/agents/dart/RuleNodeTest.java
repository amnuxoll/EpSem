package tests.agents.dart;

import agents.dart.ActionProposal;
import agents.dart.ActionSense;
import agents.dart.RuleNode;
import agents.dart.RuleNodeGoal;
import environments.fsm.FSMEnvironment;
import framework.Action;
import framework.Heuristic;
import framework.SensorData;
import framework.TestHeuristic;
import tests.EpSemTest;
import tests.EpSemTestClass;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;

import static tests.Assertions.*;


/**
 * Created by Alex on 11/16/2019.
 */
@EpSemTestClass
public class RuleNodeTest {

    //region Constructor tests

    @EpSemTest
    public void testIllegalConstructorArguments(){
        assertThrows(IllegalArgumentException.class, () -> new RuleNode(0, null, 0, new String[] {}, (index) -> null));
        assertThrows(IllegalArgumentException.class, () -> new RuleNode(0, new Action[] {}, 0, new String[] {}, (index) -> null));
        assertThrows(IllegalArgumentException.class, () -> new RuleNode(0, new Action[] {new Action("a")}, 0, new String[] {}, (index) -> null));
        Action[] potentialActions = new Action[] {new Action("a"), new Action("b")};
        assertThrows(IllegalArgumentException.class, () -> new RuleNode(0, potentialActions, 0, null, (index) -> null));
        assertThrows(IllegalArgumentException.class, () -> new RuleNode(0, potentialActions, 0, new String[] {}, null));
    }

    @EpSemTest
    public void testNegativeDepth(){
        assertThrows(IllegalArgumentException.class, () -> new RuleNode(0, new Action[] {new Action("a"), new Action("b")}, -1, new String[] {}, (index) -> null));
    }

    @EpSemTest
    public void testMaxDepth(){
        assertThrows(IllegalArgumentException.class, () -> new RuleNode(0, new Action[] {new Action("a"), new Action("b")}, RuleNode.DEPTH_LIMIT + 1, new String[] {}, (index) -> null));
    }

    @EpSemTest
    public void testNoThrow(){
        assertNotNull(new RuleNode(0, new Action[] {new Action("a"), new Action("b")}, 1, new String[] {}, (index) -> null));
    }

    //endregion

    @EpSemTest
    public void testProposalDepthLimit(){
        Heuristic h = new TestHeuristic(1);
        Action[] actions = new Action[] {new Action("a"), new Action("b")};
        RuleNode node = new RuleNode(0, actions, RuleNode.DEPTH_LIMIT, new String[] {}, (x) -> null);
        node.occurs(0); // Makes sure node is visited
        ActionProposal old = node.getCachedProposal();
        ActionProposal prop = node.getBestProposal(h);
        assertTrue(prop.infinite);
        assertEquals(node.getCachedProposal(), prop);
        assertNotEquals(prop, old);
    }

    @EpSemTest
    public void testProposalNotVisited(){
        Heuristic h = new TestHeuristic(1);
        Action[] actions = new Action[] {new Action("a"), new Action("b")};
        RuleNode node = new RuleNode(0, actions, 1, new String[] {}, (x) -> {
            return new ActionSense(actions[1], new SensorData(false));
        });
        ActionProposal old = node.getCachedProposal();
        ActionProposal notVisted = node.getBestProposal(h);

        //Recalculating prop when not visited had no effect
        assertEquals(old, notVisted);
        assertEquals(old, node.getCachedProposal());

        //When node is visited, has an effect
        node.occurs(0);
        node.incrementMoveFrequency(actions[1]);
        ActionProposal visited = node.getBestProposal(h);
        assertNotEquals(old, visited);
        assertEquals(visited, node.getCachedProposal());
        node.occurs(4);
        ActionProposal visitedAgain = node.getBestProposal(h);
        assertNotEquals(visitedAgain, visited);
        assertEquals(visitedAgain, node.getCachedProposal());

        //After node is unvisited, again has no effect
        node.reachedGoal(7, h); //Unvisit node
        ActionProposal unvisited = node.getBestProposal(h);
        assertEquals(visitedAgain, unvisited);
        assertEquals(visitedAgain, node.getCachedProposal());
    }

    @EpSemTest
    public void testProposalFrequencyOneJustOccured() {
        Heuristic h = new TestHeuristic(3);
        Action[] actions = new Action[]{new Action("a"), new Action("b")};
        RuleNode node = new RuleNode(0, actions, 1, new String[]{}, (x) -> null);
        ActionProposal old = node.getCachedProposal();
        node.occurs(0);

        //Node just occured - no action taken
        ActionProposal recent = node.getBestProposal(h);
        assertNotEquals(old, recent);
        assertEquals(recent, node.getCachedProposal());
        assertTrue(recent.infinite);
    }

    @EpSemTest
    public void testProposalFrequencyOneOtherBaseCases(){
        Heuristic h = new TestHeuristic(3);
        Action[] actions = new Action[]{new Action("a"), new Action("b")};
        RuleNode node = new RuleNode(0, actions, 1, new String[]{}, (x) -> new ActionSense(actions[0], null));
        ActionProposal old = node.getCachedProposal();
        node.occurs(0);

        //No goal index - explore with a different move
        ActionProposal noGoal = node.getBestProposal(h);
        assertNotEquals(old, noGoal);
        assertEquals(noGoal, node.getCachedProposal());
        assertTrue(noGoal.explore);
        assertEquals(noGoal.cost, h.getHeuristic(1));
        assertEquals(noGoal.action, actions[1]);
        assertFalse(noGoal.infinite);

        //Goal index worse - still return explore
        node.reachedGoal(10, h);
        ActionProposal badGoal = node.getBestProposal(h);
        assertNotEquals(noGoal, badGoal);
        assertEquals(badGoal, node.getCachedProposal());
        assertTrue(noGoal.explore);
        assertEquals(noGoal.cost, h.getHeuristic(1));
        assertEquals(noGoal.action, actions[1]);
        assertFalse(noGoal.infinite);

        //Goal index better - return exploit
        node = new RuleNode(0, actions, 1, new String[] {}, (x) -> new ActionSense(actions[0], null));
        old = node.getCachedProposal();
        node.occurs(5);
        node.reachedGoal(6, h);
        ActionProposal goodGoal = node.getBestProposal(h);
        assertNotEquals(old, goodGoal);
        assertEquals(goodGoal, node.getCachedProposal());
        assertEquals(goodGoal.action, actions[0]);
        assertFalse(goodGoal.infinite);
        assertFalse(goodGoal.explore);
        assertEquals(goodGoal.cost, 1.0);
    }

    @EpSemTest
    public void testProposalInfiniteRecursion(){
        Heuristic h = new TestHeuristic(3);
        Action[] actions = new Action[] {new Action("a"), new Action("b")};
        RuleNode node = new RuleNode(0, actions, RuleNode.DEPTH_LIMIT - 1, new String[] {}, (x) -> {
            return new ActionSense(actions[0], new SensorData(false));
        });
        ActionProposal old = node.getCachedProposal();

        //New node has infinite cost since at the depth limit, so the agent will prefer exploring
        node.occurs(5);
        node.incrementMoveFrequency(actions[0]);
        node.occurs(7);
        node.incrementMoveFrequency(actions[0]);
        ActionProposal explore = node.getBestProposal(h);
        assertNotEquals(old, explore);
        assertEquals(explore, node.getCachedProposal());
        assertTrue(explore.explore);
        assertEquals(explore.action, actions[1]);
        assertFalse(explore.infinite);
        assertEquals(explore.cost, h.getHeuristic(RuleNode.DEPTH_LIMIT - 1));

        //With both nodes, now an infinite cost for any move
        node.updateExtend(actions[1], new SensorData(false), 11);
        ActionProposal infinite = node.getBestProposal(h);
        assertNotEquals(infinite, explore);
        assertEquals(infinite, node.getCachedProposal());
        assertTrue(infinite.infinite);
    }

    @EpSemTest
    public void testProposalRecursionNoSensors(){
        Heuristic h = new TestHeuristic(5);
        Action[] actions = new Action[] {new Action("a"), new Action("b")};
        SensorData data = new SensorData(false);

        RuleNode node = new RuleNode(0, actions, 1, new String[] {}, (x) -> new ActionSense(actions[0], data));
        ActionProposal old = node.getCachedProposal();

        //Create memories
        node.occurs(3);
        node.occurs(7);
        node.updateExtend(actions[0], data, 7);
        node.occurs(8);
        node.updateExtendGoal(actions[0]);
        node.occurs(9);
        node.updateExtendGoal(actions[0]);

        //Exploit should look better
        ActionProposal exploit = node.getBestProposal(h);
        assertNotEquals(old, exploit);
        assertEquals(exploit, node.getCachedProposal());
        assertEquals(exploit.cost, 4.5);
        assertFalse(exploit.infinite);
        assertFalse(exploit.explore);
        assertEquals(exploit.action, actions[0]);

        //Exploit is worse, so explore
        node.occurs(13);
        node.updateExtend(actions[0], data, 13);
        node.occurs(15);
        node.updateExtend(actions[0], data, 15);
        ActionProposal explore = node.getBestProposal(h);
        assertNotEquals(explore, exploit);
        assertEquals(explore, node.getCachedProposal());
        assertEquals(explore.cost, 5.0);
        assertFalse(explore.infinite);
        assertTrue(explore.explore);
        assertEquals(explore.action, actions[1]);

        //Cannot explore, so exploits instead
        node.occurs(17);
        node.updateExtend(actions[1], data, 17);
        node.occurs(19);
        node.updateExtendGoal(actions[1]);
        node.occurs(21);
        node.updateExtend(actions[1], data, 21);
        node.occurs(23);
        node.updateExtendGoal(actions[1]);
        ActionProposal exploitAgain = node.getBestProposal(h);
        assertNotEquals(explore, exploitAgain);
        assertEquals(exploitAgain, node.getCachedProposal());
        assertEquals(exploitAgain.cost, 4.5);
        assertFalse(exploitAgain.infinite);
        assertFalse(exploitAgain.explore);
        assertEquals(exploitAgain.action, actions[1]);
    }

    @EpSemTest
    public void testProposalRecursionWithSensors2(){
        Heuristic h = new TestHeuristic(5);
        String[] sensors = new String[]{"IS_EVEN"};
        String[] empty = new String[]{};
        Action[] actions = new Action[] {new Action("a"), new Action("b")};

        SensorData goal = new SensorData(true);
        goal.setSensor("IS_EVEN", true);
        SensorData odd = new SensorData(false);
        odd.setSensor("IS_EVEN", false);
        SensorData even = new SensorData(false);
        even.setSensor("IS_EVEN", true);

        RuleNode node = new RuleNode(0, actions, 1, sensors, (x) -> {
            if (x < 5) return new ActionSense(actions[0], odd);
            return new ActionSense(actions[1], odd);
        });
        ActionProposal old = node.getBestProposal(h);

        //Create memories for node
        //First, make sure children are created properly by having frequency of two
        node.occurs(0);
        node.occurs(0);

        //Save the children for later use
        RuleNode[] oddChildren = node.updateExtend(actions[0], odd, 0);
        RuleNode[] evenChildren =  node.updateExtend(actions[0], even, 5);

        //Now create the bulk of memories
        for (int i = 0; i < 18; i++){ //Total 20: two occurances above
            node.occurs(0);
        }
        for (int i = 0; i < 16; i++){ //Total 18: one occurance from lookupEpisode on second occurs, one above
            node.updateExtend(actions[0], odd, 0);
        }
        node.updateExtend(actions[0], even, 5); //Total 2: one above


        //Test that sensors are not used for this EV
        ActionProposal noSensor = node.getBestProposal(h);
        assertNotEquals(old, noSensor);
        assertEquals(noSensor, node.getCachedProposal());
        assertEquals(noSensor.action, actions[1]);
        assertEquals(noSensor.explore, true);
        assertEquals(noSensor.infinite, false);
        assertEquals(noSensor.cost, 5.0);
        assertArrayEquals(noSensor.sensorKey, empty);

        //Now create grandchildren
        for (RuleNode child: oddChildren){
            for (int i = 0; i < 8; i++){
                child.updateExtend(actions[0], odd, 0);
                child.updateExtendGoal(actions[1]);
            }
            child.updateExtendGoal(actions[1]); //Has extra a -> odd from lookupEpisode
        }

        for (RuleNode child: evenChildren){ //Also has b -> odd from lookupEpisode (except for no sensor child)
            child.updateExtendGoal(actions[0]);
        }
        RuleNode child = node.getChild(actions[0], empty, 0);
        child.updateExtend(actions[1], odd, 0);

        //Test that sensors are used for this EV
        ActionProposal sensor = node.getBestProposal(h);
        assertNotEquals(noSensor, sensor);
        assertEquals(sensor, node.getCachedProposal());
        assertEquals(sensor.action, actions[0]);
        assertEquals(sensor.explore, false);
        assertEquals(sensor.infinite, false);
        assertArrayEquals(sensor.sensorKey, sensors);
        assertEquals(sensor.cost, 2.469, 0.0001);
    }

    @EpSemTest
    public void testProposalRecursionWithSensors(){
        Heuristic h = new TestHeuristic(5);
        String[] sensors = new String[]{"IS_EVEN"};
        Action[] actions = new Action[] {new Action("a"), new Action("b")};
        SensorData goal = new SensorData(true);
        goal.setSensor("IS_EVEN", true);
        SensorData odd = new SensorData(false);
        odd.setSensor("IS_EVEN", false);
        SensorData even = new SensorData(false);
        even.setSensor("IS_EVEN", true);

        RuleNode node = new RuleNode(0, actions, 1, sensors, (x) -> new ActionSense(actions[0], odd));
        ActionProposal old = node.getBestProposal(h);

        //Create memories
        node.occurs(3);
        node.updateExtend(actions[0], odd, 3);
        node.occurs(5);

        //This call causes the children to create two episodes: one from the lookup function and one from the updateExtend
        RuleNode[] children = node.updateExtend(actions[0], odd, 5);
        assertNotEquals(children.length, 0);
        for(RuleNode child: children){
            RuleNode[] grandchildren = child.updateExtend(actions[1], odd, 6);
            assertNotEquals(grandchildren.length, 0);
            for (RuleNode grandchild: grandchildren){
                grandchild.updateExtend(actions[0], odd, 20); //Causes grandchild to return explore rather than infinite
            }
            if (child.getSensors().length > 0) {
                RuleNode grandchild1 = child.getChild(actions[0], sensors, 0);
                for (int i = 0; i < 15; i++)
                    grandchild1.updateExtend(actions[0], odd, 24);
            }
            RuleNode grandchild2 = child.getChild(actions[0], new String[]{}, 0);
            for (int i = 0; i < 15; i++)
                grandchild2.updateExtend(actions[0], odd, 24);
        }

        //Continue creating memories
        node.occurs(7);
        children = node.updateExtend(actions[0], even, 7);
        for (RuleNode child: children)
            child.updateExtendGoal(actions[0]);
        node.occurs(9);
        node.updateExtend(actions[0], even, 9);
        node.occurs(10);
        node.updateExtendGoal(actions[0]);
        node.occurs(11);
        node.updateExtendGoal(actions[0]);
        node.occurs(12);
        node.updateExtendGoal(actions[0]);
        node.occurs(13);
        node.updateExtendGoal(actions[0]);

        node.occurs(14);
        node.updateExtend(actions[1], odd, 14);

        //IS_EVEN should be better
        ActionProposal exploit = node.getBestProposal(h);
        assertNotEquals(old, exploit);
        assertEquals(exploit, node.getCachedProposal());
        assertEquals(exploit.cost, 5.75);
        assertFalse(exploit.infinite);
        assertFalse(exploit.explore);
        assertEquals(exploit.action, actions[0]);
        assertArrayEquals(exploit.sensorKey, sensors);
    }

    //Test entropy calculation

//
    //region getBestProposal
//
//    @EpSemTest
//    public void testExpectedBaseCases(){
//        Heuristic h = new TestHeuristic(1);
//        Action[] actions = new Action[] {new Action("a"), new Action("b")};
//        RuleNode node = new RuleNode(actions, 0, 2, 0);
//        node.occurs();
//        node.occurs();
//        node.incrementMoveFrequency(actions[0]);
//        node.incrementMoveFrequency(actions[0]);
//        RuleNode depthLimitNode = node.getNextChild(actions[0], 0);
//        depthLimitNode.occurs();
//        RuleNodeGoal goalNode = node.getGoalChild(actions[0]);
//        goalNode.occurs();
//        ArrayList<RuleNode> current = new ArrayList<>();
//        current.add(node);
//
//        //Current not null
//        assertThrows(IllegalArgumentException.class, () -> node.getExpectation(null, false, h));
//
//        //Not in current and not top
//        assertEquals(Optional.empty(), node.getExpectation(current, false, h));
//
//        //At max depth
//        assertEquals(Optional.empty(), depthLimitNode.getExpectation(current, true, h));
//
//        //Is goal
//        assertEquals(Optional.of(0.0), goalNode.getExpectation(current, true, h));
//
//        //Not a base case
//        assertNotEquals(Optional.empty(), node.getExpectation(current, true, h));
//    }
//
//    /**
//     * Tests that the expected value is correct when in must call itself recursively
//     *
//     * TODO: Test these things with root node
//     */
//    @EpSemTest
//    public void testExpectedRecursiveCase(){
//        Action[] actions = new Action[] {new Action("a"), new Action("b")};
//
//        //Root node, makes a twice
//        RuleNode node = new RuleNode(actions, 0, 5, 0);
//        node.occurs();
//        node.occurs();
//        node.incrementMoveFrequency(actions[0]);
//        node.incrementMoveFrequency(actions[0]);
//
//        //Child a node, visited once, made a b
//        RuleNode childNode = node.getNextChild(actions[0], 0);
//        childNode.occurs();
//        childNode.incrementMoveFrequency(actions[1]);
//
//        //Child a goal node, visited once
//        RuleNodeGoal goalGrandChild = childNode.getGoalChild(actions[1]);
//        goalGrandChild.occurs();
//
//        //Grandchild b goal node, child of childNode, visited once
//        RuleNodeGoal goalNode = node.getGoalChild(actions[0]);
//        goalNode.occurs();
//
//        //Current
//        ArrayList<RuleNode> current = new ArrayList<>();
//        current.add(node);
//
//        //Returns 1.5, since it beats the heuristic
//        assertEquals(Optional.of(1.5), node.getExpectation(current, true, new TestHeuristic(2)));
//
//        //Since the heuristic is better, returns that instead
//        assertEquals(Optional.of(1.0), node.getExpectation(current, true, new TestHeuristic(1)));
//    }
//
//    @EpSemTest
//    public void testExpectedSideEffectsRecursiveCase(){
//        Action[] actions = new Action[] {new Action("a"), new Action("b")};
//        RuleNode node = new RuleNode(actions, 0, 5, 0);
//        node.occurs();
//        node.occurs();
//        node.incrementMoveFrequency(actions[0]);
//        node.incrementMoveFrequency(actions[0]);
//        RuleNode childNode = node.getNextChild(actions[0], 0);
//        childNode.occurs();
//        childNode.incrementMoveFrequency(actions[1]);
//        RuleNodeGoal goalGrandChild = childNode.getGoalChild(actions[1]);
//        goalGrandChild.occurs();
//        RuleNodeGoal goalNode = node.getGoalChild(actions[0]);
//        goalNode.occurs();
//        ArrayList<RuleNode> current = new ArrayList<>();
//        current.add(node);
//
//        //When heuristic is greater than best move, explore is false, exploit move and EV is cached
//        assertEquals(Optional.of(1.5), node.getExpectation(current, true, new TestHeuristic(2)));
//        assertEquals(false, node.getExplore());
//        assertEquals(actions[0], node.getBestAction());
//        assertEquals(1.5, node.getCachedExpectation());
//
//        //When heuristic is less, explore is true, explore move and EV cached
//        assertEquals(Optional.of(1.0), node.getExpectation(current, true, new TestHeuristic(1)));
//        assertEquals(true, node.getExplore());
//        assertEquals(actions[1], node.getBestAction());
//        assertEquals(1.0, node.getCachedExpectation());
//    }
//
//    @EpSemTest
//    public void testExpectedSideEffectsBaseCase(){
//        Action[] actions = new Action[] {new Action("a"), new Action("b")};
//        RuleNode node = new RuleNode(actions, 0, 2, 0);
//        node.occurs();
//        node.occurs();
//        node.incrementMoveFrequency(actions[0]);
//        node.incrementMoveFrequency(actions[0]);
//        RuleNode depthLimitNode = node.getNextChild(actions[0], 0);
//        depthLimitNode.occurs();
//        RuleNodeGoal goalNode = node.getGoalChild(actions[0]);
//        goalNode.occurs();
//        ArrayList<RuleNode> current = new ArrayList<>();
//        current.add(node);
//
//        //Expected side effects: explore arbitrary, best move null, and expectation -1 on fail
//
//        //Current not null throws, so no other tested side effects
//
//        //Not in current and not top
//        assertEquals(Optional.empty(), node.getExpectation(current, false, new TestHeuristic(1)));
//        assertNull(node.getBestAction());
//        assertEquals(-1.0, node.getCachedExpectation());
//
//        //At max depth
//        assertEquals(Optional.empty(), depthLimitNode.getExpectation(current, true, new TestHeuristic(1)));
//        assertNull(depthLimitNode.getBestAction());
//        assertEquals(-1.0, depthLimitNode.getCachedExpectation());
//
//        //Is goal. best move null, explore false, expectation 0
//        assertEquals(Optional.of(0.0), goalNode.getExpectation(current, true, new TestHeuristic(1)));
//        assertNull(goalNode.getBestAction());
//        assertEquals(0.0, goalNode.getCachedExpectation());
//        assertEquals(false, goalNode.getExplore());
//    }
//
//endregion

    //region updateExtend
    //check children returned, move frequencies, and children frequencies
    @EpSemTest
    public void testUpdateExtendDepthLimit(){
        Action[] actions = new Action[] {new Action("a"), new Action("b")};
        RuleNode node = new RuleNode(0, actions, 500, new String[] {}, (index) -> {
            assertFalse(true, "lookupEpisode unexpectedly called");
            return null;
        });
        SensorData data = new SensorData(false);
        node.occurs(0);
        RuleNode[] children = node.updateExtend(actions[0], data, 0);
        assertEquals(children.length, 0);
    }

    @EpSemTest
    public void testFrequencyOne(){
        Action[] actions = new Action[] {new Action("a"), new Action("b")};
        RuleNode node = new RuleNode(0, actions, 1, new String[] {}, (index) -> {
            assertFalse(true, "lookupEpisode unexpectedly called");
            return null;
        });
        assertEquals(node.getMoveFrequency(actions[0]), 0);
        SensorData data = new SensorData(false);
        node.occurs(0);
        RuleNode[] children = node.updateExtend(actions[0], data, 0);
        assertEquals(children.length, 0);
        assertEquals(node.getMoveFrequency(actions[0]), 0);
    }

    @EpSemTest
    public void testFrequencyTwoSameData(){
        Action[] actions = new Action[] {new Action("a"), new Action("b")};
        SensorData data = new SensorData(false);
        RuleNode node = new RuleNode(0, actions, 1, new String[] {}, (index) -> new ActionSense(actions[0], data));
        assertEquals(node.getMoveFrequency(actions[0]), 0);
        node.occurs(0);
        RuleNode[] children = node.updateExtend(actions[0], data, 0);
        assertEquals(children.length, 0);
        assertEquals(node.getMoveFrequency(actions[0]), 0);
        node.occurs(1);
        assertEquals(node.getMoveFrequency(actions[0]), 1);
        RuleNode child = node.getChild(actions[0], new String[]{}, 0);
        child.incrementMoveFrequency(actions[0]);
        children = node.updateExtend(actions[0], data, 1);
        assertEquals(children.length, 1);
        assertEquals(children[0].getFrequency(), 2);
        assertEquals(node.getMoveFrequency(actions[0]), 2);
    }

    @EpSemTest
    public void testFrequencyTwoDifferentData(){
        Action[] actions = new Action[] {new Action("a"), new Action("b")};
        SensorData data1 = new SensorData(false);
        SensorData data2 = new SensorData(false);
        RuleNode node = new RuleNode(0, actions, 1, new String[] {"test"}, (index) -> {
            return new ActionSense(actions[0], data1);
        });
        assertEquals(node.getMoveFrequency(actions[0]), 0);
        data1.setSensor("test", false);
        node.occurs(0);
        RuleNode[] children = node.updateExtend(actions[0], data1, 0);
        assertEquals(children.length, 0);
        assertEquals(node.getMoveFrequency(actions[0]), 0);
        data2.setSensor("test", true);
        node.occurs(1);
        assertEquals(node.getMoveFrequency(actions[0]), 1);
        RuleNode child = node.getChild(actions[0], new String[] {"test"}, 0);
        child.incrementMoveFrequency(actions[0]);
        RuleNode child2 = node.getChild(actions[0], new String[]{}, 0);
        child2.incrementMoveFrequency(actions[0]);
        children = node.updateExtend(actions[0], data2, 1);
        assertEquals(children.length, 2);
        assertEquals(children[0].getFrequency(), 2);
        assertArrayEquals(children[0].getSensors(), new String[] {});
        assertArrayEquals(children[1].getSensors(), new String[] {"test"});
        assertEquals(children[1].getFrequency(), 1);
        assertEquals(node.getMoveFrequency(actions[0]), 2);
    }

    @EpSemTest
    public void testFrequencyTwoActions(){
        Action[] actions = new Action[] {new Action("a"), new Action("b")};
        SensorData data = new SensorData(false);
        RuleNode node = new RuleNode(0, actions, 1, new String[] {}, (index) -> {
            assertEquals(index, 0);
            return new ActionSense(actions[0], data);
        });
        assertEquals(node.getMoveFrequency(actions[0]), 0);
        node.occurs(0);
        RuleNode[] children = node.updateExtend(actions[0], data, 0);
        assertEquals(children.length, 0);
        assertEquals(node.getMoveFrequency(actions[0]), 0);
        node.occurs(1);
        assertEquals(node.getMoveFrequency(actions[0]), 1);
        children = node.updateExtend(actions[1], data, 1);
        assertEquals(children.length, 1);
        assertEquals(children[0].getFrequency(), 1);
        assertEquals(node.getMoveFrequency(actions[0]), 1);
        assertEquals(node.getMoveFrequency(actions[1]), 1);
    }
    //endregion

    //region occurs
    @EpSemTest
    public void testOccursDepthLimit(){
        Action[] actions = new Action[] {new Action("a"), new Action("b")};
        RuleNode node = new RuleNode(0, actions, 500, new String[] {}, (index) -> {
            assertFalse(true, "lookupEpisode unexpectedly called");
            return null;
        });
        node.occurs(0);
        node.occurs(1);
    }

    @EpSemTest
    public void testFrequencies(){
        Action[] actions = new Action[] {new Action("a"), new Action("b")};
        RuleNode node = new RuleNode(0, actions, 1, new String[] {}, (index) -> {
            assertEquals(index, 2);
            return new ActionSense(actions[0], new SensorData(false));
        });
        assertEquals(node.getFrequency(), 0);
        assertEquals(node.getChildren(actions[0], new String[] {}).size(), 0);
        node.occurs(2);
        assertEquals(node.getFrequency(), 1);
        assertEquals(node.getChildren(actions[0], new String[] {}).size(), 0);
        assertEquals(node.getMoveFrequency(actions[0]), 0);
        node.incrementMoveFrequency(actions[0]);
        assertEquals(node.getMoveFrequency(actions[0]), 1);
        node.occurs(5);
        assertEquals(node.getMoveFrequency(actions[0]), 2);
        assertEquals(node.getFrequency(), 2);
        assertEquals(node.getChildren(actions[0], new String[] {}).size(), 1);
        //assertEquals(node.getChildren(actions[0], new String[] {}).get(0).getFrequency(), 0);
        node.occurs(10);
        assertEquals(node.getFrequency(), 3);
        assertEquals(node.getChildren(actions[0], new String[] {}).size(), 1);
        //assertEquals(node.getChildren(actions[0], new String[] {}).get(0).getFrequency(), 0);
        assertEquals(node.getMoveFrequency(actions[0]), 2);
    }
    //endregion

//    //region reachedGoal
//
//    @EpSemTest
//    public void testBadMoves(){
//        Action[] actions = new Action[] {new Action("a"), new Action("b")};
//        RuleNode node = new RuleNode(actions, 0, 2, 0);
//        assertThrows(IllegalArgumentException.class, () -> node.getGoalProbability(null, 0));
//        assertThrows(IllegalArgumentException.class, () -> node.getGoalProbability(new ArrayList<Action>(Arrays.asList(new Action("c"))), 0));
//        assertEquals(0.0, node.getGoalProbability(new ArrayList<>(), 0));
//    }
//
//    @EpSemTest
//    public void testBadIndex(){
//        Action[] actions = new Action[] {new Action("a"), new Action("b")};
//        RuleNode node = new RuleNode(actions, 0, 2, 0);
//        ArrayList<Action> sequence = new ArrayList<>(Arrays.asList(actions[0], actions[1], actions[1]));
//        assertEquals(0.0, node.getGoalProbability(sequence, 10)); //Index too high returns 0
//        assertThrows(IllegalArgumentException.class, () -> node.getGoalProbability(sequence, -1)); //Index too low throws
//    }
//
//    @EpSemTest
//    public void testBaseCases(){
//        //Setup
//        Action[] actions = new Action[] {new Action("a"), new Action("b")};
//        ArrayList<Action> sequence = new ArrayList<>(Arrays.asList(actions[0], actions[1]));
//
//        //Max depth returns 0
//        RuleNode leafNode = new RuleNode(actions, 0, 1, 0);
//        assertEquals(0.0, leafNode.getGoalProbability(sequence, 0));
//
//        //Return if move has never been tried
//        RuleNode node = new RuleNode(actions, 0, 3, 0);
//        node.getGoalChild(actions[1]);
//        assertEquals(0.0, node.getGoalProbability(sequence, 0));
//
//        //Return 0 when end of sequence reached
//        RuleNode nextNode = node.getNextChild(actions[0], 0);
//        nextNode.getNextChild(actions[1], 0);
//        assertEquals(0.0, node.getGoalProbability(sequence, sequence.size()));
//
//        //Return 1 if goal node
//        RuleNode goalNode = new RuleNodeGoal(actions, 0);
//        assertEquals(1.0, goalNode.getGoalProbability(sequence, 0));
//    }
//
//    @EpSemTest
//    public void testRecursiveCase(){
//        Action[] actions = new Action[] {new Action("a"), new Action("b")};
//        ArrayList<Action> aSequence = new ArrayList<>(Collections.singletonList(actions[0]));
//        ArrayList<Action> bSequence = new ArrayList<>(Collections.singletonList(actions[1]));
//        ArrayList<Action> twoSequence = new ArrayList<>(Arrays.asList(actions[0], actions[1]));
//        RuleNode node = new RuleNode(actions, 0, 3, 0);
//        RuleNode aChild = node.getNextChild(actions[0], 0);
//        RuleNode bChild = node.getNextChild(actions[1], 0);
//        node.getGoalChild(actions[0]).occurs(); //Goal child with an "a" move
//        node.incrementMoveFrequency(actions[0]);
//        node.getGoalChild(actions[1]).occurs();
//        node.incrementMoveFrequency(actions[1]);
//
//        //Test 1 deep recursion
//        for (int i = 0; i < 9; i++){
//            aChild.occurs();
//            node.incrementMoveFrequency(actions[0]);
//        }
//        for (int i = 0; i < 19; i++){
//            bChild.occurs();
//            node.incrementMoveFrequency(actions[1]);
//        }
//        assertEquals(0.1, node.getGoalProbability(aSequence, 0), 0);
//        assertEquals(0.05, node.getGoalProbability(bSequence, 0), 0);
//
//        //Test 2 deep recursion
//        aChild.getGoalChild(actions[1]).occurs();
//        RuleNode grandchild = aChild.getNextChild(actions[1], 0);
//        aChild.incrementMoveFrequency(actions[1]);
//        for (int i = 0; i < 4; i++){
//            aChild.incrementMoveFrequency(actions[1]);
//            grandchild.occurs();
//        }
//        assertEquals(0.1 + 0.9*0.2, node.getGoalProbability(twoSequence, 0), 0.0001);
//    }
//
//    //TODO: Test root only increases by frequency 1
//
//    //endregion
}
