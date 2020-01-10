package resultcompilers.file;

import framework.Datum;
import framework.IResultCompiler;
import utils.DirectoryUtils;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

/**
 * A {@link FileResultCompiler} intelligently manages files to support asynchronous {@link framework.TestRun}s.
 *
 * TODO -- This is not truly thread-safe at the moment. It functions as-is because I know how it's being used.
 * Future work should make this more robust.
 *
 * @author Zachary Paul Faltersack
 * @version 0.95
 */
public class FileResultCompiler implements IResultCompiler {

    //region Class Variables

    /** The directory where any output files will be written. */
    private File outputDirectory;

    private HashMap<Integer, AgentDescription> agents = new HashMap<>();

    private HashMap<Integer, String> environments = new HashMap<>();

    private HashMap<WriterKey, FileWriter> writers =  new HashMap<>();

    private ArrayList<File> files = new ArrayList<>();

    private int numberOfGoals;

    private int numberOfIterations;

    //endregion

    //region Constructors

    /**
     * Create an instance of a {@link FileResultCompiler}.
     *
     * @param directory the location to write any output files to.
     */
    public FileResultCompiler(File directory) {
        if (directory == null)
            throw new IllegalArgumentException("directory cannot be null.");
        if (directory.isDirectory() == false)
            throw new IllegalArgumentException("provided file is not a directory.");
        this.outputDirectory = directory;
    }

    //endregion

    //region IResultCompiler Members

    /**
     * Inform the result compiler of the number of expected goals and iterations.
     *
     * @param numberOfGoals The number of goals in any single test run.
     * @param numberOfIterations The number of iterations to run an Agent/Environment combination.
     */
    @Override
    public void configureOutputs(int numberOfGoals, int numberOfIterations) {
        this.numberOfGoals = numberOfGoals;
        this.numberOfIterations = numberOfIterations;
    }

    /**
     * Notifies the {@link IResultCompiler} that an agent with the provided configurations will be used during a run.
     *
     * @param agentId The ID of the agent being registered.
     * @param agentAlias A human-friendly name for the agent.
     * @param dataToTrack The set of data expected to be captured for the agent during the test run.
     */
    @Override
    public void registerAgent(int agentId, String agentAlias, String[] dataToTrack) {
        AgentDescription description = new AgentDescription();
        description.alias = agentAlias;
        description.dataToTrack = dataToTrack;
        this.agents.put(agentId, description);
    }

    /**
     * Notifies the {@link IResultCompiler} that an environment with the provided configurations will be used
     * during a run.
     *
     * @param environmentId The ID of the environment being registered.
     * @param environmentAlias A human-friendly name for the environment.
     */
    @Override
    public void registerEnvironment(int environmentId, String environmentAlias) {
        this.environments.put(environmentId, environmentAlias);
    }

    /**
     * Once all the registrations have been made, this method is invoked to allow the implementation a
     * chance to build out any internal state it will use for logging data during a test run.
     *
     * @throws IOException
     */
    @Override
    public void build() throws IOException {
    }

    /**
     * Tracks results across agents, environments, and iterations.
     *
     * @param iteration Which iteration is being updated. Since the cross-product of agent/environments is itself
     *                  executed multiple times, this groups those results.
     * @param agentId The ID of the agent being updated.
     * @param environmentId The ID of the environment being updated.
     * @param goalNumber Which goal the test is at.
     * @param data The collection of {@link Datum} containing the results to log.
     * @throws IOException
     */
    @Override
    public void logResult(int iteration, int agentId, int environmentId, int goalNumber, ArrayList<Datum> data) throws IOException {
        WriterKey key = new WriterKey(iteration, agentId, environmentId, null);
        for (Datum datum : data) {
            key.setData(datum.getStatistic());
            if (!this.writers.containsKey(key)) {
                File file = new File(this.outputDirectory, "[" + environmentId + "][" + agentId + "][" + datum.getStatistic() + "][" + iteration + "]");
                this.files.add(file);
                this.writers.put(key, new FileWriter(file));
            }
            this.writers.get(key).write(datum.getDatum() + ",");
        }
    }

    /**
     * When the suite is complete, this method is executed in order to all the implementation a chance to
     * finalize the data sets it has collected.
     *
     * @throws IOException
     */
    @Override
    public void complete() throws IOException {
        for (FileWriter writer : this.writers.values()) {
            writer.close();
        }
        for (HashMap.Entry<Integer, String> environment : this.environments.entrySet()) {
            for (HashMap.Entry<Integer, AgentDescription> agent : this.agents.entrySet()) {
                for (String datum : agent.getValue().dataToTrack) {
                    List<File> relevantFiles = this.getFilesForKey(environment.getKey(), agent.getKey(), datum);
                    File targetFile = this.generateFile(agent.getKey(), agent.getValue().alias, environment.getKey(), environment.getValue(), datum);
                    try (PrintWriter writer = new PrintWriter(targetFile)) {
                        this.mergeFiles(writer, relevantFiles);
                        this.addAverages(writer, agent.getValue().alias);
                        relevantFiles.forEach(f -> f.delete());
                    }
                }
            }
        }
    }

    //endregion

    //region Private Methods

    private String convertToColumn(int column) {
        if (column <= 0)
            return "";
        column--;
        int right = column % 26;
        return this.convertToColumn(column / 26) + (char)(((int)'A') + right);
    }

    private File generateFile(int agentIndex, String agentAlias, int environmentIndex, String environmentAlias, String resultType) {
        String fileName = "env_" + environmentAlias + "_" + environmentIndex +  "_agent_" + agentAlias + "_" + agentIndex + "_" + resultType;
        return new File(this.outputDirectory,  fileName + "." + DirectoryUtils.getTimestamp(System.currentTimeMillis()) + ".csv");
    }

    private List<File> getFilesForKey(int environmentId, int agentId, String datum) {
        String keySet = "[" + environmentId + "][" + agentId + "][" + datum + "]";
        Comparator<File> iteration = Comparator.comparingInt(file -> {
            int index = file.getName().lastIndexOf("][");
            String value = file.getName().substring(index + 2);
            value = value.substring(0, value.length() - 1);
            return Integer.parseInt(value);
        });
        return this.files.stream().filter(file -> file.getName().startsWith(keySet)).sorted(iteration).collect(Collectors.toList());
    }

    private void mergeFiles(PrintWriter targetFileWriter, List<File> sourceFiles) throws IOException {
        for (File file : sourceFiles) {
            try (BufferedReader br = new BufferedReader(new FileReader(file))) {
                String line = br.readLine();
                while (line != null) {
                    targetFileWriter.println("," +line);
                    line = br.readLine();
                }
            }
        }
    }

    private void addAverages(PrintWriter writer, String agentAlias) {
        writer.write(agentAlias + " Average,");
        // Write out the basic goal sums
        for (int i = 2; i <= this.numberOfGoals + 1; i++) {
            int startRow = 1;
            int endRow = startRow + this.numberOfIterations - 1;
            String columnLabel = this.convertToColumn(i);
            writer.write("=average(" + columnLabel + startRow + ":" + columnLabel + endRow + "),");
        }
        writer.write("\n");
        writer.write(agentAlias + " Smoothed,,,,");

        // Write out the smoothing row
        for (int i = 5; i <= this.numberOfGoals - 2; i++) {
            String leftColumn = this.convertToColumn(i - 3);
            String rightColumn = this.convertToColumn(i + 3);
            int row = 1 + this.numberOfIterations;

            writer.write("=average(" + leftColumn + row + ":" + rightColumn + row + "),");
        }
        writer.write(",,,");
    }

    //endregion

    //region Nested Classes

    private class AgentDescription
    {
        public String alias;
        public String[] dataToTrack;
    }

    private class WriterKey {
        private int iterationId;
        private int agentId;
        private int environmentId;
        private String data;
        public WriterKey(int iterationId, int agentId, int environmentId, String data) {
            this.iterationId = iterationId;
            this.agentId = agentId;
            this.environmentId = environmentId;
            this.data = data;
        }

        public void setData(String data) {
            this.data = data;
        }

        @Override
        public int hashCode() {
            return Objects.hash(this.iterationId, this.agentId, this.environmentId, this.data);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o instanceof WriterKey) {
                WriterKey key = (WriterKey) o;
                if (this.iterationId != ((WriterKey) o).iterationId)
                    return false;
                if (this.agentId != key.agentId)
                    return false;
                if (this.environmentId != key.environmentId)
                    return false;
                if (!this.data.equals(key.data))
                    return false;
                return true;
            }
            return false;
        }
    }

    //endregion
}
