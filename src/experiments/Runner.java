package experiments;

import agents.demo.DemoAgentProvider;
import agents.discr.MaRzLearnerProvider;
import agents.juno.JunoAgentProvider;
import agents.juno.JunoConfiguration;
import agents.marz.MaRzAgentProvider;
import agents.marzrules.RulesAgentProvider;
import agents.nsm.NSMAgentProvider;
import agents.phujus.PhuJusAgentProvider;
import agents.predr.PredrAgentProvider;
import agents.wfc.WFCAgentProvider;
import agents.ndxr.NdxrAgentProvider;
import agents.tfsocket.TFSocketAgentProvider;
import environments.fsm.FSMEnvironment;
import environments.fsm.FSMEnvironmentProvider;
import environments.meta.MetaConfiguration;
import environments.meta.MetaEnvironmentProvider;
import framework.*;
import resultcompilers.file.FileResultCompiler;
import utils.DirectoryUtils;
import environments.fsm.FSMTransitionTableBuilder;
import agents.marzrules.Heuristic;
import utils.Random;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.lang.reflect.Field;
import java.util.EnumSet;

public class Runner {
    //region TestSuites
    private static TestSuite JunoFSM = new TestSuite(
            TestSuiteConfiguration.MEDIUM,
            new IEnvironmentProvider[] {
                    new FSMEnvironmentProvider(new FSMTransitionTableBuilder(3, 50, Random.getTrue()), EnumSet.of(FSMEnvironment.Sensor.IS_EVEN))
            },
            new IAgentProvider[] {
                    new JunoAgentProvider(new JunoConfiguration(true, .7, Double.MAX_VALUE))
            }
    );

    private static TestSuite MarzFSM = new TestSuite(
            TestSuiteConfiguration.QUICK,
            new IEnvironmentProvider[] {
                    new FSMEnvironmentProvider(new FSMTransitionTableBuilder(4, 40, Random.getFalse()), FSMEnvironment.Sensor.NO_SENSORS),
            },
            new IAgentProvider[] {
                    new MaRzAgentProvider()
            }
    );

    private static TestSuite MarzFSMPrintout = new TestSuite(
            TestSuiteConfiguration.QUICK,
            new IEnvironmentProvider[] {
                    new FSMEnvironmentProvider(new FSMTransitionTableBuilder(2, 25, Random.getFalse()), EnumSet.of(FSMEnvironment.Sensor.IS_EVEN), true)
            },
            new IAgentProvider[] {
                    new MaRzAgentProvider()
            }
    );

    private static TestSuite ARO_AGENT = new TestSuite(
            TestSuiteConfiguration.MEDIUM,
            new IEnvironmentProvider[] {
                    //new FSMEnvironmentProvider(new FSMTransitionTableBuilder(6, 100, Random.getFalse()), EnumSet.of(FSMEnvironment.Sensor.IS_EVEN), false),
                    new FSMEnvironmentProvider(new FSMTransitionTableBuilder(3, 50, Random.getFalse()), EnumSet.of(FSMEnvironment.Sensor.NOISE1)),
                    //new FSMEnvironmentProvider(new FSMTransitionTableBuilder(2, 50, Random.getFalse()), EnumSet.of(FSMEnvironment.Sensor.IS_EVEN))
            },
            new IAgentProvider[] {
                    new RulesAgentProvider(new Heuristic(1, 0), 50),
                    //new MaRzAgentProvider()
            }
    );

    private static TestSuite HeuristicTest = new TestSuite(
            TestSuiteConfiguration.FULL,
            new IEnvironmentProvider[] {
                    new FSMEnvironmentProvider(new FSMTransitionTableBuilder(2, 15, Random.getFalse()), EnumSet.of(FSMEnvironment.Sensor.IS_EVEN), false),
                    new FSMEnvironmentProvider(new FSMTransitionTableBuilder(2, 50, Random.getFalse()), EnumSet.of(FSMEnvironment.Sensor.IS_EVEN), false),
                    new FSMEnvironmentProvider(new FSMTransitionTableBuilder(2, 100, Random.getFalse()), EnumSet.of(FSMEnvironment.Sensor.IS_EVEN), false),
                    new FSMEnvironmentProvider(new FSMTransitionTableBuilder(6, 15, Random.getFalse()), EnumSet.of(FSMEnvironment.Sensor.IS_EVEN), false),
                    new FSMEnvironmentProvider(new FSMTransitionTableBuilder(6, 50, Random.getFalse()), EnumSet.of(FSMEnvironment.Sensor.IS_EVEN), false),
                    new FSMEnvironmentProvider(new FSMTransitionTableBuilder(6, 100, Random.getFalse()), EnumSet.of(FSMEnvironment.Sensor.IS_EVEN), false)
            },
            new IAgentProvider[] {
                    //new RulesAgentProvider(new SuffixNodeProvider(), new Heuristic(0.5, -1)),
                    //new RulesAgentProvider(new SuffixNodeProvider(), new Heuristic(0.5, 0)),
                    //new RulesAgentProvider(new SuffixNodeProvider(), new Heuristic(0.5, 1)),
                    //new RulesAgentProvider(new SuffixNodeProvider(), new Heuristic(1, -1)),
                    new RulesAgentProvider(new Heuristic(1, 0))
                    //new RulesAgentProvider(new SuffixNodeProvider(), new Heuristic(1, 1))
            }
    );

    private static TestSuite SingleHeuristicTest = new TestSuite(
            TestSuiteConfiguration.MEDIUM,
            new IEnvironmentProvider[] {
                    //new FSMEnvironmentProvider(new FSMTransitionTableBuilder(4, 50, Random.getFalse()), FSMEnvironment.Sensor.NO_SENSORS),
                    new FSMEnvironmentProvider(new FSMTransitionTableBuilder(4, 100, Random.getFalse()), EnumSet.of(FSMEnvironment.Sensor.IS_EVEN))
                    //new FSMEnvironmentProvider(new FSMTransitionTableBuilder(4, 50, Random.getFalse()), EnumSet.of(FSMEnvironment.Sensor.IS_EVEN, FSMEnvironment.Sensor.MOD_3))
            },
            new IAgentProvider[] {
                    new RulesAgentProvider(new Heuristic(1, 0)),
                    new MaRzAgentProvider()
            }
    );

    private static TestSuite NsmVsMaRzFSM = new TestSuite(
            TestSuiteConfiguration.FULL,
            new IEnvironmentProvider[] {
                    new FSMEnvironmentProvider(new FSMTransitionTableBuilder(3, 30, Random.getTrue()), EnumSet.of(FSMEnvironment.Sensor.IS_EVEN)),
            },
            new IAgentProvider[] {
                    new NSMAgentProvider(),
                    new MaRzAgentProvider()
            }
    );

    private static TestSuite MarzLearnerFSM = new TestSuite(
            TestSuiteConfiguration.QUICK,
            new IEnvironmentProvider[] {
                    new FSMEnvironmentProvider(new FSMTransitionTableBuilder(3, 30, Random.getTrue()), FSMEnvironment.Sensor.ALL_SENSORS),
            },
            new IAgentProvider[] {
                    new MaRzLearnerProvider(),
                    //new MaRzAgentProvider<>(new SuffixNodeProvider())
            }
    );

    private static TestSuite JunoVMarz3_30 = new TestSuite(
            TestSuiteConfiguration.MEDIUM,
            new IEnvironmentProvider[] {
                    new FSMEnvironmentProvider(new FSMTransitionTableBuilder(3, 30, Random.getTrue()), EnumSet.of(FSMEnvironment.Sensor.IS_EVEN, FSMEnvironment.Sensor.NOISE1)),
            },
            new IAgentProvider[] {
                    new JunoAgentProvider(new JunoConfiguration(true, .7, Double.MAX_VALUE)),
                    new MaRzAgentProvider()
            }
    );

    private static TestSuite JunoVJunoBail = new TestSuite(
            TestSuiteConfiguration.MEDIUM,
            new IEnvironmentProvider[] {
                    new FSMEnvironmentProvider(new FSMTransitionTableBuilder(3, 30, Random.getTrue()), EnumSet.of(FSMEnvironment.Sensor.IS_EVEN)),
            },
            new IAgentProvider[] {
                    new JunoAgentProvider(),
                    new JunoAgentProvider(new JunoConfiguration(true, 1, Double.MAX_VALUE))
            }
    );

    private static TestSuite JunoBail = new TestSuite(
            TestSuiteConfiguration.MEDIUM,
            new IEnvironmentProvider[] {
                    new FSMEnvironmentProvider(new FSMTransitionTableBuilder(4, 40, Random.getTrue()), EnumSet.of(FSMEnvironment.Sensor.IS_EVEN)),
            },
            new IAgentProvider[] {
                    new JunoAgentProvider(new JunoConfiguration(true, 2, Double.MAX_VALUE))
            }
    );

    private static TestSuite MaRzMeta = new TestSuite(
            TestSuiteConfiguration.MEDIUM,
            new IEnvironmentProvider[] {
                    new MetaEnvironmentProvider(
                            new FSMEnvironmentProvider(new FSMTransitionTableBuilder(3,15, Random.getTrue()), FSMEnvironment.Sensor.NO_SENSORS), // 1 numswap
                            MetaConfiguration.DEFAULT),
            },
            new IAgentProvider[] {
                    new MaRzAgentProvider()
            }
    );

    private static TestSuite HAndPMasterSuite = new TestSuite(
            TestSuiteConfiguration.FULL,
            new IEnvironmentProvider[]{
                    new FSMEnvironmentProvider(new FSMTransitionTableBuilder(4, 40, Random.getTrue()), FSMEnvironment.Sensor.NO_SENSORS),
                    new FSMEnvironmentProvider(new FSMTransitionTableBuilder(4, 40, Random.getTrue()), EnumSet.of(FSMEnvironment.Sensor.IS_EVEN)),
                    new FSMEnvironmentProvider(new FSMTransitionTableBuilder(4, 40, Random.getTrue()), EnumSet.of(FSMEnvironment.Sensor.IS_EVEN, FSMEnvironment.Sensor.WITHIN_1)),
                    new FSMEnvironmentProvider(new FSMTransitionTableBuilder(4, 40, Random.getTrue()), EnumSet.of(FSMEnvironment.Sensor.IS_EVEN, FSMEnvironment.Sensor.WITHIN_1, FSMEnvironment.Sensor.WITHIN_2)),
                    new FSMEnvironmentProvider(new FSMTransitionTableBuilder(4, 40, Random.getTrue()), EnumSet.of(FSMEnvironment.Sensor.IS_EVEN, FSMEnvironment.Sensor.WITHIN_1, FSMEnvironment.Sensor.WITHIN_2, FSMEnvironment.Sensor.WITHIN_4)),
                    new FSMEnvironmentProvider(new FSMTransitionTableBuilder(4, 40, Random.getTrue()), EnumSet.of(FSMEnvironment.Sensor.IS_EVEN, FSMEnvironment.Sensor.WITHIN_1, FSMEnvironment.Sensor.WITHIN_2, FSMEnvironment.Sensor.WITHIN_4, FSMEnvironment.Sensor.WITHIN_8)),
                    new FSMEnvironmentProvider(new FSMTransitionTableBuilder(4, 40, Random.getTrue()), EnumSet.of(FSMEnvironment.Sensor.IS_EVEN, FSMEnvironment.Sensor.WITHIN_1, FSMEnvironment.Sensor.WITHIN_2, FSMEnvironment.Sensor.WITHIN_4, FSMEnvironment.Sensor.WITHIN_8, FSMEnvironment.Sensor.WITHIN_10)),
                    new FSMEnvironmentProvider(new FSMTransitionTableBuilder(4, 40, Random.getTrue()), EnumSet.of(FSMEnvironment.Sensor.WITHIN_1, FSMEnvironment.Sensor.WITHIN_2, FSMEnvironment.Sensor.WITHIN_4, FSMEnvironment.Sensor.WITHIN_8, FSMEnvironment.Sensor.WITHIN_10, FSMEnvironment.Sensor.WITHIN_20, FSMEnvironment.Sensor.IS_EVEN)),
            },
            new IAgentProvider[] {
                    new JunoAgentProvider(new JunoConfiguration(true, 0.7, Double.MAX_VALUE)
                    )
            });

    private static TestSuite ZPF_Suite = new TestSuite(
            TestSuiteConfiguration.LONG,
            new IEnvironmentProvider[] {
                    new FSMEnvironmentProvider(new FSMTransitionTableBuilder(2, 5, Random.getFalse()), EnumSet.of(FSMEnvironment.Sensor.IS_EVEN)),
                    new FSMEnvironmentProvider(new FSMTransitionTableBuilder(3, 15, Random.getFalse()), FSMEnvironment.Sensor.NO_SENSORS)
            },
            new IAgentProvider[] {
                    new MaRzAgentProvider(),
                    new NSMAgentProvider()
            }
    );

    private static TestSuite ZPF_Suite_MULTI = new TestSuite(
            TestSuiteConfiguration.LONG_MULTI,
            new IEnvironmentProvider[] {
                    new FSMEnvironmentProvider(new FSMTransitionTableBuilder(2, 5, Random.getFalse()), EnumSet.of(FSMEnvironment.Sensor.IS_EVEN)),
                    new FSMEnvironmentProvider(new FSMTransitionTableBuilder(3, 15, Random.getFalse()), FSMEnvironment.Sensor.NO_SENSORS)
            },
            new IAgentProvider[] {
                    new MaRzAgentProvider(),
                    new NSMAgentProvider()
            }
    );

    private static TestSuite ZPF_DEMO = new TestSuite(
            TestSuiteConfiguration.QUICK,
            new IEnvironmentProvider[] {
                    new FSMEnvironmentProvider(new FSMTransitionTableBuilder(2, 5, Random.getFalse()), FSMEnvironment.Sensor.NO_SENSORS)
            },
            new IAgentProvider[] {
                    new DemoAgentProvider()
            }
    );


    private static TestSuite WFC_SUITE = new TestSuite(
            TestSuiteConfiguration.ONCE,
            new IEnvironmentProvider[] {
                    new FSMEnvironmentProvider(new FSMTransitionTableBuilder(2, 6, Random.getFalse()), EnumSet.of(FSMEnvironment.Sensor.IS_ODD, FSMEnvironment.Sensor.WITHIN_1))
            },
            new IAgentProvider[] {
                    new WFCAgentProvider()
            }
    );

    private static TestSuite WFC_SUITE_NOISE = new TestSuite(
            TestSuiteConfiguration.QUICK,
            new IEnvironmentProvider[] {
                    new FSMEnvironmentProvider(new FSMTransitionTableBuilder(2, 6, Random.getFalse()), EnumSet.of(FSMEnvironment.Sensor.IS_ODD, FSMEnvironment.Sensor.WITHIN_1, FSMEnvironment.Sensor.NOISE1, FSMEnvironment.Sensor.NOISE2))
            },
            new IAgentProvider[] {
                    new WFCAgentProvider()
            }
    );

    private static TestSuite WfcVsNsm = new TestSuite(
            TestSuiteConfiguration.ONCE,
            new IEnvironmentProvider[] {
                    new FSMEnvironmentProvider(new FSMTransitionTableBuilder(3, 30, Random.getTrue()), EnumSet.of(FSMEnvironment.Sensor.IS_EVEN)),
            },
            new IAgentProvider[] {
                    new NSMAgentProvider(),
                    new WFCAgentProvider()
            }
    );

    private static TestSuite NSM_SUITE = new TestSuite(
            TestSuiteConfiguration.MEDIUM,
            new IEnvironmentProvider[] {
                    new FSMEnvironmentProvider(
                            new FSMTransitionTableBuilder(2, 6, Random.getFalse()),
                            EnumSet.of(FSMEnvironment.Sensor.NOISE1, FSMEnvironment.Sensor.IS_ODD, FSMEnvironment.Sensor.WITHIN_1))
            },
            new IAgentProvider[] {
                    new NSMAgentProvider()
            }
    );

    private static TestSuite NDXR_AGENT = new TestSuite(
            TestSuiteConfiguration.QUICK,
            new IEnvironmentProvider[] {
                    new FSMEnvironmentProvider(
                            new FSMTransitionTableBuilder(2, 5, Random.getFalse()),
                            EnumSet.of(FSMEnvironment.Sensor.NOISE1, FSMEnvironment.Sensor.IS_ODD))
//                    new FSMTransitionTableBuilder(2, 50, Random.getFalse()),
//                    EnumSet.of(FSMEnvironment.Sensor.NOISE1, FSMEnvironment.Sensor.NOISE2,
//                            FSMEnvironment.Sensor.NOISE3, FSMEnvironment.Sensor.NOISE4,
//                            FSMEnvironment.Sensor.IS_ODD, FSMEnvironment.Sensor.MOD_3))
            },
            new IAgentProvider[]{
                    new NdxrAgentProvider()
            }
        );


    //This is how a generated data for the ACS 2022 paper submission
    private static TestSuite PJ_SUITE = new TestSuite(
            TestSuiteConfiguration.MEDIUM,
            new IEnvironmentProvider[] {
                    new FSMEnvironmentProvider(
                            new FSMTransitionTableBuilder(3, 25, Random.getFalse()),
//                            EnumSet.of(FSMEnvironment.Sensor.CACTUS1, FSMEnvironment.Sensor.CACTUS2, FSMEnvironment.Sensor.IS_ODD, FSMEnvironment.Sensor.WITHIN_1),
//                            EnumSet.of(FSMEnvironment.Sensor.NOISE1, FSMEnvironment.Sensor.NOISE2, FSMEnvironment.Sensor.IS_ODD, FSMEnvironment.Sensor.WITHIN_1),
//                            EnumSet.of(FSMEnvironment.Sensor.NOISE1, FSMEnvironment.Sensor.IS_ODD, FSMEnvironment.Sensor.WITHIN_1),
                            EnumSet.of(FSMEnvironment.Sensor.IS_ODD, FSMEnvironment.Sensor.WITHIN_1),
                            false,
                            0.0),  //<-- non-determinism chance
            },
            new IAgentProvider[] {
                    new PhuJusAgentProvider()
                    //new NSMAgentProvider()
                    //new MaRzAgentProvider()
                    //new RulesAgentProvider(new Heuristic(1, 0), 50)  //Dart
            }
    );

    //This suite runs the TensorFlow proxy agent
    private static TestSuite TF_SUITE = new TestSuite(
            TestSuiteConfiguration.TEST,
            new IEnvironmentProvider[] {
                    new FSMEnvironmentProvider(new FSMTransitionTableBuilder(3, 20, Random.getTrue()), FSMEnvironment.Sensor.NO_SENSORS),
            },
            new IAgentProvider[] {
                    new TFSocketAgentProvider()
            }
    );

    //endregion

    //region Main
    public static void main(String[] args) {

        // The suite that's used is fed in as a command line argument. If an invalid command line argument is given,
        // it fails silently and uses the default suite instead. The name of the suite must match exactly.
        TestSuite defaultSuite = TF_SUITE;

        if (args.length >= 1) {
            String suiteName = args[0];
            Class<Runner> runner = Runner.class;

            try {
                Field suite = runner.getDeclaredField(suiteName);
                defaultSuite = (TestSuite) suite.get(defaultSuite);
                System.out.println("Using TestSuite " + suiteName);
            } catch (NoSuchFieldException e) {
                System.err.println("Could not find TestSuite " + suiteName + "! Using default suite.");
            } catch (IllegalAccessException e) {
                System.err.println("Could not access " + suiteName + " in Runner!");
            }
        }


        try {
            File outputDirectory = DirectoryUtils.generateCenekOutputDirectory();
            Runner.redirectOutput(outputDirectory);
            defaultSuite.run(new FileResultCompiler(outputDirectory));
        } catch (OutOfMemoryError mem) {
            mem.printStackTrace();
        } catch (Exception ex) {
            System.out.println("Runner failed with exception: " + ex.getMessage());
            ex.printStackTrace();
        } finally {
            NamedOutput.getInstance().closeAll();
        }
    }

    private static void redirectOutput(File rootDirectory) {
        NamedOutput namedOutput = NamedOutput.getInstance();
        try {
            namedOutput.configure("metadata", new FileOutputStream(new File(rootDirectory, "metadata.txt")));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }
    //endregion
}
