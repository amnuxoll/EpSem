package agents.phujus;

import framework.SensorData;
import java.util.HashMap;
import java.util.Random;

/**
 * class Rule
 *
 * describes a single rule in the PhuJusAgent
 */

public class Rule {
//    /** probability of a new rule getting an extra condition */
//    private static final double EXTRA_COND_FREQ = 0.67;
//
//    /** base reward for a rule that correctly predicts finding the goal. */
//    public static final double FOUND_GOAL_REWARD = 20.0;
//
//    //to assign a unique id to each rule this shared variable is incremented by the ctor
//    private static int nextRuleId = 1;
//
//    //same as the agent so it can be seeded for consistent behavior
//    private static final Random rand = PhuJusAgent.rand;
//
//    //The agent using this rule
//    private final PhuJusAgent agent;
//
//    //each rule has a unique integer id
//    private final int ruleId;
//
//    //define the LHS of the rule.  This consists of:
//    // - 0 or more internal sensors that have a particular value (0 or 1)
//    // - 1 or more values for external sensors
//    private HashMap<Integer, Boolean> lhsInternal = new HashMap<>();
//    private SensorData  lhsExternal = null;  //example: {1,-1,0,-1}  note: -1 is "wildcard"
//    private final char action;
//
//    //define the RHS of the rule.  The RHS always contains:
//    // - a value for exactly one external sensor
//    // - optionally, an internal sensor that turns on ('1') when this rule matched in prev episode
//    private SensorData rhsExternal = null;
//    private int rhsInternal = -1;  //index into internal sensors *or* -1 if none
//
//    // Each rule has an activation level that tracks the frequency and
//    // recency with which it fired and correctly predicted an external
//    // sensor value
//    public static final int ACTHISTLEN = 10;
//    private final int[] lastActTimes = new int[ACTHISTLEN];  // last N times the rule was activated
//    private final double[] lastActAmount = new double[ACTHISTLEN]; // amount of activation last N times
//    private int nextActPos = 0;
//    private double activationLevel;  //CAVEAT:  this value may not be correct!  Call calculateActivation() to update it.
//    public static final double DECAY_RATE = 0.95;  //activation decays exponentially over time
//
//    // Track the accuracy of this rule.  numMatches is how often it has matched (fired).
//    // numPredicts is how many times it matched and correctly predicted the next step.
//    // These values are init'd to 1 to account for the episodes the rule is created from
//    // (also prevents divide-by-zero errors).
//    private double numMatches = 1;
//    private double numPredicts = 1;
//
//    /**
//     * this ctor initializes the rule with given values
//     */
//    public Rule(PhuJusAgent agent, char act, SensorData currExt, HashMap<Integer, Boolean> currInt, String rhsSensorName, boolean rhsValue){
//        this.agent = agent;
//        this.ruleId = Rule.nextRuleId++;
//        this.action = act;
//        this.lhsExternal = currExt;
//        this.lhsInternal = currInt;
//        this.rhsExternal = SensorData.createEmpty();
//        this.rhsExternal.setSensor(rhsSensorName, rhsValue);
//    }
//
//
//    /** ctor to create random rule.  This add time idx for all previous sensors in previous episode */
//    public Rule(PhuJusAgent agent){
//        this.agent = agent;
//        this.ruleId = Rule.nextRuleId++;
//
//        //pick the agent's previous action for the new rule
//        this.action = agent.getPrevAction();
//
//        pickRandomLHS();
//        pickRandomRHS();
//    }//random ctor
//
//    /**
//     * softmaxSelect
//     *
//     * randomly selects one external sensor from a SensorData using a softmax formula.
//     * The selection is weighted so that rarer sensor values are more likely to be selected.
//     * https://en.wikipedia.org/wiki/Softmax_function
//     *
//     * @param src the SensorData to select from
//     * @return the name of the selected sensor
//     */
//    private String softmaxSelect(SensorData src) {
//        double sum = 0.0;
//        for(String sName : src.getSensorNames()) {
//            boolean val = (Boolean) src.getSensor(sName);
//            double rarity = agent.getExternalRarity(sName, val);
//            if (rarity >= 0.0) {
//                sum += Math.pow(Math.E, 1.0 - rarity);
//            }
//        }
//
//        double random = rand.nextDouble();
//
//        for(String sName : src.getSensorNames()) {
//            boolean val = (Boolean) src.getSensor(sName);
//            double rarity = agent.getExternalRarity(sName, val);
//            if (rarity >= 0.0) {
//                double weight = (rarity >= 0.0) ? 1.0 - rarity : 0.0;
//                random -= (Math.pow(Math.E, weight) / sum);
//                if (random <= 0.0) {
//                    return sName;
//                }
//            }
//        }
//
//
//        //Note:  We should only reach this point in the code at start of simulation, when there
//        // are no rarity values on any external sensor.  This would be indicated by (sum == 0.0).
//        //In this special case, just select randomly
//        int selIndex = rand.nextInt(src.getSensorNames().size());
//        return src.getSensorNames().toArray(new String[0])[selIndex];
//    }//softmaxSelect
//
//    /**
//     * softmaxSelect
//     *
//     * randomly selects one internal sensor from a hashmap using a softmax formula.
//     * The selection is weighted so that rarer sensor values are more likely to be selected.
//     * See https://en.wikipedia.org/wiki/Softmax_function
//     *
//     * @param src the internal sensor hashmap to select from
//     * @return the index of the selected sensor or -1 if none selected
//     */
//    private int softmaxSelect(HashMap<Integer, Boolean> src) {
//        double sum = 0.0;
//        for(Integer i : src.keySet()) {
//            boolean val = src.get(i);
//            double rarity = agent.getInternalRarity(i, val);
//            if (rarity >= 0.0) {
//                sum += Math.pow(Math.E, 1.0 - rarity);
//            }
//        }
//
//        double random = rand.nextDouble();
//
//        for(Integer i : src.keySet()) {
//            boolean val = src.get(i);
//            double rarity = agent.getInternalRarity(i, val);
//            if (rarity >= 0.0) {
//                double weight = (rarity >= 0) ? 1.0 - rarity : 0.0;
//                random -= (Math.pow(Math.E, weight) / sum);
//                if(random <= 0.0) {
//                    return i;
//                }
//            }
//        }
//
//        //Note:  We should only reach this point in the code at start of simulation, when there
//        // are no rarity values on any internal sensor.  This would be indicated by (sum == 0.0).
//        //In this special case, just select randomly
//        int selIndex = rand.nextInt(src.keySet().size());
//        return src.keySet().toArray(new Integer[0])[selIndex];
//    }//softmaxSelect
//
//    /**
//     * copyOneSensorVal
//     *
//     * copies a random entry from one SensorData to another.  This method
//     * fails silently if dest isn't smaller than src (which presumably
//     * means that all the entries in src are already in dest).
//     *
//     * @param src  copy an entry from here
//     * @param dest to here
//     */
//    private void copyOneSensorVal(SensorData src, SensorData dest) {
//        //Special case:  nothing left to copy
//        if (src.size() == dest.size()) return;
//
//        //select an entry from the src
//        String sName;
//        do {
//            sName = softmaxSelect(src);
//        } while(dest.hasSensor(sName));
//
//        //Copy the entry
//        Boolean val = (Boolean) src.getSensor(sName);
//        dest.setSensor(sName, val);
//    }//copyOneSensorVal
//
//    /**
//     * addExternalCondition
//     *
//     * selects a random external sensor condition for this rule.  This is
//     * always selected from the previous external sensor values to ensure
//     * that the selected condition is possible.  The method verifies that
//     * the condition is unique.
//     *
//     * CAVEAT:  if a new condition can't be added this method does nothing
//     *
//     */
//    public void addExternalCondition() {
//        //see if lhsExternal needs to be initialized
//        if(this.lhsExternal == null) {
//            this.lhsExternal = SensorData.createEmpty();
//        }
//
//        copyOneSensorVal(this.agent.getPrevExternal(), this.lhsExternal);
//
//    }//addExternalCondition
//
//    /**
//     * addInternalCondition
//     *
//     * same as {@link #addExternalCondition()} but for an internal condition
//     *
//     */
//    public void addInternalCondition() {
//        //Special case:  this rule is already using all the internal sensors
//        Integer[] options = agent.getPrevInternalKeys();
//        if (options.length == 0) return;
//        if (this.lhsInternal.size() == options.length) return;
//
//        //select a random internal sensor that's not already being used by this rule
//        int newCond;
//        do {
//            newCond = softmaxSelect(agent.getPrevInternal());
//        } while(this.lhsInternal.containsKey(newCond));
//
//        //Add the condition
//        Boolean val = agent.getPrevInternalValue(newCond);
//        this.lhsInternal.put(newCond, val);
//    }//addInternalCondition
//
//    /**
//     * pickRandomLHS
//     *
//     * picks random lhs external or internal sensor and adds it to the lhs
//     * of this rule. The value that it assigns to that sensor in this rule
//     * will be set to match a given episode. This method takes care not to
//     * add a sensor that's already present i.e. no duplicates
//     */
//    public void pickRandomLHS() {
//        // ensure at least one external condition
//        addExternalCondition();
//
//        //Add other conditions sometimes
//        while(rand.nextDouble() < EXTRA_COND_FREQ) {
//            //flip a coin: external or internal
//            if (rand.nextInt(2) == 0) {
//                addExternalCondition();
//            } else {
//                addInternalCondition();
//            }
//        }
//    }//pickRandomLHS
//
//    /**
//     * same as pickRandomLHS but for the RHS
//     */
//    private void pickRandomRHS() {
//        this.rhsExternal = SensorData.createEmpty();
//        copyOneSensorVal(agent.getCurrExternal(), this.rhsExternal);
//    }//pickRandomRHS
//
//    /**
//     * general format: [rule id]: ([internal lhs])|[external lhs] -> ([int rhs])|[ext rhs] ^ [activation] * [accuracy]
//     */
//    @Override
//    public String toString() {
//        //rule number
//        StringBuilder result = new StringBuilder();
//        result.append("#" + this.ruleId + ": ");
//
//        //LHS internal sensors
//        result.append('(');
//        int count = 0;
//        for(Integer i : this.lhsInternal.keySet()){
//            if (count > 0) result.append(',');
//            count++;
//            if (! this.lhsInternal.get(i)) result.append('!');
//            result.append(String.valueOf(i.intValue()));
//        }
//        result.append(")|");
//
//        //LHS external sensors
//        if(this.lhsExternal != null) {
//            for (String sName : this.lhsExternal.getSensorNames()) {
//                result.append('(' + sName + ',' + this.lhsExternal.getSensor(sName) + ')');
//            }
//        }
//        //action and arrow
//        result.append(this.action);
//        result.append(" -> ");
//
//        //RHS internal sensor
//        result.append("(");
//        if (this.rhsInternal != -1) {
//            result.append(this.rhsInternal);
//        }
//        result.append(")|");
//
//        //RHS external sensors
//        for(String s : this.rhsExternal.getSensorNames()) {
//            result.append('(' + s + ',' + this.rhsExternal.getSensor(s) + ')');
//        }
//
//        //activation and accuracy
//        result.append(" ^ " + String.format("%.3e", this.getActivationLevel()));
//        result.append(" * " + String.format("%.2f", this.getAccuracy()));
//
//        return result.toString();
//    }//toString
//
//    /**
//     * calculateActivation
//     *
//     * calculates the activation of the rule atm.  Activation is increased
//     * by fixed amounts and each increase decays over time.
//     * The sum of these values is the total activation.
//     *
//     * @see #addActivation(int, double)
//     */
//    public double calculateActivation(int now) {
//        double result = 0.0;
//        for(int j=0; j < lastActTimes.length; ++j) {
//            if(lastActTimes[j] != 0) {
//                double decayAmount = Math.pow(DECAY_RATE, now-lastActTimes[j]);
//                result += lastActAmount[j]*decayAmount;
//            }
//        }
//
//        this.activationLevel = result;
//        return this.activationLevel;
//    }//calculateActivation
//
//    /**
//     * addActivation
//     *
//     * adds a new activation event to this rule.
//     *
//     * @param now time of activation
//     * @param reward amount of activation (can be negative to punish)
//     *
//     * @return true if the reward was applied
//     */
//    public boolean addActivation(int now, double reward) {
//        //Check: rule can't be activated twice in the same timestep
//        int prevIdx = this.nextActPos - 1;
//        if (prevIdx < 0) prevIdx = this.lastActTimes.length - 1;
//        if (lastActTimes[prevIdx] == now) {
//            if (lastActAmount[prevIdx] < reward) {
//                this.lastActAmount[prevIdx] = reward;
//                return true;
//            }
//            return false;
//        }
//
//        this.lastActTimes[this.nextActPos] = now;
//        this.lastActAmount[this.nextActPos] = reward;
//        this.nextActPos = (this.nextActPos + 1) % ACTHISTLEN;
//        return true;
//    }
//
//    /**
//     * matches
//     *
//     * compare this rule's LHS to give sensors
//     *
//     * @return true if this rule matches the given action and sensor values
//     */
//    public boolean matches(char action, SensorData currExternal, HashMap<Integer, Boolean> currInternal)
//    {
//        //action match
//        if (this.action != action) { return false; }
//
//        //internal sensors match
//        if(this.lhsInternal != null) {
//            for (Integer i : this.lhsInternal.keySet()) {
//                if (this.lhsInternal.get(i) != currInternal.get(i)) {
//                    return false;
//                }
//            }
//        }
//
//        //external sensors match
//        if(this.lhsExternal != null) {
//            for (String sname : this.lhsExternal.getSensorNames()) {
//                if (!this.lhsExternal.getSensor(sname)
//                        .equals(currExternal.getSensor(sname))) {
//                    return false;
//                }
//            }
//        }
//
//        //all tests passed
//        return true;
//    }//matches
//
//    /**
//     * compare this rule's LHS and RHS to given sensors
//     *
//     * @return true if LHS matches and correctly predicts the RHS
//     */
//    public boolean predicts(char action, SensorData currExternal, HashMap<Integer, Boolean> currInternal, SensorData correctRHS)
//    {
//        //LHS
//        if (! matches(action, currExternal, currInternal)) return false;
//
//        //nothing to compare to
//        if (this.rhsExternal == null) return false;
//        if (correctRHS == null) return true;
//
//        //RHS
//        for (String s : this.rhsExternal.getSensorNames()) {
//            if (!rhsExternal.getSensor(s).equals(correctRHS.getSensor(s))) {
//                return false;
//            }
//        }
//        return true;
//    }//predicts
//
//    /**
//     * equals
//     *
//     * checks for rule equivalence on both LHS and RHS
//     * Note: rhsInternal is not compared, and activation levels are ignored
//     */
//    @Override
//    public boolean equals(Object obj) {
//        // If it references itself, then return a true
//        if (obj == this) return true;
//
//        // If a non-rule is passed in, return false
//        if (!(obj instanceof Rule)) return false;
//        Rule other = (Rule) obj;
//
//        //check for any missing values
//        if ((other.lhsInternal == null) || (other.lhsExternal == null) || (other.rhsExternal == null)
//         || (this.lhsInternal == null)  || (this.lhsExternal == null)  || (this.rhsExternal == null)) {
//                throw new IllegalArgumentException("Rule.equals() called with or on an incomplete Rule object.");
//        }
//
//        //sensor sets should be the same length
//        if (other.lhsInternal.size() != this.lhsInternal.size()) return false;
//        if (other.lhsExternal.size() != this.lhsExternal.size()) return false;
//        if (other.rhsExternal.size() != this.rhsExternal.size()) return false;
//
//        //The should fully match each other
//        return this.predicts(other.action, other.lhsExternal, other.lhsInternal, other.rhsExternal);
//    }//equals
//
////region Getters and Setters
//    /** CAVEAT:  this value may not have been updated in the current timestep */
//    public double getActivationLevel() {
//        return this.activationLevel;
//    }
//
//    public int getRHSInternal(){
//        return this.rhsInternal;
//    }
//
//    public void setRHSInternal(int newInt) {
//        this.rhsInternal = newInt;
//    }
//
//    public int getAssocSensor() {
//        return this.rhsInternal;
//    }
//
//    public SensorData getLHSExternal() { return this.lhsExternal; }
//
//    public HashMap<Integer, Boolean> getLHSInternal() { return this.lhsInternal; }
//
//    public String getRHSSensorName(){
//        for(String s : this.rhsExternal.getSensorNames()){
//            return s;
//        }
//        return "";
//    }
//
//    public boolean getRHSSensorValue(){
//        for(String s : this.rhsExternal.getSensorNames()) {
//            return (Boolean) this.rhsExternal.getSensor(s);
//        }
//        return false;
//    }
//
//    /** returns the RHS sensor value as an int (0 or 1)
//     * Used by TreeNode.pneHelper().
//     */
//    public int getRHSIntValue(){
//        String[] sNames = this.rhsExternal.getSensorNames().toArray(new String[0]);
//        if (sNames.length != 1) {
//            throw new IllegalArgumentException("getRHSIntValue() called on a Rule with a bad RHS");
//        }
//        Boolean val = (Boolean)this.rhsExternal.getSensor(sNames[0]);
//        return (val) ? 1 : 0;
//    }
//
//    public void incrMatches() {
//        numMatches++;
//    }
//
//    public void incrPredicts() {
//        numPredicts++;
//    }
//
//    public double getAccuracy() {
//        return numPredicts / numMatches;
//    }//getAccuracy
//
//    //endregion
//
//
//
}//class Rule
