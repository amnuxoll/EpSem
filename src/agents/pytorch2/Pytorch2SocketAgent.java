package agents.pytorch2;

import framework.*;

import java.util.Random;
import java.io.*;
import java.net.*;


/**
 *
 * This agent was written as a quick study for creating a new agent with a basic
 * heuristic for randomly selecting between a random action and the last
 * action taken before a goal.
 *
 * @author Zachary Faltersack
 */
public class Pytorch2SocketAgent implements IAgent {

    private Action[] actions; //list of actions an agent is allowed to take in a given state
    private IIntrospector introspector; //not used
    private Random random;

    private Process process; // the python agent

    private Socket sock; //use this socket to communicate with Python agent
    private OutputStream outStream; //used to send data to Python agent
    private InputStream inputStream; //used to receive data to Python agent

    //every time the agent runs it selects a random socket to use within a range 
    //calculates this using BASE_PORT + random number

    public static final int BASE_PORT = 8026;
    private int port; //port we use for the socket 

    public Pytorch2SocketAgent(Random random)
    {
        // For unit testing purposes we inject a Random object that can be seeded to produce
        // specific, repeating behaviors.
        this.random = random;
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

        //Pick a random port number in case stale python agents are sitting
        //around
        port = this.random.nextInt(1000) + BASE_PORT;
        
        // Run the python agent
        System.out.println("Pytorch2 Agent");
        System.out.println("Launching Python Agent...");
        System.out.println("python3" + " ./src/agents/pytorch2/Pytorch2Socket.py" + " " + port);
        ProcessBuilder processBuilder = new ProcessBuilder("python3", "./src/agents/pytorch2/Pytorch2Socket.py", "" + port);
        processBuilder.redirectErrorStream(true);
        try {
            Process process = processBuilder.start();

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
                System.out.println("test1a");
                sock = new Socket("127.0.0.1", port);
                System.out.println("test1b");
                inputStream = sock.getInputStream();
                System.out.println("test1c");
                outStream = sock.getOutputStream();
                break;
            } catch (ConnectException ce) {
                if (refuseCount > 0) System.err.println("Connection refused.  Retrying...");
                refuseCount++;
                try {
                    Thread.sleep(10000);
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

        //Report a failed connect
        if (refuseCount >=5) {
            System.out.println("Failed to connect to python agent.");
            sock = null;
            shutdown();
            System.exit(refuseCount);
        }

        //Pass the alphabet to the python agent
        String alphabet = "";
        for(Action act : actions) {
            alphabet += act;
        }
        sendMessage("$$$alphabet:" + alphabet);

        //wait for acknowledge signal from the python agent
        Action ack = getDemoAction();
        if (! ack.getName().equals("ack")) {
            System.out.println("Agent did not sent proper acknowledge signal.");
            sock = null;
            shutdown();
            System.exit(-16);
        }
        
        System.out.println("Connected to python agent.");
    } //end of initialize()

    @Override
    public Action getNextAction(SensorData sensorData) throws Exception {
        
        // System.out.println(hist);
        sendMessage("hit me");

        //Retrieve/use the agent's response
        Action act = getDemoAction();
        System.out.println("received action from Python agent: " + act);

        return act;
    }

    /** aborts the agent.  This is usually due to socket communication failure. */
    private void abort(int errNum, String errMsg) {
        sock=null;
        System.err.println("COMM ERROR: " + errMsg);
        shutdown();
        System.exit(errNum);
    }

    /** sends a message to the python agent via the socket.  If something goes
     * wrong this method will abort the run and exit.  */
    private void sendMessage(String msg) {
        if (sock == null) abort(-4, "socket is null");
        if (outStream == null) abort(-5, "outStream is null");

        try {
            outStream.write(msg.getBytes());
        }
        catch(IOException ioe) {
            abort(-6, "sending message to python agent");
        }
    }

     /** reads the next action from the python agent.  If, instead, a message is
     * passed then this method responds appropiately */
    private Action getDemoAction() {
        if (inputStream == null) abort(-7, "inputStream is null");

        //Attempt to read from the socket
        byte[] buf = new byte[1024];
        int numChars = -2;
        int waitDuration = 0; //how long we've been waiting so far
        while(numChars < 0) {
            try {
                numChars = inputStream.read(buf);
            }
            catch(IOException ioe) {
                abort(-8, "receiving message from python agent");
            }
            if (numChars < 0) {
                try {
                    waitDuration += 10;
                    Thread.sleep(10);
                }
                catch(InterruptedException ie) {
                    /* it's okay */
                }
            }

            //We've waited too long
            if (waitDuration > 5000) {
                abort(-14, "timeout waiting for response from python agent");
            }
        }

        //abort on problems
        if (numChars == -1) abort(-9, "inputStream was closed.");
        if (numChars == -2) abort(-12, "unknown failure reading from inputStream.");

        //check for sentinel messages
        String msg = new String(buf);
        if (msg.startsWith("$$$")) {
            if (msg.startsWith("$$$ack")) {
                System.out.println("python agent sent acknowledge signal");
                return new Action("ack");
            }
            else if (msg.startsWith("$$$abort")) {
                abort(-13, "python agent sent abort signal");
            }
        }
        msg = msg.trim();

        //Verify that the message is a valid action
        if (msg.length() != 1) {
            abort(-10, "received action of length != 1: " + msg);
        }
        boolean found = false;
        for(Action act : this.actions) {
            if (act.toString().equals(msg)) {
                return act;  //Success!
            }
        }

        abort(-11, "unknown action: " + msg);
        return null; //should never be reached

    }//getDemoAction

     /**
     * shutdown the python and java agent.  This is separate from onTestRunComplete()
     * because it can be used for both a successful end of run or for an abort
     */
    private void shutdown() {
        System.out.println("Sending quit message to python agent.");
        if (sock == null) {
            System.out.println("Socket reference is null!");
        }
        else {
            sendMessage("$$$quit");

            System.out.println("Closing the Java-side socket.");
            try {
                sock.close();
            }
            catch(IOException ioe) {
                System.out.println("Could not close socket.  Oh well...");
            }
        }

        if ((process != null) && (process.isAlive())) {
            System.out.println("Attempting to kill the python process.");
            process.destroy();
        }
    }
     
}
