package experiments;

import agents.marz.MaRzAgentProvider;
import agents.marz.nodes.SuffixNodeProvider;
import agents.nsm.NSMAgentProvider;
import environments.fsm.FSMDescription;
import environments.fsm.FSMDescriptionProvider;
import environments.meta.FSMDescriptionTweaker;
import environments.meta.MetaConfiguration;
import environments.meta.MetaEnvironmentDescriptionProvider;
import framework.*;

import java.util.EnumSet;

public class Runner {

    private static TestSuite MaRzFSM = new TestSuite(
            TestSuiteConfiguration.MEDIUM,
            new FileResultWriterProvider(),
            new FSMDescriptionProvider(3, 15, FSMDescription.Sensor.NO_SENSORS),
            new IAgentProvider[] {
                    new MaRzAgentProvider<>(new SuffixNodeProvider())
            }
    );

    private static TestSuite MaRzMeta = new TestSuite(
            TestSuiteConfiguration.MEDIUM,
            new FileResultWriterProvider(),
            new MetaEnvironmentDescriptionProvider(
                    new FSMDescriptionTweaker(3,15,FSMDescription.Sensor.NO_SENSORS),
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
            Runner.MaRzMeta.run();
        } catch (Exception ex)
        {
            System.out.println("Runner failed with exception: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

}
