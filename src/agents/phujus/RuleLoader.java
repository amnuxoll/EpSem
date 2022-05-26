package agents.phujus;

import framework.SensorData;

import java.io.File;
import java.io.FileNotFoundException;
import java.nio.file.Path;
import java.util.List;
import java.util.Scanner;
import java.util.Set;
import java.util.Vector;

/**
 * class RuleLoader
 * <p>
 * Instantiates TFRule objects from a specified .csv file
 * <p>
 */
public class RuleLoader {

    private PhuJusAgent agent;

    public RuleLoader(PhuJusAgent agent) {
        this.agent = agent;
    }

    public void loadRules(String path) throws FileNotFoundException {

        File ruleFile = new File(path);
        Scanner sc = new Scanner(ruleFile);
        sc.useDelimiter(",");

        while (sc.hasNextLine()) {
            System.out.println("");
            createTFRuleFromLine(sc.nextLine());
        }
    }

    private TFRule createTFRuleFromLine(String line) {

        try (Scanner rowScanner = new Scanner(line)) {
            rowScanner.useDelimiter(",");

            SensorData lhsExt;
            SensorData rhsExt;

            String[] lhsInt;
            String action;

            double conf;

            lhsInt = getLHSIntFromString(rowScanner.next());
            lhsExt = getSDFromString(rowScanner.next());
            action = rowScanner.next();
            rhsExt = getSDFromString(rowScanner.next());
            conf = rowScanner.nextDouble();

            return new TFRule(this.agent, action.charAt(0), lhsInt, lhsExt, rhsExt, conf);
        }
    }

    private String[] getLHSIntFromString(String token) {

        // TODO look at an ArrayList/Vector implementation instead of a hardcoded value
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
        return lhsInt.toArray(new String[0]);
    }

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

    public static void loadRulesInList(String path, List<Object> l){
//        for item in path
//                l.add(item)
        // TODO
    }


}
