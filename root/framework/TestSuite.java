package framework;

import java.io.*;
import java.nio.file.Paths;

/**
 *
 * @author Zachary Paul Faltersack
 * @version 0.95
 */
public class TestSuite implements IGoalListener, IEnvironmentListener {

    private TestSuiteConfiguration configuration;
    private IResultWriterProvider resultWriterProvider;
    private IEnvironmentDescriptionProvider environmentDescriptionProvider;
    private IAgentProvider[] agentProviders;

    private IResultWriter currentStepResultWriter;
    private IResultWriter currentDecisionResultWriter;

    public TestSuite(TestSuiteConfiguration configuration, IResultWriterProvider resultWriterProvider, IEnvironmentDescriptionProvider environmentDescriptionProvider, IAgentProvider[] agentProviders) {
        if (configuration == null)
            throw new IllegalArgumentException("configuration cannot be null.");
        if (resultWriterProvider == null)
            throw new IllegalArgumentException("resultWriterProvider cannot be null");
        if (environmentDescriptionProvider == null)
            throw new IllegalArgumentException("environmentDescriptionProvider cannot be null.");
        if (agentProviders == null)
            throw new IllegalArgumentException("agentProviders cannot be null.");
        if (agentProviders.length == 0)
            throw new IllegalArgumentException("agentProviders cannot be empty.");
        this.configuration = configuration;
        this.resultWriterProvider = resultWriterProvider;
        this.environmentDescriptionProvider = environmentDescriptionProvider;
        this.agentProviders = agentProviders;
    }

    public void run() throws Exception {
        System.out.println("Beginning test suite...");
        writeMetaData(resultWriterProvider.getOutputDirectory());

        int numberOfIterations = this.configuration.getNumberOfIterations();
        for (int i = 0; i < this.agentProviders.length; i++) {
            System.out.println("Beginning agent: " + i);
            this.currentStepResultWriter = this.resultWriterProvider.getResultWriter("agent" + i + "_steps");
            this.currentDecisionResultWriter = this.resultWriterProvider.getResultWriter("agent" + i + "_decisions");

            IAgentProvider agentProvider = this.agentProviders[i];
            this.runAgent(agentProvider, numberOfIterations);
        }
    }

    private void runAgent(IAgentProvider agentProvider, int numberOfIterations) {
        for (int i = 0; i < numberOfIterations; i++) {
            System.out.println();
            System.out.println("Beginning iteration: " + i);
            IAgent agent = agentProvider.getAgent();
            IEnvironmentDescription environmentDescription = this.environmentDescriptionProvider.getEnvironmentDescription();
            TestRun testRun = new TestRun(agent, environmentDescription, this.configuration.getNumberOfGoals());
            testRun.addGoalListener(this);
            this.currentStepResultWriter.beginNewRun();
            this.currentDecisionResultWriter.beginNewRun();
            testRun.execute();

            try {
                Services.retrieve(OutputStreamContainer.class).get("ratioOutputStream").write('\n');
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        this.currentStepResultWriter.complete();
        this.currentDecisionResultWriter.complete();
    }

    @Override
    public void goalReceived(GoalEvent event) {
        this.currentStepResultWriter.logStepsToGoal(event.getStepCountToGoal());
        this.currentDecisionResultWriter.logStepsToGoal(event.getDecisionCountToGoal());
    }

    private void writeMetaData(String parentDirectory){
        File file= new File(Paths.get(parentDirectory, "metadata.txt").toString());

        File parentFile = file.getParentFile();
        if (parentFile != null)
            parentFile.mkdirs();

        PrintWriter fos= null;
        try {
            fos= new PrintWriter(file);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return;
        }

        //information about configuration:
        fos.println(configuration.getNumberOfGoals() + " goals on " +
                    configuration.getNumberOfIterations() + " environments");

        fos.println();

        //information about environment provider
        fos.println("Environment provider type: " + environmentDescriptionProvider.getClass().getName());

        fos.println();

        //information about environment
        IEnvironmentDescription environment= environmentDescriptionProvider.getEnvironmentDescription();
        fos.println("Example of possible environment description:");
        fos.println("\tType: " + environment.getClass().getName());
        fos.println("\tNumber of moves: " + environment.getMoves().length);
        fos.println("\tNumber of states: " + environment.getNumStates());

        fos.println();

        //information about agent provider
        for(int i=0; i < agentProviders.length; i++){
            fos.println("Agent provider " + i + " type: " + agentProviders[i].getClass().getName());
        }

        fos.close();
    }

    @Override
    public void receiveEvent(EnvironmentEvent event) {
        if(event.getEventType() == EnvironmentEvent.EventType.DATA_BREAK){
            this.currentStepResultWriter.logStepsToGoal(0);
        }
    }
}
