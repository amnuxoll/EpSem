package agents.juno;

public class WindowContainsGoalException extends RuntimeException {
    private int goalIndex;

    public WindowContainsGoalException(int goalIndex){
        this.goalIndex= goalIndex;
    }

    public int getGoalIndex(){
        return goalIndex;
    }
}
