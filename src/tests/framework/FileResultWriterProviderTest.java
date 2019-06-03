package tests.framework;

import framework.FileResultWriter;
import framework.FileResultWriterProvider;
import framework.IResultWriter;

import java.io.File;

import tests.EpSemTest;
import tests.EpSemTestClass;
import static tests.Assertions.*;

/**
 *
 * @author Zachary Paul Faltersack
 * @version 0.95
 */
@EpSemTestClass
public class FileResultWriterProviderTest {
    //region Class Variables
    private File outputDirectory = new File("outputdirectory");
    //endregion

    //region Constructor Tests
    @EpSemTest
    public void constructorNullDirectoryThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> new FileResultWriterProvider(null));
    }

    @EpSemTest
    public void constructorFileNotADirectoryThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> new FileResultWriterProvider(new File("output.txt")));
    }
    //endregion

    //region getResultWriter Tests
    @EpSemTest
    public void getResultWriterNullAgentThrowsException() {
        try {
            this.outputDirectory.mkdirs();
            FileResultWriterProvider resultWriterProvider = new FileResultWriterProvider(this.outputDirectory);
            assertThrows(IllegalArgumentException.class, () -> resultWriterProvider.getResultWriter(null, "file"));
        } finally {
            this.outputDirectory.delete();
        }
    }

    @EpSemTest
    public void getResultWriterEmptyAgentThrowsException() {
        try {
            this.outputDirectory.mkdirs();
            FileResultWriterProvider resultWriterProvider = new FileResultWriterProvider(this.outputDirectory);
            assertThrows(IllegalArgumentException.class, () -> resultWriterProvider.getResultWriter("", "file"));
        } finally {
            this.outputDirectory.delete();
        }
    }

    @EpSemTest
    public void getResultWriterNullFileThrowsException() {
        try {
            this.outputDirectory.mkdirs();
            FileResultWriterProvider resultWriterProvider = new FileResultWriterProvider(this.outputDirectory);
            assertThrows(IllegalArgumentException.class, () -> resultWriterProvider.getResultWriter("agent", null));
        } finally {
            this.outputDirectory.delete();
        }
    }

    @EpSemTest
    public void getResultWriterEmptyFileThrowsException() {
        try {
            this.outputDirectory.mkdirs();
            FileResultWriterProvider resultWriterProvider = new FileResultWriterProvider(this.outputDirectory);
            assertThrows(IllegalArgumentException.class, () -> resultWriterProvider.getResultWriter("agent", ""));
        } finally {
            this.outputDirectory.delete();
        }
    }

    @EpSemTest
    public void getResultWriterLabelsAgentFile() throws Exception {
        try {
            this.outputDirectory.mkdirs();
            FileResultWriterProvider resultWriterProvider = new FileResultWriterProvider(this.outputDirectory);
            try (IResultWriter resultWriter = resultWriterProvider.getResultWriter("myagent", "myagent")) {
                assertTrue(resultWriter instanceof FileResultWriter);
            } finally {
                File file = this.outputDirectory.listFiles()[0];
                assertTrue(file.getName().matches("^.*myagent\\.\\d+\\.csv"));
                file.delete();
            }
        } finally {
            this.outputDirectory.delete();
        }
    }
    //endregion

    //region getOutputDirectory Tests
    @EpSemTest
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
