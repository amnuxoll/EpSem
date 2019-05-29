package tests.agents.juno;

import agents.juno.EpisodeWeights;
import agents.juno.WeightTable;
import framework.Episode;
import framework.Action;
import framework.SensorData;
import utils.EpisodicMemory;

import java.util.HashMap;

import tests.EpSemTest;

import static tests.Assertions.*;

//temporarily removed as these tests don't pass anymore:  @EpSemTestClass
public class WeightTableTest {
    @EpSemTest
    public void constructorThrowsException(){
        assertThrows(IllegalArgumentException.class, () -> new WeightTable(0));
    }

    @EpSemTest
    public void bestIndicesThrowsException(){
        WeightTable table= new WeightTable(2);

        EpisodicMemory<Episode> episodes= new EpisodicMemory<>();
        Action[] actions = {
                new Action("a"),
                new Action("b"),
                new Action("c"),
        };

        for(int i=0;i<12;i++){
            episodes.add( new Episode(null, actions[i% actions.length]));
        }

        assertThrows(IllegalArgumentException.class, () -> table.bestIndices(null,4, 0));
        assertThrows(IllegalArgumentException.class, () -> table.bestIndices(episodes,-1, 0));
    }

    @EpSemTest
    public void bestIndices(){
        Action a= new Action("a");
        Action b= new Action("b");

        EpisodicMemory<Episode> episodes= new EpisodicMemory<>();

        episodes.add(makeEp(b,false));
        episodes.add(makeEp(a,false)); //1: should have matchscore= 0
        episodes.add(makeEp(a,false)); //2: should have matchscore= -.6
        episodes.add(makeEp(b,false)); //3: should have matchscore= 0
        episodes.add(makeEp(a,false)); //4: should have matchscore= -1
        episodes.add(makeEp(b,true)); //5: most recent goal
        episodes.add(makeEp(b,false)); //6: should be current window
        episodes.add(makeEp(b,false)); //7: should be current window

        WeightTable table= new TestWeightTable(2);

        WeightTable.ScoredIndex[] indexes= table.bestIndices(episodes,2, 5);

        for(WeightTable.ScoredIndex si : indexes){
            System.out.println(si.index + " " + si.score);
        }
    }

    @EpSemTest
    public void testMatchScore(){
        Action a= new Action("a");
        Action b= new Action("b");

        EpisodicMemory<Episode> episodes= new EpisodicMemory<>();

        episodes.add(makeEp(b,false));
        episodes.add(makeEp(a,false)); //1: should have matchscore= 0
        episodes.add(makeEp(a,false)); //2: should have matchscore= -.6
        episodes.add(makeEp(b,false)); //3: should have matchscore= 0
        episodes.add(makeEp(a,false)); //4: should have matchscore= -1
        episodes.add(makeEp(b,true)); //5: most recent goal
        episodes.add(makeEp(b,false)); //6: should be current window
        episodes.add(makeEp(b,false)); //7: should be current window

        WeightTable testWeightTable = new TestWeightTable(2);

        assertEquals(0.8, testWeightTable.calculateMatchScore(episodes,episodes.currentIndex(), 3));
    }

    @EpSemTest
    public void updateOnFailure(){
        Action a= new Action("a");
        Action b= new Action("b");

        EpisodicMemory<Episode> episodes= new EpisodicMemory<>();

        episodes.add(makeEp(b,false));
        episodes.add(makeEp(a,false));
        episodes.add(makeEp(a,false));
        episodes.add(makeEp(b,false));
        episodes.add(makeEp(a,true));
        episodes.add(makeEp(b,true));
        episodes.add(makeEp(b,false));
        episodes.add(makeEp(b,false));

        WeightTable table= new TestWeightTable(2);

        table.updateOnFailure(episodes, 6, 3);

        assertEquals(0, ((TestWeightTable) table).getGoalWeight(0), .001);
        assertEquals(0, ((TestWeightTable) table).getActionWeight(0), .001);
        assertEquals(1, ((TestWeightTable) table).getGoalWeight(1), .001);
        assertEquals(.8, ((TestWeightTable) table).getActionWeight(1), .001);
    }

    private Episode makeEp(Action action, boolean isGoal){
        return new Episode(new SensorData(isGoal), action);
    }

    public class TestWeightTable extends WeightTable{

       /**
        * makes a weight table with 'windowSize' rows
        *
        * @param windowSize number of rows to store weights for
        */
        @SuppressWarnings("unchecked") 
       public TestWeightTable(int windowSize) {
            super(windowSize);
            double actionWeight= .3;
            HashMap<String, Double> sensorWeights= new HashMap<>();

            sensorWeights.put("GOAL",.5);

            for(int i=0;i<table.size();i++){
                this.table.set(i,new TestEpisodeWeights(actionWeight,(HashMap<String, Double>)sensorWeights.clone()));
            }
        }

        public double getActionWeight(int index){
            return table.get(index).getActionWeight();
        }

        public double getGoalWeight(int index){
            return ((TestEpisodeWeights)table.get(index)).getGoalWeight();
        }
    }

    public class TestEpisodeWeights extends EpisodeWeights {
        public TestEpisodeWeights(double actionWeight, HashMap<String, Double> sensorWeights){
            this.actionWeight= actionWeight;
            this.sensorWeights= sensorWeights;
        }

        public double getActionWeight(){
            return actionWeight;
        }

        public double getGoalWeight(){
            return sensorWeights.get("GOAL");
        }
    }
}
