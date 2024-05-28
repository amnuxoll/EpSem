package agents.tfsocket;

import framework.*;
import utils.EpisodicMemory;

import java.util.*;
import java.net.*;
import java.io.*;

/**
 *
 * This agent serves as a proxy for a TensorFlow agent written in Python.
 * It uses sockets to communicate
 *
 * @version created May 2024
 *
 * @author Andrew Nuxoll
 */
public class TFSocketAgent implements IAgent {

    private EpisodicMemory<Episode> episodicMemory;

    private Action[] actions;
    private IIntrospector introspector;
    private Random random;
    private Action lastGoalAction;
    private Process process;  //the python TensorFlow agent
    private ServerSocket server;  //We use sockets to communicate with the TF Agent
    private Socket sock;
    private PrintWriter sockWriter;
    private Scanner scriptOutput;

    public static final int PORT = 9026;

    public TFSocketAgent(Random random)
    {
        // For unit testing purposes we inject a Random object that can be seeded to produce
        // specific, repeating behaviors.
        this.random = random;

        // This data structure will retain our history of episodes that we generate.
        this.episodicMemory = new EpisodicMemory<>();
    }

    @Override
    public void initialize(Action[] actions, IIntrospector introspector) {
        // Initialize is called prior to running an Experiment in an environment.
        // The available actions for the environment are provided
        this.actions = actions;

        // The introspector can be used to query for information related to the
        // environment. This is strictly for analytics during data collection.
        // DO NOT CHEAT and use this to "discover" optimal action sequences.
        this.introspector = introspector;
        this.lastGoalAction = actions[0];

        //Run the python agent
        System.out.println("Launching Python Agent...");
        ProcessBuilder processBuilder = new ProcessBuilder("python3", "./src/agents/tfsocket/TFSocket.py", "" + PORT);
        processBuilder.redirectErrorStream(true);
        try {
            Process process = processBuilder.start();
            InputStream is = process.getInputStream();
            scriptOutput = new Scanner(is);
            printScriptOutputSoFar();
        }
        catch(IOException ioe) {
            System.err.println("ERROR launching python agent.");
            System.err.println("\tThe binary file 'python3' must exist and be in your PATH.");
            System.err.println(ioe);
            System.exit(-1);
        }

        //Listen for the python agent to connect
        System.out.println("Listening for agent on port " + PORT + "...");
        try {
            server = new ServerSocket(PORT);
            sock = server.accept();
            OutputStream outStream = sock.getOutputStream();
            sockWriter = new PrintWriter(outStream);
        }
        catch(IOException ioe) {
            System.err.println("ERROR connecting to python agent.");
            System.err.println(ioe);            
            System.exit(-2);
        }
        
    }

    /** prints any new output from the python script */
    private void printScriptOutputSoFar() {
            while(scriptOutput.hasNextLine()) {
                System.out.println("PYTHON: " + scriptOutput.nextLine());
            }
     }
    
    @Override
    public Action getNextAction(SensorData sensorData) throws Exception {

        sockWriter.println("banana republic");

        printScriptOutputSoFar();
        return new Action("a");
    }

    @Override
    public String[] getStatisticTypes() {
        // Additional data can be gathered for analysis by declaring the types of
        // analytics gathered prior to running the Experiment.
        HashSet<String> statistics = new HashSet<>();
        statistics.addAll(Arrays.asList(IAgent.super.getStatisticTypes()));
        statistics.add("actionConfidence");
        return statistics.toArray(new String[0]);
    }

    /**
     * Gets the actual statistical data for the most recent iteration of hitting a goal.
     *
     * @return the collection of {@link Datum} that indicate the agent statistical data to track.
     */
    @Override
    public ArrayList<Datum> getGoalData() {
        // When the goal is hit, report the additional analytics the agent should be tracking.
        ArrayList<Datum> data = new ArrayList<>();
        data.add(new Datum("actionConfidence", this.random.nextDouble()));
        return data;
    }

    /**
     * We want to use this opportunity to shut down the python agent
     */
    @Override
    public void onTestRunComplete() {
        //TODO:  send a kill signal
        try {
            process.waitFor();
        }
        catch(InterruptedException ie) {
            /* don't care */
        }

        printScriptOutputSoFar();
        scriptOutput.close();
    }

}
