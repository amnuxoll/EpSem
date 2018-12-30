package framework;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class NamedOutputTest {
    //region configure Tests
    @Test
    public void configureNullKeyThrowsException() {
        NamedOutput namedOutput = NamedOutput.getInstance();
        assertThrows(IllegalArgumentException.class, () -> namedOutput.configure(null, System.out));
    }

    @Test
    public void configureEmptyKeyThrowsException() {
        NamedOutput namedOutput = NamedOutput.getInstance();
        assertThrows(IllegalArgumentException.class, () -> namedOutput.configure("", System.out));
    }

    @Test
    public void configureNullOutputStreamThrowsException() {
        NamedOutput namedOutput = NamedOutput.getInstance();
        assertThrows(IllegalArgumentException.class, () -> namedOutput.configure("name", null));
    }
    //endregion

    //region writeLine Tests
    @Test
    public void writeLineNullKeyNoDataThrowsException() {
        NamedOutput namedOutput = NamedOutput.getInstance();
        assertThrows(IllegalArgumentException.class, () -> namedOutput.writeLine(null));
    }

    @Test
    public void writeLineEmptyKeyNoDataThrowsException() {
        NamedOutput namedOutput = NamedOutput.getInstance();
        assertThrows(IllegalArgumentException.class, () -> namedOutput.writeLine(""));
    }

    @Test
    public void writeLineNullKeyWithDataThrowsException() {
        NamedOutput namedOutput = NamedOutput.getInstance();
        assertThrows(IllegalArgumentException.class, () -> namedOutput.writeLine(null, ""));
    }

    @Test
    public void writeLineEmptyKeyWithDataThrowsException() {
        NamedOutput namedOutput = NamedOutput.getInstance();
        assertThrows(IllegalArgumentException.class, () -> namedOutput.writeLine("", ""));
    }

    @Test
    public void writeLineNullDataThrowsException() {
        NamedOutput namedOutput = NamedOutput.getInstance();
        assertThrows(IllegalArgumentException.class, () -> namedOutput.writeLine("name", null));
    }

    @Test
    public void writeLineNoDataAppendsNewline() {
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            NamedOutput namedOutput = NamedOutput.getInstance();
            namedOutput.configure("name", outputStream);
            namedOutput.writeLine("name");
            assertEquals("\n", outputStream.toString());
        } finally {
            NamedOutput.getInstance().closeAll();
        }
    }

    @Test
    public void writeLineAppendsNewline() {
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            NamedOutput namedOutput = NamedOutput.getInstance();
            namedOutput.configure("name", outputStream);
            namedOutput.writeLine("name", "data");
            assertEquals("data\n", outputStream.toString());
        } finally {
            NamedOutput.getInstance().closeAll();
        }
    }
    //endregion

    //region write Tests
    @Test
    public void writeNullKeyWithDataThrowsException() {
        NamedOutput namedOutput = NamedOutput.getInstance();
        assertThrows(IllegalArgumentException.class, () -> namedOutput.write(null, ""));
    }

    @Test
    public void writeEmptyKeyWithDataThrowsException() {
        NamedOutput namedOutput = NamedOutput.getInstance();
        assertThrows(IllegalArgumentException.class, () -> namedOutput.write("", ""));
    }

    @Test
    public void writeNullDataThrowsException() {
        NamedOutput namedOutput = NamedOutput.getInstance();
        assertThrows(IllegalArgumentException.class, () -> namedOutput.write("name", (String) null));
    }

    @Test
    public void writeNullKeyWithExceptionThrowsException() {
        NamedOutput namedOutput = NamedOutput.getInstance();
        assertThrows(IllegalArgumentException.class, () -> namedOutput.write(null, new Exception()));
    }

    @Test
    public void writeEmptyKeyWithExceptionThrowsException() {
        NamedOutput namedOutput = NamedOutput.getInstance();
        assertThrows(IllegalArgumentException.class, () -> namedOutput.write("", new Exception()));
    }

    @Test
    public void writeNullExceptionThrowsException() {
        NamedOutput namedOutput = NamedOutput.getInstance();
        assertThrows(IllegalArgumentException.class, () -> namedOutput.write("name", (Exception) null));
    }

    @Test
    public void writeData() {
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            NamedOutput namedOutput = NamedOutput.getInstance();
            namedOutput.configure("name", outputStream);
            namedOutput.write("name", "data");
            assertEquals("data", outputStream.toString());
        } finally {
            NamedOutput.getInstance().closeAll();
        }
    }

    @Test
    public void writeException() {
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            NamedOutput namedOutput = NamedOutput.getInstance();
            namedOutput.configure("name", outputStream);
            namedOutput.write("name", new Exception("message"));
            assertTrue(outputStream.toString().startsWith("Exception logged: message\n"));
        } finally {
            NamedOutput.getInstance().closeAll();
        }
    }
    //endregion
}
