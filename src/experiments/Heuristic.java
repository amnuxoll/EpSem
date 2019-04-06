package experiments;

public class Heuristic {
    private double alpha;
    private double beta;

    public Heuristic(double alpha, double beta) {
        this.alpha = alpha;
        this.beta = beta;
    }

    public double getHeuristic(double goalProbability, int depth) {
        double heuristicValue = alpha / goalProbability + beta * depth;
        if (heuristicValue < 1) {
            return 1;
        } else {
            return heuristicValue;
        }
    }
}
