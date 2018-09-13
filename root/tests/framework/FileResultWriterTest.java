package framework;

import org.junit.jupiter.api.Test;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class FileResultWriterTest {

    @Test
    public void testConstructorNullNameThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> new FileResultWriter(null));
    }


    @Test
    public void testConstructorEmptyNameThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> new FileResultWriter(""));
    }

    @Test
    public void testGetFileName() throws Exception {
        String fileName = "output.csv";
        try {
            FileResultWriter writer = new FileResultWriter(fileName);
            assertEquals(fileName, writer.getFileName());
        }
        finally {
            try {
                new File(fileName).delete();
            } catch (Exception ex) {

            }
        }
    }

    @Test
    public void testLogStepsToGoalSingleStep()throws Exception {
        String fileName = "output.csv";
        try {
            FileResultWriter writer = new FileResultWriter(fileName);
            writer.logStepsToGoal(13);
            try {
                List<String> lines = Files.readAllLines(Paths.get(fileName));
                assertEquals(1, lines.size());
                String firstLine = lines.get(0);
                assertEquals("13,", firstLine);
            } catch (IOException ex) {
                fail(ex.getMessage());
            }
        } finally {
            try {
                new File(fileName).delete();
            } catch (Exception ex) {

            }
        }
    }

    @Test
    public void testLogStepsToGoalMultipleSteps() throws Exception {
        String fileName = "output.csv";
        try {
            FileResultWriter writer = new FileResultWriter(fileName);
            writer.logStepsToGoal(13);
            writer.logStepsToGoal(7);
            writer.logStepsToGoal(2);
            writer.logStepsToGoal(15);
            try {
                List<String> lines = Files.readAllLines(Paths.get(fileName));
                assertEquals(1, lines.size());
                String firstLine = lines.get(0);
                assertEquals("13,7,2,15,", firstLine);
            } catch (IOException ex) {
                fail(ex.getMessage());
            }
        } finally {
            try {
                new File(fileName).delete();
            } catch (Exception ex) {

            }
        }
    }

    @Test
    public void testBeginNewRunSingleRun() throws Exception {
        String fileName = "output.csv";
        try {
            FileResultWriter writer = new FileResultWriter(fileName);
            writer.beginNewRun();
            try {
                List<String> lines = Files.readAllLines(Paths.get(fileName));
                assertEquals(1, lines.size());
                String firstLine = lines.get(0);
                assertEquals("", firstLine);
            } catch (IOException ex) {
                fail(ex.getMessage());
            }
        } finally {
            try {
                new File(fileName).delete();
            } catch (Exception ex) {

            }
        }
    }

    @Test
    public void testBeginNewRunMultipleRuns()throws Exception {
        String fileName = "output.csv";
        try {
            FileResultWriter writer = new FileResultWriter(fileName);
            writer.beginNewRun();
            writer.beginNewRun();
            writer.beginNewRun();
            writer.beginNewRun();
            try {
                List<String> lines = Files.readAllLines(Paths.get(fileName));
                assertEquals(4, lines.size());
                for (String line : lines) {
                    assertEquals("", line);
                }
            } catch (IOException ex) {
                fail(ex.getMessage());
            }
        } finally {
            try {
                new File(fileName).delete();
            } catch (Exception ex) {

            }
        }
    }

    @Test
    public void testCompleteFinalizesStandardReportRun()throws Exception {
        String fileName = "output.csv";
        try {
            FileResultWriter writer = new FileResultWriter(fileName);
            for (int runs = 0; runs < 10; runs++) {
                writer.beginNewRun();
                for (int goals = 0; goals < 30; goals++) {
                    writer.logStepsToGoal(goals);
                }
            }
            writer.complete();
            try {
                List<String> expected = Arrays.asList(
                        "",
                        "0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26,27,28,29,",
                        "0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26,27,28,29,",
                        "0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26,27,28,29,",
                        "0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26,27,28,29,",
                        "0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26,27,28,29,",
                        "0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26,27,28,29,",
                        "0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26,27,28,29,",
                        "0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26,27,28,29,",
                        "0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26,27,28,29,",
                        "0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26,27,28,29,",
                        "=average(A2:A11),=average(B2:B11),=average(C2:C11),=average(D2:D11),=average(E2:E11),=average(F2:F11),=average(G2:G11),=average(H2:H11),=average(I2:I11),=average(J2:J11),=average(K2:K11),=average(L2:L11),=average(M2:M11),=average(N2:N11),=average(O2:O11),=average(P2:P11),=average(Q2:Q11),=average(R2:R11),=average(S2:S11),=average(T2:T11),=average(U2:U11),=average(V2:V11),=average(W2:W11),=average(X2:X11),=average(Y2:Y11),=average(Z2:Z11),=average(AA2:AA11),=average(AB2:AB11),=average(AC2:AC11),=average(AD2:AD11),",
                        ",,,=average(A12:G12),=average(B12:H12),=average(C12:I12),=average(D12:J12),=average(E12:K12),=average(F12:L12),=average(G12:M12),=average(H12:N12),=average(I12:O12),=average(J12:P12),=average(K12:Q12),=average(L12:R12),=average(M12:S12),=average(N12:T12),=average(O12:U12),=average(P12:V12),=average(Q12:W12),=average(R12:X12),=average(S12:Y12),=average(T12:Z12),=average(U12:AA12),=average(V12:AB12),=average(W12:AC12),=average(X12:AD12),,,,"
                );
                List<String> lines = Files.readAllLines(Paths.get(fileName));
                assertArrayEquals(expected.toArray(), lines.toArray());
            } catch (IOException ex) {
                fail(ex.getMessage());
            }
        } finally {
            try {
                new File(fileName).delete();
            } catch (Exception ex) {

            }
        }
    }

    // TODO edge cases around condition where smoothing lines get added
    // TODO edge cases around news with different numbers of goals found.
}
