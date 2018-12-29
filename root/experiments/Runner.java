package experiments;

import agents.discr.MaRzLearnerProvider;
import agents.juno.JunoAgentProvider;
import agents.juno.JunoConfiguration;
import agents.marz.MaRzAgentProvider;
import agents.marz.nodes.SuffixNodeProvider;
import agents.nsm.NSMAgentProvider;
import environments.fsm.FSMDescription;
import environments.fsm.FSMDescriptionProvider;
import environments.fsm.FSMDescriptionTweakingProvider;
import environments.meta.MetaConfiguration;
import environments.meta.MetaEnvironmentDescriptionProvider;
import framework.*;
import utils.DirectoryUtils;
import environments.fsm.FSMTransitionTableBuilder;
import utils.Randomizer;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.EnumSet;

public class Runner {
    //region TestSuites
    private static TestSuite JunoFSM = new TestSuite(
            TestSuiteConfiguration.MEDIUM,
            new IEnvironmentDescriptionProvider[] {
                new FSMDescriptionProvider(new FSMTransitionTableBuilder(3, 50, new Randomizer()), EnumSet.of(FSMDescription.Sensor.EVEN_ODD))
            },
            new IAgentProvider[] {
                    new JunoAgentProvider(new SuffixNodeProvider(), new JunoConfiguration(true, .7, Double.MAX_VALUE))
            }
    );

    private static TestSuite MarzFSM = new TestSuite(
            TestSuiteConfiguration.FULL,
            new IEnvironmentDescriptionProvider[] {
                    new FSMDescriptionProvider(new FSMTransitionTableBuilder(4, 40, new Randomizer()), FSMDescription.Sensor.ALL_SENSORS),
            },
            new IAgentProvider[] {
                    new MaRzAgentProvider<>(new SuffixNodeProvider())
            }
    );

    private static TestSuite NsmVsMaRzFSM = new TestSuite(
            TestSuiteConfiguration.FULL,
            new IEnvironmentDescriptionProvider[] {
                    new FSMDescriptionProvider(new FSMTransitionTableBuilder(3, 30, new Randomizer()), EnumSet.of(FSMDescription.Sensor.EVEN_ODD)),
            },
            new IAgentProvider[] {
                    new NSMAgentProvider(),
                    new MaRzAgentProvider<>(new SuffixNodeProvider())
            }
    );

    private static TestSuite MarzLearnerFSM = new TestSuite(
            TestSuiteConfiguration.QUICK,
            new IEnvironmentDescriptionProvider[] {
                    new FSMDescriptionProvider(new FSMTransitionTableBuilder(3, 30, new Randomizer()), FSMDescription.Sensor.ALL_SENSORS),
            },
            new IAgentProvider[] {
                    new MaRzLearnerProvider<>(new SuffixNodeProvider()),
                    //new MaRzAgentProvider<>(new SuffixNodeProvider())
            }
    );

    private static TestSuite JunoVMarz3_30 = new TestSuite(
            TestSuiteConfiguration.MEDIUM,
            new IEnvironmentDescriptionProvider[] {
                    new FSMDescriptionProvider(new FSMTransitionTableBuilder(3, 30, new Randomizer()), EnumSet.of(FSMDescription.Sensor.EVEN_ODD, FSMDescription.Sensor.NOISE)),
            },
            new IAgentProvider[] {
                    new JunoAgentProvider(new SuffixNodeProvider(), new JunoConfiguration(true, .7, Double.MAX_VALUE)),
                    new MaRzAgentProvider<>(new SuffixNodeProvider())
            }
    );

    private static TestSuite JunoVJunoBail = new TestSuite(
            TestSuiteConfiguration.MEDIUM,
            new IEnvironmentDescriptionProvider[] {
                    new FSMDescriptionProvider(new FSMTransitionTableBuilder(3, 30, new Randomizer()), EnumSet.of(FSMDescription.Sensor.EVEN_ODD)),
            },
            new IAgentProvider[] {
                    new JunoAgentProvider(new SuffixNodeProvider()),
                    new JunoAgentProvider(new SuffixNodeProvider(),
                            new JunoConfiguration(true, 1, Double.MAX_VALUE))
            }
    );

    private static TestSuite JunoBail = new TestSuite(
            TestSuiteConfiguration.MEDIUM,
            new IEnvironmentDescriptionProvider[] {
                    new FSMDescriptionProvider(new FSMTransitionTableBuilder(4, 40, new Randomizer()), EnumSet.of(FSMDescription.Sensor.EVEN_ODD)),
            },
            new IAgentProvider[] {
                    new JunoAgentProvider(new SuffixNodeProvider(), new JunoConfiguration(true, 2, Double.MAX_VALUE))
            }
    );

    private static TestSuite MaRzMeta = new TestSuite(
            TestSuiteConfiguration.MEDIUM,
            new IEnvironmentDescriptionProvider[] {
                    new MetaEnvironmentDescriptionProvider(
                    new FSMDescriptionTweakingProvider(new FSMTransitionTableBuilder(3,15, new Randomizer()),FSMDescription.Sensor.NO_SENSORS), // 1 numswap
                    MetaConfiguration.DEFAULT),
            },
            new IAgentProvider[] {
                    new MaRzAgentProvider<>(new SuffixNodeProvider())
            }
    );

    private static TestSuite Suite2 = new TestSuite(
            TestSuiteConfiguration.FULL,
            new IEnvironmentDescriptionProvider[] {
                    new MetaEnvironmentDescriptionProvider(new FSMDescriptionProvider(new FSMTransitionTableBuilder(2, 5, new Randomizer()), EnumSet.of(FSMDescription.Sensor.EVEN_ODD)), MetaConfiguration.DEFAULT),
            },
            new IAgentProvider[] {
                    new NSMAgentProvider()
            }
    );

    private static TestSuite HAndPMasterSuite = new TestSuite(
            TestSuiteConfiguration.FULL,
            new IEnvironmentDescriptionProvider[]{
                    new FSMDescriptionProvider(new FSMTransitionTableBuilder(4, 40, new Randomizer()), FSMDescription.Sensor.NO_SENSORS),
                    new FSMDescriptionProvider(new FSMTransitionTableBuilder(4, 40, new Randomizer()), EnumSet.of(FSMDescription.Sensor.EVEN_ODD)),
                    new FSMDescriptionProvider(new FSMTransitionTableBuilder(4, 40, new Randomizer()), EnumSet.of(FSMDescription.Sensor.EVEN_ODD, FSMDescription.Sensor.WITHIN_1)),
                    new FSMDescriptionProvider(new FSMTransitionTableBuilder(4, 40, new Randomizer()), EnumSet.of(FSMDescription.Sensor.EVEN_ODD, FSMDescription.Sensor.WITHIN_1, FSMDescription.Sensor.WITHIN_2)),
                    new FSMDescriptionProvider(new FSMTransitionTableBuilder(4, 40, new Randomizer()), EnumSet.of(FSMDescription.Sensor.EVEN_ODD, FSMDescription.Sensor.WITHIN_1, FSMDescription.Sensor.WITHIN_2, FSMDescription.Sensor.WITHIN_4)),
                    new FSMDescriptionProvider(new FSMTransitionTableBuilder(4, 40, new Randomizer()), EnumSet.of(FSMDescription.Sensor.EVEN_ODD, FSMDescription.Sensor.WITHIN_1, FSMDescription.Sensor.WITHIN_2, FSMDescription.Sensor.WITHIN_4, FSMDescription.Sensor.WITHIN_8)),
                    new FSMDescriptionProvider(new FSMTransitionTableBuilder(4, 40, new Randomizer()), EnumSet.of(FSMDescription.Sensor.EVEN_ODD, FSMDescription.Sensor.WITHIN_1, FSMDescription.Sensor.WITHIN_2, FSMDescription.Sensor.WITHIN_4, FSMDescription.Sensor.WITHIN_8, FSMDescription.Sensor.WITHIN_10)),
                    new FSMDescriptionProvider(new FSMTransitionTableBuilder(4, 40, new Randomizer()), EnumSet.of(FSMDescription.Sensor.WITHIN_1,FSMDescription.Sensor.WITHIN_2,FSMDescription.Sensor.WITHIN_4,FSMDescription.Sensor.WITHIN_8,FSMDescription.Sensor.WITHIN_10,FSMDescription.Sensor.WITHIN_20, FSMDescription.Sensor.EVEN_ODD)),
            },
            new IAgentProvider[] {
                    new JunoAgentProvider(new SuffixNodeProvider(), new JunoConfiguration(true, 0.7, Double.MAX_VALUE)
        )
        //new MaRzAgentProvider<>(new SuffixNodeProvider())
    });

    private static TestSuite TempExperiment = new TestSuite(
            TestSuiteConfiguration.QUICK,
            new IEnvironmentDescriptionProvider[] {
                    new FSMDescriptionProvider(new FSMTransitionTableBuilder(3, 10, new Randomizer()), FSMDescription.Sensor.NO_SENSORS),
                    new MetaEnvironmentDescriptionProvider(new FSMDescriptionTweakingProvider(new FSMTransitionTableBuilder(2, 15, new Randomizer()), EnumSet.of(FSMDescription.Sensor.EVEN_ODD)), new MetaConfiguration(100, 0))
            },
            new IAgentProvider[] {
                    new NSMAgentProvider(),
                    new MaRzAgentProvider<>(new SuffixNodeProvider())
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
