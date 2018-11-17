package framework;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class FileResultWriterProviderTest {

    // getResultWriter Tests
    @Test
    public void getResultWriterNullAgentThrowsException() {
        FileResultWriterProvider resultWriterProvider = new FileResultWriterProvider();
        assertThrows(IllegalArgumentException.class, () -> resultWriterProvider.getResultWriter(null));
    }

    @Test
    public void getResultWriterEmptyAgentThrowsException() {
        FileResultWriterProvider resultWriterProvider = new FileResultWriterProvider();
        assertThrows(IllegalArgumentException.class, () -> resultWriterProvider.getResultWriter(""));
    }

    @Test
    public void getResultWriterLabelsAgentFile() throws Exception {
        FileResultWriterProvider resultWriterProvider = new FileResultWriterProvider();
        IResultWriter resultWriter = resultWriterProvider.getResultWriter("myagent");
        assertTrue(resultWriter instanceof FileResultWriter);
        FileResultWriter fileResultWriter = (FileResultWriter)resultWriter;
        assertTrue(fileResultWriter.getFileName().matches("^.*myagent\\.\\d+\\.csv"));
    }
}
