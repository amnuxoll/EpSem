package framework;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class FileResultWriterProviderTest {

    // getResultWriter Tests
    @Test
    public void getResultWriterNullAgentThrowsException() {
        FileResultWriterProvider resultWriterProvider = new FileResultWriterProvider("outputdirectory");
        assertThrows(IllegalArgumentException.class, () -> resultWriterProvider.getResultWriter(null, "file"));
    }

    @Test
    public void getResultWriterEmptyAgentThrowsException() {
        FileResultWriterProvider resultWriterProvider = new FileResultWriterProvider("outputdirectory");
        assertThrows(IllegalArgumentException.class, () -> resultWriterProvider.getResultWriter("", "file"));
    }

    @Test
    public void getResultWriterNullFileThrowsException() {
        FileResultWriterProvider resultWriterProvider = new FileResultWriterProvider("outputdirectory");
        assertThrows(IllegalArgumentException.class, () -> resultWriterProvider.getResultWriter("agent", null));
    }

    @Test
    public void getResultWriterEmptyFileThrowsException() {
        FileResultWriterProvider resultWriterProvider = new FileResultWriterProvider("outputdirectory");
        assertThrows(IllegalArgumentException.class, () -> resultWriterProvider.getResultWriter("agent", ""));
    }

    @Test
    public void getResultWriterLabelsAgentFile() throws Exception {
        FileResultWriterProvider resultWriterProvider = new FileResultWriterProvider("outputdirectory");
        IResultWriter resultWriter = resultWriterProvider.getResultWriter("myagent", "myagent");
        assertTrue(resultWriter instanceof FileResultWriter);
        FileResultWriter fileResultWriter = (FileResultWriter)resultWriter;
        assertTrue(fileResultWriter.getFileName().matches("^.*myagent\\.\\d+\\.csv"));
    }
}
