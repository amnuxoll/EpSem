package agents.wfc;

import framework.Action;
import framework.IAgent;
import framework.IIntrospector;
import framework.SensorData;

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

    public static final int EPSILON     = 250; // The duration (in timesteps) of the agent's exploration
    public static final int MAXNUMRULES = -1; // The maximum number of rules (-1 if no cap)

    public static final boolean DEBUGPRINTSWITCH = true; // Turns on/off debugPrint()
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

        debugPrintln("Received SensorData: " + sensorData);

        printPathRules();

        // Update realPathTraversedSoFar to contain the current external. Action is unknown at this point
        if (sensorData.isGoal()) {
            updatePathRules();
            this.realPathTraversedSoFar.clear();
        } else {
            this.realPathTraversedSoFar.add(new PathNode(
                    sensorData,
                    '*'
            ));
            System.out.println("Here");
        }

        // Deciding on what action to take next
        char action;
        if (this.now <= EPSILON) {
            action = getRandomAction();
        } else {
            action = getActionFromExperiences();
        }

        // We add the action to our current path once it's decided
        if (this.realPathTraversedSoFar.size() > 0) {
            this.realPathTraversedSoFar.lastElement().setAction(action);
        }

        return new Action(action + "");
    }//getNextAction

    /**
     * matchPattern
     * <p>
     * Looks for where the given sequence of PathNodes is found in the agent's PathRules. Works similar to pattern
     * matching in grep.
     * TODO this method is also too big
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

            if (!rule.toString().contains(patternString)) continue;

            Vector<PathNode> ruleNodes = rule.getNodes();
            for (int i = 0; i < ruleNodes.size(); i++) {
                PathNode node = ruleNodes.get(i);

                // If we've found a match for the first part of the pattern, add it to the candidates
                if (node.equals(pattern.get(0))) {
                    List<PathNode> candidate = ruleNodes.subList(i, ruleNodes.size());
                    candidates.add(candidate);
                    //System.out.println(candidate);
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
                if (!pathChk.equals(patternChk)) {
                    addFlag = false;
                    break;
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
     *
     * TODO This method is too big
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

        // Return the action with the most votes
        // TODO This is gross
        int max = votes.get(this.actionList[0]);
        Action returnAction = this.actionList[0];
        for (Action a : this.actionList) {
            if (votes.get(a) > max) {
                max = votes.get(a);
                returnAction = a;
            }
        }

        // Also atrocious
        return returnAction.getName().toCharArray()[0];
    }//getActionFromExperiences

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
