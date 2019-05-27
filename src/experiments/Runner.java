package experiments;

import agents.discr.MaRzLearnerProvider;
import agents.juno.JunoAgentProvider;
import agents.juno.JunoConfiguration;
import agents.marz.MaRzAgentProvider;
import agents.marzrules.RulesAgentProvider;
import agents.nsm.NSMAgentProvider;
import environments.fsm.FSMDescription;
import environments.fsm.FSMDescriptionProvider;
import environments.fsm.FSMDescriptionTweakingProvider;
import environments.meta.MetaConfiguration;
import environments.meta.MetaEnvironmentDescriptionProvider;
import framework.*;
import utils.DirectoryUtils;
import environments.fsm.FSMTransitionTableBuilder;
import agents.marzrules.Heuristic;
import utils.Random;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.EnumSet;

public class Runner {
    //region TestSuites
    private static TestSuite JunoFSM = new TestSuite(
            TestSuiteConfiguration.MEDIUM,
            new IEnvironmentDescriptionProvider[] {
                new FSMDescriptionProvider(new FSMTransitionTableBuilder(3, 50, Random.getTrue()), EnumSet.of(FSMDescription.Sensor.EVEN_ODD))
            },
            new IAgentProvider[] {
                    new JunoAgentProvider(new JunoConfiguration(true, .7, Double.MAX_VALUE))
            }
    );

    private static TestSuite MarzFSM = new TestSuite(
            TestSuiteConfiguration.FULL,
            new IEnvironmentDescriptionProvider[] {
                    new FSMDescriptionProvider(new FSMTransitionTableBuilder(4, 40, Random.getFalse()), FSMDescription.Sensor.NO_SENSORS),
            },
            new IAgentProvider[] {
                    new MaRzAgentProvider()
            }
    );

    private static TestSuite MarzFSMPrintout = new TestSuite(
            TestSuiteConfiguration.QUICK,
            new IEnvironmentDescriptionProvider[] {
                    new FSMDescriptionProvider(new FSMTransitionTableBuilder(2, 25, Random.getFalse()), EnumSet.of(FSMDescription.Sensor.EVEN_ODD), true)
            },
            new IAgentProvider[] {
                    new MaRzAgentProvider()
            }
    );

    private static TestSuite RulesAgent = new TestSuite(
            TestSuiteConfiguration.MEDIUM,
            new IEnvironmentDescriptionProvider[] {
                    new FSMDescriptionProvider(new FSMTransitionTableBuilder(2, 50, Random.getFalse()), EnumSet.of(FSMDescription.Sensor.EVEN_ODD), false),
                    new FSMDescriptionProvider(new FSMTransitionTableBuilder(2, 50, Random.getFalse()), FSMDescription.Sensor.NO_SENSORS)
            },
            new IAgentProvider[] {
                    new RulesAgentProvider(new Heuristic(1, 0)),
                    new MaRzAgentProvider()
            }
    );

    private static TestSuite HeuristicTest = new TestSuite(
            TestSuiteConfiguration.FULL,
            new IEnvironmentDescriptionProvider[] {
                    new FSMDescriptionProvider(new FSMTransitionTableBuilder(2, 15, Random.getFalse()), EnumSet.of(FSMDescription.Sensor.EVEN_ODD), false),
                    new FSMDescriptionProvider(new FSMTransitionTableBuilder(2, 50, Random.getFalse()), EnumSet.of(FSMDescription.Sensor.EVEN_ODD), false),
                    new FSMDescriptionProvider(new FSMTransitionTableBuilder(2, 100, Random.getFalse()), EnumSet.of(FSMDescription.Sensor.EVEN_ODD), false),
                    new FSMDescriptionProvider(new FSMTransitionTableBuilder(6, 15, Random.getFalse()), EnumSet.of(FSMDescription.Sensor.EVEN_ODD), false),
                    new FSMDescriptionProvider(new FSMTransitionTableBuilder(6, 50, Random.getFalse()), EnumSet.of(FSMDescription.Sensor.EVEN_ODD), false),
                    new FSMDescriptionProvider(new FSMTransitionTableBuilder(6, 100, Random.getFalse()), EnumSet.of(FSMDescription.Sensor.EVEN_ODD), false)
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
            new IEnvironmentDescriptionProvider[] {
                    //new FSMDescriptionProvider(new FSMTransitionTableBuilder(4, 50, Random.getFalse()), FSMDescription.Sensor.NO_SENSORS),
                    new FSMDescriptionProvider(new FSMTransitionTableBuilder(4, 100, Random.getFalse()), EnumSet.of(FSMDescription.Sensor.EVEN_ODD))
                    //new FSMDescriptionProvider(new FSMTransitionTableBuilder(4, 50, Random.getFalse()), EnumSet.of(FSMDescription.Sensor.EVEN_ODD, FSMDescription.Sensor.MOD_3))
            },
            new IAgentProvider[] {
                    new RulesAgentProvider(new Heuristic(1, 0)),
                    new MaRzAgentProvider()
            }
    );

    private static TestSuite NsmVsMaRzFSM = new TestSuite(
            TestSuiteConfiguration.FULL,
            new IEnvironmentDescriptionProvider[] {
                    new FSMDescriptionProvider(new FSMTransitionTableBuilder(3, 30, Random.getTrue()), EnumSet.of(FSMDescription.Sensor.EVEN_ODD)),
            },
            new IAgentProvider[] {
                    new NSMAgentProvider(),
                    new MaRzAgentProvider()
            }
    );

    private static TestSuite MarzLearnerFSM = new TestSuite(
            TestSuiteConfiguration.QUICK,
            new IEnvironmentDescriptionProvider[] {
                    new FSMDescriptionProvider(new FSMTransitionTableBuilder(3, 30, Random.getTrue()), FSMDescription.Sensor.ALL_SENSORS),
            },
            new IAgentProvider[] {
                    new MaRzLearnerProvider(),
                    //new MaRzAgentProvider<>(new SuffixNodeProvider())
            }
    );

    private static TestSuite JunoVMarz3_30 = new TestSuite(
            TestSuiteConfiguration.MEDIUM,
            new IEnvironmentDescriptionProvider[] {
                    new FSMDescriptionProvider(new FSMTransitionTableBuilder(3, 30, Random.getTrue()), EnumSet.of(FSMDescription.Sensor.EVEN_ODD, FSMDescription.Sensor.NOISE1)),
            },
            new IAgentProvider[] {
                    new JunoAgentProvider(new JunoConfiguration(true, .7, Double.MAX_VALUE)),
                    new MaRzAgentProvider()
            }
    );

    private static TestSuite JunoVJunoBail = new TestSuite(
            TestSuiteConfiguration.MEDIUM,
            new IEnvironmentDescriptionProvider[] {
                    new FSMDescriptionProvider(new FSMTransitionTableBuilder(3, 30, Random.getTrue()), EnumSet.of(FSMDescription.Sensor.EVEN_ODD)),
            },
            new IAgentProvider[] {
                    new JunoAgentProvider(),
                    new JunoAgentProvider(new JunoConfiguration(true, 1, Double.MAX_VALUE))
            }
    );

    private static TestSuite JunoBail = new TestSuite(
            TestSuiteConfiguration.MEDIUM,
            new IEnvironmentDescriptionProvider[] {
                    new FSMDescriptionProvider(new FSMTransitionTableBuilder(4, 40, Random.getTrue()), EnumSet.of(FSMDescription.Sensor.EVEN_ODD)),
            },
            new IAgentProvider[] {
                    new JunoAgentProvider(new JunoConfiguration(true, 2, Double.MAX_VALUE))
            }
    );

    private static TestSuite MaRzMeta = new TestSuite(
            TestSuiteConfiguration.MEDIUM,
            new IEnvironmentDescriptionProvider[] {
                    new MetaEnvironmentDescriptionProvider(
                    new FSMDescriptionTweakingProvider(new FSMTransitionTableBuilder(3,15, Random.getTrue()),FSMDescription.Sensor.NO_SENSORS), // 1 numswap
                    MetaConfiguration.DEFAULT),
            },
            new IAgentProvider[] {
                    new MaRzAgentProvider()
            }
    );

    private static TestSuite Suite2 = new TestSuite(
            TestSuiteConfiguration.FULL,
            new IEnvironmentDescriptionProvider[] {
                    new MetaEnvironmentDescriptionProvider(new FSMDescriptionProvider(new FSMTransitionTableBuilder(2, 5, Random.getTrue()), EnumSet.of(FSMDescription.Sensor.EVEN_ODD)), MetaConfiguration.DEFAULT),
            },
            new IAgentProvider[] {
                    new NSMAgentProvider()
            }
    );

    private static TestSuite HAndPMasterSuite = new TestSuite(
            TestSuiteConfiguration.FULL,
            new IEnvironmentDescriptionProvider[]{
                    new FSMDescriptionProvider(new FSMTransitionTableBuilder(4, 40, Random.getTrue()), FSMDescription.Sensor.NO_SENSORS),
                    new FSMDescriptionProvider(new FSMTransitionTableBuilder(4, 40, Random.getTrue()), EnumSet.of(FSMDescription.Sensor.EVEN_ODD)),
                    new FSMDescriptionProvider(new FSMTransitionTableBuilder(4, 40, Random.getTrue()), EnumSet.of(FSMDescription.Sensor.EVEN_ODD, FSMDescription.Sensor.WITHIN_1)),
                    new FSMDescriptionProvider(new FSMTransitionTableBuilder(4, 40, Random.getTrue()), EnumSet.of(FSMDescription.Sensor.EVEN_ODD, FSMDescription.Sensor.WITHIN_1, FSMDescription.Sensor.WITHIN_2)),
                    new FSMDescriptionProvider(new FSMTransitionTableBuilder(4, 40, Random.getTrue()), EnumSet.of(FSMDescription.Sensor.EVEN_ODD, FSMDescription.Sensor.WITHIN_1, FSMDescription.Sensor.WITHIN_2, FSMDescription.Sensor.WITHIN_4)),
                    new FSMDescriptionProvider(new FSMTransitionTableBuilder(4, 40, Random.getTrue()), EnumSet.of(FSMDescription.Sensor.EVEN_ODD, FSMDescription.Sensor.WITHIN_1, FSMDescription.Sensor.WITHIN_2, FSMDescription.Sensor.WITHIN_4, FSMDescription.Sensor.WITHIN_8)),
                    new FSMDescriptionProvider(new FSMTransitionTableBuilder(4, 40, Random.getTrue()), EnumSet.of(FSMDescription.Sensor.EVEN_ODD, FSMDescription.Sensor.WITHIN_1, FSMDescription.Sensor.WITHIN_2, FSMDescription.Sensor.WITHIN_4, FSMDescription.Sensor.WITHIN_8, FSMDescription.Sensor.WITHIN_10)),
                    new FSMDescriptionProvider(new FSMTransitionTableBuilder(4, 40, Random.getTrue()), EnumSet.of(FSMDescription.Sensor.WITHIN_1,FSMDescription.Sensor.WITHIN_2,FSMDescription.Sensor.WITHIN_4,FSMDescription.Sensor.WITHIN_8,FSMDescription.Sensor.WITHIN_10,FSMDescription.Sensor.WITHIN_20, FSMDescription.Sensor.EVEN_ODD)),
            },
            new IAgentProvider[] {
                    new JunoAgentProvider(new JunoConfiguration(true, 0.7, Double.MAX_VALUE)
        )
    });

    private static TestSuite TempExperiment = new TestSuite(
            TestSuiteConfiguration.QUICK,
            new IEnvironmentDescriptionProvider[] {
                    new FSMDescriptionProvider(new FSMTransitionTableBuilder(3, 15, Random.getTrue()), FSMDescription.Sensor.NO_SENSORS)
            },
            new IAgentProvider[] {
                    new MaRzAgentProvider(),
                    new NSMAgentProvider()
            },
            rootDirectory -> {
                NamedOutput namedOutput = NamedOutput.getInstance();
                try {
                    namedOutput.configure("metadata", new FileOutputStream(new File(rootDirectory, "metadata.txt")));
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
            }
    );
    //endregion

    //region Main
    public static void main(String[] args) {
        try {
            File outputDirectory = DirectoryUtils.generateNewOutputDirectory();
            Runner.TempExperiment.run(new FileResultWriterProvider(outputDirectory));
        } catch (OutOfMemoryError mem) {
            mem.printStackTrace();
        } catch (Exception ex) {
            System.out.println("Runner failed with exception: " + ex.getMessage());
            ex.printStackTrace();
        } finally {
            NamedOutput.getInstance().closeAll();
        }
    }
    //endregion
}
