package tests.framework;

import framework.NamedOutput;
import java.io.ByteArrayOutputStream;

import tests.EpSemTest;
import tests.EpSemTestClass;
import static tests.Assertions.*;

/**
 *
 * @author Zachary Paul Faltersack
 * @version 0.95
 */
@EpSemTestClass
public class NamedOutputTest {
    //region configure Tests
    @EpSemTest
    public void configureNullKeyThrowsException() {
        NamedOutput namedOutput = NamedOutput.getInstance();
        assertThrows(IllegalArgumentException.class, () -> namedOutput.configure(null, System.out));
    }

    @EpSemTest
    public void configureEmptyKeyThrowsException() {
        NamedOutput namedOutput = NamedOutput.getInstance();
        assertThrows(IllegalArgumentException.class, () -> namedOutput.configure("", System.out));
    }

    @EpSemTest
    public void configureNullOutputStreamThrowsException() {
        NamedOutput namedOutput = NamedOutput.getInstance();
        assertThrows(IllegalArgumentException.class, () -> namedOutput.configure("name", null));
    }
    //endregion

    //region writeLine Tests
    @EpSemTest
    public void writeLineNullKeyNoDataThrowsException() {
        NamedOutput namedOutput = NamedOutput.getInstance();
        assertThrows(IllegalArgumentException.class, () -> namedOutput.writeLine(null));
    }

    @EpSemTest
    public void writeLineEmptyKeyNoDataThrowsException() {
        NamedOutput namedOutput = NamedOutput.getInstance();
        assertThrows(IllegalArgumentException.class, () -> namedOutput.writeLine(""));
    }

    @EpSemTest
    public void writeLineNullKeyWithDataThrowsException() {
        NamedOutput namedOutput = NamedOutput.getInstance();
        assertThrows(IllegalArgumentException.class, () -> namedOutput.writeLine(null, ""));
    }

    @EpSemTest
    public void writeLineEmptyKeyWithDataThrowsException() {
        NamedOutput namedOutput = NamedOutput.getInstance();
        assertThrows(IllegalArgumentException.class, () -> namedOutput.writeLine("", ""));
    }

    @EpSemTest
    public void writeLineNullDataThrowsException() {
        NamedOutput namedOutput = NamedOutput.getInstance();
        assertThrows(IllegalArgumentException.class, () -> namedOutput.writeLine("name", null));
    }

    @EpSemTest
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

    @EpSemTest
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
    @EpSemTest
    public void writeNullKeyWithDataThrowsException() {
        NamedOutput namedOutput = NamedOutput.getInstance();
        assertThrows(IllegalArgumentException.class, () -> namedOutput.write(null, ""));
    }

    @EpSemTest
    public void writeEmptyKeyWithDataThrowsException() {
        NamedOutput namedOutput = NamedOutput.getInstance();
        assertThrows(IllegalArgumentException.class, () -> namedOutput.write("", ""));
    }

    @EpSemTest
    public void writeNullDataThrowsException() {
        NamedOutput namedOutput = NamedOutput.getInstance();
        assertThrows(IllegalArgumentException.class, () -> namedOutput.write("name", (String) null));
    }

    @EpSemTest
    public void writeNullKeyWithExceptionThrowsException() {
        NamedOutput namedOutput = NamedOutput.getInstance();
        assertThrows(IllegalArgumentException.class, () -> namedOutput.write(null, new Exception()));
    }

    @EpSemTest
    public void writeEmptyKeyWithExceptionThrowsException() {
        NamedOutput namedOutput = NamedOutput.getInstance();
        assertThrows(IllegalArgumentException.class, () -> namedOutput.write("", new Exception()));
    }

    @EpSemTest
    public void writeNullExceptionThrowsException() {
        NamedOutput namedOutput = NamedOutput.getInstance();
        assertThrows(IllegalArgumentException.class, () -> namedOutput.write("name", (Exception) null));
    }

    @EpSemTest
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

    @EpSemTest
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
