package agents.wfc;

import agents.phujus.PhuJusAgent;
import framework.*;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

/**
 * class WFCAgent
 * <p>
 * An episodic memory learner which uses a similar approach to wave function collapse algorithms
 * to navigate FSMs
 * <p>
 * TODO Investigate WFC further and look at other mathematical/physical models of probability
 * TODO Exploration vs. exploitation
 */
public class WFCAgent implements IAgent {

    //region InstanceVariables
    // The number of timesteps the agent has been running in this FSM
    private int now;

    // These keep track of the number of random actions which take us to the goal/haven't
    private int totalRands     = 1;
    private int succesfulRands = 1;

    // Ratio of succesful random actions to total random actions
    private double ratio           = 0d;
    private double prevRatio       = 0d;
    private double ratioDerivative = 1.0d;

    private boolean tookRandomAction = false; // If we took a random action in the previous timestep

    public static final int EPSILON     = 500; // The duration (in timesteps) of the agent's exploration
    public static final int MAXNUMRULES = -1; // The maximum number of rules (-1 if no cap)

    public static final boolean DEBUGPRINTSWITCH = false; // Turns on/off debugPrint()
    public static final boolean OUTPUT_PATHRULES = false; // Whether or not the agent exports PathRules on goal
    public static final boolean INPUT_PATHRULES  = false; // Whether or not the agent imports PathRules
    public static final boolean PARTIAL_MATCHING = true;

    public static final File OUTPUT_FILE = new File("C:\\Users\\sirma\\fsm_output\\00logs\\wfc_pathrule_output.txt");
    public static final File INPUT_FILE = new File("C:\\Users\\sirma\\fsm_output\\00logs\\wfc_pathrule_input.txt");
    // TODO Agent should automatically decide when to explore and when to exploit

    // The path that the agent has travelled along since reaching the goal
    private Vector<PathNode> realPathTraversedSoFar = new Vector<>();

    // The lists of rules that this agent is using
    private Vector<WFCPathRule> wfcPathRules  = new Vector<>();
    private Hashtable<Integer, WFCRule> rules = new Hashtable<>();

    // Random number generator with fixed seed to reproduce results
    public static Random rand = new Random(2);

    // The list of actions the agent may take
    private Action[] actionList;
    //endregion InstanceVariables

    //region InnerClasses

    /**
     * class Pair
     * <p>
     * Represents a pair of values.
     * @param <T>
     * @param <Y>
     */
    private static class Pair<T, Y> {

        private T val1;
        private Y val2;

        public Pair(T val1, Y val2) {
            this.val1 = val1;
            this.val2 = val2;
        }

        public T getVal1() {
            return val1;
        }

        public void setVal1(T val1) {
            this.val1 = val1;
        }

        public Y getVal2() {
            return val2;
        }

        public void setVal2(Y val2) {
            this.val2 = val2;
        }
    }

    //endregion InnerClasses

    @Override
    public void initialize(Action[] actions, IIntrospector introspector) {
        this.actionList = actions;
    }

    //region DecisionMaking
    /**
     * getNextAction
     * <p>
     * Calculates the next action the agent should take based on the given SensorData and previous experiences.
     * @param sensorData The {@link SensorData} from the current move.
     * @return the action taken
     */
    @Override
    public Action getNextAction(SensorData sensorData) throws Exception {

        this.now++;

        if (this.now == 1) {

            if (OUTPUT_PATHRULES && OUTPUT_FILE.exists()) {
                OUTPUT_FILE.delete();
            }

            if (INPUT_PATHRULES) {
                importPathRules(INPUT_FILE);
            }
        }

        debugPrintln("Received SensorData: " + sensorData);

        printPathRules();

        // Update realPathTraversedSoFar to contain the current external. Action is unknown at this point
        if (sensorData.isGoal()) {

            if (tookRandomAction) {
                succesfulRands++;
                tookRandomAction = false;

                this.prevRatio = this.ratio;
                this.ratio = (double) succesfulRands / (double) totalRands;
                this.ratioDerivative = (ratio - prevRatio);
                debugPrintln("Success rand ratio: " + ratio);
                debugPrintln("Derivative: " + ratioDerivative);
            }
            // Calculate derivative of rand ratio

            updatePathRules();
            this.realPathTraversedSoFar.clear();
        } else {
            this.realPathTraversedSoFar.add(new PathNode(
                    sensorData,
                    '*'
            ));
        }

        // Deciding on what acion to take next
        char action;
        if (INPUT_PATHRULES) {
            action = getActionFromExperiences();
        } else {
            action = calcActionRSB();
        }

        // We add the action to our current path once it's decided
        if (this.realPathTraversedSoFar.size() > 0) {
            this.realPathTraversedSoFar.lastElement().setAction(action);
        }


        debugPrintln("TIME: " + now);
        return new Action(action + "");
    }//getNextAction

    /**
     * importPathRules
     * <p>
     * Imports a set of WFCPathRules from a given file.
     * @param input
     * @throws IOException
     */
    public void importPathRules(File input) throws IOException {
        if (!input.exists()) {
            return;
        }

        // Every line is a different rule
        try (Scanner sc = new Scanner(input)) {
            while (sc.hasNextLine()) {
                String line = sc.nextLine();
                addRule(new WFCPathRule(this, line));
            }
        }
        debugPrintln("Done");
    }//importPathRules

    /**
     * exportPathRules
     * <p>
     * Exports all of the PathRules currently generated so far to the given output file. This method is called
     * everytime a new rule is added, so it probably bogs down the performance quite a bit when OUTPUT_PATHRULES is
     * set to true.
     * @param output
     * @throws IOException
     */
    private void exportPathRules(File output) throws IOException {

        if (this.wfcPathRules.size() == 0) {
            return;
        }

        if (!output.exists()) {
            output.createNewFile();
        }

        // Output all string representations of the rules to the file
        StringBuilder sb = new StringBuilder();
        for (WFCPathRule rule : this.wfcPathRules) {
            sb.append(rule.toString(false) + "\n");
        }

        FileWriter writer = new FileWriter(output);
        writer.write(sb.toString());
        writer.close();
    }//exportPathRules

    /**
     * calcActionPSB
     * <p>
     * Uses predicted success based exploitation to determine the exploration condition. This one is always
     * worse than the random one.
     * @return the action
     */
    private char calcActionPSB() {
        char action;

        // EXPLORE CONDITION
        // Predicted success based exploitation: Instead of using totally random actions, we use the least
        // explored action (the one with the least number of votes from previous experiences). If the derivative of
        // the ratio between successful least-explored actions and unsuccesful least-explored actions is 0, we
        // take an action based on previous experiences.
        if (Math.abs(this.ratioDerivative) > 0.0001) {
            debugPrintln("EXPLORING...");
            action = getLeastExploredAction();
            totalRands++;
            tookRandomAction = true;
        }
        // EXPLOIT CONDITION
        else {
            action = getActionFromExperiences();
        }
        return action;
    }//calcActionPSB

    /**
     * calcActionRSB
     * <p>
     * Uses random success based exploitation to determine the exploration condition.
     * @return the action
     */
    private char calcActionRSB() {
        char action;

        // EXPLORE CONDITION
        // Random success based exploitation: If the derivative of the ratio of successful random actions to unsuccesful
        // random actions is close to zero, use rules the agent has come up with.
        if (Math.abs(this.ratioDerivative) > 0.0001) {
            debugPrintln("EXPLORING...");
            action = getRandomAction();
            totalRands++;

            tookRandomAction = true;
        }
        // EXPLOIT CONDITION
        else {
            action = getActionFromExperiences();
        }
        return action;
    }//calcActionRSB


    /**
     * calcActionEpsilon
     * <p>
     * If the number of time steps is less than EPSILON, take a random action. Else, take an action based on info
     * gathered from previous experiences.
     * @return the action
     */
    private char calcActionEpsilon() {
        char action;

        // EXPLORE CONDITION
        if (this.now < EPSILON) {
            debugPrintln("EXPLORING...");
            action = getRandomAction();
            totalRands++;
            tookRandomAction = true;
        }
        // EXPLOIT CONDITION
        else {
            action = getActionFromExperiences();
        }
        return action;
    }//calcActionEpsilon

    /**
     * getLeastExploredAction
     * <p>
     * Finds all paths which match the current experience and picks the outcome with the least number of votes.
     * @return
     */
    private char getLeastExploredAction() {

        if (this.realPathTraversedSoFar.size() == 0) { return getRandomAction(); }

        Vector<List<PathNode>> matches = matchPattern(this.realPathTraversedSoFar);

        if (matches == null || matches.size() == 0) { return getRandomAction(); }


        HashMap<Action, Integer> votes = getPathVotes(matches);

        return getLoserAction(votes).getName().charAt(0);
    }//getLeastExploredAction

    /**
     * matchPattern
     * <p>
     * Looks for where the given sequence of PathNodes is found in the agent's PathRules. Works similar to pattern
     * matching in grep.
     * TODO this method is also too big
     * TODO put this method inside of WFCPathRule?
     * @param pattern the sequence of PathNodes that is matched
     * @return a list of every single sequence in every PathRule which matches the pattern in descending order (based
     * on length)
     */
    public Vector<List<PathNode>> matchPattern(Vector<PathNode> pattern) {

        // Guard clauses
        if (this.wfcPathRules.size() == 0) {
            return null;
        }

        if (pattern.size() == 0) {
            return null;
        }
        Vector<List<PathNode>> candidates = new Vector<>();

        // Checking if the patternString is contained inside the rule. If not, then there's no way this
        // pattern could be in the rule
        String patternString = WFCPathRule.toString(pattern);
        for (WFCPathRule rule : this.wfcPathRules) {

            //if (!rule.toString().contains(patternString)) continue;

            Vector<PathNode> ruleNodes = rule.getNodes();
            for (int i = 0; i < ruleNodes.size(); i++) {
                PathNode node = ruleNodes.get(i);

                // If we've found a match for the first part of the pattern, add it to the candidates
                if (PARTIAL_MATCHING) {
                    if (node.partialEquals(pattern.get(0), true) > 0.0d) {
                        List<PathNode> candidate = ruleNodes.subList(i, ruleNodes.size());
                        candidates.add(candidate);
                        //System.out.println(candidate);
                    }
                } else {
                    if (node.equals(pattern.get(0))) {
                        List<PathNode> candidate = ruleNodes.subList(i, ruleNodes.size());
                        candidates.add(candidate);
                        //System.out.println(candidate);
                    }
                }
            }
        }//for

        // If we have a small pattern, then we've already found all the perfect matches
        if (pattern.size() == 1) {
            return candidates;
        }

        //System.out.println("CHECKING FINAL CANDIDATES");
        Vector<List<PathNode>> finalCandidates = new Vector<>();
        // If we have a bigger pattern, we need to do some more checking. In the previous step, we looked for
        // sequences where just the first part of the pattern matched. Now we're checking those sequences to see if
        // they match the rest of the pattern.
        for (List<PathNode> nodeList : candidates) {

            // The pattern is too big to match this section
            if (nodeList.size() < pattern.size()) {
                continue;
            }

            boolean addFlag = true;
            // If at any point the pattern doesn't match, continue
            for (int i = 0; i < pattern.size(); i++) {
                PathNode pathChk = nodeList.get(i);
                PathNode patternChk = pattern.get(i);
                if (PARTIAL_MATCHING) {
                    if (pathChk.partialEquals(patternChk, true) == 0.0d) {
                        addFlag = false;
                        break;
                    }
                } else {
                    if (pathChk.equals(patternChk)) {
                        addFlag = false;
                        break;
                    }
                }
            }

            // If we've made it past all these checks, add it to the finalCandidates list
            if (addFlag) {
                finalCandidates.add(nodeList);
            }
        }
        return finalCandidates;
    }//matchPattern

    /**
     * getActionFromExperiences
     *<p>
     * Determines what action the agent will take next based upon the current path and previous paths. It does this
     * by looking through its previous paths to find if it has travelled along this same path before, and then
     * using the information from those paths to determine what to do next.
     */
    public char getActionFromExperiences() {
        Vector<List<PathNode>> possiblePaths = matchPattern(this.realPathTraversedSoFar);

        if (possiblePaths == null || possiblePaths.size() == 0) {
            // No previous experiences match our current circumstance. Pick a random action.
            debugPrintln("No patterns matched. Picking random action.");
            return getRandomAction();
        }

        // We've found matching previous experiences. We tally up the votes of the shortest matching ones,
        // since those are closest to the goal
        // TODO other options for voting?

        HashMap<Action, Integer> votes = getPathVotes(possiblePaths);

        // Return the action with the most votes
        Action returnAction = getElectedAction(votes);

        return returnAction.getName().toCharArray()[0];
    }//getActionFromExperiences

    /**
     * getPathVotes
     * <p>
     * Helper method which generates a HashMap of actions to integer votes based on the outcomes of the provided paths.
     * @param possiblePaths
     * @return
     */
    public HashMap<Action, Integer> getPathVotes(Vector<List<PathNode>> possiblePaths) {
        HashMap<Action, Integer> votes = new HashMap<>();

        for (Action a : this.actionList) {
            votes.put(a, 0);
        }

        int minSize = possiblePaths.lastElement().size(); // The list is sorted in descending order based on length

        // Gathering the votes from each of the paths which match our current path
        for (List<PathNode> possiblePath: possiblePaths) {
            if (possiblePath.size() <= minSize) {

                // TODO is this right??
                Action pathAct;
                if (minSize == 1) {
                    pathAct = possiblePath.get(0).getAction();
                } else {
                    pathAct = possiblePath.get(realPathTraversedSoFar.size()-1).getAction();
                }

                // Add +1 to vote count
                int prevVotes = votes.get(pathAct);
                votes.put(pathAct, prevVotes + 1);
            }
        }
        return votes;
    }//getPathVotes

    /**
     * getElectedAction
     * <p>
     * Helper method which returns the action with the highest number of votes.
     * @param votes a mapping of actions to the number of votes they have
     * @return the action with the most votes in the given set
     */
    public Action getElectedAction(HashMap<Action, Integer> votes) {
        int max = votes.get(this.actionList[0]);
        Action returnAction = this.actionList[0];
        for (Action a : this.actionList) {
            if (votes.get(a) > max) {
                max = votes.get(a);
                returnAction = a;
            }
        }
        return returnAction;
    }//getElectedAction

    /**
     * getLoserAction
     * <p>
     * Helper method which returns the action with the lowest number of votes.
     * @param votes a mapping of actions to the number of votes they have
     * @return the action with the least votes in the given set
     */
    public Action getLoserAction(HashMap<Action, Integer> votes) {
        int min = votes.get(this.actionList[0]);
        Action returnAction = this.actionList[0];
        for (Action a : this.actionList) {
            if (votes.get(a) < min) {
                min = votes.get(a);
                returnAction = a;
            }
        }
        return returnAction;
    }//getElectedAction

    /**
     * updatePathRules
     * <p>
     * Updates the PathRules that this agent has. If the path that the agent has travelled is unique, then it's added
     * as a new PathRule.
     */
    private void updatePathRules() {

        if (this.realPathTraversedSoFar.size() == 0) return;

        WFCPathRule ruleToAdd = new WFCPathRule(this, this.realPathTraversedSoFar);

        // Checking for duplicate rules
        for (WFCPathRule rule : this.wfcPathRules) {
            if (rule.equals(ruleToAdd)) {
                return;
            }
        }
        addRule(ruleToAdd);
    }//updateMPathRules
    //endregion DecisionMaking

    //region GettersSettersAdders
    /**
     * addRule
     * <p>
     * Adds a given {@link WFCRule} to the agent's repertoire. Will fail silently if the amount of rules is >
     * MAXNUMRULES.
     */
    public void addRule(WFCRule newRule) {
        if (MAXNUMRULES > 0 && this.rules.size() >= MAXNUMRULES) {
            System.err.println("ERROR: Exceeded MAXNUMRULES!");
        }

        rules.put(newRule.ruleId,newRule);

        if (newRule instanceof WFCPathRule) {
            this.wfcPathRules.add((WFCPathRule) newRule);
        }

        //DEBUG
        debugPrintln("added: " + newRule);
        if (OUTPUT_PATHRULES) {
            try {
                exportPathRules(OUTPUT_FILE);
            } catch (IOException e) {
                // Don't care
            }
        }
    }

    /** Only used for unit tests! Will break stuff if you use it incorrectly! */
    public void setRealPathTraversedSoFar(Vector<PathNode> path) {
        this.realPathTraversedSoFar = new Vector<>(path);
    }

    public char getRandomAction() {
        int i = rand.nextInt(this.actionList.length);
        return this.actionList[i].getName().charAt(0);
    }
    //endregion GettersSettersAdders

    //region PrintMethods
    private void printPathRules() {

        if (!DEBUGPRINTSWITCH) {
            return;
        }
        System.out.println("WFCPathRules: ");
        for (WFCPathRule pr : this.wfcPathRules) {
            System.out.println(pr);
        }
    }

    private void debugPrintln(String msg) {
        if (DEBUGPRINTSWITCH) System.out.println(msg);
    }

    private void debugPrint(String msg) {
        if (DEBUGPRINTSWITCH) System.out.print(msg);
    }
    //endregion PrintMethods
}
