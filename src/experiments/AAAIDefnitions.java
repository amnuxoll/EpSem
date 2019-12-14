package experiments;

import agents.marz.MaRzAgentProvider;
import agents.marzrules.Heuristic;
import agents.marzrules.RulesAgentProvider;
import agents.nsm.NSMAgentProvider;
import environments.fsm.FSMEnvironment;
import environments.fsm.FSMEnvironmentProvider;
import environments.fsm.FSMTransitionTableBuilder;
import framework.*;
import utils.Random;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.EnumSet;

/**
 * This class contains the parameters used for data collection for AAAI.
 */
public class AAAIDefnitions {

    /** TestSuite Configurations */
    static final int NumberOfIterations = 1000;
    static final int NumberOfGoals = 1000;

    /** FSM Environment sizes */
    static final int SmallFSM = 10;
    static final int MediumFSM = 30;
    static final int LargeFSM = 50;
    static final int XLargeFSM = 70;
    static final int XXLargeFSM = 90;

    /** Alphabet sizes */
    static final int SmallAlphabet = 2;
    static final int MediumAlphabet = 3;
    static final int LargeAlphabet = 5;
    static final int XLargeAlphabet = 7;
    static final int XXLargeAlphabet = 9;

    private static TestSuiteConfiguration configuration = new TestSuiteConfiguration(NumberOfIterations, NumberOfGoals);

    private static IEnvironmentProvider[] environmentProviders = new IEnvironmentProvider[] {
            new FSMEnvironmentProvider(new FSMTransitionTableBuilder(SmallAlphabet, SmallFSM, Random.getTrue()), FSMEnvironment.Sensor.NO_SENSORS),
            new FSMEnvironmentProvider(new FSMTransitionTableBuilder(SmallAlphabet, MediumFSM, Random.getTrue()), FSMEnvironment.Sensor.NO_SENSORS),
            new FSMEnvironmentProvider(new FSMTransitionTableBuilder(SmallAlphabet, LargeFSM, Random.getTrue()), FSMEnvironment.Sensor.NO_SENSORS),
            new FSMEnvironmentProvider(new FSMTransitionTableBuilder(SmallAlphabet, XLargeFSM, Random.getTrue()), FSMEnvironment.Sensor.NO_SENSORS),
            new FSMEnvironmentProvider(new FSMTransitionTableBuilder(SmallAlphabet, XXLargeFSM, Random.getTrue()), FSMEnvironment.Sensor.NO_SENSORS),

            new FSMEnvironmentProvider(new FSMTransitionTableBuilder(MediumAlphabet, SmallFSM, Random.getTrue()), FSMEnvironment.Sensor.NO_SENSORS),
            new FSMEnvironmentProvider(new FSMTransitionTableBuilder(MediumAlphabet, MediumFSM, Random.getTrue()), FSMEnvironment.Sensor.NO_SENSORS),
            new FSMEnvironmentProvider(new FSMTransitionTableBuilder(MediumAlphabet, LargeFSM, Random.getTrue()), FSMEnvironment.Sensor.NO_SENSORS),
            new FSMEnvironmentProvider(new FSMTransitionTableBuilder(MediumAlphabet, XLargeFSM, Random.getTrue()), FSMEnvironment.Sensor.NO_SENSORS),
            new FSMEnvironmentProvider(new FSMTransitionTableBuilder(MediumAlphabet, XXLargeFSM, Random.getTrue()), FSMEnvironment.Sensor.NO_SENSORS),

            new FSMEnvironmentProvider(new FSMTransitionTableBuilder(LargeAlphabet, SmallFSM, Random.getTrue()), FSMEnvironment.Sensor.NO_SENSORS),
            new FSMEnvironmentProvider(new FSMTransitionTableBuilder(LargeAlphabet, MediumFSM, Random.getTrue()), FSMEnvironment.Sensor.NO_SENSORS),
            new FSMEnvironmentProvider(new FSMTransitionTableBuilder(LargeAlphabet, LargeFSM, Random.getTrue()), FSMEnvironment.Sensor.NO_SENSORS),
            new FSMEnvironmentProvider(new FSMTransitionTableBuilder(LargeAlphabet, XLargeFSM, Random.getTrue()), FSMEnvironment.Sensor.NO_SENSORS),
            new FSMEnvironmentProvider(new FSMTransitionTableBuilder(LargeAlphabet, XXLargeFSM, Random.getTrue()), FSMEnvironment.Sensor.NO_SENSORS),

            new FSMEnvironmentProvider(new FSMTransitionTableBuilder(XLargeAlphabet, SmallFSM, Random.getTrue()), FSMEnvironment.Sensor.NO_SENSORS),
            new FSMEnvironmentProvider(new FSMTransitionTableBuilder(XLargeAlphabet, MediumFSM, Random.getTrue()), FSMEnvironment.Sensor.NO_SENSORS),
            new FSMEnvironmentProvider(new FSMTransitionTableBuilder(XLargeAlphabet, LargeFSM, Random.getTrue()), FSMEnvironment.Sensor.NO_SENSORS),
            new FSMEnvironmentProvider(new FSMTransitionTableBuilder(XLargeAlphabet, XLargeFSM, Random.getTrue()), FSMEnvironment.Sensor.NO_SENSORS),
            new FSMEnvironmentProvider(new FSMTransitionTableBuilder(XLargeAlphabet, XXLargeFSM, Random.getTrue()), FSMEnvironment.Sensor.NO_SENSORS),

            new FSMEnvironmentProvider(new FSMTransitionTableBuilder(XXLargeAlphabet, SmallFSM, Random.getTrue()), FSMEnvironment.Sensor.NO_SENSORS),
            new FSMEnvironmentProvider(new FSMTransitionTableBuilder(XXLargeAlphabet, MediumFSM, Random.getTrue()), FSMEnvironment.Sensor.NO_SENSORS),
            new FSMEnvironmentProvider(new FSMTransitionTableBuilder(XXLargeAlphabet, LargeFSM, Random.getTrue()), FSMEnvironment.Sensor.NO_SENSORS),
            new FSMEnvironmentProvider(new FSMTransitionTableBuilder(XXLargeAlphabet, XLargeFSM, Random.getTrue()), FSMEnvironment.Sensor.NO_SENSORS),
            new FSMEnvironmentProvider(new FSMTransitionTableBuilder(XXLargeAlphabet, XXLargeFSM, Random.getTrue()), FSMEnvironment.Sensor.NO_SENSORS),

    };

    private static IEnvironmentProvider[] environmentProvidersSensors = new IEnvironmentProvider[] {
            new FSMEnvironmentProvider(new FSMTransitionTableBuilder(SmallAlphabet, SmallFSM, Random.getTrue()), EnumSet.of(FSMEnvironment.Sensor.IS_EVEN)),
            new FSMEnvironmentProvider(new FSMTransitionTableBuilder(SmallAlphabet, MediumFSM, Random.getTrue()), EnumSet.of(FSMEnvironment.Sensor.IS_EVEN)),
            new FSMEnvironmentProvider(new FSMTransitionTableBuilder(SmallAlphabet, LargeFSM, Random.getTrue()), EnumSet.of(FSMEnvironment.Sensor.IS_EVEN)),
            new FSMEnvironmentProvider(new FSMTransitionTableBuilder(SmallAlphabet, XLargeFSM, Random.getTrue()), EnumSet.of(FSMEnvironment.Sensor.IS_EVEN)),
            new FSMEnvironmentProvider(new FSMTransitionTableBuilder(SmallAlphabet, XXLargeFSM, Random.getTrue()), EnumSet.of(FSMEnvironment.Sensor.IS_EVEN)),

            new FSMEnvironmentProvider(new FSMTransitionTableBuilder(MediumAlphabet, SmallFSM, Random.getTrue()), EnumSet.of(FSMEnvironment.Sensor.IS_EVEN)),
            new FSMEnvironmentProvider(new FSMTransitionTableBuilder(MediumAlphabet, MediumFSM, Random.getTrue()), EnumSet.of(FSMEnvironment.Sensor.IS_EVEN)),
            new FSMEnvironmentProvider(new FSMTransitionTableBuilder(MediumAlphabet, LargeFSM, Random.getTrue()), EnumSet.of(FSMEnvironment.Sensor.IS_EVEN)),
            new FSMEnvironmentProvider(new FSMTransitionTableBuilder(MediumAlphabet, XLargeFSM, Random.getTrue()), EnumSet.of(FSMEnvironment.Sensor.IS_EVEN)),
            new FSMEnvironmentProvider(new FSMTransitionTableBuilder(MediumAlphabet, XXLargeFSM, Random.getTrue()), EnumSet.of(FSMEnvironment.Sensor.IS_EVEN)),

            new FSMEnvironmentProvider(new FSMTransitionTableBuilder(LargeAlphabet, SmallFSM, Random.getTrue()), EnumSet.of(FSMEnvironment.Sensor.IS_EVEN)),
            new FSMEnvironmentProvider(new FSMTransitionTableBuilder(LargeAlphabet, MediumFSM, Random.getTrue()), EnumSet.of(FSMEnvironment.Sensor.IS_EVEN)),
            new FSMEnvironmentProvider(new FSMTransitionTableBuilder(LargeAlphabet, LargeFSM, Random.getTrue()), EnumSet.of(FSMEnvironment.Sensor.IS_EVEN)),
            new FSMEnvironmentProvider(new FSMTransitionTableBuilder(LargeAlphabet, XLargeFSM, Random.getTrue()), EnumSet.of(FSMEnvironment.Sensor.IS_EVEN)),
            new FSMEnvironmentProvider(new FSMTransitionTableBuilder(LargeAlphabet, XXLargeFSM, Random.getTrue()), EnumSet.of(FSMEnvironment.Sensor.IS_EVEN)),

            new FSMEnvironmentProvider(new FSMTransitionTableBuilder(XLargeAlphabet, SmallFSM, Random.getTrue()), EnumSet.of(FSMEnvironment.Sensor.IS_EVEN)),
            new FSMEnvironmentProvider(new FSMTransitionTableBuilder(XLargeAlphabet, MediumFSM, Random.getTrue()), EnumSet.of(FSMEnvironment.Sensor.IS_EVEN)),
            new FSMEnvironmentProvider(new FSMTransitionTableBuilder(XLargeAlphabet, LargeFSM, Random.getTrue()), EnumSet.of(FSMEnvironment.Sensor.IS_EVEN)),
            new FSMEnvironmentProvider(new FSMTransitionTableBuilder(XLargeAlphabet, XLargeFSM, Random.getTrue()), EnumSet.of(FSMEnvironment.Sensor.IS_EVEN)),
            new FSMEnvironmentProvider(new FSMTransitionTableBuilder(XLargeAlphabet, XXLargeFSM, Random.getTrue()), EnumSet.of(FSMEnvironment.Sensor.IS_EVEN)),

            new FSMEnvironmentProvider(new FSMTransitionTableBuilder(XXLargeAlphabet, SmallFSM, Random.getTrue()), EnumSet.of(FSMEnvironment.Sensor.IS_EVEN)),
            new FSMEnvironmentProvider(new FSMTransitionTableBuilder(XXLargeAlphabet, MediumFSM, Random.getTrue()), EnumSet.of(FSMEnvironment.Sensor.IS_EVEN)),
            new FSMEnvironmentProvider(new FSMTransitionTableBuilder(XXLargeAlphabet, LargeFSM, Random.getTrue()), EnumSet.of(FSMEnvironment.Sensor.IS_EVEN)),
            new FSMEnvironmentProvider(new FSMTransitionTableBuilder(XXLargeAlphabet, XLargeFSM, Random.getTrue()), EnumSet.of(FSMEnvironment.Sensor.IS_EVEN)),
            new FSMEnvironmentProvider(new FSMTransitionTableBuilder(XXLargeAlphabet, XXLargeFSM, Random.getTrue()), EnumSet.of(FSMEnvironment.Sensor.IS_EVEN)),

    };

    public static TestSuite MaRz = new TestSuite(
            AAAIDefnitions.configuration,
            AAAIDefnitions.environmentProviders,
            new IAgentProvider[] { new MaRzAgentProvider() }
    );

    public static TestSuite NSM = new TestSuite(
            AAAIDefnitions.configuration,
            AAAIDefnitions.environmentProviders,
            new IAgentProvider[] { new NSMAgentProvider() }
    );

    public static TestSuite ARONoSensor = new TestSuite(
            AAAIDefnitions.configuration,
            AAAIDefnitions.environmentProviders,
            new IAgentProvider[] { new RulesAgentProvider(new Heuristic(1, 0), 500) }
    );

    public static TestSuite AROWithSensor = new TestSuite(
            AAAIDefnitions.configuration,
            AAAIDefnitions.environmentProvidersSensors,
            new IAgentProvider[] { new RulesAgentProvider(new Heuristic(1, 0), 500) }
    );
}
