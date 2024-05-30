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
    private Process process; // the python TensorFlow agent
    // We use sockets to communicate with the TF Agent
    private Socket sock;
    private Scanner scriptOutput;
    private OutputStream outStream;
    private InputStream inputStream; // Line added in pair programming session

    public static final int BASE_PORT = 8026;
    private int port;

    public TFSocketAgent(Random random) {
        // For unit testing purposes we inject a Random object that can be seeded to
        // produce
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

        //Pick a random port number in case stale python agents are sitting
        //around
        port = this.random.nextInt(1000) + BASE_PORT;
        
        // Run the python agent
        System.out.println("Launching Python Agent...");
        ProcessBuilder processBuilder = new ProcessBuilder("python3", "./src/agents/tfsocket/TFSocket.py", "" + port);
        processBuilder.redirectErrorStream(true);
        try {
            Process process = processBuilder.start();

            //TODO We would like the pythong output to show up in Java program's
            // stdout but this code below doesn't seem to work.
            
            // InputStream is = process.getInputStream();
            // scriptOutput = new Scanner(is);
            // printScriptOutputSoFar();
        } catch (IOException ioe) {
            System.err.println("ERROR launching python agent.");
            System.err.println("\tThe binary file 'python3' must exist and be in your PATH.");
            System.err.println(ioe);
            System.exit(-1);
        }

        // Listen for the python agent to connect
        System.out.println("Listening for agent on port " + port + "...");
        int refuseCount = 0;
        while (refuseCount < 5) {
            try {
                sock = new Socket("127.0.0.1", port);
                inputStream = sock.getInputStream(); // Line added in pair programming session
                outStream = sock.getOutputStream();
                break;
            } catch (ConnectException ce) {
                printScriptOutputSoFar();
                System.err.println("Connection refused.  Retrying...");
                refuseCount++;
                try {
                    Thread.sleep(1000);
                }
                catch (InterruptedException ie) {
                    System.err.println("Interrupted while waiting to retry connection.");
                    System.err.println(ie);
                }
            } catch (IOException ioe) {
                System.err.println("ERROR connecting to python agent.");
                System.err.println(ioe);
                System.exit(-2);
            }
        }

        if (refuseCount >=5) {
            System.out.println("Failed to connect to python agent.");
            sock = null;
            onTestRunComplete();
            System.exit(refuseCount);
        }
        System.out.println("Connected to python agent.");
    }

    /** prints any new output from the python script */
    private void printScriptOutputSoFar() {
        if (scriptOutput == null) {
            System.out.println("PYTHON: (no output)");
            return;
        }
        while (scriptOutput.hasNextLine()) {
            System.out.println("PYTHON: " + scriptOutput.nextLine());
        }

    }

    @Override
    public Action getNextAction(SensorData sensorData) throws Exception {

        if (sock == null) System.exit(-3);
        
        outStream.write("banana republic".getBytes());
        System.out.println((char)inputStream.read()); // Line added in pair programming session
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
     * Gets the actual statistical data for the most recent iteration of hitting a
     * goal.
     *
     * @return the collection of {@link Datum} that indicate the agent statistical
     *         data to track.
     */
    @Override
    public ArrayList<Datum> getGoalData() {
        // When the goal is hit, report the additional analytics the agent should be
        // tracking.
        ArrayList<Datum> data = new ArrayList<>();
        data.add(new Datum("actionConfidence", this.random.nextDouble()));
        return data;
    }

    /**
     * We want to use this opportunity to shut down the python agent
     */
    @Override
    public void onTestRunComplete() {
        // TODO: send a kill signal
        printScriptOutputSoFar();
        if (scriptOutput != null) {
            scriptOutput.close();
        }
        if ((process != null) && (process.isAlive())) {
            process.destroy();
        }
    }

}
