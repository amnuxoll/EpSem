package experiments;

public class Heuristic {
    private double alpha;
    private double beta;

    public Heuristic(double alpha, double beta) {
        this.alpha = alpha;
        this.beta = beta;
    }

    public double getHeuristic(double goalProbability, int depth) {
        return alpha / goalProbability - beta * depth;
    }
}
