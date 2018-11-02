package environments.meta;

import framework.*;
import utils.Sequence;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class MetaEnvironmentDescription implements IEnvironmentDescription {
    private IEnvironmentDescriptionProvider environmentDescriptionProvider;
    private IEnvironmentDescription currDescription;

    private List<IEnvironmentListener> listeners= new ArrayList<>();

    //how many transitions the agent took to reach the goal
    private LinkedList<Integer> successQueue= new LinkedList<>();

    //number of moves since last goal
    private int transitionCounter= 0;
    private int numGoals= 0;
    private MetaConfiguration config;

    MetaEnvironmentDescription(IEnvironmentDescriptionProvider environmentDescriptionProvider, MetaConfiguration config) {

        if(environmentDescriptionProvider == null){
            throw new IllegalArgumentException("environmentDescriptionProvider cannot be null");
        }
        if(config == null){
            throw new IllegalArgumentException("config cannot be null");
        }

        this.environmentDescriptionProvider = environmentDescriptionProvider;

        this.currDescription = environmentDescriptionProvider.getEnvironmentDescription();

        this.config= config;
    }

    @Override
    public Move[] getMoves() {
        return currDescription.getMoves();
    }

    /**
     * query to determine result of a move
     * @param currentState The state to transition from.
     * @param move The move to make from the current state.
     * @return The state resulting from the move from currentState
     */
    @Override
    public int transition(int currentState, Move move) {
        //check for illegal arg
        if(move == null){
            throw new IllegalArgumentException("move cannot be null");
        }
        if(currentState < 0 || currentState >= currDescription.getNumStates()) {
            throw new IllegalArgumentException("currentState out of range");
        }

        transitionCounter++;
        return currDescription.transition(currentState,move);
    }

    @Override
    public boolean isGoalState(int state) {
        boolean isGoal= currDescription.isGoalState(state);

        if(isGoal) {
            //makeNewDescription relies on transitionCounter,
            //so do this first
            numGoals++;
            makeNewDescription();
            transitionCounter= 0;
        }

        return isGoal;
    }

    @Override
    public int getNumStates() {
        return currDescription.getNumStates();
    }

    @Override
    public int getNumGoalStates(){ return currDescription.getNumGoalStates(); }

    @Override
    public void applySensors(int lastState, Move move, int currentState, SensorData sensorData) {
        if(sensorData == null) throw new IllegalArgumentException("sensor data cannot be null.");
        if(move == null)
            throw new IllegalArgumentException("move cannot be null");

        if(lastState <0 || lastState >= currDescription.getNumStates())
            throw new IllegalArgumentException("lastState number out of range.");
        if(currentState <0 || currentState >= currDescription.getNumStates())
            throw new IllegalArgumentException("currentState number out of range.");

        currDescription.applySensors(lastState,move,currentState,sensorData);
    }

    /**
     * takes the average of the last successQueueSize transition counts
     * and update the fsm description is the target threshold is reached
     */
    private void makeNewDescription() {
        successQueue.add(transitionCounter);
        while (successQueue.size() > config.getSuccessQueueMaxSize()) {
            successQueue.remove();
        }

        int average= averageEnqueuedSuccesses();

        //if the average is less than our threshold and we have collected enough data
        if (numGoals%config.getTweakPoint() == 0){

            //make a new environment
            currDescription = environmentDescriptionProvider.getEnvironmentDescription();
            //tell any listeners that we changed the environment
            fireDataBreakEvent();
            //and clear the queue
            successQueue.clear();
        }
    }

    /**
     *
     * @return the average of the number of moves to goal in the successQueue
     *          or -1 if the queue is empty
     */
    public int averageEnqueuedSuccesses(){
        if(successQueue.size() == 0) return -1;

        int sum= 0;
        for(Integer tc : successQueue){
            sum+= tc;
        }
        return sum/successQueue.size();
    }

    /**
     *
     * @return number of moves since last goal
     */
    public int getTransitionCounter() {
        return transitionCounter;
    }

    /**
     *
     * @return the last several number of moves it took to reach the goal
     */
    public LinkedList<Integer> getSuccessQueue() {
        return successQueue;
    }

    @Override
    public synchronized void addEnvironmentListener(IEnvironmentListener listener) {
        this.listeners.add(listener);
    }

    @Override
    public boolean validateSequence(int state, Sequence sequence) {
        return currDescription.validateSequence(state, sequence);
    }

    private synchronized void fireDataBreakEvent() {
        EnvironmentEvent event= new EnvironmentEvent(this, EnvironmentEvent.EventType.DATA_BREAK);
        for (IEnvironmentListener listener : this.listeners) {
            //we want like 8 data breaks
            for(int i=0;i<8;i++) {
                listener.receiveEvent(event);
            }
        }
    }
}