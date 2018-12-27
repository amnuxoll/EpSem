package framework;

import utils.ExceptionStackTraceToString;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

/**
 * A FileResultWriter will take result logging statements and write them in a structured way
 * to some provided file.
 * @author Zachary Paul Faltersack
 * @version 0.95
 */
public class FileResultWriter implements IResultWriter {
    private String fileName;
    private FileWriter fileWriter;
    private int currentNumberOfResults = 0;
    private int maxNumberOfResults = 0;
    private int numberOfRuns = 0;
    private String agent;

    /**
     * Create an instance of a {@link FileResultWriter} that writes to the given file path.
     * @param outputFile The path to an output file which will receive the results.
     */
    public FileResultWriter(String agent, String outputFile) throws IOException {
        if (agent == null)
            throw new IllegalArgumentException("agent cannot be null");
        if (agent == "")
            throw new IllegalArgumentException("agent cannot be empty");
        if (outputFile == null)
            throw new IllegalArgumentException("outputFile cannot be null");
        if (outputFile == "")
            throw new IllegalArgumentException("outputFile cannot be empty");
        this.agent = agent;
        this.fileName = outputFile;
        File file = new File(outputFile);
        File parentFile = file.getParentFile();
        if (parentFile != null)
            parentFile.mkdirs();
        file.createNewFile();
        this.fileWriter = new FileWriter(file);
    }

    /**
     * Get the name of the output file for this {@link FileResultWriter} instance.
     * @return The path of the output file.
     */
    public String getFileName() {
        return this.fileName;
    }

    /**
     * Log the number of steps taken to reach the last goal.
     * @param result The step count to the last located goal.
     */
    @Override
    public void logResult(String result) {
        this.currentNumberOfResults++;
        try {
            this.fileWriter.write(result + ",");
            this.fileWriter.flush();
        } catch (IOException ex) {
            // do anything here?
        }
    }

    /**
     * Begin a new batch of attempts to locate the goal.
     */
    @Override
    public void beginNewRun() {
        this.numberOfRuns++;
        this.maxNumberOfResults = Math.max(this.maxNumberOfResults, this.currentNumberOfResults);
        this.currentNumberOfResults = 0;
        try {
            this.fileWriter.write("\n");
            this.fileWriter.write(this.numberOfRuns + ",");
            this.fileWriter.flush();
        } catch (IOException ex) {
            // do anything here?
        }
    }

    /**
     * Complete the result file by adding in the sum and smoothing rows and closing the file.
     */
    @Override
    public void complete() {
        try {
            this.fileWriter.write("\n");
            this.fileWriter.write(this.agent + " Average,");
            // Write out the basic goal sums
            for (int i = 2; i <= this.maxNumberOfResults + 1; i++) {
                int startRow = 2;
                int endRow = startRow + this.numberOfRuns - 1;
                String columnLabel = this.convertToColumn(i);
                this.fileWriter.write("=average(" + columnLabel + startRow + ":" + columnLabel + endRow + "),");
            }
            this.fileWriter.write("\n");
            this.fileWriter.write(this.agent + " Smoothed,,,,");

            // Write out the smoothing row
            for (int i = 5; i <= this.maxNumberOfResults - 2; i++) {
                String leftColumn = this.convertToColumn(i - 3);
                String rightColumn = this.convertToColumn(i + 3);
                int row = 2 + this.numberOfRuns;

                this.fileWriter.write("=average(" + leftColumn + row + ":" + rightColumn + row + "),");
            }
            this.fileWriter.write(",,,");
            this.fileWriter.close();
        } catch (IOException ex) {
            NamedOutput namedOutput = NamedOutput.getInstance();
            namedOutput.write("framework", "TestRun failed with exception: " + ex.getMessage());
            namedOutput.write("framework", ExceptionStackTraceToString.getString(ex));
        }
    }

    private String convertToColumn(int column) {
        if (column <= 0)
            return "";
        column--;
        int right = column % 26;
        return this.convertToColumn(column / 26) + (char)(((int)'A') + right);
    }

}
