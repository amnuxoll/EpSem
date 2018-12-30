package framework;

import org.junit.jupiter.api.Test;

import java.io.File;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 *
 * @author Zachary Paul Faltersack
 * @version 0.95
 */
public class FileResultWriterProviderTest {
    //region Class Variables
    private File outputDirectory = new File("outputdirectory");
    //endregion

    //region Constructor Tests
    @Test
    public void constructorNullDirectoryThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> new FileResultWriterProvider(null));
    }

    @Test
    public void constructorFileNotADirectoryThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> new FileResultWriterProvider(new File("output.txt")));
    }
    //endregion

    //region getResultWriter Tests
    @Test
    public void getResultWriterNullAgentThrowsException() {
        try {
            this.outputDirectory.mkdirs();
            FileResultWriterProvider resultWriterProvider = new FileResultWriterProvider(this.outputDirectory);
            assertThrows(IllegalArgumentException.class, () -> resultWriterProvider.getResultWriter(null, "file"));
        } finally {
            this.outputDirectory.delete();
        }
    }

    @Test
    public void getResultWriterEmptyAgentThrowsException() {
        try {
            this.outputDirectory.mkdirs();
            FileResultWriterProvider resultWriterProvider = new FileResultWriterProvider(this.outputDirectory);
            assertThrows(IllegalArgumentException.class, () -> resultWriterProvider.getResultWriter("", "file"));
        } finally {
            this.outputDirectory.delete();
        }
    }

    @Test
    public void getResultWriterNullFileThrowsException() {
        try {
            this.outputDirectory.mkdirs();
            FileResultWriterProvider resultWriterProvider = new FileResultWriterProvider(this.outputDirectory);
            assertThrows(IllegalArgumentException.class, () -> resultWriterProvider.getResultWriter("agent", null));
        } finally {
            this.outputDirectory.delete();
        }
    }

    @Test
    public void getResultWriterEmptyFileThrowsException() {
        try {
            this.outputDirectory.mkdirs();
            FileResultWriterProvider resultWriterProvider = new FileResultWriterProvider(this.outputDirectory);
            assertThrows(IllegalArgumentException.class, () -> resultWriterProvider.getResultWriter("agent", ""));
        } finally {
            this.outputDirectory.delete();
        }
    }

    @Test
    public void getResultWriterLabelsAgentFile() throws Exception {
        try {
            this.outputDirectory.mkdirs();
            FileResultWriterProvider resultWriterProvider = new FileResultWriterProvider(this.outputDirectory);
            IResultWriter resultWriter = resultWriterProvider.getResultWriter("myagent", "myagent");
            assertTrue(resultWriter instanceof FileResultWriter);
            FileResultWriter fileResultWriter = (FileResultWriter) resultWriter;
            File file = this.outputDirectory.listFiles()[0];
            assertTrue(file.getName().matches("^.*myagent\\.\\d+\\.csv"));
            fileResultWriter.closeFile();
            file.delete();
        } finally {
            this.outputDirectory.delete();
        }
    }
    //endregion

    //region getOutputDirectory Tests
    @Test
    public void getOutputDirectory() {
        try {
            this.outputDirectory.mkdirs();
            FileResultWriterProvider resultWriterProvider = new FileResultWriterProvider(this.outputDirectory);
            assertSame(this.outputDirectory, resultWriterProvider.getOutputDirectory());
        } finally {
            this.outputDirectory.delete();
        }
    }
    //endregion
}
