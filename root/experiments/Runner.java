package experiments;

import agents.juno.JunoAgentProvider;
import agents.juno.JunoConfiguration;
import agents.marz.MaRzAgentProvider;
import agents.marz.nodes.SuffixNodeProvider;
import agents.nsm.NSMAgentProvider;
import environments.fsm.FSMDescription;
import environments.fsm.FSMDescriptionProvider;
import environments.meta.FSMDescriptionTweaker;
import environments.meta.MetaConfiguration;
import environments.meta.MetaEnvironmentDescriptionProvider;
import framework.*;

import java.io.FileOutputStream;
import java.util.EnumSet;

public class Runner {

    private static TestSuite JunoFSM = new TestSuite(
            TestSuiteConfiguration.MEDIUM,
            new FileResultWriterProvider(),
            new FSMDescriptionProvider(3, 30, EnumSet.of(FSMDescription.Sensor.EVEN_ODD)),
            new IAgentProvider[] {
                    new JunoAgentProvider(new SuffixNodeProvider())
            }
    );

    private static TestSuite MarzFSM = new TestSuite(
            TestSuiteConfiguration.FULL,
            new FileResultWriterProvider(),
            new FSMDescriptionProvider(8, 40, FSMDescription.Sensor.NO_SENSORS),
            new IAgentProvider[] {
                    new MaRzAgentProvider<>(new SuffixNodeProvider())
            }
    );

    private static TestSuite JunoVMarz = new TestSuite(
            TestSuiteConfiguration.FULL,
            new FileResultWriterProvider(),
            new FSMDescriptionProvider(8, 40, EnumSet.of(FSMDescription.Sensor.EVEN_ODD)),
            new IAgentProvider[] {
                    new JunoAgentProvider(new SuffixNodeProvider(), new JunoConfiguration(true, .9, 0.001)),
                    new MaRzAgentProvider<>(new SuffixNodeProvider())
            }
    );

    private static TestSuite JunoVJunoBail = new TestSuite(
            TestSuiteConfiguration.MEDIUM,
            new FileResultWriterProvider(),
            new FSMDescriptionProvider(3, 30, EnumSet.of(FSMDescription.Sensor.EVEN_ODD)),
            new IAgentProvider[] {
                    new JunoAgentProvider(new SuffixNodeProvider()),
                    new JunoAgentProvider(new SuffixNodeProvider(),
                            new JunoConfiguration(true, 1, Double.MAX_VALUE))
            }
    );

    private static TestSuite JunoBail = new TestSuite(
            new TestSuiteConfiguration(1, 500),
            new FileResultWriterProvider(),
            new FSMDescriptionProvider(4, 40, EnumSet.of(FSMDescription.Sensor.EVEN_ODD)),
            new IAgentProvider[] {
                    new JunoAgentProvider(new SuffixNodeProvider(), new JunoConfiguration(true, 1, Double.MAX_VALUE)),
//                    new JunoAgentProvider(new SuffixNodeProvider(), new JunoConfiguration(true, .9)),
//                    new JunoAgentProvider(new SuffixNodeProvider(), new JunoConfiguration(true, .8)),
//                    new JunoAgentProvider(new SuffixNodeProvider(), new JunoConfiguration(true, .7)),
//                    new JunoAgentProvider(new SuffixNodeProvider(), new JunoConfiguration(true, .6)),
//                    new JunoAgentProvider(new SuffixNodeProvider(), new JunoConfiguration(true, .5)),
//                    new JunoAgentProvider(new SuffixNodeProvider(), new JunoConfiguration(true, .4)),
//                    new JunoAgentProvider(new SuffixNodeProvider(), new JunoConfiguration(true, .3)),
//                    new JunoAgentProvider(new SuffixNodeProvider(), new JunoConfiguration(true, .2)),
//                    new JunoAgentProvider(new SuffixNodeProvider(), new JunoConfiguration(true, .1)),
//                    new JunoAgentProvider(new SuffixNodeProvider(), new JunoConfiguration(false, 0)),
            }
    );

    private static TestSuite MaRzMeta = new TestSuite(
            TestSuiteConfiguration.MEDIUM,
            new FileResultWriterProvider(),
            new MetaEnvironmentDescriptionProvider(
                    new FSMDescriptionTweaker(3,15,FSMDescription.Sensor.NO_SENSORS, 1),
                    MetaConfiguration.DEFAULT),
            new IAgentProvider[] {
                    new MaRzAgentProvider<>(new SuffixNodeProvider())
            }
    );

    private static TestSuite Suite2 = new TestSuite(
            TestSuiteConfiguration.FULL,
            new FileResultWriterProvider(),
            new MetaEnvironmentDescriptionProvider(new FSMDescriptionProvider(2, 5, EnumSet.of(FSMDescription.Sensor.EVEN_ODD)), MetaConfiguration.DEFAULT),
            new IAgentProvider[] {
                    new NSMAgentProvider()
            }
    );

    public static void main(String[] args) {
        try {
            Services.register(IRandomizer.class, new Randomizer());
            OutputStreamContainer outputStreamContainer=
                    new OutputStreamContainer("tableInfo", new FileOutputStream("info.txt"));
            outputStreamContainer.put("ratioOutputStream", new FileOutputStream("ratios.csv"));
            outputStreamContainer.put("adjustValOutputStream", new FileOutputStream("adjustval.csv"));
            Services.register(OutputStreamContainer.class, outputStreamContainer);
            Runner.JunoVMarz.run();
            outputStreamContainer.closeAll();
        }
        catch (OutOfMemoryError mem){
            mem.printStackTrace();
        }
        catch (Exception ex)
        {
            System.out.println("Runner failed with exception: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

}
