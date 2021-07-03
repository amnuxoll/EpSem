package agents.phujus;

import framework.Action;
import framework.IAgent;
import framework.IIntrospector;
import framework.SensorData;

import java.io.*;
import java.util.ArrayList;
import java.util.*;
import java.util.HashMap;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * class PhuJusAgent
 */
public class PhuJusAgent implements IAgent {
    //graphing activation levels
    private List<String[]> rulesActivationCSV = new ArrayList<>();

    public static final int MAXDEPTH = 3;
    public static final int MAXNUMRULES = 8;
    public static final int NUMINTERNAL = 4;

    //activation
    public static int RULEMATCHHISTORYLEN = 40;
    private Rule matchArray[] = new Rule[RULEMATCHHISTORYLEN]; //stores last 100 rules that match and correctly matched the future
    private int lastMatchIdx = 0;

    //a list of all the rules in the system (can't exceed some maximum)
    private Vector<Rule> rules = new Vector<Rule>(); // convert to arraylist

    //map to find a rule that activates a particular internal sensor.
    //key:  an internal sensor index
    //value:  the rule that lights it up
    //e.g., Rule #631 causes internal sensor #298 to turn on when it fires
    private Rule[] internalMap = new Rule[NUMINTERNAL];

    private int now = 0; //current timestep 't'


    private Action[] actionList;
    private IIntrospector introspector;

    //current sensor values
    private HashMap<Integer, Boolean> currInternal = new HashMap<>();
    private SensorData currExternal;

    //values from previous instance to build rules
    private SensorData prevExternal;
    private HashMap<Integer, Boolean> prevInternal = new HashMap<>();
    private char prevAction;

    //predicted sensor values for t+1
    int[] nextInternal = new int[NUMINTERNAL];

    //2 represents the number of arrays of external sensors. NUMEXTERNAL represents the number of external sensors we're using
    private HashMap<String, double[]> predictedExternal = new HashMap<>();

    //Path to take
    private String path = "";

    //root of the prediction tree
    TreeNode root; // init this somewhere ...

    private int cycle = 0;


    //random numbers are useful sometimes
    private static Random rand = new Random(2);

    public String convertToCSV(String[] data) {
        return Stream.of(data)
                .map(this::escapeSpecialCharacters)
                .collect(Collectors.joining(","));
    }

    public void givenDataArray_whenConvertToCSV_thenOutputCreated() throws IOException {
        //Get the current rules' activation levels during this time step
        ArrayList<Double> currRuleActLvls = new ArrayList<>();
        for (Rule r : this.rules) {
            currRuleActLvls.add(r.calculateActivation(this.now));
        }

        //Store each rule's activation level on a row
        String[] row = new String[this.rules.size() + 1];
        row[0] = String.valueOf(this.now); //First column of the row is the timestep
        int j = 1;
        for (int i = 0; i < this.rules.size(); i++) {
            //Fill in the rest of the row
            row[j] = String.valueOf(currRuleActLvls.get(i));
            j++;
        }
        this.rulesActivationCSV.add(row);

        //Create output file and append if it already exists
        FileWriter csvOutputFile = new FileWriter("output.csv", false);
        try (PrintWriter pw = new PrintWriter(csvOutputFile, true)) {
//            this.rulesActivationCSV.stream()
//                    .map(this::convertToCSV)
//                    .forEach(pw::println);
            for(String[] rw : this.rulesActivationCSV){
                for(String s: rw){
                    pw.print(s + ",");
                }
                pw.println();
            }
            pw.flush();
        } catch (Exception e) {
            System.out.println("something went wrong");
        }
    }

    public String escapeSpecialCharacters(String data) {
        String escapedData = data.replaceAll("\\R", " ");
        if (data.contains(",") || data.contains("\"") || data.contains("'")) {
            data = data.replace("\"", "\"\"");
            escapedData = "\"" + data + "\"";
        }
        return escapedData;
    }

    @Override
    public void initialize(Action[] actions, IIntrospector introspector) {
        //Store the actions the agents can take and introspector for data analysis later on
        this.rules = new Vector<Rule>();
        this.actionList = actions;
        this.introspector = introspector;
        this.now = 0;
        this.prevAction = '\0';
//        this.prevInternal = new HashMap<>();
//        this.currInternal = new HashMap<>();
        this.initCurrInternal();
        //prev internal has no previous internal sensors, assign it to curr
        this.prevInternal = this.currInternal;
        for(int i = 0; i < PhuJusAgent.NUMINTERNAL; ++i) {
            Rule.intSensorInUse[i] = false;
        }

        defineColumnHeaders();
    }

    public void defineColumnHeaders() {
        //Write column names into the first row of a CSV file
        String[] columnNames = new String[MAXNUMRULES + 1];
        columnNames[0] = "Timestep"; //First column of the row is the timestep
        int j = 1;
        for (int i = 0; i < MAXNUMRULES; i++) {
            //Label each column by rule's index in the inventory
            columnNames[j] = "Rule " + (i + 1) + " Activation Level";
            j++;
        }
        this.rulesActivationCSV.add(columnNames);
    }


    //DEBUG:  print out internal sensors
    private void printInternalSensors(HashMap<Integer, Boolean> printMe) {
        System.out.println("Internal Sensors: ");
        for(Integer i : printMe.keySet()) {
            System.out.println("\t" + i.toString() + ":" + printMe.get(i));
        }
    }

    /**
     * randomActionPath
     *
     * generates a string single valid action letter in it
     */
    private String randomActionPath() {
        return actionList[this.rand.nextInt(getNumActions())].getName();
    }


    /**
     * Gets a subsequent move based on the provided sensorData.
     *
     * @param sensorData The {@link SensorData} from the current move.
     * @return the next Action to try.
     */
    @Override
    public Action getNextAction(SensorData sensorData) throws Exception {
        this.now++;
        this.cycle++;
        System.out.println("TIME STEP: " + this.now);

//        if(now == 1) {
//            for(int i = 0; i < PhuJusAgent.NUMINTERNAL; ++i) {
//                Rule.intSensorInUse[i] = false;
//            }
//        }

        if((now > 5 && cycle == 1) || (cycle > 5)) {
            this.updateRules();
        }

        Action action = new Action("a" + "");
        char act = '\0';

        if(this.currExternal == null) { //Use case: time step t = 0 when the agent has not taken any action yet
            this.currExternal = sensorData;
            this.prevExternal = this.currExternal;
        }
        else { //get currExternal values from sensorData
            this.currExternal = sensorData;
        }

        if(this.currInternal.size() == 0) {
            this.initCurrInternal();
            //prev internal has no previous internal sensors, assign it to curr
            this.prevInternal = this.currInternal;
        }

        //if the agents' rule was predicted correctly, update the activation level
        this.getPredictionActivation(now);



        //Create rules based on the agent's current internal and external sensors
        if (sensorData.isGoal()) {
            System.out.println("We hit the goal!");
            //Generate random rules if none are in inventory
            if(this.rules.size() == 0) {
                this.generateRules();
            } else{
                //Getting rid of half the lowest rules to update them
                //this.updateRules();
                defineColumnHeaders();
            }
            //Resetting the path after we've reached the goal
            this.path = "";
            cycle = 0;
            action = new Action(randomActionPath().charAt(0) + "");
        } else{
            //If we don't have a path to follow, find a goal path
            if(this.path.equals("")) {
                this.buildPathFromEmpty();
                action = new Action(this.path.charAt(0) + "");

                //Shorten the path by one so that subsequent calls to this method can grab the right action to take
                this.path = this.path.substring(1);
            }
            else{
                //path has been found in previous call to this method, so get the next action
                action = new Action(this.path.charAt(0) + "");

                //the agent next action will be random until it finds the goal if there was only 1 action in the path
                if(this.path.length() == 1){
                    this.path = randomActionPath();
                }
                else {
                    this.path = this.path.substring(1);
                }
            }
        }



        //DEBUG:
        printExternalSensors(this.currExternal);
        printInternalSensors(this.currInternal);
        printRules(action);
//        givenDataArray_whenConvertToCSV_thenOutputCreated();


        //Now that we know what action to take, all curr values are now previous values
        this.prevInternal = this.currInternal;
        this.prevExternal = this.currExternal;
        this.prevAction = action.getName().charAt(0);
        //get the new currInternal sensors for next action based on char action taken this step
        this.getNewInternalSensors(action);

        return action;
    }

    public void getPredictionActivation(int now) {
        if (now != 1) {
            for (Rule r : this.rules) {
                // If prediction was correct, reward it
                if (r.correctMatch(prevAction, this.prevExternal, this.prevInternal, this.currExternal)) {
                    boolean punish = false;
                    String check = r.getRHSSensorName();
                    boolean goal;
                    if (check.equals(SensorData.goalSensor)) {
                        goal = true;
                    } else {
                        goal = false;
                        this.matchArray[this.lastMatchIdx] = r;
                        this.lastMatchIdx = (this.lastMatchIdx + 1) % RULEMATCHHISTORYLEN;
                    }
                    int prevMatch = this.lastMatchIdx - 1;
                    if(prevMatch < 0) {
                        prevMatch = RULEMATCHHISTORYLEN - 1;
                    }
                    r.setActivationLevel(now, goal, punish, this.matchArray, prevMatch, this.getRuleMatchHistoryLen());
                    r.calculateActivation(now);
                    // If prediction was incorrect, punish it
                } //else {
//                    if(r.correctMatch(prevAction, this.prevExternal, this.prevInternal, null)) {
//                        boolean punish = true;
//                        String check = r.getRHSSensorName();
//                        boolean goal;
//                        if (check.equals(SensorData.goalSensor)) {
//                            goal = true;
//                        } else {
//                            goal = false;
//                            this.matchArray[this.lastMatchIdx % RULEMATCHHISTORYLEN] = r;
//                            this.lastMatchIdx++;
//                        }
//                        r.setActivationLevel(now, goal, punish, this.matchArray, this.lastMatchIdx-1, this.getRuleMatchHistoryLen());
//                        r.calculateActivation(now);
//                        //DEBUG don't use a loop, instead just reallocate the array
//                        if(goal) {
////                            for(int i = this.lastMatchIdx-1; i>=0; i--){
////                                this.matchArray[i] = null;
////                            }
//                            this.matchArray = new Rule[RULEMATCHHISTORYLEN];
//                            this.lastMatchIdx = 0;
//                        }
//                    }
//                    r.calculateActivation(now);
//                }
            }
        }

    }

    public void getNewInternalSensors(Action action) {
        this.currInternal = new HashMap<Integer, Boolean>();
        //This variable is needed to make the call but we will throw away the data that it is filled with (for now!)
        HashMap<String, double[]> deleteMe =
                new HashMap<String, double[]>();

        //create temporary treeNode to send in correct currInternal
        TreeNode tempNode = new TreeNode(this, this.rules, this.now, this.prevInternal,
                this.currExternal, '\0', "");

        tempNode.genNextSensors(action.getName().charAt(0), currInternal, deleteMe, false);
    }


    public void printExternalSensors(SensorData sensorData) {
        System.out.println("External Sensors: ");
        for(String s : sensorData.getSensorNames()) {
            System.out.println("\t" + s + ":" + sensorData.getSensor(s));
        }
    }

    /**
     * printRules
     *
     * prints all the rules in an abbreviated format
     *
     * @param action  the action the agent has selected.  This can be set to null if the action is not yet known
     */
    public void printRules(Action action) {
        if (action != null) {
            System.out.println("Selected action: " + action.getName().charAt(0));
        }

        for (Rule r : this.rules) {
            if( (action != null) && (r.matches(action.getName().charAt(0), this.currExternal, this.currInternal)) ){
                System.out.print("@");
            } else {
                System.out.print(" ");
            }
            r.calculateActivation(now);
            r.printRule();
        }
    }

    /** convenience version of printRules with no selected action */
    public void printRules() { printRules(null); }


    private void initCurrInternal() {
        //Initialize current and previous internal sensors with random values
        for(int i = 0; i < NUMINTERNAL; i++) {
            this.currInternal.put(i, false);
        }
    }

    public void buildPathFromEmpty() {
        this.root = new TreeNode(this, this.rules, this.now, this.currInternal,
                this.currExternal, '\0', "");
        this.root.genSuccessors(3);

        //Find an entire sequence of characters that can reach the goal and get the current action to take
        this.path = this.root.bfFindBestGoalPath(this.root).replace("\0", "");
        //if unable to find path, produce random path for it to take
        if (this.path.equals("")) {
            this.path = randomActionPath();
            System.out.println("random path: " + this.path);
        }else {
            System.out.println("path: " + this.path);
        }
    }

    public void updateRules() {
//        for(int i = 0; i < this.rules.size()/5; i++) {
//            double lowestActivation = 1000.0;
            Rule worstRule = this.rules.get(0);
            worstRule.calculateActivation(this.now);
            // Find for lowest rule
            for (Rule r : this.rules) {
                if(r.calculateActivation(this.now) < worstRule.getActivationLevel()){
                    worstRule = r;
                }
            }
            this.removeRule(worstRule);
            System.out.print("Removing rule: ");
            worstRule.printRule();
//        }
        this.generateRules();
    }

    /**
     * Fills up agent's rule inventory
     *
     */
    public void generateRules(){
//        //DEBUG: add a good rule to start: (IS_EVEN, false) a -> (GOAL, true)
//        SensorData s = new SensorData(false);
//        s.setSensor("IS_EVEN", false);
//        s.removeSensor("GOAL");
//        addRule(new Rule('a', s, new HashMap<Integer, Boolean>(), "GOAL", true));
//
//        //DEBUG: add another good rule: (IS_EVEN, true) b -> (IS_EVEN, false)
//        SensorData secondSensor = new SensorData(false);
//        secondSensor.setSensor("IS_EVEN", true);
//        secondSensor.removeSensor("GOAL");
//        addRule(new Rule('b', secondSensor, new HashMap<Integer, Boolean>(), "IS_EVEN", false));

        if(rules.size() == 0) {
            // Add a good rule: (IS_EVEN, false) a -> (GOAL, true)
            SensorData gr1 = new SensorData(false);
            gr1.setSensor("IS_EVEN", false);
            gr1.removeSensor("GOAL");
            Rule cheater = new Rule('a', gr1, new HashMap<Integer, Boolean>(), "GOAL", true);
//        newbie.setRHSInternal(0);
//        Rule.intSensorInUse[0] = true;
            addRule(cheater);
            cheater.addActivation(now, 15);

            // Add a good rule: (IS_EVEN, true) b -> (IS_EVEN, false)
            SensorData gr2 = new SensorData(false);
            gr2.setSensor("IS_EVEN", true);
            gr2.removeSensor("GOAL");
            Rule cheater2 = new Rule('b', gr2, new HashMap<Integer, Boolean>(), "IS_EVEN", false);
            addRule(cheater2);
            cheater2.addActivation(now, 15);

        }

        while(!ruleLimitReached()){
            Rule newRule = new Rule(this);
            // Make sure there are no duplicate rules
            while(this.rules.contains(newRule)) {
                newRule = new Rule(this);
            }
            addRule(newRule);
            newRule.addActivation(now, 15); // TODO this should not be a hard coded value
            //newRule.printRule();
        }
    }

    public boolean ruleLimitReached() {
        if (rules.size() >= MAXNUMRULES) { return true; }
        return false;
    }

    public void addRule(Rule newRule) {
        if (this.ruleLimitReached()) {
            return;
        }
        rules.add(newRule);
        //assign a sensor if one is available
        int isBase = rand.nextInt(PhuJusAgent.NUMINTERNAL);
        for(int i = 0; i < PhuJusAgent.NUMINTERNAL; ++i) {
            int isIndex = (isBase + i) % PhuJusAgent.NUMINTERNAL;
            if (! newRule.intSensorInUse[isIndex]) {
                newRule.setRHSInternal(isIndex);
                newRule.intSensorInUse[isIndex] = true;
                break;
            }
        }
    }

    public void removeRule(Rule r) {
        while(rules.contains(r)) {
            rules.remove(r);
            if(r.getRHSInternal() != -1) {
                System.out.println("Removed rule has internal sensor ID: " + r.getRHSInternal());
                r.turnOffIntSensorInUse(r.getRHSInternal());
            }
        }
    }

    public Rule[] getMatchArray() { return this.matchArray; }
    public void incrementMatchIdx() { this.lastMatchIdx=(this.lastMatchIdx+1)%RULEMATCHHISTORYLEN; }
    public int getLastMatchIdx() { return this.lastMatchIdx; }
    public int getRuleMatchHistoryLen() { return RULEMATCHHISTORYLEN; }

    public Vector<Rule> getRules() {return this.rules;}

    public void setNow(int now) { this.now = now; }
    public int getNow() { return now; }

    public boolean getPrevInternalValue(int id){
        if(this.prevInternal == null) {
            System.out.println("Null!");
        }
        if(this.prevInternal.get(Integer.valueOf(id)) == null) {
            System.out.println("Null!");
        }
        return this.prevInternal.get(Integer.valueOf(id));
    }

    public void setPrevInternal(HashMap<Integer, Boolean> prevInternal) { this.prevInternal = prevInternal; }

    public HashMap<Integer, Boolean> getCurrInternal() {return this.currInternal;}
    public void setCurrInternal(HashMap<Integer, Boolean> curIntern) {this.currInternal = curIntern;}

    public SensorData getCurrExternal() {return this.currExternal;}
    public void setCurrExternal(SensorData curExtern) {this.currExternal = curExtern;}

    public SensorData getPrevExternal() {return this.prevExternal;}
    public void setPrevExternal(SensorData prevExtern) {this.prevExternal = prevExtern;}

    public int getNumActions() { return actionList.length; }
    public Action[] getActionList() {return actionList;}

}
