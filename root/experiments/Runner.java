package experiments;

import agents.discr.MaRzLearnerProvider;
import agents.juno.JunoAgentProvider;
import agents.juno.JunoConfiguration;
import agents.marz.MaRzAgentProvider;
import agents.marz.nodes.SuffixNodeProvider;
import agents.nsm.NSMAgentProvider;
//import com.sun.corba.se.spi.orbutil.fsm.FSM;
import environments.fsm.FSMDescription;
import environments.fsm.FSMDescriptionProvider;
import environments.fsm.FSMDescriptionTweakingProvider;
import environments.meta.MetaConfiguration;
import environments.meta.MetaEnvironmentDescriptionProvider;
import framework.*;
import utils.FSMTransitionTableBuilder;
import utils.Randomizer;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.EnumSet;

public class Runner {

    private static TestSuite JunoFSM = new TestSuite(
            TestSuiteConfiguration.MEDIUM,
            new FileResultWriterProvider(),
            new FSMDescriptionProvider(new FSMTransitionTableBuilder(3, 50, new Randomizer()), EnumSet.of(FSMDescription.Sensor.EVEN_ODD)),
            new IAgentProvider[] {
                    new JunoAgentProvider(new SuffixNodeProvider(), new JunoConfiguration(true, .7, Double.MAX_VALUE))
            }
    );

    private static TestSuite MarzFSM = new TestSuite(
            TestSuiteConfiguration.FULL,
            new FileResultWriterProvider(),
            new FSMDescriptionProvider(new FSMTransitionTableBuilder(4, 40, new Randomizer()), FSMDescription.Sensor.ALL_SENSORS),
            new IAgentProvider[] {
                    new MaRzAgentProvider<>(new SuffixNodeProvider())
            }
    );

    private static TestSuite NsmVsMaRzFSM = new TestSuite(
            TestSuiteConfiguration.FULL,
            new FileResultWriterProvider(),
            new FSMDescriptionProvider(new FSMTransitionTableBuilder(3, 30, new Randomizer()), EnumSet.of(FSMDescription.Sensor.EVEN_ODD)),
            new IAgentProvider[] {
                    new NSMAgentProvider(),
                    new MaRzAgentProvider<>(new SuffixNodeProvider())
            }
    );

    private static TestSuite MarzLearnerFSM = new TestSuite(
            TestSuiteConfiguration.QUICK,
            new FileResultWriterProvider(),
            new FSMDescriptionProvider(new FSMTransitionTableBuilder(3, 30, new Randomizer()), FSMDescription.Sensor.ALL_SENSORS),
            new IAgentProvider[] {
                    new MaRzLearnerProvider<>(new SuffixNodeProvider()),
                    //new MaRzAgentProvider<>(new SuffixNodeProvider())
            }
    );

    private static TestSuite JunoVMarz3_30 = new TestSuite(
            TestSuiteConfiguration.MEDIUM,
            new FileResultWriterProvider(),
            new FSMDescriptionProvider(new FSMTransitionTableBuilder(3, 30, new Randomizer()), EnumSet.of(FSMDescription.Sensor.EVEN_ODD, FSMDescription.Sensor.NOISE)),
            new IAgentProvider[] {
                    new JunoAgentProvider(new SuffixNodeProvider(), new JunoConfiguration(true, .7, Double.MAX_VALUE)),
                    new MaRzAgentProvider<>(new SuffixNodeProvider())
            }
    );

    private static TestSuite JunoVJunoBail = new TestSuite(
            TestSuiteConfiguration.MEDIUM,
            new FileResultWriterProvider(),
            new FSMDescriptionProvider(new FSMTransitionTableBuilder(3, 30, new Randomizer()), EnumSet.of(FSMDescription.Sensor.EVEN_ODD)),
            new IAgentProvider[] {
                    new JunoAgentProvider(new SuffixNodeProvider()),
                    new JunoAgentProvider(new SuffixNodeProvider(),
                            new JunoConfiguration(true, 1, Double.MAX_VALUE))
            }
    );

    private static TestSuite JunoBail = new TestSuite(
            TestSuiteConfiguration.MEDIUM,
            new FileResultWriterProvider(),
            new FSMDescriptionProvider(new FSMTransitionTableBuilder(4, 40, new Randomizer()), EnumSet.of(FSMDescription.Sensor.EVEN_ODD)),
            new IAgentProvider[] {
                    new JunoAgentProvider(new SuffixNodeProvider(), new JunoConfiguration(true, 2, Double.MAX_VALUE))
            }
    );

    private static TestSuite MaRzMeta = new TestSuite(
            TestSuiteConfiguration.MEDIUM,
            new FileResultWriterProvider(),
            new MetaEnvironmentDescriptionProvider(
                    new FSMDescriptionTweakingProvider(new FSMTransitionTableBuilder(3,15, new Randomizer()),FSMDescription.Sensor.NO_SENSORS), // 1 numswap
                    MetaConfiguration.DEFAULT),
            new IAgentProvider[] {
                    new MaRzAgentProvider<>(new SuffixNodeProvider())
            }
    );

    private static TestSuite Suite2 = new TestSuite(
            TestSuiteConfiguration.FULL,
            new FileResultWriterProvider(),
            new MetaEnvironmentDescriptionProvider(new FSMDescriptionProvider(new FSMTransitionTableBuilder(2, 5, new Randomizer()), EnumSet.of(FSMDescription.Sensor.EVEN_ODD)), MetaConfiguration.DEFAULT),
            new IAgentProvider[] {
                    new NSMAgentProvider()
            }
    );

    public static void main(String[] args) {
        ArrayList<TestSuite> suites = new ArrayList<>();
        EnumSet withinAndEO = FSMDescription.Sensor.WITHIN_SENSORS;
        withinAndEO.add(FSMDescription.Sensor.EVEN_ODD);
        EnumSet sensorSets[] = new EnumSet[]{
                FSMDescription.Sensor.NO_SENSORS,
                EnumSet.of(FSMDescription.Sensor.EVEN_ODD),
                EnumSet.of(FSMDescription.Sensor.EVEN_ODD, FSMDescription.Sensor.WITHIN_1),
                EnumSet.of(FSMDescription.Sensor.EVEN_ODD, FSMDescription.Sensor.WITHIN_1, FSMDescription.Sensor.WITHIN_2),
                EnumSet.of(FSMDescription.Sensor.EVEN_ODD, FSMDescription.Sensor.WITHIN_1, FSMDescription.Sensor.WITHIN_2, FSMDescription.Sensor.WITHIN_4),
                EnumSet.of(FSMDescription.Sensor.EVEN_ODD, FSMDescription.Sensor.WITHIN_1, FSMDescription.Sensor.WITHIN_2, FSMDescription.Sensor.WITHIN_4, FSMDescription.Sensor.WITHIN_8),
                EnumSet.of(FSMDescription.Sensor.EVEN_ODD, FSMDescription.Sensor.WITHIN_1, FSMDescription.Sensor.WITHIN_2, FSMDescription.Sensor.WITHIN_4, FSMDescription.Sensor.WITHIN_8, FSMDescription.Sensor.WITHIN_10),
                withinAndEO
        };
        for(EnumSet i : sensorSets) {
            suites.add(new TestSuite(
                    TestSuiteConfiguration.FULL,
                    new FileResultWriterProvider(),
                    new FSMDescriptionProvider(new FSMTransitionTableBuilder(4, 40, new Randomizer()), i),
                    new IAgentProvider[]{
                            new JunoAgentProvider(
                                    new SuffixNodeProvider(),
                                    new JunoConfiguration(true, 0.7, Double.MAX_VALUE)
                            )
                            //new MaRzAgentProvider<>(new SuffixNodeProvider())
                    }
            ));
        }
        //TestSuite suite = JunoFSM;
        for(TestSuite suite : suites) {
            //System.out.println("beginning suite "+suites.indexOf(suite));
            try {
                System.out.println("Beginning suite "+suites.indexOf(suite));
                //Services.register(IRandomizer.class, new Randomizer(541)); //determined seed for debug
                String outputPath = suite.getResultWriterProvider().getOutputDirectory();
                NamedOutput namedOutput = createOutputStreamContainer(outputPath);

                suite.run();

                namedOutput.closeAll();
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

    private static NamedOutput createOutputStreamContainer(String outputPath) throws IOException {
        NamedOutput namedOutput = NamedOutput.getInstance();
        namedOutput.configure("metaData", Runner.getFileOutputStream(outputPath, "metadata.txt"));
        namedOutput.configure("agentDidAGood", Runner.getFileOutputStream(outputPath, "goodRatios.csv"));
        namedOutput.configure("goodDecisionBail", Runner.getFileOutputStream(outputPath, "goodDecisionBailRatio.csv"));
        namedOutput.configure("badDecisionBail", Runner.getFileOutputStream(outputPath, "badDecisionBailRatio.csv"));
        namedOutput.configure("properBails", Runner.getFileOutputStream(outputPath, "properBailRatio.csv"));
        namedOutput.configure("junoRatios", Runner.getFileOutputStream(outputPath, "junoRatios.csv"));
        return namedOutput;
    }

    private static FileOutputStream getFileOutputStream(String directory, String filename) throws IOException {
        String path =  Paths.get(directory, filename).toString();
        File file= new File(path);
        File parentFile = file.getParentFile();
        if (parentFile != null)
            parentFile.mkdirs();
        file.createNewFile();
        return new FileOutputStream(file);
    }

}
