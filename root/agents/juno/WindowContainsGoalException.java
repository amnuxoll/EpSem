package agents.juno;

public class WindowContainsGoalException extends RuntimeException {
    //region Class Variables
    private int goalIndex;
    //endregion

    //region Constructors
    public WindowContainsGoalException(int goalIndex){
        this.goalIndex= goalIndex;
    }
    //endregion

    //region Public Methods
    public int getGoalIndex(){
        return goalIndex;
    }
    //endregion
}
