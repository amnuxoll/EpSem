package agents.phujus;

import framework.SensorData;

import java.io.File;
import java.io.FileNotFoundException;
import java.nio.file.Path;
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
    public void loadRules(String path, Collection<TFRule> t) {
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

        while (sc.hasNextLine()) {
            System.out.println("");
            TFRule newbie = createTFRuleFromLine(sc.nextLine());
            if (newbie != null)  t.add(newbie);
        }
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

        //remove any comment from this line
        int commentPos = line.indexOf(",#");
        if (commentPos != -1) {
            line = line.substring(0, commentPos);
        }

        //skip blank lines
        line = line.trim();
        if (line.length() == 0) return null;

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

    /**
     * Loads a set of rules from a file into a given collection of TFRules by overwriting any existing rules.
     * @param path the path to a .csv file containing the rules
     * @param c the collection
     */
    public void overwriteRulesInList(String path, Vector<TFRule> c) {
        c.clear();
        loadRules(path, c);
    }


}
