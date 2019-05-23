package agents.marzrules;

public class Heuristic {
    private final double alpha;
    private final double beta;
    private double p = 0.0;

    public Heuristic(double alpha, double beta) {
        this.alpha = alpha;
        this.beta = beta;
    }

    public void setGoalProbability(double p){
        this.p = p;
    }

    public double getHeuristic(int depth) {
        double heuristicValue = alpha / p + beta * depth;
        if (heuristicValue < 1) {
            return 1;
        } else {
            return heuristicValue;
        }
    }
}
