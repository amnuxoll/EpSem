package experiments;

import agents.discr.MaRzLearnerProvider;
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

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.EnumSet;

public class Runner {

    private static TestSuite JunoFSM = new TestSuite(
            TestSuiteConfiguration.MEDIUM,
            new FileResultWriterProvider(),
            new FSMDescriptionProvider(2, 6, EnumSet.of(FSMDescription.Sensor.EVEN_ODD)),
            new IAgentProvider[] {
                    new JunoAgentProvider(new SuffixNodeProvider(), new JunoConfiguration(true, .7, Double.MAX_VALUE))
            }
    );

    private static TestSuite MarzFSM = new TestSuite(
            TestSuiteConfiguration.MEDIUM,
            new FileResultWriterProvider(),
            new FSMDescriptionProvider(3, 30, FSMDescription.Sensor.ALL_SENSORS),
            new IAgentProvider[] {
                    new NSMAgentProvider()
            }
    );

    private static TestSuite NsmVsMaRzFSM = new TestSuite(
            TestSuiteConfiguration.QUICK,
            new FileResultWriterProvider(),
            new FSMDescriptionProvider(3, 30, EnumSet.of(FSMDescription.Sensor.EVEN_ODD)),
            new IAgentProvider[] {
                    new NSMAgentProvider(),
                    new MaRzAgentProvider<>(new SuffixNodeProvider())
            }
    );

    private static TestSuite MarzLearnerFSM = new TestSuite(
            TestSuiteConfiguration.QUICK,
            new FileResultWriterProvider(),
            new FSMDescriptionProvider(3, 30, FSMDescription.Sensor.ALL_SENSORS),
            new IAgentProvider[] {
                    new MaRzLearnerProvider<>(new SuffixNodeProvider()),
                    //new MaRzAgentProvider<>(new SuffixNodeProvider())
            }
    );

    private static TestSuite JunoVMarz = new TestSuite(
            TestSuiteConfiguration.MEDIUM,
            new FileResultWriterProvider(),
            new FSMDescriptionProvider(3, 30, EnumSet.of(FSMDescription.Sensor.EVEN_ODD, FSMDescription.Sensor.NOISE)),
            new IAgentProvider[] {
                    new JunoAgentProvider(new SuffixNodeProvider(), new JunoConfiguration(true, .7, Double.MAX_VALUE)),
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
                    new JunoAgentProvider(new SuffixNodeProvider(), new JunoConfiguration(true, .9, Double.MAX_VALUE)),
                    new JunoAgentProvider(new SuffixNodeProvider(), new JunoConfiguration(true, .8, Double.MAX_VALUE)),
                    new JunoAgentProvider(new SuffixNodeProvider(), new JunoConfiguration(true, .7, Double.MAX_VALUE)),
                    new JunoAgentProvider(new SuffixNodeProvider(), new JunoConfiguration(true, .6, Double.MAX_VALUE)),
                    new JunoAgentProvider(new SuffixNodeProvider(), new JunoConfiguration(true, .5, Double.MAX_VALUE)),
                    new JunoAgentProvider(new SuffixNodeProvider(), new JunoConfiguration(true, .4, Double.MAX_VALUE)),
                    new JunoAgentProvider(new SuffixNodeProvider(), new JunoConfiguration(true, .3, Double.MAX_VALUE)),
                    new JunoAgentProvider(new SuffixNodeProvider(), new JunoConfiguration(true, .2, Double.MAX_VALUE)),
                    new JunoAgentProvider(new SuffixNodeProvider(), new JunoConfiguration(true, .1, Double.MAX_VALUE)),
                    new JunoAgentProvider(new SuffixNodeProvider(), new JunoConfiguration(false, 0, Double.MAX_VALUE)),
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
            TestSuite suite= JunoFSM;

            //Services.register(IRandomizer.class, new Randomizer(541)); //determined seed for debug
            Services.register(IRandomizer.class, new Randomizer());
            String outputPath= suite.getResultWriterProvider().getOutputDirectory();
            OutputStreamContainer outputStreamContainer= createOutputStreamContainer(outputPath);
            Services.register(OutputStreamContainer.class, outputStreamContainer);

            suite.run();

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

    private static OutputStreamContainer createOutputStreamContainer(String outputPath) throws IOException {
        OutputStreamContainer outputStreamContainer=
                new OutputStreamContainer(outputPath);
        outputStreamContainer.put("metaData", "metadata.txt");
        outputStreamContainer.put("agentDidAGood", "goodRatios.csv");
        outputStreamContainer.put("goodDecisionBail", "goodDecisionBailRatio.csv");
        outputStreamContainer.put("badDecisionBail", "badDecisionBailRatio.csv");
        outputStreamContainer.put("properBails", "properBailRatio.csv");
        outputStreamContainer.put("junoRatios", "junoRatios.csv");

        return outputStreamContainer;
    }

}
