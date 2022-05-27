package tests.agents.phujus;

import agents.phujus.PhuJusAgent;
import agents.phujus.RuleLoader;
import agents.phujus.TFRule;
import framework.SensorData;
import tests.Assertions;
import tests.EpSemTest;
import tests.EpSemTestClass;

import java.util.*;

@EpSemTestClass
public class RuleLoaderTest {

    private static  SensorData data_00;
    private static  SensorData data_10;
    private static  SensorData data_01;
    private static  SensorData data_11;

    private static PhuJusAgent agent;
    private static RuleLoader loader;

    private static void initialize() throws Exception {

        data_00 = new SensorData(false);
        data_00.setSensor("IS_ODD", false);
        data_10 = new SensorData(false);
        data_10.setSensor("IS_ODD", true);
        data_01 = new SensorData(true);
        data_01.setSensor("IS_ODD", false);
        data_11 = new SensorData(true);
        data_11.setSensor("IS_ODD", true);

        agent = PhuJusAgentTest.quickAgentGen("ab", "0100");
        agent.setCurrExternal(data_00);
        agent.getNextAction(data_00);
        loader = new RuleLoader(agent);
    }

    @EpSemTest
    public static void testCreateTFRuleFromLine() throws Exception {

        initialize();

        String[] tokens = {
                "0,10,a,00,1.0",
                "1,10,a,10,0.66",
                "3;7,00,b,01,0.7",
                "4;10;22,01,a,00,0.9"
        };

        TFRule rule = loader.createTFRuleFromLine(tokens[0]);
        Assertions.assertTrue(rule.isMatch(
                new TFRule(agent, 'a', null, data_10, data_00, 1.0d)
        ));

        rule = loader.createTFRuleFromLine(tokens[1]);
        Assertions.assertTrue(rule.isMatch(
                new TFRule(agent, 'a', new String[]{"1"}, data_10, data_10, 0.66d)
        ));

        rule = loader.createTFRuleFromLine(tokens[2]);
        Assertions.assertTrue(rule.isMatch(
                new TFRule(agent, 'b', new String[]{"3", "7"}, data_00, data_01, 0.7d)
        ));


        rule = loader.createTFRuleFromLine(tokens[3]);
        Assertions.assertTrue(rule.isMatch(
                new TFRule(agent, 'a', new String[]{"4", "10", "22"}, data_01, data_00, 0.9d)
        ));
    }

    @EpSemTest
    public static void testLoadRulesInList() throws Exception {

        initialize();
        Vector<TFRule> test_list = new Vector<>();
        loader.loadRules("./src/agents/phujus/res/test.csv",
                test_list);

        Assertions.assertTrue(test_list.size() == 24);
    }
}
