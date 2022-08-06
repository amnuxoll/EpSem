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

    private final PhuJusAgent agent;

    //the first time rules are loaded Rule.nextRuleId == 1.
    //but if rules are reloaded it can be different and internal
    //sensors references needed to be adjusted.  This variable
    //records the first rule id for each load for that purpose.
    private final int ruleIdOffset;

    public RuleLoader(PhuJusAgent agent) {
        this.agent = agent;
        this.ruleIdOffset = Rule.getNextRuleId() - 1;
    }

    /**
     * Loads a set of rules from a .csv file as a vector of TFRule objects
     * @param path the path to the csv file
     */
    public void loadRules(String path) {

        // Checking if the file exists...
        File ruleFile = new File(path);
        if (!ruleFile.exists()) {
            System.err.println("The file at " + path + " could not be found in RuleLoader!");
            return;
        }

        Scanner sc;
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
            // PathRules start with a ~/= to indicate that they are pathrules
            if (readLine.startsWith("&")) {
                //TODO:  Fix the PathRule loading code
                agent.debugPrintln("PathRule loading code not implemented.");
                //pathNewbie = createPathRuleFromLine(readLine);
            }
            else {
                tfNewbie = createTFRuleFromLine(readLine);
                System.out.println("Loading rule " + readLine);
            }
            if (tfNewbie != null)   agent.addRule(tfNewbie);
        }
    }

    /**
     * Instantiates a TFRule object from a given string. The string must be in the following format:
     * <p>
     * [LHSInt;LHSInt;...],(LHSExt),(action),(RHSExt),(confidence)
     * <p>
     * If there are no LHSInt, then leave the field as zero.
     * <p>
     * e.g, 0,10,c,00,1.0 or 3;7,00,b,01,0.7
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

            int[] lhsInt = null;
            String action = "\0";

            double conf = -1.0;

            String lhsIntData = rowScanner.next().trim();

            try {
                lhsInt = getLHSIntFromString(lhsIntData);
                lhsExt = getSDFromString(rowScanner.next().trim());
                action = rowScanner.next().trim();
                rhsExt = getSDFromString(rowScanner.next().trim());
                String dblStr = rowScanner.next().trim();
                conf = Double.parseDouble(dblStr);
            } catch(InputMismatchException ime) {
                System.err.println("oh no!");
            }

            return new TFRule(this.agent, action.charAt(0), lhsInt, lhsExt, rhsExt, conf);
        }
    }

    /**
     * removeTailComment
     *
     * Removes a comment at the end of a line, such as
     * <p>
     *      input:  *,00,b,10,0.33,#0a->3
     *      output: *,00,b,10,0.33
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
     * @return an array of strings that represent the LHSInt values or null if there are none
     */
    //TODO:  Change this to turn int[] instead of String[].  Then you can remove extraneous ctor and initInternal methods in TFRule
    private int[] getLHSIntFromString(String token) {

        if (token == null) {
            return null;
        }

        if (token.length() < 1) {
            return null;
        }

        Vector<String> lhsInt = new Vector<>();

        try (Scanner colonScanner = new Scanner(token)) {
            colonScanner.useDelimiter(";");

            while (colonScanner.hasNext()) {
                String next = colonScanner.next();
                if (next.equals("0")) {
                    return null;
                }

                lhsInt.add(next);
            }
        }

        //Convert to array of int
        int[] lhsIntArr = new int[lhsInt.size()];
        for(int i = 0; i < lhsIntArr.length; ++i) {
            lhsIntArr[i] = Integer.parseInt(lhsInt.get(i));
        }

        //If these rules are being reloaded, the LHS int values need to be adjusted
        if (this.ruleIdOffset > 0) {
            for(int i = 0; i < lhsIntArr.length; ++i) {
                lhsIntArr[i] += this.ruleIdOffset;
            }
        }

        return lhsIntArr;
    }//getLHSIntFromString

    /**
     * Helper method that Instantiates a SensorData object from a given string
     * representation. Token should be in a binary form. It's not very clear how
     * this works, so I'll do my best to explain:
     * <p>
     * SensorData contains an instance variable called 'data', which is a
     * HashMap of String keys to Object values. In our case, we're mapping
     * String keys to boolean values. Currently, a sensor really only has two
     * values: "GOAL" and "IS_ODD". In the future, however, this could
     * definitely change. Every index in the given token represents a boolean
     * value for one of these String keys in the SensorData. For example, the
     * token '10' represents a SensorData where the "IS_ODD" value is true and
     * the "GOAL" value is false. This method maps tokens to SensorData for any
     * number of slots in the SensorData (beyond just IS_ODD or GOAL). It knows
     * which values to map to by referencing the SensorData values that the
     * agent is currently using.
     *
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
