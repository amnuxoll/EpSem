package framework;

/**
 * An IEnvironment represents an environment in which some {@link IAgent} may operat.
 *
 * @author Zachary Paul Faltersack
 * @version 0.95
 */
public interface IEnvironment {
    //region Methods
    Action[] getActions();

    SensorData applyAction(Action action);

    SensorData getNewStart();

    default Boolean validateSequence(Sequence sequence) {return null;}

//    @Override
//    public boolean validateSequence(Sequence sequence) {
//        if (sequence == null)
//            throw new IllegalArgumentException("sequence cannot be null.");
//        int tempState = this.currentState;
//        for (Action action : sequence.getActions()){
//            TransitionResult result = this.environmentDescription.transition(tempState, action);
//            if (result.getSensorData().isGoal())
//                return true;
//            tempState = result.getState();
//        }
//        return false;
//    }
    //endregion
}
