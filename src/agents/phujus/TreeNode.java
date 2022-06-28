package agents.phujus;

import framework.SensorData;

import java.util.*;

/**
 * class TreeNode
 * <p>
 * Each instance is a node in an N-ary tree where N is the number of actions (i.e., the FSM's
 * alphabet) that tries to predict outcomes of sequences of actions.  Thus, we can find a
 * "best" (shortest, most confident) sequence of actions to reach the goal
 */

public class TreeNode {

    //Agent's current state and ruleset is needed to build the tree
    private final PhuJusAgent agent;

    // all rules in the system
    Hashtable<Integer, Rule> rules;

    //associated timestep for this node
    private final int episodeIndex;

    //the action that led to this node
    private char action;

    //sensor values for this node
    private final HashSet<Integer> currInternal;
    private final SensorData currExternal;

    //This is the parent node
    private final TreeNode parent;

    //child nodes
    private final Vector<TreeNode> children = new Vector<>();;

    //bool for if tree has children or not
    private boolean isLeaf = false;

    //This is the path used to reach this node
    private final Vector<TreeNode> path;

    //The same path expressed as a String of actions
    private final String pathStr;

    //A measure from 0.0 to 1.0 of how confident the agent is that this path is correct
    private double confidence;

    //A list of all possible external sensor combinations for this agent
    // e.g., 000, 001, 010, etc.
    static String[] comboArr = null;  //init'd by 1st ctor call
    /**
     * This root node constructor is built from the agent.
     *
     */
    public TreeNode(PhuJusAgent initAgent) {
        //initializing agent and its children
        this.agent = initAgent;
        this.rules = agent.getRules();
        this.parent = null;
        this.episodeIndex = agent.getNow();
        this.action = '\0';
        this.currInternal = agent.getCurrInternal();
        this.currExternal = agent.getCurrExternal();
        this.path = new Vector<>();
        this.pathStr = "";
        //full confidence because drawn directly from agent's present sensing
        this.confidence = 1.0;

        if (TreeNode.comboArr == null) {
            TreeNode.initComboArr(this.currExternal.size());
        }
    }

    /**
     * constructs a treenode with given sensors and action
     * @param parent
     * @param action action taken to reach this node
     * @param newExternal predicted external sensors for this node
     * @param confidence confidence in newExternal
     *
     */
    public TreeNode(TreeNode parent, char action, SensorData newExternal, double confidence) {
        //initializing agent and its children
        this.agent = parent.agent;
        this.rules = parent.rules;
        this.parent = parent;
        this.episodeIndex = parent.episodeIndex + 1;
        this.action = action;
        this.currInternal = agent.genNextInternal(action, parent.currInternal, parent.currExternal, newExternal);
        this.currExternal = newExternal;
        this.path = new Vector<>(parent.path);
        this.path.add(this);
        this.pathStr = parent.pathStr + action;
        this.confidence = parent.confidence * confidence;
    }

    /**
     * initComboArr
     *
     * initializes TreeNode.comboArr static variable
     *
     * @param numExt  number of external sensors the agent has
     */
    private static void initComboArr(int numExt) {
        //Create arrays to store a string rep and confidence value for each possible sensor combination
        int numCombos = (int)Math.pow(2, numExt);
        TreeNode.comboArr = new String[numCombos];
        for(int i = 0; i < numCombos; ++i) {
            String combo = Integer.toBinaryString(i);
            while (combo.length() < numExt) {
                combo = "0" + combo;
            }
            TreeNode.comboArr[i] = combo;
        }
    }//initComboArr

    /**
     * predicts what the externals sensors will be for a child node of this one
     *
     * TODO:  remove?  Currently this is replaced by the bloc-voting method but
     *        not sure it's better.  So we're keeping this method here for now.
     *
     * @param action  the action taken to reach the child
     * @param predicted  this object should have the desired external sensors
     *                   names.  The values will changed by the method based
     *                   on the rule votes.  (Note:  Probably just pass in
     *                   currExternal from an existing node.)
     * @return how confident we are in this prediction
     */
    public double predictExternalByVote(char action, SensorData predicted) {
        int numExtSensors = predicted.size();
        String[] sensorNames = predicted.getSensorNames().toArray(new String[0]);
        //first index: which sensor; second index: false/true
        double[][] votes = new double[numExtSensors][2];

        //Let each rule vote based on its match score
        for(TFRule rule: this.agent.getTfRules()) {
            if(rule.getAction() != action) continue;

            double score = rule.lhsMatchScore(action,this.currInternal, this.currExternal);

            for(int j = 0; j < sensorNames.length; j++){
                boolean on = (boolean) rule.getRHSExternal().getSensor(sensorNames[j]);
                votes[j][on ? 1 : 0] += score;
            }
        }

        //Calculate external values based on votes (winner take all)
        double confidence = 1.0;  //let's be optimistic to start...
        for(int j = 0; j < numExtSensors; j++) {
            predicted.setSensor(sensorNames[j], (votes[j][0] <= votes[j][1]));

            //indiv sensor confidence is based on fraction of votes received for
            // that sensor value (e.g., 3.61 "no" votes and 19.3 "yes" votes
            // = 0.84 confidence in "yes")
            //overall confidence is the product of confidence in each indiv sensor
            //TODO:  should we use an average instead of a product??
            double totalVotes = (votes[j][0] + votes[j][1]);
            if (totalVotes > 0.0) {
                confidence *= Math.max(votes[j][0], votes[j][1]) / totalVotes;
            } else {
                confidence = 0.0;  // no votes == no confidence
            }
        }

        return confidence;
    }//predictExternalByVote


    /**
     * predictExternalByBloc
     *
     * predicts what the externals sensors will be for a child node of this one
     * by going through the rule list and summing up each bloc's votes for external
     * sensors. The highest voted state for each sensor will be selected. A bloc is
     * determined by external sensors and action for a given rule. ie: 00a -> 01
     * and 00a -> 10 will be in the same bloc
     *
     * @param action  the action taken to reach the child
     * @param predicted  this object should have the desired external sensors
     *                   names.  The values will be changed by the method based
     *                   on the rule votes.  (Note:  Probably just pass in
     *                   currExternal from an existing node.)
     * @return how confident we are in this prediction
     */
    public double predictExternalByBloc(char action, SensorData predicted) {
        int numExtSensors = this.currExternal.size();

        // String is a String representation of a bloc (i.e "a00" or "b01")
        // TODO: Consider creating a BlocData object to make cleaner?
        HashMap<String, double[]> blocData = new HashMap<>();

        String[] sensorNames = predicted.getSensorNames().toArray(new String[0]);

        Vector<TFRule> tfRules = this.agent.getTfRules();

        // Go through each rule and add its vote to the blocData
        for (TFRule rule: tfRules) {
            // Ignore the rule if the action doesn't match
            if(rule.getAction() != action) continue;

            double score = rule.lhsMatchScore(action, this.currInternal,this.currExternal);
            if (score <= 0.0) continue;
            score *= rule.getConfidence();
            if (score <= 0.0) continue;

            String lhsKey = ""; // HashMap key String

            // Array containing data about the bloc in the following order:
            //first sensor on, first sensor off, second sensor on, second sensor off, bloc size
            //TODO:  this should not be hard-coded for two external sensors.
            double[] predictedExternal = new double[5];

            // Creates the key String for indexing into the HashMap and adds the rule's
            // vote for each lhs external sensor
            for(int j = 0; j < numExtSensors; j++){

                // Gets the value of the lhs external sensor
                boolean on = (boolean) rule.getLHSExternal().getSensor(sensorNames[j]);

                // Adds "1" or "0" to the key String depending on the sensor value
                lhsKey += on ? "1" : "0";

                // Gets the value of the prediction for the sensor
                on = (boolean) rule.getRHSExternal().getSensor(sensorNames[j]);

                // Adds the vote
                // Use a negative score for off and positive score for on to make summing the
                // total easier... maybe :)  //TODO:  negative score?
                predictedExternal[on ? 2 * j : 2 * j + 1] += score;
            }

            // Set the bloc size to one, which is used to increment in later function calls
            predictedExternal[4] = 1.0;

            // If the bloc is already created, we update it with the new vote of the current rule
            if (blocData.containsKey(lhsKey)){
                double[] old = blocData.get(lhsKey);    // Gets the old data
                for(int i = 0; i < old.length; i++){

                    // Updates the data
                    // This also increments the bloc size by 1
                    predictedExternal[i] += old[i];
                }
            }
            // Puts the updated data back into the HashMap with the new value
            blocData.put(lhsKey, predictedExternal);

        }//for each tf rule

        // Go through the hashMap and calculate the highest vote for each action
        double confidence = 1.0;

        double[] totalVotes = new double[4];  //TODO:  shouldn't be hardcoded for two ext sensors

        // Counts up all the votes across the blocs, factoring in the size of the bloc
        for(double[] d: blocData.values()) {

            // Each sensor vote total is divided by the size of the bloc to ensure each
            // bloc has a single vote
            totalVotes[0] += d[0] / d[4];
            totalVotes[1] += d[1] / d[4];
            totalVotes[2] += d[2] / d[4];
            totalVotes[3] += d[3] / d[4];
        }

        //Construct the predicted SensorData
        for(int i = 0; i < numExtSensors; i++) {
            predicted.setSensor(sensorNames[i], totalVotes[2*i] >= totalVotes [2*i+1]);

            //calculate confidence in this sensor value
            double max = Math.max(totalVotes[2*i],totalVotes[2*i+1]);
            if(max > 0.0){
                confidence *= max / (totalVotes[2*i] + totalVotes[2*i+1]);
            } else {
                confidence = 0.0;
            }
        }

        return confidence;
    }

    /**
     * class BlocData
     *
     * One instance of this class corresponds to a single sensor in the
     * external sensor array.  It tracks the total votes from each voting
     * bloc as well as the max voter from each block.
     */
    public class BlocData {
        public String sensorName;

        //Each bloc (e.g., 00a, 10b, 11a, etc.) its most confident constituent's vote stored here
        //There is a separate map for on vs. off votes
        public HashMap<String, Double> onVotes = new HashMap<>();
        public HashMap<String, Double> offVotes = new HashMap<>();

        public int offVoteCount = 0;
        public int onVoteCount = 0;

        public BlocData(String initName) {
            this.sensorName = initName;
        }

        /** scales a vote's weight on an exponential scale so that (near)
         * perfect match scores get much more weight than partial matches */
        public double scale(double matchScore) {
            //We want higher math scores to have exponentially higher weight so
            //that a (near-)perfect match will outweigh many partial matches but
            //partial matches can still have enough weight to make a difference
            //The formula 2^(2*r) - 1 gives an appropriately steep curve that
            // starts at zero.
            double result = matchScore * 2.0;  //doing this makes the max resulting score 1.0
            result = Math.pow(2.0, result);
            result -= 1.0;  //This makes min score 0.0
            return result;
        }//BlocData.scale

        /** registers one vote */
        public void vote(TFRule voter, double matchScore) {
            if (matchScore <= 0.0) return;  //ignore weightless vote

            //scale the score for a vote weight
            double weight = scale(matchScore);

            //Calculate the voting bloc this rule is in
            String bloc = voter.getLHSExternal().toString(false);

            //Add the vote to the proper vote sum
            boolean on = (boolean) voter.getRHSExternal().getSensor(this.sensorName);
            HashMap<String, Double> ballotBox = on ? this.onVotes : this.offVotes;
            if (on) { this.onVoteCount++; } else { this.offVoteCount++; }
            double oldVal = 0.0;
            if (ballotBox.containsKey(bloc)) {
                oldVal = ballotBox.get(bloc);
            }
            double newVal = Math.max(oldVal,weight);
            ballotBox.put(bloc, newVal);
        }//BlocData.vote


        /**
         * Calculates how confident the node is that this sensor is on or off
         *
         * @return an array containing the confidence values for off and on respectively
         */
        public double[] getOutcomes() {
            //0=off confidence,  1=on confidence
            double outArr[] = new double[2];

            //Calculate the vote totals and max vote for on and off
            double onVotes = 0.0;
            double offVotes = 0.0;
            double onVoteMax = 0.0;
            double offVoteMax = 0.0;
            for (double d : this.onVotes.values()) {
                onVotes += d;
                if (d > onVoteMax) onVoteMax = d;
            }
            for (double d : this.offVotes.values()) {
                offVotes += d;
                if (d > offVoteMax) offVoteMax = d;
            }

            //Calculate the confidences
            if(onVotes + offVotes == 0) {
                outArr[0] = 0;
                outArr[1] = 0;
            } else {
                int totalVotes = this.onVoteCount + this.offVoteCount;
                if (totalVotes == 0) {
                    outArr[0] = 0;
                    outArr[1] = 0;
                } else {
                    outArr[0] = (offVoteMax * this.offVoteCount) / totalVotes;
                    outArr[1] = (onVoteMax * this.onVoteCount) / totalVotes;
                }
            }
            return outArr;
        }//BlocData.getOutcomes



    }//Class BlocData


    /**
     * predictExternalMark3
     *
     * This is a re-revised version of predictExternalByBloc that weights the votes and
     * an exponential scale and then further weights the final confidence by the
     * match score of the best matching voter.  Rather than pick the winner (highest
     * confidence) it calculates the confidence for all possible ext sensor combos
     * as defined in TreeNode.comboArray.  This method is super complicated but I think
     * it provides the best confidence calculation (at least until Mark 4...)
     *
     * CAVEAT:  The sensor combos are based on the sensor names in alphabetical
     *          order so GOAL may not be the last sensor as it appears in the toString
     *
     * @param action  the action taken to reach the child
     * @param predicted  an emtpy array whose length == TreeNode.comboArray.length.
     *                   This will be filled in with appropriate SensorData objects
     *                   that are in the same order as comboArray
     * @return how confident we are in the prediction for each SensorData
     */
    public double[] predictExternalMark3(char action, SensorData[] predicted) {
        //Get a list of external sensor names
        int numExtSensors = this.currExternal.size();
        String[] sensorNames = currExternal.getSensorNames().toArray(new String[0]);

        //Create a BlocData object to track the votes for each sensor
        HashMap<String, BlocData> votingData = new HashMap<>();
        for(String sName : sensorNames) {
            votingData.put(sName, new BlocData(sName));
        }

        // Record the vote from each rule for each sensor
        Vector<TFRule> tfRules = this.agent.getTfRules();
        for (TFRule rule: tfRules) {
            // Skip rules with zero match scores
            if (rule.getAction() != action) continue;
            double score = rule.lhsMatchScore(action, this.currInternal, this.currExternal);
            if (score <= 0.0) continue;
            //TODO:  should rule confidence really be factored in here?  Leave it in for now...
            score *= rule.getConfidence();
            if (score <= 0.0) continue;

            //Record this rule's vote for each sensor
            for (String sName : sensorNames) {
                BlocData bd = votingData.get(sName);
                bd.vote(rule, score);
            }
        }//for

        //Create an array to store a confidence value for each possible sensor combination
        double confArr[] = new double[TreeNode.comboArr.length];
        for(int i = 0; i < confArr.length; ++i) {
            confArr[i] = 1.0;  //start with max conf and whittle it down
        }

        //Calculate the confidence in each combo.  This feels a bit bass-ackwards
        //because we want to iterate over sensor names first rather than
        //iterating over each combination first so that getOutcomes only
        //gets called once per sensor for max efficiency.
        for (int sId = 0; sId < sensorNames.length; ++sId) {
            //get the confidences in this sensor's values
            String sName = sensorNames[sId];
            BlocData bd = votingData.get(sName);
            double outcomes[] = bd.getOutcomes();

            //Update all sensor combos for this sensor
            for (int i = 0; i < confArr.length; ++i) {
                if (TreeNode.comboArr[i].charAt(sId) == '0') {
                    confArr[i] *= outcomes[0];
                } else {
                    confArr[i] *= outcomes[1];
                }
            }
        }//for

        //Scale the confidence values to the range [0.0..1.0]
        //This makes the agent more likely to consider longer paths
        //which ends up being an important nudge toward learning.
        double max = 0.0;
        for(double conf : confArr) {
            max = Math.max(max, conf);
        }
        for(int i = 0; i < confArr.length; ++i) {
            if (max == 0) {
                confArr[i] = 0;
            } else {
                confArr[i] /= max;
            }
        }

        //Populate the given SensorData array (predicted)
        for(int sdIndex = 0; sdIndex < predicted.length; ++sdIndex) {
            predicted[sdIndex] = new SensorData(false); //set GOAL to false for now

            //set the sensor valus for each SensorData object
            for (int sId = 0; sId < sensorNames.length; ++sId) {
                String sName = sensorNames[sId];
                boolean val = TreeNode.comboArr[sdIndex].charAt(sId) == '1';
                predicted[sdIndex].setSensor(sName, val);
            }
        }

        return confArr;
    }//predictExternalMark3


    /**
     * expand
     *
     * populates this.children
     *
     * a(1) -> 00
     *   ..
     *     ..
     * a(2) -> 10
     *   ..
     *     ..
     *       .. -> 01*
     */
    private void expand() {
        //check:  already expanded
        if (this.children.size() != 0) return;

        if (agent.getTfRules().size() < 1) return;

        int numActions = agent.getActionList().length;

        // bloc = lhs external sensors + action
        //  00a, 00b, 01a, 01b, etc.

        //Create predicted child nodes for each possible action
        for(int actId = 0; actId < numActions; actId++) {

            char act = agent.getActionList()[actId].getName().charAt(0);

            //Calculate the confidence in each combination
            SensorData sdArr[] = new SensorData[TreeNode.comboArr.length];
            double confArr[] = predictExternalMark3(act, sdArr);

            for(int i = 0; i < sdArr.length; i++) {
                //skip zero confidence
                //TODO:  for even better efficiency: skip any node with confidence less
                //       than best goal path found so far
                if (confArr[i] <= 0.0) continue;

                //create a child for this action + ext sensor combo
                TreeNode child = new TreeNode(this, act, sdArr[i], confArr[i]);
                if (child.confidence > 0.00001)
                    this.children.add(child);
            }
        }//for


    }//expand

    /**
     * findBestGoalPath
     * <p>
     * uses an iterative deepening search tree to find a path to the goal
     *
     * //TODO:  Could do A*Search instead if this is too slow
     */
    public Vector<TreeNode> findBestGoalPath() {
        double bestScore = 0.0;
        Vector<TreeNode> bestPath = null;
        for(int max = 1; max <= PhuJusAgent.MAX_SEARCH_DEPTH; ++max) {
            Vector<TreeNode> path = fbgpHelper(0, max);
            if (path != null) {
                double foundScore = path.lastElement().confidence;

                //Adjust confidence based upon the PathRules eval
                foundScore *= agent.metaScorePath(path);

                if(foundScore > bestScore) {
                    bestPath = path;
                    bestScore = foundScore;
                }
            }
        }

        return bestPath;  //null if failed to find a path to goal
    }//findBestGoalPath


    /**
     * fbgpHelper
     *
     * recursive helper method for {@link #findBestGoalPath}
     *
     * @param depth  depth of this node
     * @param maxDepth maximum depth allowed
     *
     * @return a path to the goal (or null if not found)
     */
    private Vector<TreeNode> fbgpHelper(int depth, int maxDepth) {
        //base case:  found goal
        if (this.isGoalNode()) {
            this.isLeaf = true;
            return this.path;
        }

        //base case:  max depth
        if (depth >= maxDepth) {
            this.isLeaf = true;
            return null;
        }

        /* ====================================
           Recursive case: examine child nodes
           ------------------------------------ */

        //Keeps track of the best path we've seen so far
        Vector<TreeNode> bestPath = null;
        double bestScore = 0.0;

        //Create the child nodes if they don't exist yet
        if (this.children.size() == 0) {
            expand();
        } else {
            for (TreeNode child : this.children) {
                child.isLeaf = false; //reset from any prev use of this child
            }
        }

        for(TreeNode child : this.children) {
            //Recursive case: test all children and return shortest path
            Vector<TreeNode> foundPath = child.fbgpHelper(depth + 1, maxDepth);

            if ( foundPath != null ) {
                double confidence = foundPath.lastElement().confidence;

                //TODO: Braeden is unsatisfied with the 1/length , think about another metric?
                double foundScore = (1.0 / (foundPath.size())) * confidence;
                if (foundScore > bestScore) {
                    bestPath = foundPath;
                    bestScore = foundScore;
                }
            }
        }//for

        return bestPath;

    }//fbgpHelper

    /**
     * findMostUncertainPath
     *
     * is called on a completed tree in which no path to GOAL was found.
     * It selects a relatively short path with a high degree of
     * uncertainty about the outcome.
     *
     * TODO:  This code is only looking at paths of length 1 right now
     *        Consider this case:
     *           ->(3, 20, 21, 33, 34)|00	c1.0
     *              a->()|00	c0.563817
     *                  aa->()|00	c0.317515
     *                  ab->(3, 20, 21, 33, 34)|00	c0.447692
     *               b->(3, 15, 20, 21, 33, 34, 35)|00	c0.817221
     *                   ba->()|00	c0.005266
     *                   bb->(3, 15, 34, 35)|00	c0.670597
     *          Shouldn't path 'ba' be selected?  Some code below
     *          attempted to that but has been commented out
     * @return the path found
     */
    public Vector<TreeNode> findMostUncertainPath() {

        int i = PhuJusAgent.rand.nextInt(agent.getActionList().length);
        char action = agent.getActionList()[i].getName().charAt(0);
        TreeNode random = new TreeNode(this, action, this.currExternal, 0.0);
        return random.path;

//        //pick the action (depth 1) with the greatest uncertainty
//        char mostUncertain = '\0';
//        double lowestConfSoFar = 1.1; //can't be this high
//        for (TreeNode child : this.children) {
//            //see if this child has more uncertainty
//            if (child.confidence < lowestConfSoFar) {
//                lowestConfSoFar = child.confidence;
//                mostUncertain = child.action;
//            }
//        }
//
//        //Just in case:  what if there are no children?
//        if (mostUncertain == '\0') {
//            mostUncertain = agent.getActionList()[rand.nextInt(agent.getActionList().length)].getName().charAt(0);
//        }
//
//        //Return a path with this action
//        TreeNode tmpNode = new TreeNode(this, mostUncertain, this.currExternal, 1.0);
//        return tmpNode.path;


        // TODO: Make code function how we want. Currently commented out until we get it working
       /* Vector<TreeNode> toSearch = new Vector<>();
        toSearch.add(this);

        double bestScore = -99.9;  //no score can be this low
        Vector<TreeNode> bestPath = null;
        while(toSearch.size() > 0) {
            TreeNode curr = toSearch.remove(0);
            double invConf = 1.0 - curr.confidence;
            if (curr.path.size() > 0) {  //node with no path can't be selected
                double score = (1.0 / ((double) curr.path.size())) * invConf;
                if (score > bestScore) {
                    bestScore = score;
                    bestPath = curr.path;
                }
                else if(score == bestScore) {

                    if(rand.nextBoolean()){
                        bestScore = score;
                        bestPath = curr.path;
                    }
                }
            }

            if (curr.children.size() == this.agent.getActionList().length) {
                //queue children to be searched
                for (TreeNode child : curr.children) {
                    if (child != null) toSearch.add(child);
                }
            //else if not at a leaf node...
            } else if (curr.path.size() < PhuJusAgent.MAX_SEARCH_DEPTH) {
                //If we reach this point, there is an action that has never been
                // taken in this scenario.  So find that action and build a path
                // with it.
                ArrayList<String> unusedActionsArr = new ArrayList<String>();
                int numUnusedActions = 0;
                for (int i = 0; i < agent.getActionList().length; i++) {
                    char action = agent.getActionList()[i].getName().charAt(0);
                    boolean found = false;
                    for(TreeNode child : curr.children) {
                        if (child.getAction() == action) {
                            found = true;
                            break;
                        }
                    }
                    //found an unused action
                    if(!found) {
                        unusedActionsArr.add(action + "");
                        numUnusedActions++;
                    }
                }
                if (numUnusedActions > 0) {
                    //create a fake tree node with this unused action so
                    // that there will be a path that uses it
                    // TODO:  Is this a mistake?

                    char action = unusedActionsArr.get(rand.nextInt(numUnusedActions)).charAt(0);
                    TreeNode fake = new TreeNode(curr, action, this.currExternal, 1.0);
                    return fake.path;

                }
            }

        }//while

        return bestPath; */
    }//findMostUncertainPath


    /**
     * sortedKeys
     *
     * @return a sorted Vector of the Integer keys in an internal sensor HashMap
     */
    private Vector<Integer> sortedKeys(HashSet<Integer> internal) {
        Vector<Integer> result = new Vector<>(internal);
        Collections.sort(result);
        return result;
    }

    /**
     * sortedKeys
     *
     * @return a sorted Vector of the Integer keys in an external sensor SensorData.
     * The GOAL sensor is always put at the end, however.
     */
    private static Vector<String> sortedKeys(SensorData external) {
        Vector<String> result = new Vector<>(external.getSensorNames());
        Collections.sort(result);

        //Move the goal to the end
        if (result.contains(SensorData.goalSensor)) {
            result.remove(SensorData.goalSensor);
            result.add(SensorData.goalSensor);
        }

        return result;
    }//sortedKeys

    /**
     * extToString
     *
     * a shortened bit-based toString() method for SensorData
     */
    public static String extToString(SensorData stringMe) {
        StringBuilder result = new StringBuilder();
        for (String s : sortedKeys(stringMe)) {
            Boolean val = (Boolean) stringMe.getSensor(s);
            result.append(val ? "1" : "0");
        }
        return result.toString();
    }//extToString

    /**
     * toString
     * <p>
     * creates a terse ASCII representation of this node.  The child nodes are not depicted.
     */
    public String toString(boolean includeConf) {
        StringBuilder result = new StringBuilder();

        //the actions that led to this node
        result.append(this.pathStr);
        result.append("->");

        //Internal Sensors
        result.append("(");
        int count = 0;
        for (Integer i : sortedKeys(this.currInternal)) {
            if (count > 0) {
                result.append(", ");
            }
            count++;
            result.append(i);
        }
        result.append(")|");

        //External Sensors
        result.append(TreeNode.extToString(this.currExternal));

        //Confidence
        if (includeConf) {
            result.append("\tc");
            //note:  replaceAll call removes extra trailing 0's to improve readability
            result.append(String.format("%.6f", this.confidence).replaceAll("0+$", "0"));
        }

        return result.toString();
    }

    @Override
    public String toString() {
        return toString(true);
    }


    /**
     * prints the tree in an "easily" readable format.  This method is the front facing interface.
     * {@link #printTreeHelper} is the recursive helper method that traverses the tree
     */
    public void printTree() {
        //print the nodes recursively
        printTreeHelper("");
    }

    /**
     * printTreeHelper
     * <p>
     * is the helper method for {@link #printTree}
     *
     * @param indent how much to indent any output from this method
     */
    private void printTreeHelper(String indent) {
        System.out.print(indent + "  " + this);

        //base case #1: Goal Node found (not at root)
        if ( isGoalNode() ) {
            System.out.println("*");
            return;
        }

        //base case #2:  Leaf Node (no goal found)
        if (this.isLeaf) {
            System.out.println();
            return;
        }

        System.out.println();

        //recursive case: print child nodes
        for(TreeNode child : this.children) {
            child.printTreeHelper(indent + "   ");
        }
    }//printTreeHelper

    /**
     * isGoalNode
     *
     * @return true if this node's external sensors include GOAL=true
     */
    public boolean isGoalNode() {
        return ( (this.currExternal.getSensor(SensorData.goalSensor) != null)  //goal sensor is present
                && (this.currExternal.isGoal())  //goal sensor is true
                && (this.parent != null) );   //this isn't the root
    }

    /**
     * nearEquals
     *
     * This method is like {@link #equals } but it only requires that the
     * other node have at least one LHS internal sensor in common.  This method
     * is used directly by {@link PathRule#matchLen}
     */
    public boolean nearEquals(TreeNode other) {

        //compare actions
        if (! this.pathStr.equals(other.pathStr)) return false;

        //compare external sensors
        if (! other.currExternal.equals(this.currExternal)) return false;

        //look for one internal sensor in common
        if (this.currInternal.size() == 0) {
            return (other.currInternal.size() == 0);  //not 100% sure about this
        }
        for(Integer i : this.currInternal) {
            if (other.currInternal.contains(i)) return true;
        }
        return false; //no luck

    }//nearEquals



    /**
     * equals
     *
     * method is overridden to only compare sensors and actions
     */
    @Override
    public boolean equals(Object obj) {
        if (! (obj instanceof TreeNode) ) return false;
        TreeNode other = (TreeNode)obj;

        if (nearEquals(other)) {
            return (other.currInternal.equals(this.currInternal));
        }

        return false;
    }//equals

    public char getAction() { return this.pathStr.charAt(this.pathStr.length() - 1); }
    public String getPathStr() { return this.pathStr; }
    public HashSet<Integer> getCurrInternal() { return this.currInternal; }
    public SensorData getCurrExternal() { return this.currExternal; }
    public TreeNode getParent() { return this.parent; }
    public double getConfidence() { return this.confidence; }


}//class TreeNode
