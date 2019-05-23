package agents.marzrules;

import agents.marzrules.Heuristic;

/**
 * Created by Ryan on 4/11/2019.
 */
public class TestHeuristic extends Heuristic {

    private double c;

    public TestHeuristic(double constant){
        super(0, 0);
        c = constant;
    }

    @Override
    public double getHeuristic(int depth){
        return c;
    }
}
