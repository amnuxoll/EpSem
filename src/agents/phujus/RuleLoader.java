package agents.phujus;

import framework.SensorData;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.*;

/**
 * class RuleLoader
 * <p>
 * Instantiates TFRule objects from a specified .csv file
 * <p>
 */
public class RuleLoader {

    private PhuJusAgent agent;

    //the first time rules are loaded Rule.nextRuleId == 1.
    //but if rules are reloaded it can be different and internal
    //sensors references needed to be adjusted.  This variable
    //records the first rule id for each load for that purpose.
    private int ruleIdOffset = 1;

    public RuleLoader(PhuJusAgent agent) {
        this.agent = agent;
        this.ruleIdOffset = Rule.getNextRuleId() - 1;
    }

    /**
     * Loads a set of rules from a .csv file as a vector of TFRule objects
     * @param path the path to the csv file
     * @return vector containing all rules in the file
     */
    public void loadRules(String path) {

        // Checking if the file exists...
        File ruleFile = new File(path);
        if (!ruleFile.exists()) {
            System.err.println("The file at " + path + " could not be found in RuleLoader!");
            return;
        }

        Scanner sc = null;
        try {
            sc = new Scanner(ruleFile);
        } catch (FileNotFoundException e) {
            System.err.println("The file at " + path + " could not be found in RuleLoader!");
            return;
        }
        sc.useDelimiter(",");

        // Now begin reading each line in the file.
        //  - Lines which are blank or start with # (comment) are ignored.
        //  - PathRules start with an & to indicate that they are
        //    referencing an existing rule.
        //  - Rules created from each line are added to the PhuJusAgent via the
        //    addRule method.
        while (sc.hasNextLine()) {
            String readLine = sc.nextLine();

            // Ignore blank lines or comments
            if (readLine.length() == 0 || readLine.startsWith("#")) {
                continue;
            }

            TFRule tfNewbie = null;
            PathRule pathNewbie = null;
            // PathRules start with a ~/= to indicate that they are pathrules
            if (readLine.startsWith("&")) {
                pathNewbie = createPathRuleFromLine(readLine);
            }
            else {
                tfNewbie = createTFRuleFromLine(readLine);
                System.out.println("Loading rule " + readLine);
            }
            if (tfNewbie != null)   agent.addRule(tfNewbie);
            if (pathNewbie != null) agent.addRule(pathNewbie);
        }
    }

    /**
     * createPathRuleFromLine
     *
     * This method generates a PathRule object from a given String token. The token should
     * be in the format:
     *<p>
     * &X,&Y,??,?.?
     * <p>
     * It's important to note that PathRules are essentially: rule1 + rule2 -> sensors.
     * <p>
     * The & represents the ID of a rule. The integers X and Y represent the ID number of the rule being referenced.
     * The ?? represents a set of external sensors that the rule predicts will be true given the LHS rule conditions
     * are confirmed. The final value, ?.?, represents confidence.
     * <p>
     * e.g &3,&1,01,1.0
     * <p>
     * Represents a PathRule which predicts that if Rule 3 and Rule 1 are activated, the outcome will
     * be external sensors of 01 with a confidence of 1.0.
     * @param line The string representing the PathRule in the form &X,&Y,??,?.?
     * @return a new PathRule generated from the line
     */
    public PathRule createPathRuleFromLine(String line) {

        line = removeTailComment(line);

        line = line.trim();

        try (Scanner rowScanner = new Scanner(line)) {
            rowScanner.useDelimiter(",");

            System.out.println("PATHRULE:");

            // PathRules are in the format: rule1 + rule2 -> action
            // [~X],~Y,??,?.?
            String next = rowScanner.next().trim();
            Rule rule1 = getRuleFromLHS(next);

            // ~X,[~Y],??,?.?
            next = rowScanner.next().trim();
            Rule rule2 = getRuleFromLHS(next);

            // ~X,~Y,[??],?.?
            next = rowScanner.next().trim();
            SensorData exSensor = getSDFromString(next);

            // ~X,~Y,??,[?.?]
            // TODO Should we add confidence as an init value for PathRules?
            double conf = rowScanner.nextDouble();

            return new PathRule(agent, rule1, rule2, exSensor);
        }
    }//createPathRuleFromLine

    /**
     * getRuleFromLHS
     *
     * Should only be called by createPathRuleFromLine. Returns the rule referenced by a given token
     * in the format [~=]X.
     * <p>
     * e.g ~3 gives a TFRule with ID 3.
     * <p>
     * e.g =4 gives a PathRule with ID 4.
     * @param next
     * @return
     */
    private Rule getRuleFromLHS(String next) {
        Rule rule;
        String ruleToken = next.substring(1);
        int ruleID = Integer.parseInt(ruleToken);

        // The ~X ~Y values in the PathRule string indicate rule numbers. An = indicates a PathRule ID number,
        // and a ~ indicates a normal rule ID number.
        rule = agent.getRules().get(ruleID + ruleIdOffset);
        return rule;
    }

    /**
     * Instantiates a TFRule object from a given string. The string must be in the following format:
     * <p>
     * [LHSInt;LHSInt;...],(LHSExt),(action),(RHSExt),(confidence)
     * <p>
     * If there are no LHSInt, then leave the field as zero.
     * <p>
     * e.g, 0,10,a,00,1.0 or 3;7,00,b,01,0.7
     * @param line string representation of a TFRule
     * @return a TFRule created from the given line
     */
    public TFRule createTFRuleFromLine(String line) {

        line = removeTailComment(line);

        //skip blank lines or comments
        line = line.trim();

        try (Scanner rowScanner = new Scanner(line)) {
            rowScanner.useDelimiter(",");

            SensorData lhsExt = null;
            SensorData rhsExt = null;

            String[] lhsInt = null;
            String action = null;

            double conf = -1.0;

            String lhsIntData = rowScanner.next().trim();

            TFRule.RuleOperator operator = getRuleOperatorFromString(lhsIntData);
            try {
                lhsInt = getLHSIntFromString(lhsIntData, operator);
                lhsExt = getSDFromString(rowScanner.next().trim());
                action = rowScanner.next().trim();
                rhsExt = getSDFromString(rowScanner.next().trim());
                String dblStr = rowScanner.next().trim();
                conf = Double.parseDouble(dblStr);
            } catch(InputMismatchException ime) {
                System.err.println("oh no!");
            }

            return new TFRule(this.agent, action.charAt(0), lhsInt, lhsExt, rhsExt, conf, operator);
        }
    }

    /**
     * removeTailComment
     *
     * Removes a comment at the end of a line, such as
     * <p>
     * *,00,a,10,0.33,#0a->3
     * @param line
     * @return The given String without the tail comment.
     */
    private String removeTailComment(String line) {
        //remove any comment from this line
        int commentPos = line.indexOf(",#");
        if (commentPos != -1) {
            line = line.substring(0, commentPos);
        }
        return line;
    }

    /**
     * Helper method for getting the operator of the rule (*[any], /[andor], ;[and])
     * @param token
     * @return
     */
    private TFRule.RuleOperator getRuleOperatorFromString(String token) {

        TFRule.RuleOperator operator = TFRule.RuleOperator.AND;

        if(token.contains("/")) {
            operator = TFRule.RuleOperator.ANDOR;
        }
        else if (token.contains("*")) {
            operator = TFRule.RuleOperator.ALL;
        }

        return operator;
    }

    /**
     * Helper method for retrieving the LHSInt values from a string. The token provided
     * should be in the format <LHSInt[/*;]LHSInt[/*;]...>
     * <p>
     * e.g 3;7 or 4/10/7 or *
     * <p>
     * The token is then converted into an array of strings, where every index is one of the
     * LHSInt values. This array should be used as a parameter for the lhsInt parameter of the
     * TFRule constructor.
     * <p>
     * e.g 3;7 -> ["3","7"]
     * @param token
     * @return an array of strings that represent the LHSInt values or null if there are none
     */
    private String[] getLHSIntFromString(String token, TFRule.RuleOperator operator) {

        if (token == null) {
            return null;
        }

        if (token.length() < 1) {
            return null;
        }

        if(operator == TFRule.RuleOperator.ALL){
            return new String[]{"-1"};
        }

        Vector<String> lhsInt = new Vector<>();

        try (Scanner colonScanner = new Scanner(token)) {
            colonScanner.useDelimiter(operator.character);

            while (colonScanner.hasNext()) {
                String next = colonScanner.next();
                if (next.equals("0")) {
                    return null;
                }

                lhsInt.add(next);
            }
        }
        String[] lhsIntArr = lhsInt.toArray(new String[0]);

        //If these rules are being reloaded, the LHS int values need to be adjusted
        if (this.ruleIdOffset > 0) {
            for(int i = 0; i < lhsIntArr.length; ++i) {
                int tmp = Integer.parseInt(lhsIntArr[i]);
                tmp += this.ruleIdOffset;
                lhsIntArr[i] = "" + tmp;
            }
        }

        return lhsIntArr;
    }//getLHSIntFromString

    /**
     * Helper method that Instantiates a SensorData object from a given string representation. Token should be
     * in a binary form. It's not very clear how this works so I'll do my best to explain:
     * <p>
     * SensorData contains an instance variable called 'data', which is a HashMap of String keys
     * to Object values. In our case, we're mapping String keys to boolean values. Currently,
     * a sensor really only has two values: "GOAL" and "IS_ODD". In the future, however, this could
     * definitely change. Every index in the given token represents a boolean value for one of these
     * String keys in the SensorData. For example, the token '10' represents a SensorData where the
     * "IS_ODD" value is true and the "GOAL" value is false. This method maps tokens to SensorData
     * for any number of slots in the SensorData (beyond just IS_ODD or GOAL). It knows which values
     * to map to by referencing the SensorData values that the agent is currently using.
     * @param token binary representation of a SensorData object
     * @return the SensorData object that the token represents
     */
    private SensorData getSDFromString(String token) {
        final Set<String> sensorNames = agent.getPrevExternal().getSensorNames();

        int numSensorNames = sensorNames.size();

        SensorData data = new SensorData(false);
        int idx = 0;
        for (String name : sensorNames) {
            idx++;
            data.setSensor(name, token.charAt(numSensorNames - idx) == '1');
        }
        return data;
    }
}
